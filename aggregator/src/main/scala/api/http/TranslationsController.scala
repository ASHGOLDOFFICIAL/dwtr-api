package org.aulune.aggregator
package api.http


import api.http.circe.TranslationCodecs.given
import api.http.tapir.translation.TranslationExamples.{
  CreateRequest,
  GetSelfHostedLocationResponse,
  ListResponse,
  Resource,
}
import api.http.tapir.translation.TranslationSchemas.given
import application.TranslationService
import application.dto.translation.{
  CreateTranslationRequest,
  DeleteTranslationRequest,
  GetTranslationLocationRequest,
  GetTranslationRequest,
  ListTranslationsRequest,
  ListTranslationsResponse,
  TranslationLocationResource,
  TranslationResource,
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


/** Controller with Tapir endpoints for translations.
 *
 *  @param pagination pagination config.
 *  @param service [[TranslationService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class TranslationsController[F[_]: Applicative](
    pagination: AggregatorConfig.PaginationParams,
    service: TranslationService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val translationId = path[UUID]("translation_id")
    .description("ID of the translation")

  private val collectionPath = "translations"
  private val elementPath = collectionPath / translationId
  private val tag = "Translations"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[TranslationResource]
      .description("Requested translation if found.")
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetTranslation")
    .summary("Returns a translation with given ID for given parent.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetTranslationRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(
      MethodSpecificQueryParams.pagination.and(MethodSpecificQueryParams.filter),
    )
    .out(statusCode(StatusCode.Ok).and(jsonBody[ListTranslationsResponse]
      .description("List of translations and a token to retrieve next page.")
      .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListTranslations")
    .summary("Returns the list of translations.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken, filter) =>
      val request = ListTranslationsRequest(
        pageSize = pageSize,
        pageToken = pageToken,
        filter = filter,
      )
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateTranslationRequest]
      .description("Translation to create")
      .example(CreateRequest))
    .out(statusCode(StatusCode.Created).and(jsonBody[TranslationResource]
      .description("Created translation.")
      .example(Resource)))
    .name("CreateTranslation")
    .summary("Creates a new translation resource and returns it.")
    .tag(tag)
    .serverLogic { user => request =>
      for result <- service.create(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val deleteEndpoint = securedEndpoint.delete
    .in(elementPath)
    .out(statusCode(StatusCode.NoContent))
    .name("DeleteTranslation")
    .summary("Deletes translation resource with given ID.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = DeleteTranslationRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val getLocationEndpoint = securedEndpoint.get
    .in(elementPath / "location")
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[TranslationLocationResource]
        .description("Location of a self-hosted translation.")
        .example(GetSelfHostedLocationResponse)))
    .name("GetTranslationLocation")
    .summary("Gets location of a self-hosted translation.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = GetTranslationLocationRequest(name = id)
      for result <- service.getLocation(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for translations. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(
    getEndpoint,
    listEndpoint,
    postEndpoint,
    deleteEndpoint,
    getLocationEndpoint,
  )

end TranslationsController
