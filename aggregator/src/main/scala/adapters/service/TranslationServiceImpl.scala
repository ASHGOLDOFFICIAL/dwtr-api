package org.aulune.aggregator
package adapters.service


import adapters.service.TranslationServiceImpl.Cursor
import adapters.service.errors.TranslationServiceErrorResponses as ErrorResponses
import adapters.service.mappers.TranslationMapper
import application.AggregatorPermission.{Modify, SeeSelfHostedLocation}
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
import application.dto.work.GetWorkRequest
import application.errors.WorkServiceError.WorkNotFound
import application.{AggregatorPermission, TranslationService, WorkService}
import domain.model.translation.{Translation, TranslationField}
import domain.model.work.Work
import domain.repositories.TranslationRepository

import cats.MonadThrow
import cats.data.EitherT
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.filter.Filter.Operator.GreaterThan
import org.aulune.commons.filter.Filter.{Condition, Literal}
import org.aulune.commons.filter.{Filter, FilterParser}
import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.pagination.params.PaginationParamsParser
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.service.permission.PermissionClientService.requirePermissionOrDeny
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.{NonEmptyString, Uuid}
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.LoggerInterpolator
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[TranslationService]] implementation. */
object TranslationServiceImpl:
  /** Builds a service.
   *
   *  @param pagination pagination config.
   *  @param repo translation repository.
   *  @param audioPlayService [[WorkService]] implementation to check original
   *    existence.
   *  @param permissionService [[PermissionClientService]] implementation to
   *    perform permission checks.
   *  @tparam F effect type.
   *  @throws IllegalArgumentException if pagination params are invalid.
   */
  def build[F[_]: MonadThrow: SortableUUIDGen: LoggerFactory](
      pagination: AggregatorConfig.PaginationParams,
      repo: TranslationRepository[F],
      audioPlayService: WorkService[F],
      permissionService: PermissionClientService[F],
  ): F[TranslationService[F]] =
    given Logger[F] = LoggerFactory[F].getLogger
    val filterParser = FilterParser.make[TranslationField]
    val maybeParser = PaginationParamsParser
      .build[Cursor](pagination.default, pagination.max)

    for
      _ <- info"Building service."
      parser <- MonadThrow[F]
        .fromOption(maybeParser, new IllegalArgumentException())
        .onError(_ => error"Invalid parser parameters are given.")
      _ <- permissionService.registerPermission(Modify)
    yield new TranslationServiceImpl[F](
      parser,
      filterParser,
      repo,
      audioPlayService,
      permissionService)

  /** Cursor to resume pagination of translations.
   *  @param id ID of this translation.
   *  @param filter previously used filter expression.
   */
  private final case class Cursor(
      id: Uuid[Translation],
      filter: Option[NonEmptyString],
  ) derives CursorEncoder,
        CursorDecoder:
    /** Converts cursor to filter AST. */
    def toFilter: Filter[TranslationField] =
      Condition(TranslationField.Id, GreaterThan, Literal(id.toString))

end TranslationServiceImpl


