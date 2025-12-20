package org.aulune.aggregator
package application.dto.work


/** Body of request to list works.
 *  @param pageSize maximum expected number of elements.
 *  @param pageToken token to retrieve next page.
 */
final case class ListWorksRequest(
    pageSize: Option[Int],
    pageToken: Option[String],
)
