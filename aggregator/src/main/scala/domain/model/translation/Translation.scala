package org.aulune.aggregator
package domain.model.translation


import domain.errors.TranslationValidationError
import domain.model.work.Work
import Translation.ValidationResult
import domain.model.shared.{ExternalResource, Language, SelfHostedLocation}

import cats.data.{NonEmptyList, Validated, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.types.Uuid

import java.net.URI


/** Translation representation.
 *  @param originalId original work's ID.
 *  @param id translation ID.
 *  @param title translated title.
 *  @param translationType translation type.
 *  @param language translation language.
 *  @param selfHostedLocation link to self-hosted place where this translation
 *    can be consumed.
 *  @param externalResources links to different resources.
 */
final case class Translation private (
    originalId: Uuid[Work],
    id: Uuid[Translation],
    title: TranslatedTitle,
    translationType: TranslationType,
    language: Language,
    selfHostedLocation: Option[SelfHostedLocation],
    externalResources: List[ExternalResource],
):

  /** Copies with validation.
   *  @return new state validation result.
   */
  def update(
      originalId: Uuid[Work] = originalId,
      id: Uuid[Translation] = id,
      title: TranslatedTitle = title,
      translationType: TranslationType = translationType,
      language: Language = language,
      selfHostedLocation: Option[SelfHostedLocation] = selfHostedLocation,
      externalResources: List[ExternalResource] = externalResources,
  ): ValidationResult[Translation] = Translation(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
  )


object Translation:
  private type ValidationResult[A] = ValidatedNec[TranslationValidationError, A]

  /** Creates a translation with state validation.
   *  @param originalId original work ID.
   *  @param id ID.
   *  @param title translated title.
   *  @param translationType translation type.
   *  @param language translation language.
   *  @param selfHostedLocation link to self-hosted place where this translation
   *    can be consumed.
   *  @param externalResources links to different resources.
   *  @return translation validation result.
   */
  def apply(
      originalId: Uuid[Work],
      id: Uuid[Translation],
      title: TranslatedTitle,
      translationType: TranslationType,
      language: Language,
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): ValidationResult[Translation] = validateState(
    new Translation(
      originalId = originalId,
      id = id,
      title = title,
      translationType = translationType,
      language = language,
      selfHostedLocation = selfHostedLocation,
      externalResources = externalResources,
    ))

  /** Unsafe constructor to use only inside always-valid boundary.
   *  @throws TranslationValidationError if constructs invalid object.
   */
  def unsafe(
      originalId: Uuid[Work],
      id: Uuid[Translation],
      title: TranslatedTitle,
      translationType: TranslationType,
      language: Language,
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): Translation = Translation(
    originalId = originalId,
    id = id,
    title = title,
    translationType = translationType,
    language = language,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
  ) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head

  /** Validates translation state.
   *  @param translation translation to validate.
   *  @return validation result.
   */
  private def validateState(
      translation: Translation,
  ): ValidationResult[Translation] = translation.validNec
