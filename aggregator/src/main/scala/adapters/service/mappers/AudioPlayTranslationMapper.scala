package org.aulune.aggregator
package adapters.service.mappers


import application.dto.audioplay.translation.{
  AudioPlayTranslationResource,
  CreateAudioPlayTranslationRequest,
  ListAudioPlayTranslationsResponse,
}
import domain.errors.TranslationValidationError
import domain.model.audioplay.AudioPlay
import domain.model.audioplay.translation.AudioPlayTranslation
import domain.model.shared.{SelfHostedLocation, TranslatedTitle}
import domain.repositories.AudioPlayTranslationRepository.AudioPlayTranslationCursor

import cats.data.{NonEmptyList, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.pagination.cursor.CursorEncoder
import org.aulune.commons.types.Uuid


/** Mapper between external audio play translation DTOs and domain's
 *  [[AudioPlayTranslation]].
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object AudioPlayTranslationMapper:
  /** Converts request to domain object and verifies it.
   *  @param request audio play request DTO.
   *  @param id ID assigned to this audio play translation.
   *  @return created domain object if valid.
   */
  def fromRequest(
      request: CreateAudioPlayTranslationRequest,
      id: Uuid[AudioPlayTranslation],
  ): ValidatedNec[TranslationValidationError, AudioPlayTranslation] = (for
    title <- TranslatedTitle(request.title)
    translationType = AudioPlayTranslationTypeMapper
      .toDomain(request.translationType)
    language = LanguageMapper.toDomain(request.language)
    location <- request.selfHostedLocation.map(SelfHostedLocation.apply)
    resources = request.externalResources.map(ExternalResourceMapper.toDomain)
  yield AudioPlayTranslation(
    originalId = Uuid[AudioPlay](request.originalId),
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = location,
    externalResources = resources,
  )).getOrElse(TranslationValidationError.InvalidArguments.invalidNec)

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   */
  def makeResource(domain: AudioPlayTranslation): AudioPlayTranslationResource =
    AudioPlayTranslationResource(
      originalId = domain.originalId,
      id = domain.id,
      title = domain.title,
      translationType = AudioPlayTranslationTypeMapper
        .fromDomain(domain.translationType),
      language = LanguageMapper.fromDomain(domain.language),
      externalResources = domain.externalResources
        .map(ExternalResourceMapper.fromDomain),
    )

  /** Converts list of domain objects to one list response.
   *  @param translations list of domain objects.
   */
  def toListResponse(
      translations: List[AudioPlayTranslation],
  ): ListAudioPlayTranslationsResponse =
    val nextPageToken = translations.lastOption.map { elem =>
      val cursor = AudioPlayTranslationCursor(elem.id)
      CursorEncoder[AudioPlayTranslationCursor].encode(cursor)
    }
    val elements = translations.map(makeResource)
    ListAudioPlayTranslationsResponse(elements, nextPageToken)
