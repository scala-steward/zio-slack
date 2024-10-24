package com.github.dapperware.slack.models

import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.time.LocalDate

sealed trait Block {
  val `type`: String
  val block_id: Option[String]
}

case class Divider(block_id: Option[String] = None) extends Block {
  override val `type`: String = "divider"
}

case class Section(text: TextObject,
                   fields: Option[Seq[TextObject]] = None,
                   accessory: Option[BlockElement] = None,
                   block_id: Option[String] = None)
    extends Block {
  override val `type`: String = "section"
}

case class ImageBlock(image_url: String,
                      alt_text: String,
                      title: Option[PlainTextObject],
                      block_id: Option[String] = None)
    extends Block {
  override val `type`: String = "image"
  require(title.forall(_.`type` == "plain_text"))
}

case class ActionsBlock(elements: Seq[BlockElement], block_id: Option[String] = None) extends Block {
  override val `type`: String = "actions"
  require(elements.size <= 5, "Maximum of 5 elements in each action block")
}

case class HeaderBlock(text: PlainTextObject, block_id: Option[String] = None) extends Block {
  override val `type`: String = "header"
}

case class ContextBlock(elements: Seq[Either[ImageElement, TextObject]], block_id: Option[String] = None)
    extends Block {
  override val `type`: String = "context"
}

case class InputBlock(label: PlainTextObject,
                      element: InputBlockElement,
                      dispatch_action: Option[Boolean] = None,
                      block_id: Option[String] = None,
                      hint: Option[PlainTextObject] = None,
                      optional: Option[Boolean] = None)
    extends Block {
  override val `type` = "input"
}

sealed trait InputBlockElement {
  self: BlockElement =>

  val `type`: String
}

case class PlainTextInput(
  action_id: String,
  placeholder: Option[PlainTextObject] = None,
  initial_value: Option[String] = None,
  multiline: Option[Boolean] = None,
  min_length: Option[Int] = None,
  max_length: Option[Int] = None,
  dispatch_action_config: Option[DispatchActionConfig] = None
) extends BlockElement
    with InputBlockElement {
  override val `type` = "plain_text_input"
}

case class DispatchActionConfig(trigger_actions_on: List[TriggerAction])

object DispatchActionConfig {
  implicit val codec: Codec[DispatchActionConfig] = deriveCodec[DispatchActionConfig]
}

sealed trait TriggerAction

object TriggerAction {
  case object OnEnterPressed     extends TriggerAction
  case object OnCharacterEntered extends TriggerAction

  implicit val encoder: Encoder[TriggerAction] = Encoder.instance {
    case OnEnterPressed     => "on_enter_pressed".asJson
    case OnCharacterEntered => "on_character_entered".asJson
  }

  implicit val decoder: Decoder[TriggerAction] = Decoder.decodeString.emap[TriggerAction] {
    case "on_enter_pressed"     => Right(OnEnterPressed)
    case "on_character_entered" => Right(OnCharacterEntered)
    case s                      => Left(s"Could not decode TriggerAction from $s")
  }
}

trait TextObject {
  val `type`: String
  val text: String
}

case class PlainTextObject(text: String, emoji: Option[Boolean] = None, `type`: String = "plain_text")
    extends TextObject

case class MarkdownTextObject(text: String, verbatim: Option[Boolean] = None, `type`: String = "mrkdwn")
    extends TextObject

object TextObject {
  implicit private[slack] val plainTextFmt: Codec.AsObject[PlainTextObject]     = deriveCodec[PlainTextObject]
  implicit private[slack] val mrkdwnTextFmt: Codec.AsObject[MarkdownTextObject] = deriveCodec[MarkdownTextObject]

  private val textEncoder = Encoder.AsObject.instance[TextObject] { text =>
    val json = text match {
      case t: PlainTextObject    => t.asJsonObject
      case t: MarkdownTextObject => t.asJsonObject
    }
    json.add("type", text.`type`.asJson)
  }

