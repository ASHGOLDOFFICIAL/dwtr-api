package org.aulune.aggregator
package application.dto.work


/** Response to search request.
 *  @param works suggested works.
 */
final case class SearchWorksResponse(
    works: List[WorkResource],
)
