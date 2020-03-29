package slack.models

// TODO: 22 field limit :(
case class SlackFile(id: String,
                     created: Long,
                     timestamp: Long,
                     name: Option[String],
                     title: String,
                     mimetype: String,
                     filetype: String,
                     pretty_type: String,
                     user: String,
                     mode: String,
                     editable: Boolean,
                     is_external: Boolean,
                     external_type: String,
                     size: Long,
                     is_public: Boolean,
                     public_url_shared: Boolean,
                     permalink: String,
                     channels: Seq[String] = Nil,
                     url: Option[String] = None,
                     url_download: Option[String] = None,
                     url_private: Option[String] = None,
                     url_private_download: Option[String] = None,
                     initial_comment: Option[SlackComment] = None,
                     thumb_64: Option[String] = None,
                     thumb_80: Option[String] = None,
                     thumb_360: Option[String] = None,
                     thumb_360_gif: Option[String] = None,
                     thumb_360_w: Option[String] = None,
                     thumb_360_h: Option[String] = None,
                     edit_link: Option[String] = None,
                     preview: Option[String] = None,
                     preview_highlight: Option[String] = None,
                     lines: Option[Int] = None,
                     lines_more: Option[Int] = None,
                     groups: Option[Seq[String]] = None,
                     num_stars: Option[Int] = None,
                     is_starred: Option[Boolean] = None)
