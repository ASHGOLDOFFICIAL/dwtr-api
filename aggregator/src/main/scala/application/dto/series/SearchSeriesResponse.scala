package org.aulune.aggregator
package application.dto.series


/** Response to search request.
 *  @param series suggested series.
 */
final case class SearchSeriesResponse(
    series: List[SeriesResource],
)
