package org.aulune.auth
package adapters.service


import application.AuthenticationService
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  UserInfo,
}
import application.errors.AuthenticationServiceError.{
  ExternalServiceFailure,
  InvalidAccessToken,
  InvalidCredentials,
  InvalidOAuthCode,
  UserAlreadyExists,
  UserNotFound,
  UsernameAlreadyTaken,
}
import domain.errors.UserConstraint
import domain.model.{TokenString, User, Username}
import domain.repositories.UserRepository
import domain.services.{
  AccessTokenService,
  BasicAuthenticationHandler,
  IdTokenService,
}

import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.testing.ErrorAssertions.{
  assertDomainError,
  assertInternalError,
}
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID


/** Tests for [[AuthenticationServiceImpl]]. */
final class AuthenticationServiceImplTest
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:
  private given LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val mockRepo = mock[UserRepository[IO]]
  private val mockAccess = mock[AccessTokenService[IO]]
  private val mockId = mock[IdTokenService[IO]]
  private val mockBasic = mock[BasicAuthenticationHandler[IO]]

  private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private given UUIDGen[IO] = new UUIDGen[IO]:
    override def randomUUID: IO[UUID] = uuid.pure[IO]

  private def stand(
      testCase: AuthenticationService[IO] => IO[Assertion],
  ): IO[Assertion] = testCase(
    AuthenticationServiceImpl(
      repo = mockRepo,
      accessTokenService = mockAccess,
      idTokenService = mockId,
      basicAuthHandler = mockBasic,
    ))

  private val username = Username.unsafe("username")
  private val password = "password"
  private val accessToken = TokenString.unsafe("access_token_string")
  private val idToken = TokenString.unsafe("id_token_string")

  private val user = User.unsafe(
    id = Uuid(uuid),
    username = username,
    hashedPassword = None,
  )
  private val userInfo = UserInfo(
    id = user.id,
    username = user.username,
  )

  private val basicAuthenticateUserRequest =
    AuthenticateUserRequest.BasicAuthentication(
      username = username,
      password = password,
    )
  private val authenticateResponse = AuthenticateUserResponse(
    accessToken = accessToken,
    idToken = idToken,
  )

  private def mockBasicAuthenticate(returning: IO[Option[User]]) =
    (mockBasic.authenticate _)
      .expects(user.username, password)
      .returning(returning)

  private def mockPersist(returning: IO[User]) = (mockRepo.persist _)
    .expects(user)
    .returning(returning)

  private def mockGet(returning: IO[Option[User]]) = (mockRepo.get _)
    .expects(user.id)
    .returning(returning)

  private def mockDecodeAccessToken(returning: IO[Option[Uuid[User]]]) =
    (mockAccess.decodeAccessToken _)
      .expects(accessToken)
      .returning(returning)

  private def mockGenerateAccessToken(returning: IO[TokenString]) =
    (mockAccess.generateAccessToken _)
      .expects(user)
      .returning(returning)

  private def mockGenerateIdToken(returning: IO[TokenString]) =
    (mockId.generateIdToken _)
      .expects(user)
      .returning(returning)

  "login method with basic authentication " - {
    "should " - {
      "return tokens for user if everything is OK" in stand { service =>
        val _ = mockBasicAuthenticate(user.some.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(idToken.pure)
        for result <- service.login(basicAuthenticateUserRequest)
        yield result shouldBe authenticateResponse.asRight
      }

      "return InvalidCredentials if couldn't authenticate" in stand { service =>
        val _ = mockBasicAuthenticate(None.pure)
        val login = service.login(basicAuthenticateUserRequest)
        assertDomainError(login)(InvalidCredentials)
      }

      "handle exceptions from authenticate gracefully" in stand { service =>
        val _ = mockBasicAuthenticate(IO.raiseError(new Throwable()))
        val login = service.login(basicAuthenticateUserRequest)
        assertInternalError(login)
      }

      "handle exceptions from generateAccessToken gracefully" in stand {
        service =>
          val _ = mockBasicAuthenticate(user.some.pure)
          val _ = mockGenerateAccessToken(IO.raiseError(new Throwable()))
          val login = service.login(basicAuthenticateUserRequest)
          assertInternalError(login)
      }

      "handle exceptions from generateIdToken gracefully" in stand { service =>
        val _ = mockBasicAuthenticate(user.some.pure)
        val _ = mockGenerateAccessToken(accessToken.pure)
        val _ = mockGenerateIdToken(IO.raiseError(new Throwable()))
        val login = service.login(basicAuthenticateUserRequest)
        assertInternalError(login)
      }
    }
  }

  "getUserInfo method" - {
    "should " - {
      "return user info if everything is OK" in stand { service =>
        val _ = mockDecodeAccessToken(user.id.some.pure)
        val _ = mockGet(user.some.pure)
        for result <- service.getUserInfo(accessToken)
        yield result shouldBe userInfo.asRight
      }

      "result in InvalidAccessToken if token cannot be decoded" in stand {
        service =>
          val _ = mockDecodeAccessToken(None.pure)
          val userInfo = service.getUserInfo(accessToken)
          assertDomainError(userInfo)(InvalidAccessToken)
      }

      "result in UserNotFound if token owner cannot be found" in stand {
        service =>
          val _ = mockDecodeAccessToken(user.id.some.pure)
          val _ = mockGet(None.pure)
          val userInfo = service.getUserInfo(accessToken)
          assertDomainError(userInfo)(UserNotFound)
      }

      "handle exceptions from decodeAccessToken gracefully" in stand {
        service =>
          val _ = mockDecodeAccessToken(IO.raiseError(new Throwable()))
          val userInfo = service.getUserInfo(accessToken)
          assertInternalError(userInfo)
      }

      "handle exceptions from repo.get gracefully" in stand { service =>
        val _ = mockDecodeAccessToken(user.id.some.pure)
        val _ = mockGet(IO.raiseError(new Throwable()))
        val userInfo = service.getUserInfo(accessToken)
        assertInternalError(userInfo)
      }
    }
  }

end AuthenticationServiceImplTest
