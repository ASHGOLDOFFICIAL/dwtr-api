package org.aulune.aggregator
package api.http


import api.http.circe.SeriesCodecs.given
import api.http.tapir.series.SeriesExamples.{
  BatchGetRequest,
  BatchGetResponse,
  CreateRequest,
  ListResponse,
  Resource,
  SearchResponse,
}
import api.http.tapir.series.SeriesSchemas.given
import application.SeriesService
import application.dto.series.{
  BatchGetSeriesRequest,
  BatchGetSeriesResponse,
  CreateSeriesRequest,
  DeleteSeriesRequest,
  GetSeriesRequest,
  ListSeriesRequest,
  ListSeriesResponse,
  SearchSeriesRequest,
  SearchSeriesResponse,
  SeriesResource,
}

import cats.Applicative
import cats.syntax.all.given
import org.aulune.commons.adapters.circe.ErrorResponseCodecs.given
import org.aulune.commons.adapters.tapir.AuthenticationEndpoints.securedEndpoint
import org.aulune.commons.adapters.tapir.ErrorResponseSchemas.given
import org.aulune.commons.adapters.tapir.{
  ErrorStatusCodeMapper,
  MethodSpecificQueryParams,
}
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.AuthenticationClientService
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{endpoint, path, statusCode, stringToPath}

import java.util.UUID


/** Controller with Tapir endpoints for series.
 *
 *  @param pagination pagination config.
 *  @param service [[SeriesService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class SeriesController[F[_]: Applicative](
    pagination: AggregatorConfig.PaginationParams,
    service: SeriesService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val seriesId = path[UUID]("series_id")
    .description("ID of the series.")

  private val collectionPath = "series"
  private val elementPath = collectionPath / seriesId
  private val tag = "Series"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[SeriesResource]
      .description("Requested series if found.")
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetSeries")
    .summary("Returns a series with given ID.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetSeriesRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val batchGetEndpoint = endpoint.get
    .in(collectionPath + ":batchGet")
    .in(jsonBody[BatchGetSeriesRequest]
      .description("Request with IDs of series to find.")
      .example(BatchGetRequest))
    .out(statusCode(StatusCode.Ok).and(jsonBody[BatchGetSeriesResponse]
      .description("List of requested series.")
      .example(BatchGetResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("BatchGetSeries")
    .summary("Returns series for given IDs.")
    .tag(tag)
    .serverLogic { request =>
      for result <- service.batchGet(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(MethodSpecificQueryParams.pagination)
    .out(statusCode(StatusCode.Ok).and(jsonBody[ListSeriesResponse]
      .description("List of series with token to get next page.")
      .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListSeries")
    .summary("Returns the list of series resources.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken) =>
      val request =
        ListSeriesRequest(pageSize = pageSize, pageToken = pageToken)
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val searchEndpoint = endpoint.get
    .in(collectionPath + ":search")
    .in(MethodSpecificQueryParams.search)
    .out(statusCode(StatusCode.Ok).and(jsonBody[SearchSeriesResponse]
      .description("List of matched series.")
      .example(SearchResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("SearchSeries")
    .summary("Searches series by given query.")
    .tag(tag)
    .serverLogic { (query, limit) =>
      val request = SearchSeriesRequest(query = query, limit = limit)
      for result <- service.search(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val createEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateSeriesRequest]
      .description("Series to create.")
      .example(CreateRequest))
    .out(statusCode(StatusCode.Created).and(jsonBody[SeriesResource]
      .description("Created series.")
      .example(Resource)))
    .name("CreateSeries")
    .summary("Creates a new series and returns the result.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = securedEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteSeries")
    .summary("Deletes series with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = DeleteSeriesRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for series. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    batchGetEndpoint,
    listEndpoint,
    createEndpoint,
    deleteEndpoint,
    searchEndpoint,
  )
