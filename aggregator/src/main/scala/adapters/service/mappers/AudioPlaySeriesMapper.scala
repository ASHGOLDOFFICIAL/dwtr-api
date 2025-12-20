package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.series.{
  AudioPlaySeriesResource,
  CreateAudioPlaySeriesRequest,
  ListAudioPlaySeriesResponse,
  SearchAudioPlaySeriesResponse,
}
import domain.errors.AudioPlaySeriesValidationError
import domain.model.audioplay.series.{AudioPlaySeries, AudioPlaySeriesName}
import domain.repositories.AudioPlaySeriesRepository

import cats.data.ValidatedNec
import cats.syntax.all.given
import org.aulune.commons.pagination.cursor.CursorEncoder
import org.aulune.commons.types.Uuid


/** Mapper between external [[AudioPlaySeriesResource]] and domain's
 *  [[AudioPlaySeries]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlaySeriesMapper:
  /** Converts request to domain object and verifies it.
   *  @param request series create request.
   *  @param id ID assigned to this series.
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: CreateAudioPlaySeriesRequest,
      id: Uuid[AudioPlaySeries],
  ): ValidatedNec[AudioPlaySeriesValidationError, AudioPlaySeries] =
    (for name <- AudioPlaySeriesName(request.name)
    yield AudioPlaySeries(
      id = id,
      name = name,
    )).getOrElse(AudioPlaySeriesValidationError.InvalidArguments.invalidNec)

  /** Convert [[AudioPlaySeries]] to [[AudioPlaySeriesResource]].
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def toResponse(domain: AudioPlaySeries): AudioPlaySeriesResource =
    AudioPlaySeriesResource(id = domain.id, name = domain.name)

  /** Converts list of domain objects to one search response.
   *  @param elems list of domain objects.
   */
  def toSearchResponse(
      elems: List[AudioPlaySeries],
  ): SearchAudioPlaySeriesResponse =
    SearchAudioPlaySeriesResponse(elems.map(toResponse))
