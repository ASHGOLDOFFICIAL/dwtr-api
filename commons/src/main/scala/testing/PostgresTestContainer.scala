package org.aulune.commons
package testing


import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import org.scalatest.{Assertion, Suite}
import org.testcontainers.utility.DockerImageName


trait PostgresTestContainer extends Suite with TestContainerForEach:
  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:17-alpine"),
  )

  private final type Init[A] = Transactor[IO] => IO[A]
  private final type TestCase[A] = A => IO[Assertion]

  /** @param init function that returns a service given [[Transactor]].
   *  @tparam A service type.
   *  @return function that returns [[Assertion]] from after performing actions
   *    with service of type [[A]].
   */
  protected def makeStand[A](init: Init[A]): TestCase[A] => IO[Assertion] =
    (app: TestCase[A]) =>
      withContainers { container =>
        val config = new HikariConfig()
        config.setDriverClassName(container.driverClassName)
        config.setJdbcUrl(container.jdbcUrl)
        config.setUsername(container.username)
        config.setPassword(container.password)
        config.setMaximumPoolSize(1)
        val transactor = HikariTransactor.fromHikariConfig[IO](config, None)

        transactor.use { t =>
          for
            service <- init(t)
            result <- app(service)
          yield result
        }
      }
