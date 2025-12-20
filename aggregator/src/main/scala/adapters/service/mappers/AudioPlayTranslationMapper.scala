package org.aulune.aggregator
package adapters.service.mappers


import application.dto.translation.{
  CreateTranslationRequest,
  TranslationResource,
}
import domain.errors.TranslationValidationError
import domain.model.audioplay.AudioPlay
import domain.model.shared.SelfHostedLocation
import domain.model.translation.{TranslatedTitle, Translation}

import cats.data.ValidatedNec
import cats.syntax.all.given
import org.aulune.commons.types.Uuid


/** Mapper between external audio play translation DTOs and domain's
 *  [[Translation]].
 *
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
      request: CreateTranslationRequest,
      id: Uuid[Translation],
  ): ValidatedNec[TranslationValidationError, Translation] = (for
    title <- TranslatedTitle(request.title)
    translationType = AudioPlayTranslationTypeMapper
      .toDomain(request.translationType)
    language = LanguageMapper.toDomain(request.language)
    location <- request.selfHostedLocation.map(SelfHostedLocation.apply)
    resources = request.externalResources.map(ExternalResourceMapper.toDomain)
  yield Translation(
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
  def makeResource(domain: Translation): TranslationResource =
    TranslationResource(
      originalId = domain.originalId,
      id = domain.id,
      title = domain.title,
      translationType = AudioPlayTranslationTypeMapper
        .fromDomain(domain.translationType),
      language = LanguageMapper.fromDomain(domain.language),
      externalResources = domain.externalResources
        .map(ExternalResourceMapper.fromDomain),
    )
