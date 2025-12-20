package org.aulune.aggregator
package application.dto.series


/** Response to list request series.
 *  @param series list of series.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListSeriesResponse(
    series: List[SeriesResource],
    nextPageToken: Option[String],
)
