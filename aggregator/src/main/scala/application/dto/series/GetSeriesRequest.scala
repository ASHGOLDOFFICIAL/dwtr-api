package org.aulune.aggregator
package application.dto.series

import java.util.UUID


/** Request to get a series.
 *  @param name resource identifier.
 */
final case class GetSeriesRequest(
    name: UUID,
)
