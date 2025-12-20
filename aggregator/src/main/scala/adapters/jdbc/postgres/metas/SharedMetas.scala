package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import domain.model.shared.ReleaseDate.DateAccuracy
import domain.model.shared.{
  ExternalResource,
  ExternalResourceType,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}

import cats.Show
import doobie.Meta
import doobie.postgres.implicits.JavaLocalDateMeta
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config
import org.aulune.commons.adapters.doobie.postgres.Metas.{jsonbMeta, uriMeta}

import java.net.URI
import java.time.LocalDate


/** [[Meta]] instances for shared domain objects. */
private[postgres] object SharedMetas:
  private given Show[LocalDate] = Show.show(_.toString)
  private given Show[URI] = Show.show(_.toString)

  given selfHostUriMeta: Meta[SelfHostedLocation] = Meta[URI]
    .imap(SelfHostedLocation.unsafe)(identity)
  given imageUriMeta: Meta[ImageUri] = Meta[URI]
    .imap(ImageUri.unsafe)(identity)

  given dateAccuracyMeta: Meta[DateAccuracy] =
    val toInt = DateAccuracy.values.map {
      case t @ DateAccuracy.Unknown => t -> 0
      case t @ DateAccuracy.Day     => t -> 1
      case t @ DateAccuracy.Month   => t -> 2
      case t @ DateAccuracy.Year    => t -> 3
    }.toMap
    val fromInt = toInt.map(_.swap)
    Meta[Int].imap(fromInt)(toInt)

  given synopsisMeta: Meta[Synopsis] = Meta[String]
    .imap(Synopsis.unsafe)(identity)

  given externalResourceTypeMeta: Meta[ExternalResourceType] = Meta[Int]
    .imap(resourceTypeFromInt.apply)(resourceTypeToInt.apply)
  given externalResourceMeta: Meta[ExternalResource] = jsonbMeta.imap(json =>
    json.as[ExternalResource].fold(throw _, identity))(_.asJson)
  given externalResourcesMeta: Meta[List[ExternalResource]] = jsonbMeta.imap(
    json => json.as[List[ExternalResource]].fold(throw _, identity))(_.asJson)

  private val resourceTypeToInt = ExternalResourceType.values.map {
    case t @ ExternalResourceType.Purchase  => t -> 1
    case t @ ExternalResourceType.Streaming => t -> 2
    case t @ ExternalResourceType.Download  => t -> 3
    case t @ ExternalResourceType.Other     => t -> 4
    case t @ ExternalResourceType.Private   => t -> 5
  }.toMap
  private val resourceTypeFromInt = resourceTypeToInt.map(_.swap)

  private given Decoder[ExternalResourceType] =
    Decoder.decodeInt.map(resourceTypeFromInt)
  private given Encoder[ExternalResourceType] =
    Encoder.encodeInt.contramap(resourceTypeToInt)
  private given Decoder[ExternalResource] = deriveConfiguredDecoder
  private given Encoder[ExternalResource] = deriveConfiguredEncoder
