package org.aulune.aggregator
package adapters.service.mappers


import application.dto.person.PersonResource
import application.dto.series.SeriesResource
import application.dto.work.{CreateWorkRequest, WorkResource}
import domain.errors.WorkValidationError
import domain.errors.WorkValidationError.{
  InvalidCast,
  InvalidReleaseDate,
  InvalidSeason,
  InvalidSelfHostedLocation,
  InvalidSeriesNumber,
  InvalidSynopsis,
  InvalidTitle,
}
import domain.model.person.Person
import domain.model.series.Series
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}
import domain.model.work.{
  CastMember,
  EpisodeType,
  SeasonNumber,
  SeriesNumber,
  Title,
  Work,
}

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.types.Uuid

import java.util.UUID


/** Mapper between external work DTOs and domain's [[Work]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object WorkMapper:
  /** Converts request to domain object and verifies it.
   *  @param request work request DTO.
   *  @param id ID assigned to this work.
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: CreateWorkRequest,
      id: UUID,
  ): ValidatedNec[WorkValidationError, Work] = (
    Uuid[Work](id).validNec,
    Title(request.title)
      .toValidNec(InvalidTitle),
    Synopsis(request.synopsis)
      .toValidNec(InvalidSynopsis),
    ReleaseDateMapper
      .toDomain(request.releaseDate)
      .toValidNec(InvalidReleaseDate),
    request.writers.map(Uuid[Person]).validNec,
    request.cast
      .traverse(CastMemberMapper.fromDTO)
      .toValidNec(InvalidCast),
    request.seriesId.map(Uuid[Series]).validNec,
    request.seriesSeason
      .traverse(SeasonNumber(_).toValidNec(InvalidSeason)),
    request.seriesNumber
      .traverse(SeriesNumber(_).toValidNec(InvalidSeriesNumber)),
    request.episodeType.map(EpisodeTypeMapper.toDomain).validNec,
    Option.empty[ImageUri].validNec,
    request.selfHostedLocation
      .traverse(SelfHostedLocation(_).toValidNec(InvalidSelfHostedLocation)),
    request.externalResources.map(ExternalResourceMapper.toDomain).validNec,
  ).mapN(Work.apply).andThen(identity)

  /** Converts domain object to response object.
   *
   *  @param domain entity to use as a base.
   *  @param series prefetched work series resource.
   *  @param personMap map between ID and persons to populate writers and cast.
   *  @note Can throw if function doesn't yield result for some required IDs.
   */
  def makeResource(
      domain: Work,
      series: Option[SeriesResource],
      personMap: UUID => PersonResource,
  ): WorkResource = WorkResource(
    id = domain.id,
    title = domain.title,
    synopsis = domain.synopsis,
    releaseDate = ReleaseDateMapper.fromDomain(domain.releaseDate),
    writers = domain.writers.map(personMap),
    cast = domain.cast
      .map(p => CastMemberMapper.toResource(p, personMap(p.actor))),
    series = series,
    seriesSeason = domain.seriesSeason,
    seriesNumber = domain.seriesNumber,
    episodeType = domain.episodeType.map(EpisodeTypeMapper.fromDomain),
    coverUri = domain.coverUri,
    externalResources = domain.externalResources
      .map(ExternalResourceMapper.fromDomain),
  )
