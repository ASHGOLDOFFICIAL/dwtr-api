package org.aulune.auth
package api.http


import api.http.circe.AuthenticationCodecs.given
import api.http.tapir.examples.AuthenticationExamples.{
  requestExamples,
  responseExample,
}
import api.http.tapir.schemas.AuthenticationSchemas.given
import application.AuthenticationService
import application.dto.{AuthenticateUserRequest, AuthenticateUserResponse}

import cats.Functor
import cats.syntax.all.given
import org.aulune.commons.adapters.circe.ErrorResponseCodecs.given
import org.aulune.commons.adapters.tapir.ErrorResponseSchemas.given
import org.aulune.commons.adapters.tapir.ErrorStatusCodeMapper
import org.aulune.commons.errors.ErrorResponse
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{endpoint, statusCode, stringToPath}


/** Controller with Tapir endpoints for authentication.
 *  @param service [[AuthenticationService]] to use.
 *  @tparam F effect type.
 */
final class AuthenticationController[F[_]: Functor](
    service: AuthenticationService[F],
):
  private val collection = "users"
  private val tag = "Users"

  private val loginEndpoint = endpoint.post
    .in(collection + ":authenticate")
    .in(jsonBody[AuthenticateUserRequest]
      .description("Login information.")
      .examples(requestExamples))
    .out(statusCode(StatusCode.Ok).and(jsonBody[AuthenticateUserResponse]
      .description("Tokens to use in calls to API.")
      .example(responseExample)))
    .errorOut(statusCode.and(jsonBody[ErrorResponse]))
    .name("Login")
    .summary("Authenticate to receive token.")
    .tag(tag)
    .serverLogic { request =>
      for result <- service.login(request)
      yield result.leftMap(ErrorStatusCodeMapper.toApiResponse)
    }

  /** Returns Tapir endpoints for authentication. */
  def endpoints: List[ServerEndpoint[Any, F]] = List(loginEndpoint)
