package org.aulune.aggregator
package application.dto.translation

import org.aulune.commons.types.NonEmptyString


/** Body of request to list translations.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 *  @param filter optional filter expression.
 */
final case class ListTranslationsRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
    filter: Option[NonEmptyString],
)
