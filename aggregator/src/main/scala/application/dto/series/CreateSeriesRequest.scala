package org.aulune.aggregator
package application.dto.series


/** Series creation request body.
 *  @param name series name.
 */
final case class CreateSeriesRequest(
    name: String,
)
