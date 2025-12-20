package org.aulune.aggregator
package application.dto.work

import java.util.UUID


/** Request to get a work.
 *  @param name resource identifier.
 */
final case class GetWorkRequest(
    name: UUID,
)
