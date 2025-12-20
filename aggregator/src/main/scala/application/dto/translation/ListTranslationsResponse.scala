package org.aulune.aggregator
package application.dto.translation


/** Response to list request.
 *  @param translations list of translations.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListTranslationsResponse(
    translations: List[TranslationResource],
    nextPageToken: Option[String],
)
