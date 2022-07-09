package com.github.dapperware.slack

import com.github.dapperware.slack.Slack.request
import com.github.dapperware.slack.generated.GeneratedReminders
import com.github.dapperware.slack.generated.requests.{
  CompleteRemindersRequest,
  DeleteRemindersRequest,
  InfoRemindersRequest
}
import com.github.dapperware.slack.generated.responses.{ InfoRemindersResponse, ListRemindersResponse }
import com.github.dapperware.slack.models.Reminder
import io.circe.Json
import zio.{ URIO, ZIO }
import com.github.dapperware.slack.models.reminderCodec

trait Reminders { self: Slack =>

  def addReminder(text: String, time: String, user: String): URIO[AccessToken, SlackResponse[Reminder]] =
    apiCall(
      request("reminders.add")
        .jsonBody(
          Json.obj(
            "text" -> Json.fromString(text),
            "time" -> Json.fromString(time),
            "user" -> Json.fromString(user)
          )
        )
        .at[Reminder]("reminder")
    )

  // FIXME the arguments shouldn't be optional
  def completeReminder(reminder: String): URIO[AccessToken, SlackResponse[Unit]] =
    apiCall(Reminders.completeReminders(CompleteRemindersRequest(Some(reminder))))

  def deleteReminder(reminder: String): URIO[AccessToken, SlackResponse[Unit]] =
    apiCall(Reminders.deleteReminders(DeleteRemindersRequest(Some(reminder))))

  def getReminderInfo(reminder: String): URIO[AccessToken, SlackResponse[InfoRemindersResponse]] =
    apiCall(Reminders.infoReminders(InfoRemindersRequest(Some(reminder))))

  def listReminders: URIO[AccessToken, SlackResponse[ListRemindersResponse]] =
    apiCall(Reminders.listReminders)

}

private[slack] trait RemindersAccessors { self: Slack.type =>

  def addReminder(
    text: String,
    time: String,
    user: String
  ): URIO[Slack with AccessToken, SlackResponse[Reminder]] =
    ZIO.serviceWithZIO[Slack](_.addReminder(text, time, user))

  // FIXME the arguments shouldn't be optional
  def completeReminder(reminder: String): URIO[Slack with AccessToken, SlackResponse[Unit]] =
    ZIO.serviceWithZIO[Slack](_.completeReminder(reminder))

  def deleteReminder(reminder: String): URIO[Slack with AccessToken, SlackResponse[Unit]] =
    ZIO.serviceWithZIO[Slack](_.deleteReminder(reminder))

  def getReminderInfo(reminder: String): URIO[Slack with AccessToken, SlackResponse[InfoRemindersResponse]] =
    ZIO.serviceWithZIO[Slack](_.getReminderInfo(reminder))

  def listReminders: URIO[Slack with AccessToken, SlackResponse[ListRemindersResponse]] =
    ZIO.serviceWithZIO[Slack](_.listReminders)

}

object Reminders extends GeneratedReminders
