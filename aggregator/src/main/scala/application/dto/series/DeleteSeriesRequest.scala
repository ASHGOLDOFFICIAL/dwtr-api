package org.aulune.aggregator
package application.dto.series

import java.util.UUID


/** Request to delete a series.
 *  @param name resource identifier.
 */
final case class DeleteSeriesRequest(
    name: UUID,
)
