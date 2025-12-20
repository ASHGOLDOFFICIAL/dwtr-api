package org.aulune.aggregator
package application.dto.translation

import java.util.UUID


/** Request to get a self-hosted location where this translation can be
 *  consumed.
 *  @param name resource identifier.
 */
final case class GetTranslationLocationRequest(
    name: UUID,
)
