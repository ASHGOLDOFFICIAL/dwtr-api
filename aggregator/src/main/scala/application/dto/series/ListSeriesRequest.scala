package org.aulune.aggregator
package application.dto.series


/** Body of request to list series.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 */
final case class ListSeriesRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
)
