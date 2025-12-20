package org.aulune.aggregator
package adapters.service


import adapters.service.errors.AudioPlayTranslationServiceErrorResponses as ErrorResponses
import adapters.service.mappers.AudioPlayTranslationMapper
import application.AggregatorPermission.{Modify, SeeSelfHostedLocation}
import application.dto.audioplay.GetAudioPlayRequest
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
import application.errors.AudioPlayServiceError.AudioPlayNotFound
import application.{
  AggregatorPermission,
  AudioPlayService,
  AudioPlayTranslationService,
}
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.translation.{
  AudioPlayTranslation,
  AudioPlayTranslationFilterField,
}
import domain.repositories.AudioPlayTranslationRepository
import domain.repositories.AudioPlayTranslationRepository.{
  AudioPlayTranslationCursor,
  given,
}

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.filter.FilterParser
import org.aulune.commons.pagination.params.PaginationParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[AudioPlayTranslationService]] implementation. */
object AudioPlayTranslationServiceImpl:
  /** Builds a service.
   *  @param pagination pagination config.
   *  @param repo translation repository.
   *  @param audioPlayService [[AudioPlayService]] implementation to check
   *    original existence.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      pagination: AggregatorConfig.PaginationParams,
      repo: AudioPlayTranslationRepository[F],
      audioPlayService: AudioPlayService[F],
      permissionService: PermissionClientService[F],
  ): F[AudioPlayTranslationService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val filterParser = FilterParser.make[AudioPlayTranslationFilterField]
    val maybeParser = PaginationParamsParser
      .build[AudioPlayTranslationCursor](pagination.default, pagination.max)

    for
      _ <- info"Building service."
      parser <- MonadThrow[F]
        .fromOption(maybeParser, new IllegalArgumentException())
        .onError(_ => error"Invalid parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
    yield new AudioPlayTranslationServiceImpl[F](
      parser,
      filterParser,
      repo,
      audioPlayService,
      permissionService)

end AudioPlayTranslationServiceImpl


private final class AudioPlayTranslationServiceImpl[F[
    _,
]: MonadThrow: SortableUUIDGen: LoggerFactory](
    paginationParser: PaginationParamsParser[AudioPlayTranslationCursor],
    filterParser: FilterParser[AudioPlayTranslationFilterField],
    repo: AudioPlayTranslationRepository[F],
    audioPlayService: AudioPlayService[F],
    permissionService: PermissionClientService[F],
) extends AudioPlayTranslationService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]] =
    val uuid = Uuid[AudioPlayTranslation](request.name)
    (for
      _ <- eitherTLogger.info(s"Find request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.translationNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      response = AudioPlayTranslationMapper.makeResource(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListAudioPlayTranslationsRequest,
  ): F[Either[ErrorResponse, ListAudioPlayTranslationsResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")
      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .leftSemiflatTap(_ => warn"Invalid pagination params are given.")
      filter <- request.filter
        .traverse(filter => EitherT.fromEither(filterParser.parse(filter)))
        .leftMap(_ => ErrorResponses.invalidFilter)
      listResult = repo.list(params.pageSize, params.cursor, filter)
      elems <- EitherT.liftF(listResult)
      response = AudioPlayTranslationMapper.toListResponse(elems)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationResource]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = SortableUUIDGen.randomTypedUUID[F, AudioPlayTranslation]
      val originalId = Uuid[AudioPlay](request.originalId)
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        audioPlayRequest = GetAudioPlayRequest(name = originalId)
        original <- EitherT(audioPlayService.get(audioPlayRequest))
          .leftMap(handleAudioPlayNotFound)
          .leftSemiflatTap(_ => warn"Original was not found: $request")
        id <- EitherT.liftF(uuid)
        translation <- EitherT
          .fromEither(makeAudioPlayTranslation(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(translation))
        response = AudioPlayTranslationMapper.makeResource(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteAudioPlayTranslationRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[AudioPlayTranslation](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  override def getLocation(
      user: User,
      request: GetAudioPlayTranslationLocationRequest,
  ): F[Either[ErrorResponse, AudioPlayTranslationLocationResource]] =
    requirePermissionOrDeny(SeeSelfHostedLocation, user) {
      val uuid = Uuid[AudioPlayTranslation](request.name)
      (for
        _ <- eitherTLogger.info(s"Get location request: $request.")
        elem <- EitherT
          .fromOptionF(repo.get(uuid), ErrorResponses.translationNotFound)
          .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
        link <- EitherT
          .fromOption(elem.selfHostedLocation, ErrorResponses.notSelfHosted)
        response = AudioPlayTranslationLocationResource(link)
      yield response).value.handleErrorWith(handleInternal)
    }

  /** Converts [[AudioPlayNotFound]] response to original not found. Other
   *  responses are left as is.
   *  @param err error response.
   */
  private def handleAudioPlayNotFound(err: ErrorResponse) =
    err.details.info match
      case Some(info) if info.reason == AudioPlayNotFound =>
        ErrorResponses.originalNotFound
      case _ => err

  /** Makes translation from given creation request and assigned ID.
   *  @param request creation request.
   *  @param id ID assigned to this translation.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeAudioPlayTranslation(
      request: CreateAudioPlayTranslationRequest,
      id: Uuid[AudioPlayTranslation],
  ) = AudioPlayTranslationMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidAudioPlayTranslation)
    .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
