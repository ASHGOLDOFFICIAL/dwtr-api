package org.aulune.aggregator
package api.http.tapir.work


import api.http.tapir.SharedSchemas.given
import api.http.tapir.person.PersonSchemas.given
import api.http.tapir.series.SeriesSchemas.given
import api.mappers.EpisodeTypeMapper
import application.dto.series.SeriesResource
import application.dto.shared.ExternalResourceDTO
import application.dto.work.WorkResource.CastMemberResource
import application.dto.work.{
  CastMemberDTO,
  CreateWorkRequest,
  EpisodeTypeDTO,
  ListWorksResponse,
  SearchWorksResponse,
  WorkLocationResource,
  WorkResource,
}

import sttp.tapir.{Schema, Validator}

import java.net.URI


/** Tapir [[Schema]]s for work objects. */
object WorkSchemas:
  given Schema[CreateWorkRequest] = Schema.derived
  given Schema[WorkResource] = Schema.derived

  given Schema[ListWorksResponse] = Schema.derived
  given Schema[SearchWorksResponse] = Schema.derived
  given Schema[WorkLocationResource] = Schema.derived

  private given Schema[EpisodeTypeDTO] = Schema.string
    .validate(
      Validator
        .enumeration(EpisodeTypeDTO.values.toList)
        .encode(EpisodeTypeMapper.toString))
  private given Schema[CastMemberDTO] = Schema.derived
  private given Schema[CastMemberResource] = Schema.derived