  private val textDecoder: Decoder[TextObject] = Decoder.instance[TextObject] { c =>
    for {
      value <- c.downField("type").as[String]
      result <- value match {
                 case "plain_text" => c.as[PlainTextObject]
                 case "mrkdwn"     => c.as[MarkdownTextObject]
                 case other        => Left(DecodingFailure(s"Invalid text object type: $other", List.empty))
               }
    } yield result
  }

  implicit val format: Codec[TextObject] = Codec.from(textDecoder, textEncoder)
}

case class OptionObject(text: PlainTextObject, value: String)

case class OptionGroupObject(label: PlainTextObject, options: Seq[OptionObject])

case class ConfirmationObject(title: PlainTextObject, text: TextObject, confirm: PlainTextObject, deny: PlainTextObject)

trait BlockElement {
  val `type`: String
}

case class ImageElement(image_url: String, alt_text: String, `type`: String = "image") extends BlockElement {}

case class ButtonElement(text: PlainTextObject,
                         action_id: String,
                         url: Option[String] = None,
                         value: Option[String] = None,
                         confirm: Option[ConfirmationObject] = None)
    extends BlockElement {
  override val `type`: String = "button"
}

case class StaticSelectElement(placeholder: PlainTextObject,
                               action_id: String,
                               options: Seq[OptionObject],
                               option_groups: Option[Seq[OptionGroupObject]] = None,
                               initial_option: Option[Either[OptionObject, OptionGroupObject]] = None,
                               confirm: Option[ConfirmationObject] = None)
    extends BlockElement
    with InputBlockElement {
  override val `type`: String = "static_select"
}

case class ExternalSelectElement(placeholder: PlainTextObject,
                                 action_id: String,
                                 min_query_length: Option[Int] = None,
                                 initial_option: Option[Either[OptionObject, OptionGroupObject]] = None,
                                 confirm: Option[ConfirmationObject] = None)
    extends BlockElement {
  override val `type`: String = "external_select"
}

case class UserSelectElement(placeholder: PlainTextObject,
                             action_id: String,
                             initial_user: Option[String] = None,
                             confirm: Option[ConfirmationObject] = None)
    extends BlockElement
    with InputBlockElement {
  override val `type`: String = "users_select"
}

case class MultiUsersSelectElement(
  placeholder: PlainTextObject,
  action_id: String
) extends BlockElement
    with InputBlockElement {
  override val `type`: String = "multi_users_select"
}

case class ChannelSelectElement(placeholder: PlainTextObject,
                                action_id: String,
                                initial_channel: Option[String] = None,
                                confirm: Option[ConfirmationObject] = None)
    extends BlockElement {
  override val `type`: String = "channels_select"
}

case class ConversationSelectElement(placeholder: PlainTextObject,
                                     action_id: String,
                                     initial_conversation: Option[String] = None,
                                     confirm: Option[ConfirmationObject] = None,
                                     response_url_enabled: Option[Boolean] = None)
    extends BlockElement
    with InputBlockElement {
  override val `type`: String = "conversations_select"
}

case class MultiConversationsSelectElement(placeholder: PlainTextObject,
                                           action_id: String,
                                           initial_conversations: Option[List[String]] = None,
                                           default_to_current_conversation: Option[Boolean] = None,
                                           confirm: Option[ConfirmationObject] = None,
                                           max_selected_items: Option[Int] = None,
                                           response_url_enabled: Option[Boolean] = None)
    extends BlockElement
    with InputBlockElement {
  override val `type`: String = "multi_conversations_select"
}

case class OverflowElement(action_id: String, options: Seq[OptionObject], confirm: Option[ConfirmationObject] = None)
    extends BlockElement {
  override val `type`: String = "overflow"
}

case class DatePickerElement(action_id: String,
                             placeholder: PlainTextObject,
                             initial_date: Option[LocalDate] = None,
                             confirm: Option[ConfirmationObject] = None)
    extends BlockElement
    with InputBlockElement {
  override val `type`: String = "datepicker"
}

object BlockElement {
  implicit val plainTextFmt: Codec.AsObject[PlainTextObject] = deriveCodec[PlainTextObject]

