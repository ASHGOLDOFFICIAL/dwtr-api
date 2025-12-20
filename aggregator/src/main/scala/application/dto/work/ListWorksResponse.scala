package org.aulune.aggregator
package application.dto.work


/** Response to list request.
 *  @param works list of works.
 *  @param nextPageToken token that can be sent to retrieve the next page.
 */
final case class ListWorksResponse(
    works: List[WorkResource],
    nextPageToken: Option[String],
)
