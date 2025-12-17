package org.aulune.aggregator
package application.dto.audioplay.translation

import org.aulune.commons.types.NonEmptyString


/** Body of request to list audio play translations.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 *  @param filter optional filter expression.
 */
final case class ListAudioPlayTranslationsRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
    filter: Option[NonEmptyString],
)
