package slack.core

import sttp.client.RequestT
import zio.{ Managed, UIO, URIO, ZIO }

trait ClientSecret {
  val clientSecret: ClientSecret.Service[Any]
}

object ClientSecret {
  trait Service[R] {
    def authenticateM[U[_], T, S](request: RequestT[U, T, S]): URIO[R, RequestT[U, T, S]]
  }

  def make(id: String, secret: String): UIO[ClientSecret] =
    UIO.succeed(new ClientSecret {
      override val clientSecret: Service[Any] = new Service[Any] {
        override def authenticateM[U[_], T, S](request: RequestT[U, T, S]): URIO[Any, RequestT[U, T, S]] =
          UIO.succeed(request.auth.basic(id, secret))
      }
    })

  def makeManaged(id: String, secret: String): Managed[Nothing, ClientSecret] =
    make(id, secret).toManaged_

  def authenticateM[R, U[_], T, S](request: RequestT[U, T, S]): URIO[ClientSecret, RequestT[U, T, S]] =
    ZIO.accessM[ClientSecret](_.clientSecret.authenticateM(request))
}