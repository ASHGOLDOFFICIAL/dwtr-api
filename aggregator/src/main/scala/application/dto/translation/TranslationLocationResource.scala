package org.aulune.aggregator
package application.dto.translation

import java.net.URI


/** Self-hosted location where this translation can be consumed.
 *  @param uri location of self-hosted place.
 */
final case class TranslationLocationResource(
    uri: URI,
)
