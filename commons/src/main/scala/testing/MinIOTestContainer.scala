package org.aulune.commons
package testing


import cats.effect.IO
import com.dimafeng.testcontainers.MinIOContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import io.minio.MinioClient
import org.scalatest.{Assertion, Suite}
import org.testcontainers.utility.DockerImageName


/** Test container with MinIO. */
trait MinIOTestContainer extends Suite with TestContainerForEach:
  private val minioPort = 9000
  private val accessKey: String = "minioadmin"
  private val secretKey: String = "minioadmin"

  override val containerDef: MinIOContainer.Def = MinIOContainer.Def(
    dockerImageName = DockerImageName.parse("minio/minio:latest"),
    userName = accessKey,
    password = secretKey,
  )

  private final type Init[A] = (MinioClient, String) => IO[A]
  private final type TestCase[A] = A => IO[Assertion]

  /** @param init function that returns a service given [[MinIOClient]] and its
   *    endpoint.
   *  @tparam A service type.
   *  @return function that returns [[Assertion]] from after performing actions
   *    with service of type [[A]].
   */
  protected def makeStand[A](init: Init[A]): TestCase[A] => IO[Assertion] =
    (app: TestCase[A]) =>
      withContainers { container =>
        val host = container.host
        val port = container.mappedPort(minioPort)
        val endpoint = s"http://$host:$port"

        val client = MinioClient
          .builder()
          .endpoint(endpoint)
          .credentials(accessKey, secretKey)
          .build()

        for
          service <- init(client, endpoint)
          result <- app(service)
        yield result
      }
