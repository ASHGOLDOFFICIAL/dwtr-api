package org.aulune.aggregator
package application.dto.work

import java.util.UUID


/** Request to delete a work.
 *  @param name resource identifier.
 */
final case class DeleteWorkRequest(
    name: UUID,
)
