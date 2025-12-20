package org.aulune.aggregator
package adapters.service.mappers


import application.dto.series.{
  CreateSeriesRequest,
  ListSeriesResponse,
  SearchSeriesResponse,
  SeriesResource,
}
import domain.errors.SeriesValidationError
import domain.model.series.{Series, SeriesName}
import domain.repositories.SeriesRepository

import cats.data.ValidatedNec
import cats.syntax.all.given
import org.aulune.commons.pagination.cursor.CursorEncoder
import org.aulune.commons.types.Uuid


/** Mapper between external [[SeriesResource]] and domain's [[Series]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object SeriesMapper:
  /** Converts request to domain object and verifies it.
   *  @param request series create request.
   *  @param id ID assigned to this series.
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: CreateSeriesRequest,
      id: Uuid[Series],
  ): ValidatedNec[SeriesValidationError, Series] = (for name <-
      SeriesName(request.name)
  yield Series(
    id = id,
    name = name,
  )).getOrElse(SeriesValidationError.InvalidArguments.invalidNec)

  /** Convert [[Series]] to [[SeriesResource]].
   *
   *  @param domain inner domain object.
   *  @return mapped external object.
   */
  def toResponse(domain: Series): SeriesResource =
    SeriesResource(id = domain.id, name = domain.name)

  /** Converts list of domain objects to one search response.
   *  @param elems list of domain objects.
   */
  def toSearchResponse(
      elems: List[Series],
  ): SearchSeriesResponse = SearchSeriesResponse(elems.map(toResponse))
