package org.aulune.aggregator
package application.dto.series


/** Response to batch get of series.
 *  @param works found series.
 */
final case class BatchGetSeriesResponse(
    works: List[SeriesResource],
)
