package com.github.dapperware.slack.api

import com.github.dapperware.slack.SlackError
import com.github.dapperware.slack.models.Channel
import com.github.dapperware.slack.models.channelFmt
import io.circe.Json
import zio.IO

sealed trait ChannelLike[T] {
  type ChannelType
  def isFull: Boolean
  def extract(t: Json, key: String): IO[SlackError, ChannelType]
}

case object ChannelLikeChannel extends ChannelLike[Channel] {
  override type ChannelType = Channel
  override def isFull: Boolean = true

  override def extract(t: Json, key: String): IO[SlackError, Channel] =
    as[Channel](key)(t)

}
case object ChannelLikeId extends ChannelLike[String] {
  override type ChannelType = String
  override def isFull: Boolean = false

  override def extract(t: Json, key: String): IO[SlackError, String] =
    as[String](key)(t)
}
