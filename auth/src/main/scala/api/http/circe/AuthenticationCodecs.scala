package org.aulune.auth
package api.http.circe


import application.dto.{AuthenticateUserRequest, AuthenticateUserResponse}

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config


/** [[Decoder]] and [[Encoder]] instances for [[AuthenticationController]]. */
private[api] object AuthenticationCodecs:
  private given Configuration = config.copy(
    transformConstructorNames = {
      case "BasicAuthentication" => "basic"
      case other                 => other
    },
  )

  given Encoder[AuthenticateUserRequest] = deriveConfiguredEncoder
  given Decoder[AuthenticateUserRequest] = deriveConfiguredDecoder

  given Encoder[AuthenticateUserResponse] = deriveConfiguredEncoder
  given Decoder[AuthenticateUserResponse] = deriveConfiguredDecoder
