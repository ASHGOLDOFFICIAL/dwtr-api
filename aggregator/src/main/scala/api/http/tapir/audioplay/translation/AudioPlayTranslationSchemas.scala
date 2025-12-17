package org.aulune.aggregator
package api.http.tapir.audioplay.translation


import api.http.tapir.SharedSchemas.given
import api.http.tapir.audioplay.AudioPlayExamples
import api.http.tapir.audioplay.translation.AudioPlayTranslationExamples.{
  CreateRequest,
  ListResponse,
  Resource,
}
import api.mappers.{AudioPlayTranslationTypeMapper, LanguageMapper}
import application.dto.audioplay.translation.{
  AudioPlayTranslationLocationResource,
  AudioPlayTranslationResource,
  AudioPlayTranslationTypeDTO,
  CreateAudioPlayTranslationRequest,
  ListAudioPlayTranslationsRequest,
  ListAudioPlayTranslationsResponse,
}
import application.dto.shared.LanguageDTO

import io.circe.syntax.given
import org.aulune.commons.adapters.tapir.CommonTypeCodecs.nonEmptyStringSchema
import sttp.tapir.{Schema, Validator}

import java.net.URI
import java.util.UUID


object AudioPlayTranslationSchemas:
  given Schema[AudioPlayTranslationResource] = Schema
    .derived[AudioPlayTranslationResource]
    .modify(_.id) {
      _.encodedExample(Resource.id.asJson.toString)
        .description(idDescription)
    }
    .modify(_.originalId) {
      _.encodedExample(AudioPlayExamples.Resource.id.asJson.toString)
        .description(originalIdDescription)
    }
    .modify(_.title) {
      _.encodedExample(Resource.title.asJson.toString)
        .description(titleDescription)
    }

  given Schema[CreateAudioPlayTranslationRequest] = Schema
    .derived[CreateAudioPlayTranslationRequest]
    .modify(_.title) {
      _.encodedExample(CreateRequest.title.asJson.toString)
        .description(titleDescription)
    }

  given Schema[ListAudioPlayTranslationsRequest] = Schema
    .derived[ListAudioPlayTranslationsRequest]
    .modify(_.pageSize) {
      _.description(pageSizeDescription)
    }
    .modify(_.pageToken) {
      _.description(pageTokenDescription)
    }

  given Schema[ListAudioPlayTranslationsResponse] = Schema
    .derived[ListAudioPlayTranslationsResponse]
    .modify(_.nextPageToken) {
      _.encodedExample(ListResponse.nextPageToken)
        .description(nextPageDescription)
    }

  given Schema[AudioPlayTranslationLocationResource] = Schema.derived

  private val idDescription = "UUID of the translation."
  private val originalIdDescription =
    "UUID of the original audio play for which this is a translation."
  private val titleDescription = "Translated version of audio play's title."
  private val translationTypeDescription = "Type of translation: one of " +
    AudioPlayTranslationTypeMapper.stringValues.mkString(", ")
  private val pageSizeDescription = "Desirable number of elements in response."
  private val pageTokenDescription =
    "Page token to continue previously started listing."
  private val nextPageDescription = "Token to retrieve next page."

  private given Schema[AudioPlayTranslationTypeDTO] = Schema.string
    .validate(
      Validator
        .enumeration(AudioPlayTranslationTypeDTO.values.toList)
        .encode(AudioPlayTranslationTypeMapper.toString))
    .encodedExample(
      AudioPlayTranslationTypeMapper
        .toString(Resource.translationType)
        .asJson
        .toString)
    .description(translationTypeDescription)