  implicit val optionObjFmt: Codec.AsObject[OptionObject]    = deriveCodec[OptionObject]
  implicit val optionGrpObjFmt: Codec.AsObject[OptionGroupObject] = deriveCodec[OptionGroupObject]
  implicit val confirmObjFmt: Codec.AsObject[ConfirmationObject]   = deriveCodec[ConfirmationObject]

  implicit val eitherOptFmt: Codec[Either[OptionObject,OptionGroupObject]]                    = eitherObjectFormat[OptionObject, OptionGroupObject]("text", "label")
  implicit val buttonElementFmt: Codec.AsObject[ButtonElement]                = deriveCodec[ButtonElement]
  implicit val imageElementFmt: Codec.AsObject[ImageElement]                 = deriveCodec[ImageElement]
  implicit val staticMenuElementFmt: Codec.AsObject[StaticSelectElement]            = deriveCodec[StaticSelectElement]
  implicit val extMenuElementFmt: Codec.AsObject[ExternalSelectElement]               = deriveCodec[ExternalSelectElement]
  implicit val userMenuElementFmt: Codec.AsObject[UserSelectElement]              = deriveCodec[UserSelectElement]
  implicit val multiUsersSelectElementFmt: Codec.AsObject[MultiUsersSelectElement]      = deriveCodec[MultiUsersSelectElement]
  implicit val channelMenuElementFmt: Codec.AsObject[ChannelSelectElement]           = deriveCodec[ChannelSelectElement]
  implicit val conversationMenuElementFmt: Codec.AsObject[ConversationSelectElement]      = deriveCodec[ConversationSelectElement]
  implicit val multiConversationMenuElementFmt: Codec.AsObject[MultiConversationsSelectElement] = deriveCodec[MultiConversationsSelectElement]
  implicit val overflowElementFmt: Codec.AsObject[OverflowElement]              = deriveCodec[OverflowElement]
  implicit val datePickerElementFmt: Codec.AsObject[DatePickerElement]            = deriveCodec[DatePickerElement]

  private val elemWrites: Encoder[BlockElement] = new Encoder[BlockElement] {
    def apply(element: BlockElement): Json = {
      val json = element match {
        case elem: ButtonElement                   => elem.asJson
        case elem: ImageElement                    => elem.asJson
        case elem: StaticSelectElement             => elem.asJson
        case elem: ExternalSelectElement           => elem.asJson
        case elem: UserSelectElement               => elem.asJson
        case elem: MultiUsersSelectElement         => elem.asJson
        case elem: ChannelSelectElement            => elem.asJson
        case elem: ConversationSelectElement       => elem.asJson
        case elem: MultiConversationsSelectElement => elem.asJson
        case elem: OverflowElement                 => elem.asJson
        case elem: DatePickerElement               => elem.asJson
      }
      Json.obj("type" -> element.`type`.asJson).deepMerge(json)
    }
  }
  private val elemReads: Decoder[BlockElement] = new Decoder[BlockElement] {

    override def apply(c: HCursor): Result[BlockElement] =
      for {
        value <- c.downField("type").as[String]
        result <- value match {
                   case "button"                     => c.as[ButtonElement]
                   case "image"                      => c.as[ImageElement]
                   case "static_select"              => c.as[StaticSelectElement]
                   case "external_select"            => c.as[ExternalSelectElement]
                   case "users_select"               => c.as[UserSelectElement]
                   case "multi_users_select"         => c.as[MultiUsersSelectElement]
                   case "conversations_select"       => c.as[ConversationSelectElement]
                   case "multi_conversations_select" => c.as[MultiConversationsSelectElement]
                   case "channels_select"            => c.as[ChannelSelectElement]
                   case "overflow"                   => c.as[OverflowElement]
                   case "datepicker"                 => c.as[DatePickerElement]
                   case other                        => Left(DecodingFailure(s"Invalid element type: $other", List.empty))
                 }
      } yield result
  }

