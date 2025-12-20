package org.aulune.aggregator
package api.http


import api.http.circe.WorkCodecs.given
import api.http.tapir.work.WorkExamples.{
  CreateRequest,
  GetSelfHostedLocationResponse,
  ListResponse,
  Resource,
  SearchResponse,
}
import api.http.tapir.work.WorkSchemas.given
import application.WorkService
import application.dto.work.{
  CreateWorkRequest,
  DeleteWorkRequest,
  GetWorkLocationRequest,
  GetWorkRequest,
  ListWorksRequest,
  ListWorksResponse,
  SearchWorksRequest,
  SearchWorksResponse,
  UploadWorkCoverRequest,
  WorkLocationResource,
  WorkResource,
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
import sttp.tapir.{byteArrayBody, endpoint, path, statusCode, stringToPath}

import java.util.UUID


/** Controller with Tapir endpoints for works.
 *
 *  @param pagination pagination config.
 *  @param service [[WorkService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class WorkController[F[_]: Applicative](
    pagination: AggregatorConfig.PaginationParams,
    service: WorkService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val workId = path[UUID]("work_id")
    .description("ID of the work.")

  private val collectionPath = "works"
  private val elementPath = collectionPath / workId
  private val tag = "Works"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[WorkResource]
      .description("Requested work if found.")
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetWork")
    .summary("Returns a work with given ID.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetWorkRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(MethodSpecificQueryParams.pagination)
    .out(statusCode(StatusCode.Ok).and(jsonBody[ListWorksResponse]
      .description("List of works with token to get next page.")
      .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListWorks")
    .summary("Returns the list of work resources.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken) =>
      val request = ListWorksRequest(pageSize = pageSize, pageToken = pageToken)
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val searchEndpoint = endpoint.get
    .in(collectionPath + ":search")
    .in(MethodSpecificQueryParams.search)
    .out(statusCode(StatusCode.Ok).and(jsonBody[SearchWorksResponse]
      .description("List of matched works.")
      .example(SearchResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("SearchWorks")
    .summary("Searches works by given query.")
    .tag(tag)
    .serverLogic { (query, limit) =>
      val request = SearchWorksRequest(query = query, limit = limit)
      for result <- service.search(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateWorkRequest]
      .description("Work to create.")
      .example(CreateRequest))
    .out(statusCode(StatusCode.Created).and(jsonBody[WorkResource]
      .description("Created work.")
      .example(Resource)))
    .name("CreateWork")
    .summary("Creates a new work and returns the created resource.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = securedEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteWork")
    .summary("Deletes work resource with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = DeleteWorkRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val uploadCoverEndpoint = securedEndpoint.post
    .in(elementPath / ":uploadCover")
    .in(byteArrayBody)
    .out(statusCode(StatusCode.Ok).and(jsonBody[WorkResource]
      .description("Updated work if found.")
      .example(Resource)))
    .name("UploadWorkCover")
    .summary("Uploads cover to given work.")
    .tag(tag)
    .serverLogic { user => (id, bytes) =>
      val ibytes = IArray.unsafeFromArray(bytes)
      val request = UploadWorkCoverRequest(id, ibytes)
      for result <- service.uploadCover(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val getLocationEndpoint = securedEndpoint.get
    .in(elementPath / "location")
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[WorkLocationResource]
        .description("Location of a self-hosted work.")
        .example(GetSelfHostedLocationResponse)))
    .name("GetWorkLocation")
    .summary("Gets location of a self-hosted work.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = GetWorkLocationRequest(name = id)
      for result <- service.getLocation(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for works. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    searchEndpoint,
    postEndpoint,
    deleteEndpoint,
    uploadCoverEndpoint,
    getLocationEndpoint,
  )
