package org.aulune.aggregator
package application.dto.series

import java.util.UUID


/** Series response body.
 *  @param id unique series ID.
 *  @param name series name.
 */
final case class SeriesResource(
    id: UUID,
    name: String,
)
