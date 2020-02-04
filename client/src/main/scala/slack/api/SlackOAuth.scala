package slack.api

import io.circe
import io.circe.{ Decoder, Json }
import io.circe.generic.semiauto._
import slack.SlackError
import slack.core.{ ClientSecret, SlackClient }
import sttp.client.Request
import zio.{ URIO, ZIO }

trait SlackOAuth {
  val slackOAuth: SlackOAuth.Service[Any]
}

object SlackOAuth {

  trait OAuthV2Service[R] {
    def accessOAuthV2(
      code: String,
      redirectUri: Option[String] = None
    ): ZIO[R with SlackClient with ClientSecret, SlackError, FullAccessTokenV2] =
      request("oauth.v2.access", "code" -> code, "redirect_uri" -> redirectUri) >>= ClientSecret.authenticateM >>= SlackClient
        .send[Json, circe.Error] >>= as[FullAccessTokenV2]
  }

  trait Service[R] extends OAuthV2Service[R] {
    def accessOAuth(
      code: String,
      redirectUri: Option[String] = None,
      singleChannel: Option[Boolean] = None
    ): ZIO[R with SlackClient with ClientSecret, SlackError, FullAccessToken] =
      sendRaw(
        request(
          "oauth.access",
          "code"           -> code,
          "redirect_uri"   -> redirectUri,
          "single_channel" -> singleChannel
        )
      ) >>= as[FullAccessToken]

    protected def sendRaw[T](
      request: URIO[R, Request[SlackResponse[T], Nothing]]
    ): ZIO[R with SlackClient with ClientSecret, Throwable, T] =
      request >>= ClientSecret.authenticateM >>= SlackClient.send[T, circe.Error]
  }
}

object oauth extends SlackOAuth.Service[SlackClient with ClientSecret]

case class BotAccessToken(
  bot_user_id: String,
  bot_access_token: String
)

object BotAccessToken {
  implicit val decoder: Decoder[BotAccessToken] = deriveDecoder[BotAccessToken]
}

case class FullAccessToken(access_token: String,
                           scope: String,
                           team_name: String,
                           team_id: String,
                           enterprise_id: Option[String],
                           bot: Option[BotAccessToken])

object FullAccessToken {
  implicit val decoder: Decoder[FullAccessToken] = deriveDecoder[FullAccessToken]
}

case class FullAccessTokenV2(
  access_token: Option[String],
  token_type: Option[String],
  scope: Option[String],
  bot_user_id: Option[String],
  app_id: String,
  team: MinimalTeam,
  enterprise: Option[MinimalTeam],
  authed_user: Option[AuthedUser]
)

case class MinimalTeam(
  id: String,
  name: String
)

object MinimalTeam {
  implicit val decoder: Decoder[MinimalTeam] = deriveDecoder[MinimalTeam]
}

case class AuthedUser(
  id: String,
  scope: Option[String],
  access_token: Option[String],
  token_type: Option[String]
)

object AuthedUser {
  implicit val decoder: Decoder[AuthedUser] = deriveDecoder[AuthedUser]
}

object FullAccessTokenV2 {
  implicit val decoder: Decoder[FullAccessTokenV2] = deriveDecoder[FullAccessTokenV2]
}