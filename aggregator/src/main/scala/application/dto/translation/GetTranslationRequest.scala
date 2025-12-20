package org.aulune.aggregator
package application.dto.translation

import java.util.UUID


/** Request to get a translation. */
final case class GetTranslationRequest(
    name: UUID,
)
