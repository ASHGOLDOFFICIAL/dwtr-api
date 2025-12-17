package org.aulune.aggregator
package api.http


import api.http.circe.AudioPlayTranslationCodecs.given
import api.http.tapir.audioplay.translation.AudioPlayTranslationExamples.{
  CreateRequest,
  GetSelfHostedLocationResponse,
  ListResponse,
  Resource,
}
import api.http.tapir.audioplay.translation.AudioPlayTranslationSchemas.given
import application.AudioPlayTranslationService
import application.dto.audioplay.translation.{
  AudioPlayTranslationLocationResource,
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  DeleteAudioPlayTranslationRequest,
  GetAudioPlayTranslationLocationRequest,
  GetAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
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
 *  @param pagination pagination config.
 *  @param service [[AudioPlayTranslationService]] to use.
 *  @param authService [[AuthenticationClientService]] to use for restricted
 *    endpoints.
 *  @tparam F effect type.
 */
final class AudioPlayTranslationsController[F[_]: Applicative](
    pagination: AggregatorConfig.PaginationParams,
    service: AudioPlayTranslationService[F],
    authService: AuthenticationClientService[F],
):
  private given AuthenticationClientService[F] = authService

  private val translationId = path[UUID]("translation_id")
    .description("ID of the translation")

  private val collectionPath = "audioPlayTranslations"
  private val elementPath = collectionPath / translationId
  private val tag = "AudioPlayTranslations"

  private val getEndpoint = endpoint.get
    .in(elementPath)
    .out(statusCode(StatusCode.Ok).and(jsonBody[AudioPlayTranslationResource]
      .description("Requested audio play translation if found.")
      .example(Resource)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("GetTranslation")
    .summary("Returns a translation with given ID for given parent.")
    .tag(tag)
    .serverLogic { id =>
      val request = GetAudioPlayTranslationRequest(name = id)
      for result <- service.get(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val listEndpoint = endpoint.get
    .in(collectionPath)
    .in(
      MethodSpecificQueryParams.pagination.and(MethodSpecificQueryParams.filter),
    )
    .out(
      statusCode(StatusCode.Ok).and(jsonBody[ListAudioPlayTranslationsResponse]
        .description("List of audio plays and a token to retrieve next page.")
        .example(ListResponse)))
    .errorOut(statusCode.and(
      jsonBody[ErrorResponse].description("Description of error.")))
    .name("ListTranslations")
    .summary("Returns the list of translation for given parent.")
    .tag(tag)
    .serverLogic { (pageSize, pageToken, filter) =>
      val request = ListAudioPlayTranslationsRequest(
        pageSize = pageSize,
        pageToken = pageToken,
        filter = filter,
      )
      for result <- service.list(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val postEndpoint = securedEndpoint.post
    .in(collectionPath)
    .in(jsonBody[CreateAudioPlayTranslationRequest]
      .description("Translation to create")
      .example(CreateRequest))
    .out(
      statusCode(StatusCode.Created).and(jsonBody[AudioPlayTranslationResource]
        .description("Created translation.")
        .example(Resource)))
    .name("CreateTranslation")
    .summary("Creates a new translation for parent resource and returns it.")
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
      val request = DeleteAudioPlayTranslationRequest(name = id)
      for result <- service.delete(user, request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  private val getLocationEndpoint = securedEndpoint.get
    .in(elementPath / "location")
    .out(
      statusCode(StatusCode.Ok).and(
        jsonBody[AudioPlayTranslationLocationResource]
          .description("Location of a self-hosted translation.")
          .example(GetSelfHostedLocationResponse)))
    .name("GetAudioPlayTranslationLocation")
    .summary("Gets location of a self-hosted translation.")
    .tag(tag)
    .serverLogic { user => id =>
      val request = GetAudioPlayTranslationLocationRequest(name = id)
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

end AudioPlayTranslationsController
