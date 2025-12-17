package org.aulune.commons
package adapters.circe


import types.NonEmptyString

import io.circe.{Decoder, Encoder}


/** [[Encoder]]s and [[Decoder]]s for common types. */
object CommonTypeCodecs:
  given nonEmptyStringEncoder: Encoder[NonEmptyString] =
    Encoder.encodeString.contramap(identity)
  given nonEmptyStringDecoder: Decoder[NonEmptyString] = Decoder.decodeString
    .emap(NonEmptyString.from(_).toRight("Failed to decode NonEmptyString"))
