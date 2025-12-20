package org.aulune.aggregator


import adapters.jdbc.postgres.{
  WorkRepositoryImpl,
  SeriesRepositoryImpl,
  PersonRepositoryImpl,
  TranslationRepositoryImpl,
}
import adapters.s3.CoverImageStorageImpl
import adapters.service.{
  SeriesServiceImpl,
  WorkServiceImpl,
  PersonServiceImpl,
  TranslationServiceImpl,
}
import api.http.{
  SeriesController,
  WorkController,
  PersonsController,
  TranslationsController,
}

import cats.effect.Async
import cats.effect.std.SecureRandom
import cats.syntax.all.given
import doobie.Transactor
import io.minio.MinioClient
import org.aulune.commons.instances.UUIDv7Gen.uuidv7Instance
import org.aulune.commons.service.auth.AuthenticationClientService
import org.aulune.commons.service.permission.PermissionClientService
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.utils.imaging.ImageConverter
import org.typelevel.log4cats.LoggerFactory
import sttp.tapir.server.ServerEndpoint


/** Aggregator app with Tapir endpoints.
 *  @tparam F effect type.
 */
trait AggregatorApp[F[_]]:
  val endpoints: List[ServerEndpoint[Any, F]]


object AggregatorApp:
  /** Builds an aggregator app.
   *  @param config aggregator app config.
   *  @param transactor transactor for DB.
   *  @tparam F effect type.
   */
  def build[F[_]: Async: SecureRandom: LoggerFactory](
      config: AggregatorConfig,
      authServ: AuthenticationClientService[F],
      permissionServ: PermissionClientService[F],
      transactor: Transactor[F],
      minioClient: MinioClient,
  ): F[AggregatorApp[F]] =
    given SortableUUIDGen[F] = uuidv7Instance
    for
      personRepo <- PersonRepositoryImpl.build[F](transactor)
      personServ <- PersonServiceImpl.build[F](
        config.maxBatchGet,
        config.pagination,
        config.search,
        personRepo,
        permissionServ)
      personEndpoints = new PersonsController[F](personServ, authServ).endpoints

      seriesRepo <- SeriesRepositoryImpl.build[F](transactor)
      seriesServ <- SeriesServiceImpl.build[F](
        config.maxBatchGet,
        config.pagination,
        config.search,
        seriesRepo,
        permissionServ,
      )
      seriesEndpoints = new SeriesController[F](
        config.pagination,
        seriesServ,
        authServ).endpoints

      coverStorage <- CoverImageStorageImpl.build[F](
        minioClient,
        config.coverStorage.publicUrl,
        config.coverStorage.bucket,
        config.coverStorage.partSize,
      )
      workRepo <- WorkRepositoryImpl.build[F](transactor)
      workService <- WorkServiceImpl
        .build[F](
          config.pagination,
          config.search,
          config.coverLimits,
          workRepo,
          coverStorage,
          seriesServ,
          personServ,
          permissionServ,
          ImageConverter[F])
      workEndpoints = new WorkController[F](
        config.pagination,
        workService,
        authServ).endpoints

      transRepo <- TranslationRepositoryImpl.build[F](transactor)
      transServ <- TranslationServiceImpl
        .build[F](config.pagination, transRepo, workService, permissionServ)
      translationEndpoints = new TranslationsController[F](
        config.pagination,
        transServ,
        authServ,
      ).endpoints

      allEndpoints =
        seriesEndpoints ++ workEndpoints ++ translationEndpoints ++ personEndpoints
    yield new AggregatorApp[F]:
      override val endpoints: List[ServerEndpoint[Any, F]] = allEndpoints
