package org.aulune.auth
package api.http.tapir.schemas


import application.dto.{AuthenticateUserRequest, AuthenticateUserResponse}

import sttp.tapir.{Schema, Validator}


private[api] object AuthenticationSchemas:
  given Schema[AuthenticateUserRequest] = Schema.derived
  given Schema[AuthenticateUserResponse] = Schema.derived
