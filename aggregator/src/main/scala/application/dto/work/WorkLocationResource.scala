package org.aulune.aggregator
package application.dto.work

import java.net.URI


/** Self-hosted location where this work can be consumed.
 *  @param uri location of self-hosted place.
 */
final case class WorkLocationResource(
    uri: URI,
)
