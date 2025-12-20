package org.aulune.aggregator
package application.dto.work


/** Request to search works.
 *  @param query query string.
 *  @param limit maximum number of elements required.
 */
final case class SearchWorksRequest(
    query: String,
    limit: Option[Int],
)
