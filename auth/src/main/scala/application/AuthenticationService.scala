package org.aulune.auth
package application


import application.dto.{
  AuthenticateUserRequest,
  AuthenticateUserResponse,
  UserInfo,
}
import application.errors.AuthenticationServiceError.{
  InvalidAccessToken,
  InvalidCredentials,
  InvalidOAuthCode,
  InvalidUser,
  UserAlreadyExists,
}

import org.aulune.commons.errors.ErrorResponse


/** Service managing user authentication.
 *  @tparam F effect type.
 */
trait AuthenticationService[F[_]]:
  /** Returns access token if given authentication info is correct.
   *
   *  [[InvalidCredentials]] will be returned when given invalid credentials.
   *
   *  @param request request with authentication info.
   */
  def login(
      request: AuthenticateUserRequest,
  ): F[Either[ErrorResponse, AuthenticateUserResponse]]

  /** Returns authenticated user's info if token is valid.
   *
   *  [[InvalidAccessToken]] will be returned if token is invalid.
   *
   *  @param accessToken user's access token.
   */
  def getUserInfo(
      accessToken: String,
  ): F[Either[ErrorResponse, UserInfo]]
