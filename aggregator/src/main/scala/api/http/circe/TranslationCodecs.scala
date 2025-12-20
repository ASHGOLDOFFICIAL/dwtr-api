package org.aulune.aggregator
package api.http.circe


import api.http.circe.SharedCodecs.given
import api.mappers.TranslationTypeMapper
import application.dto.shared.{ExternalResourceDTO, LanguageDTO}
import application.dto.translation.{
  CreateTranslationRequest,
  ListTranslationsRequest,
  ListTranslationsResponse,
  TranslationLocationResource,
  TranslationResource,
  TranslationTypeDTO,
}

import io.circe.generic.extras.semiauto.{
  deriveConfiguredDecoder,
  deriveConfiguredEncoder,
}
import io.circe.{Decoder, Encoder}
import org.aulune.commons.adapters.circe.CirceUtils.config
import org.aulune.commons.adapters.circe.CommonTypeCodecs.given

import java.net.URI


/** [[Encoder]] and [[Decoder]] instances for translation DTOs. */
private[api] object TranslationCodecs:
  given Decoder[TranslationResource] = deriveConfiguredDecoder
  given Encoder[TranslationResource] = deriveConfiguredEncoder

  given Decoder[CreateTranslationRequest] = deriveConfiguredDecoder
  given Encoder[CreateTranslationRequest] = deriveConfiguredEncoder

  given Decoder[ListTranslationsRequest] = deriveConfiguredDecoder
  given Encoder[ListTranslationsRequest] = deriveConfiguredEncoder

  given Decoder[ListTranslationsResponse] = deriveConfiguredDecoder
  given Encoder[ListTranslationsResponse] = deriveConfiguredEncoder

  private given Decoder[TranslationTypeDTO] = Decoder.decodeString
    .emap { str =>
      TranslationTypeMapper
        .fromString(str)
        .toRight(s"Invalid TranslationType: $str")
    }
  private given Encoder[TranslationTypeDTO] =
    Encoder.encodeString.contramap(TranslationTypeMapper.toString)

  given Encoder[TranslationLocationResource] = deriveConfiguredEncoder
  given Decoder[TranslationLocationResource] = deriveConfiguredDecoder
