package org.aulune.aggregator
package api.http.tapir.series


import application.dto.series.{
  BatchGetSeriesRequest,
  BatchGetSeriesResponse,
  CreateSeriesRequest,
  ListSeriesRequest,
  ListSeriesResponse,
  SearchSeriesRequest,
  SearchSeriesResponse,
  SeriesResource,
}

import sttp.tapir.{Schema, Validator}


/** Tapir [[Schema]]s for series objects. */
object SeriesSchemas:
  given Schema[SeriesResource] = Schema.derived

  given Schema[CreateSeriesRequest] = Schema.derived

  given Schema[BatchGetSeriesResponse] = Schema.derived
  given Schema[BatchGetSeriesRequest] = Schema.derived

  given Schema[ListSeriesRequest] = Schema.derived
  given Schema[ListSeriesResponse] = Schema.derived

  given Schema[SearchSeriesResponse] = Schema.derived
  given Schema[SearchSeriesRequest] = Schema.derived
