package org.aulune.aggregator
package api.http.tapir.series


import application.dto.series.{
  BatchGetSeriesRequest,
  BatchGetSeriesResponse,
  CreateSeriesRequest,
  ListSeriesResponse,
  SearchSeriesResponse,
  SeriesResource,
}

import java.util.{Base64, UUID}


/** Example objects for series DTOs. */
private[http] object SeriesExamples:

  val Resource: SeriesResource = SeriesResource(
    id = UUID.fromString("3cb893bf-5382-49ef-b881-2f07e75bfcdd"),
    name = "Cicero",
  )

  private val NextPageToken =
    Some(Base64.getEncoder.encodeToString(Resource.name.getBytes))

  val BatchGetRequest: BatchGetSeriesRequest = BatchGetSeriesRequest(
    List(Resource.id),
  )

  val BatchGetResponse: BatchGetSeriesResponse = BatchGetSeriesResponse(
    List(Resource),
  )

  val CreateRequest: CreateSeriesRequest = CreateSeriesRequest(
    name = Resource.name,
  )

  val ListResponse: ListSeriesResponse = ListSeriesResponse(
    series = List(Resource),
    nextPageToken = NextPageToken,
  )

  val SearchResponse: SearchSeriesResponse = SearchSeriesResponse(
    series = List(Resource),
  )
