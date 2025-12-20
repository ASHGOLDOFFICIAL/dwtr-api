package org.aulune.aggregator
package application.dto.series


/** Request to search series.
 *  @param query query string.
 *  @param limit maximum number of elements required.
 */
final case class SearchSeriesRequest(
    query: String,
    limit: Option[Int],
)