private final class TranslationServiceImpl[F[
    _,
]: MonadThrow: SortableUUIDGen: LoggerFactory] private (
    paginationParser: PaginationParamsParser[Cursor],
    filterParser: FilterParser[TranslationField],
    repo: TranslationRepository[F],
    workService: WorkService[F],
    permissionService: PermissionClientService[F],
) extends TranslationService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger
  private given PermissionClientService[F] = permissionService

  override def get(
      request: GetTranslationRequest,
  ): F[Either[ErrorResponse, TranslationResource]] =
    val uuid = Uuid[Translation](request.name)
    (for
      _ <- eitherTLogger.info(s"Find request: $request.")
      elem <- EitherT
        .fromOptionF(repo.get(uuid), ErrorResponses.translationNotFound)
        .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
      response = TranslationMapper.makeResource(elem)
    yield response).value.handleErrorWith(handleInternal)

  override def list(
      request: ListTranslationsRequest,
  ): F[Either[ErrorResponse, ListTranslationsResponse]] =
    val paramsV = paginationParser.parse(request.pageSize, request.pageToken)
    (for
      _ <- eitherTLogger.info(s"List request: $request.")

      params <- EitherT
        .fromOption(paramsV.toOption, ErrorResponses.invalidPaginationParams)
        .ensure(ErrorResponses.invalidFilter) { p =>
          p.cursor.fold(true)(_.filter == request.filter)
        }
      requestFilter <- request.filter
        .traverse(s => EitherT.fromEither(filterParser.parse(s)))
        .leftMap(_ => ErrorResponses.invalidFilter)

      filter = List(params.cursor.map(_.toFilter), requestFilter).flatten
        .reduceOption(_ and _)
      elems <- EitherT.liftF(repo.list(params.pageSize, filter))

      nextPageToken = elems.lastOption
        .map(e => CursorEncoder[Cursor].encode(Cursor(e.id, request.filter)))
        .filter(_ => elems.size == params.pageSize)
      response = ListTranslationsResponse(
        elems.map(TranslationMapper.makeResource),
        nextPageToken)
    yield response).value.handleErrorWith(handleInternal)

  override def create(
      user: User,
      request: CreateTranslationRequest,
  ): F[Either[ErrorResponse, TranslationResource]] =
    requirePermissionOrDeny(Modify, user) {
      val uuid = SortableUUIDGen.randomTypedUUID[F, Translation]
      val originalId = Uuid[Work](request.originalId)
      (for
        _ <- eitherTLogger.info(s"Create request $request from $user.")
        audioPlayRequest = GetWorkRequest(name = originalId)
        original <- EitherT(workService.get(audioPlayRequest))
          .leftMap(handleWorkNotFound)
          .leftSemiflatTap(_ => warn"Original was not found: $request")
        id <- EitherT.liftF(uuid)
        translation <- EitherT
          .fromEither(makeTranslation(request, id))
          .leftSemiflatTap(_ => warn"Request to create bad element: $request.")
        persisted <- EitherT.liftF(repo.persist(translation))
        response = TranslationMapper.makeResource(persisted)
      yield response).value
    }.handleErrorWith(handleInternal)

  override def delete(
      user: User,
      request: DeleteTranslationRequest,
  ): F[Either[ErrorResponse, Unit]] = requirePermissionOrDeny(Modify, user) {
    val uuid = Uuid[Translation](request.name)
    info"Delete request $request from $user" >> repo.delete(uuid).map(_.asRight)
  }.handleErrorWith(handleInternal)

  override def getLocation(
      user: User,
      request: GetTranslationLocationRequest,
  ): F[Either[ErrorResponse, TranslationLocationResource]] =
    requirePermissionOrDeny(SeeSelfHostedLocation, user) {
      val uuid = Uuid[Translation](request.name)
      (for
        _ <- eitherTLogger.info(s"Get location request: $request.")
        elem <- EitherT
          .fromOptionF(repo.get(uuid), ErrorResponses.translationNotFound)
          .leftSemiflatTap(_ => warn"Couldn't find element with ID: $request")
        link <- EitherT
          .fromOption(elem.selfHostedLocation, ErrorResponses.notSelfHosted)
        response = TranslationLocationResource(link)
      yield response).value.handleErrorWith(handleInternal)
    }

  /** Converts [[WorkNotFound]] response to original not found. Other responses
   *  are left as is.
   *
   *  @param err error response.
   */
  private def handleWorkNotFound(err: ErrorResponse) = err.details.info match
    case Some(info) if info.reason == WorkNotFound =>
      ErrorResponses.originalNotFound
    case _ => err

  /** Makes translation from given creation request and assigned ID.
   *  @param request creation request.
   *  @param id ID assigned to this translation.
   *  @note It's only purpose is to improve readability of [[create]] method.
   */
  private def makeTranslation(
      request: CreateTranslationRequest,
      id: Uuid[Translation],
  ) = TranslationMapper
    .fromRequest(request, id)
    .leftMap(ErrorResponses.invalidTranslation)
    .toEither

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
