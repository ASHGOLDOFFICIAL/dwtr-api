package org.aulune.commons
package pagination.params


/** Parameters to use for cursor-based pagination.
 *  @param pageSize page size.
 *  @param cursor cursor to use resume listing.
 *  @tparam Cursor cursor type.
 */
final case class PaginationParams[Cursor] private[pagination] (
    pageSize: Int,
    cursor: Option[Cursor],
)
