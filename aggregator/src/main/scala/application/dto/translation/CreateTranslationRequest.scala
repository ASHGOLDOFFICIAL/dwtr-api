package org.aulune.aggregator
package application.dto.translation


import application.dto.shared.{ExternalResourceDTO, LanguageDTO}

import java.net.URI
import java.util.UUID


/** Translation request body.
 *  @param originalId ID of original work this translation translates.
 *  @param title translated title.
 *  @param translationType type of translation.
 *  @param language translation language.
 *  @param selfHostedLocation link to self-hosted place where this translation
 *    can be consumed.
 *  @param externalResources links to external resources.
 */
final case class CreateTranslationRequest(
    originalId: UUID,
    title: String,
    translationType: TranslationTypeDTO,
    language: LanguageDTO,
    selfHostedLocation: Option[URI],
    externalResources: List[ExternalResourceDTO],
)
