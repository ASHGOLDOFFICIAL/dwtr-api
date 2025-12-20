package org.aulune.aggregator
package application.dto.series

import java.util.UUID


/** Request for batch get.
 *  @param names names of resources.
 */
final case class BatchGetSeriesRequest(
    names: List[UUID],
)
