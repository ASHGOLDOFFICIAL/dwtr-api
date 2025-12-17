package org.aulune.commons
package adapters.tapir


import types.NonEmptyString

import sttp.tapir.CodecFormat.TextPlain
import sttp.tapir.{Codec, Schema}


/** [[Codec]]s for common types. */
object CommonTypeCodecs:
  given nonEmptyStringCodec: Codec[String, NonEmptyString, TextPlain] =
    Codec.string.mapEither { s =>
      NonEmptyString.from(s).toRight("Empty string is given")
    }(identity)

  given nonEmptyStringSchema: Schema[NonEmptyString] =
    Schema.schemaForString.map(NonEmptyString.from)(identity)