  implicit val format: Codec[BlockElement] = Codec.from(elemReads, elemWrites)
}

object InputBlockElement {
  implicit val plainTextInputCodec: Codec.AsObject[PlainTextInput] = deriveCodec[PlainTextInput]

  implicit val encoder: Encoder[InputBlockElement] = Encoder.AsObject.instance[InputBlockElement] { ibe =>
    val json = ibe match {
      case i: PlainTextInput                  => i.asJsonObject
      case i: StaticSelectElement             => i.asJsonObject
      case i: DatePickerElement               => i.asJsonObject
      case i: ConversationSelectElement       => i.asJsonObject
      case i: MultiConversationsSelectElement => i.asJsonObject
      case i: UserSelectElement               => i.asJsonObject
      case i: MultiUsersSelectElement         => i.asJsonObject
    }

    json.add("type", ibe.`type`.asJson)
  }

  val typeDecoder: Decoder[String] = Decoder.decodeString.at("type")

  implicit val decoder: Decoder[InputBlockElement] = Decoder.instance[InputBlockElement] { c =>
    typeDecoder(c).flatMap {
      case "plain_text_input"           => c.as[PlainTextInput]
      case "static_select"              => c.as[StaticSelectElement]
      case "datepicker"                 => c.as[DatePickerElement]
      case "conversations_select"       => c.as[ConversationSelectElement]
      case "multi_conversations_select" => c.as[MultiConversationsSelectElement]
      case "users_select"               => c.as[UserSelectElement]
      case "multi_users_select"         => c.as[MultiUsersSelectElement]
      case t                            => Left(DecodingFailure(s"Unknown input block element type $t", c.history))
    }
  }

}

object Block {
  implicit val plainTextFmt: Codec.AsObject[PlainTextObject]    = deriveCodec[PlainTextObject]
  implicit val imageElementFmt: Codec.AsObject[ImageElement] = deriveCodec[ImageElement]

  implicit val eitherContextFmt: Codec[Either[ImageElement,TextObject]] = eitherObjectFormat[ImageElement, TextObject]("image_url", "text")
  implicit val dividerFmt: Codec.AsObject[Divider]     = deriveCodec[Divider]
  implicit val imageBlockFmt: Codec.AsObject[ImageBlock]    = deriveCodec[ImageBlock]
  implicit val actionBlockFmt: Codec.AsObject[ActionsBlock]   = deriveCodec[ActionsBlock]
  implicit val contextBlockFmt: Codec.AsObject[ContextBlock]  = deriveCodec[ContextBlock]
  implicit val sectionFmt: Codec.AsObject[Section]       = deriveCodec[Section]
  implicit val headerBlockCodec: Codec.AsObject[HeaderBlock] = deriveCodec[HeaderBlock]
  implicit val inputBlockCodec: Codec.AsObject[InputBlock]  = deriveCodec[InputBlock]

  private val blockEncoder = Encoder.AsObject.instance[Block] { block =>
    val json = block match {
      case b: Divider      => b.asJsonObject
      case b: Section      => b.asJsonObject
      case b: ImageBlock   => b.asJsonObject
      case b: ActionsBlock => b.asJsonObject
      case b: ContextBlock => b.asJsonObject
      case b: HeaderBlock  => b.asJsonObject
      case b: InputBlock   => b.asJsonObject
    }
    json.add("type", block.`type`.asJson)
  }

  private val blockDecoder = Decoder.instance[Block] { c =>
    for {
      value <- c.downField("type").as[String]
      result <- value match {
                 case "divider" => c.as[Divider]
                 case "image"   => c.as[ImageBlock]
                 case "actions" => c.as[ActionsBlock]
                 case "context" => c.as[ContextBlock]
                 case "header"  => c.as[HeaderBlock]
                 case "input"   => c.as[InputBlock]
                 case "section" => c.as[Section]
                 case other     => Left(DecodingFailure(s"Invalid block type: $other", List.empty))
               }
    } yield result
  }

  implicit val format: Codec[Block] = Codec.from(blockDecoder, blockEncoder)
}
