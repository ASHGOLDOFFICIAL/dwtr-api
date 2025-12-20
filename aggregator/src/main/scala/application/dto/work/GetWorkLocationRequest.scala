package org.aulune.aggregator
package application.dto.work

import java.util.UUID


/** Request to get a self-hosted location for given work.
 *  @param name resource identifier.
 */
final case class GetWorkLocationRequest(
    name: UUID,
)
