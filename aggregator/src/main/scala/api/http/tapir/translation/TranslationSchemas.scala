package org.aulune.aggregator
package api.http.tapir.translation


import api.http.tapir.SharedSchemas.given
import api.http.tapir.work.WorkExamples
import api.http.tapir.translation.TranslationExamples.{
  CreateRequest,
  ListResponse,
  Resource,
}
import api.mappers.{LanguageMapper, TranslationTypeMapper}
import application.dto.shared.LanguageDTO
import application.dto.translation.{
  CreateTranslationRequest,
  ListTranslationsRequest,
  ListTranslationsResponse,
  TranslationLocationResource,
  TranslationResource,
  TranslationTypeDTO,
}

import io.circe.syntax.given
import org.aulune.commons.adapters.tapir.CommonTypeCodecs.nonEmptyStringSchema
import sttp.tapir.{Schema, Validator}

import java.net.URI
import java.util.UUID


object TranslationSchemas:
  given Schema[TranslationResource] = Schema
    .derived[TranslationResource]
    .modify(_.id) {
      _.encodedExample(Resource.id.asJson.toString)
        .description(idDescription)
    }
    .modify(_.originalId) {
      _.encodedExample(WorkExamples.Resource.id.asJson.toString)
        .description(originalIdDescription)
    }
    .modify(_.title) {
      _.encodedExample(Resource.title.asJson.toString)
        .description(titleDescription)
    }

  given Schema[CreateTranslationRequest] = Schema
    .derived[CreateTranslationRequest]
    .modify(_.title) {
      _.encodedExample(CreateRequest.title.asJson.toString)
        .description(titleDescription)
    }

  given Schema[ListTranslationsRequest] = Schema
    .derived[ListTranslationsRequest]
    .modify(_.pageSize) {
      _.description(pageSizeDescription)
    }
    .modify(_.pageToken) {
      _.description(pageTokenDescription)
    }

  given Schema[ListTranslationsResponse] = Schema
    .derived[ListTranslationsResponse]
    .modify(_.nextPageToken) {
      _.encodedExample(ListResponse.nextPageToken)
        .description(nextPageDescription)
    }

  given Schema[TranslationLocationResource] = Schema.derived

  private val idDescription = "UUID of the translation."
  private val originalIdDescription =
    "UUID of the original work for which this is a translation."
  private val titleDescription = "Translated version of title."
  private val translationTypeDescription = "Type of translation: one of " +
    TranslationTypeMapper.stringValues.mkString(", ")
  private val pageSizeDescription = "Desirable number of elements in response."
  private val pageTokenDescription =
    "Page token to continue previously started listing."
  private val nextPageDescription = "Token to retrieve next page."

  private given Schema[TranslationTypeDTO] = Schema.string
    .validate(
      Validator
        .enumeration(TranslationTypeDTO.values.toList)
        .encode(TranslationTypeMapper.toString))
    .encodedExample(
      TranslationTypeMapper
        .toString(Resource.translationType)
        .asJson
        .toString)
    .description(translationTypeDescription)
