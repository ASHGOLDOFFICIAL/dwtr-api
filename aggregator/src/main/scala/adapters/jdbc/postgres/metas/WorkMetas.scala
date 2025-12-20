package org.aulune.aggregator
package adapters.jdbc.postgres.metas


import domain.model.person.Person
import domain.model.series.SeriesName
import domain.model.work.{
  ActorRole,
  CastMember,
  EpisodeType,
  SeasonNumber,
  SeriesNumber,
  Title,
  Work,
}

import doobie.Meta
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.syntax.given
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.doobie.postgres.Metas.jsonbMeta
import org.aulune.commons.types.Uuid


/** [[Meta]] instances for [[Work]]. */
private[postgres] object WorkMetas:

  given episodeTypeMeta: Meta[EpisodeType] =
    val toInt = EpisodeType.values.map {
      case t @ EpisodeType.Regular => t -> 1
      case t @ EpisodeType.Special => t -> 2
    }.toMap
    val fromInt = toInt.map(_.swap)
    Meta[Int].imap(fromInt)(toInt)

  given actorRoleMeta: Meta[ActorRole] =
    Meta[String].imap(ActorRole.unsafe)(identity)

  given titleMeta: Meta[Title] = Meta[String].imap(Title.unsafe)(identity)

  given seasonMeta: Meta[SeasonNumber] =
    Meta[Int].imap(SeasonNumber.unsafe)(identity)

  given seriesNameMeta: Meta[SeriesName] =
    Meta[String].imap(SeriesName.unsafe)(identity)

  given seriesNumberMeta: Meta[SeriesNumber] =
    Meta[Int].imap(SeriesNumber.unsafe)(identity)

  given castMemberMeta: Meta[CastMember] = jsonbMeta.imap(json =>
    json.as[CastMember].fold(throw _, identity))(_.asJson)
  given castMembersMeta: Meta[List[CastMember]] = jsonbMeta.imap(json =>
    json.as[List[CastMember]].fold(throw _, identity))(_.asJson)

  given writersMeta: Meta[List[Uuid[Person]]] = jsonbMeta.imap(json =>
    json.as[List[Uuid[Person]]].fold(throw _, identity))(_.asJson)

  private given Configuration = Configuration.default.withSnakeCaseMemberNames
  private given [A]: Decoder[Uuid[A]] = Decoder.decodeUUID.map(Uuid[A])
  private given [A]: Encoder[Uuid[A]] = Encoder.encodeUUID.contramap(identity)
  private given Decoder[ActorRole] = Decoder.decodeString.map(ActorRole.unsafe)
  private given Encoder[ActorRole] = Encoder.encodeString.contramap(identity)
  private given Decoder[CastMember] = deriveConfiguredDecoder
  private given Encoder[CastMember] = deriveConfiguredEncoder
