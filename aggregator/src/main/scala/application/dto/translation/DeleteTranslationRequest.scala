package org.aulune.aggregator
package application.dto.translation

import java.util.UUID


/** Request to delete an audio play translation. */
final case class DeleteTranslationRequest(
    name: UUID,
)
