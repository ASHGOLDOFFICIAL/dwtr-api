package org.aulune.aggregator
package application.dto.translation


import application.dto.shared.{ExternalResourceDTO, LanguageDTO}

import java.net.URI
import java.util.UUID


/** Translation response body.
 *
 *  @param originalId original ID.
 *  @param id translation ID.
 *  @param title translated title.
 *  @param translationType type of translation.
 *  @param externalResources links to external resources.
 */
final case class TranslationResource(
    originalId: UUID,
    id: UUID,
    title: String,
    translationType: TranslationTypeDTO,
    language: LanguageDTO,
    externalResources: List[ExternalResourceDTO],
)
