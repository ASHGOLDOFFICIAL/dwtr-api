package org.aulune.auth
package adapters.service


import adapters.service.errors.AuthenticationServiceErrorResponses as ErrorResponses
import application.AuthenticationService
import application.dto.AuthenticateUserRequest.BasicAuthentication
import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  UserInfo,
}
import domain.errors.{UserConstraint, UserValidationError}
import domain.model.{TokenString, User, Username}
import domain.repositories.UserRepository
import domain.services.{
  AccessTokenService,
  BasicAuthenticationHandler,
  IdTokenService,
}

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.std.UUIDGen
import cats.syntax.all.given
import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.repositories.RepositoryError
import org.aulune.commons.types.Uuid
import org.typelevel.log4cats.Logger.eitherTLogger
import org.typelevel.log4cats.syntax.given
import org.typelevel.log4cats.{Logger, LoggerFactory}


/** [[AuthenticationService]] implementation.
 *  @param repo repository with users.
 *  @param accessTokenService service that generates and decodes token.
 *  @param idTokenService service that generates ID tokens.
 *  @param basicAuthHandler service to which basic authentication requests will
 *    be delegated.
 *  @tparam F effect type.
 */
final class AuthenticationServiceImpl[F[_]: MonadThrow: UUIDGen: LoggerFactory](
    repo: UserRepository[F],
    accessTokenService: AccessTokenService[F],
    idTokenService: IdTokenService[F],
    basicAuthHandler: BasicAuthenticationHandler[F],
) extends AuthenticationService[F]:

  private given Logger[F] = LoggerFactory[F].getLogger

  override def login(
      request: AuthenticateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]] = (for
    _ <- eitherTLogger.info(s"Authentication request: $request")
    user <- delegateLogin(request)
    _ <- eitherTLogger.info(s"Successful login for $request")
    response <- EitherT(makeResponseForUser(user))
  yield response).value.handleErrorWith(handleInternal)

  override def getUserInfo(
      accessToken: String,
  ): F[Either[ErrorResponse, UserInfo]] = (for
    _ <- eitherTLogger.info(s"User info request for token: $accessToken.")
    token <- EitherT.fromOption(
      TokenString(accessToken),
      ErrorResponses.invalidAccessToken)
    id <- EitherT.fromOptionF(
      accessTokenService.decodeAccessToken(token),
      ErrorResponses.invalidAccessToken)
    user <- EitherT.fromOptionF(repo.get(id), ErrorResponses.userNotFound)
    userInfo = UserInfo(id = user.id, username = user.username)
  yield userInfo).value.handleErrorWith(handleInternal)

  /** Makes [[AuthenticateUserResponse]] for given user.
   *  @param user user for whom response is being made.
   */
  private def makeResponseForUser(
      user: User,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]] =
    for
      accessToken <- accessTokenService.generateAccessToken(user)
      idToken <- idTokenService.generateIdToken(user)
      response = AuthenticateUserResponse(accessToken, idToken = idToken)
      _ <- info"Made response for user: $user."
    yield response.asRight

  /** Creates user from registration request and third-party id.
   *  @param username user chosen username.
   *  @return user or NEC of errors.
   */
  private def createUser(
      id: Uuid[User],
      username: String,
  ): Either[ErrorResponse, User] = Username(username)
    .toValidNec(UserValidationError.InvalidUsername)
    .andThen(username => User.create(id = id, username = username))
    .toEither
    .leftMap(ErrorResponses.invalidRegistrationDetails)

  /** Delegates login request to a service that can manage it.
   *  @param request login request.
   *  @return user if login is successful, otherwise `None`.
   */
  private def delegateLogin(
      request: AuthenticateUserRequest,
  ): EitherT[F, ErrorResponse, User] = request match
    case req @ BasicAuthentication(username, password) =>
      for
        username <- EitherT
          .fromOption(Username(username), ErrorResponses.invalidCredentials)
          .leftSemiflatTap(_ => warn"Login with invalid username: $request.")
        user <- EitherT
          .fromOptionF(
            basicAuthHandler.authenticate(username, password),
            ErrorResponses.invalidCredentials)
          .leftSemiflatTap(_ => warn"Basic authentication failed: $request.")
      yield user

  /** Logs any error and returns internal error response. */
  private def handleInternal[A](e: Throwable) =
    for _ <- Logger[F].error(e)("Uncaught exception.")
    yield ErrorResponses.internal.asLeft[A]
