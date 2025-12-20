package org.aulune.aggregator
package api.http.circe


import api.http.circe.PersonCodecs.given
import api.http.circe.SharedCodecs.given
import api.mappers.EpisodeTypeMapper
import application.dto.series.SeriesResource
import application.dto.shared.ReleaseDateDTO
import application.dto.work.WorkResource.CastMemberResource
import application.dto.work.{
  CastMemberDTO,
  CreateWorkRequest,
  EpisodeTypeDTO,
  ListWorksRequest,
  ListWorksResponse,
  SearchWorksRequest,
  SearchWorksResponse,
  WorkLocationResource,
  WorkResource,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Encoder]] and [[Decoder]] instances for work DTOs. */
private[api] object WorkCodecs:
  given Encoder[SeriesResource] = deriveConfiguredEncoder
  given Decoder[SeriesResource] = deriveConfiguredDecoder

  given Encoder[CreateWorkRequest] = deriveConfiguredEncoder
  given Decoder[CreateWorkRequest] = deriveConfiguredDecoder

  given Encoder[WorkResource] = deriveConfiguredEncoder
  given Decoder[WorkResource] = deriveConfiguredDecoder

  given Encoder[ListWorksRequest] = deriveConfiguredEncoder
  given Decoder[ListWorksRequest] = deriveConfiguredDecoder

  given Encoder[ListWorksResponse] = deriveConfiguredEncoder
  given Decoder[ListWorksResponse] = deriveConfiguredDecoder

  given Encoder[CastMemberDTO] = deriveConfiguredEncoder
  given Decoder[CastMemberDTO] = deriveConfiguredDecoder

  given Encoder[CastMemberResource] = deriveConfiguredEncoder
  given Decoder[CastMemberResource] = deriveConfiguredDecoder

  given Encoder[SearchWorksRequest] = deriveConfiguredEncoder
  given Decoder[SearchWorksRequest] = deriveConfiguredDecoder

  given Encoder[SearchWorksResponse] = deriveConfiguredEncoder
  given Decoder[SearchWorksResponse] = deriveConfiguredDecoder

  given Encoder[WorkLocationResource] = deriveConfiguredEncoder
  given Decoder[WorkLocationResource] = deriveConfiguredDecoder

  private given Encoder[EpisodeTypeDTO] =
    Encoder.encodeString.contramap(EpisodeTypeMapper.toString)
  private given Decoder[EpisodeTypeDTO] = Decoder.decodeString.emap { str =>
    EpisodeTypeMapper
      .fromString(str)
      .toRight(s"Invalid EpisodeType: $str")
  }
