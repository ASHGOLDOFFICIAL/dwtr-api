package org.aulune.aggregator
package api.http.tapir.translation


import api.http.tapir.work.WorkExamples
import application.dto.shared.ExternalResourceDTO
import application.dto.shared.ExternalResourceTypeDTO.Download
import application.dto.shared.LanguageDTO.Russian
import application.dto.translation.TranslationTypeDTO.Subtitles
import application.dto.translation.{
  CreateTranslationRequest,
  ListTranslationsRequest,
  ListTranslationsResponse,
  TranslationLocationResource,
  TranslationResource,
}

import java.net.URI
import java.util.{Base64, UUID}


object TranslationExamples:
  private val titleExample = "Но негодяи были пойманы"
  private val translationTypeExample = Subtitles
  private val languageExample = Russian
  private val externalResourcesExample = List(
    ExternalResourceDTO(
      Download,
      URI.create(
        "https://www.opensubtitles.com/ru/subtitles/though-scoundrels-are-discovered"),
    ),
  )
  private val selfHostedLocationExample = URI.create(
    "https://selfhosted.org:8096/stable/web/#/details?id=c846c7456e0f40978b726fea454b6a7c",
  )
  private val nextTokenExample =
    Some(Base64.getEncoder.encodeToString(titleExample.getBytes))

  val CreateRequest: CreateTranslationRequest = CreateTranslationRequest(
    originalId = WorkExamples.Resource.id,
    title = titleExample,
    translationType = translationTypeExample,
    language = languageExample,
    selfHostedLocation = None,
    externalResources = externalResourcesExample,
  )

  val Resource: TranslationResource = TranslationResource(
    originalId = WorkExamples.Resource.id,
    id = UUID.fromString("8f7c586f-7043-4e47-9021-45e41a9e6f9c"),
    title = titleExample,
    translationType = translationTypeExample,
    language = languageExample,
    externalResources = externalResourcesExample,
  )

  val ListResponse: ListTranslationsResponse = ListTranslationsResponse(
    translations = List(Resource),
    nextPageToken = nextTokenExample,
  )

  val GetSelfHostedLocationResponse: TranslationLocationResource =
    TranslationLocationResource(
      uri = selfHostedLocationExample,
    )
