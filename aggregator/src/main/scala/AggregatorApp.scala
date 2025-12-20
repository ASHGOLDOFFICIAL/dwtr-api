package org.aulune.aggregator


import adapters.jdbc.postgres.{
  AudioPlayRepositoryImpl,
  AudioPlaySeriesRepositoryImpl,
  PersonRepositoryImpl,
  TranslationRepositoryImpl,
}
import adapters.s3.CoverImageStorageImpl
import adapters.service.{
  AudioPlaySeriesServiceImpl,
  AudioPlayServiceImpl,
  PersonServiceImpl,
  TranslationServiceImpl,
}
import api.http.{
  AudioPlaySeriesController,
  AudioPlaysController,
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

      seriesRepo <- AudioPlaySeriesRepositoryImpl.build[F](transactor)
      seriesServ <- AudioPlaySeriesServiceImpl.build[F](
        config.maxBatchGet,
        config.pagination,
        config.search,
        seriesRepo,
        permissionServ,
      )
      seriesEndpoints = new AudioPlaySeriesController[F](
        config.pagination,
        seriesServ,
        authServ).endpoints

      coverStorage <- CoverImageStorageImpl.build[F](
        minioClient,
        config.coverStorage.publicUrl,
        config.coverStorage.bucket,
        config.coverStorage.partSize,
      )
      audioRepo <- AudioPlayRepositoryImpl.build[F](transactor)
      audioServ <- AudioPlayServiceImpl
        .build[F](
          config.pagination,
          config.search,
          config.coverLimits,
          audioRepo,
          coverStorage,
          seriesServ,
          personServ,
          permissionServ,
          ImageConverter[F])
      audioEndpoints = new AudioPlaysController[F](
        config.pagination,
        audioServ,
        authServ).endpoints

      transRepo <- TranslationRepositoryImpl.build[F](transactor)
      transServ <- TranslationServiceImpl
        .build[F](config.pagination, transRepo, audioServ, permissionServ)
      translationEndpoints = new TranslationsController[F](
        config.pagination,
        transServ,
        authServ,
      ).endpoints

      allEndpoints =
        seriesEndpoints ++ audioEndpoints ++ translationEndpoints ++ personEndpoints
    yield new AggregatorApp[F]:
      override val endpoints: List[ServerEndpoint[Any, F]] = allEndpoints
