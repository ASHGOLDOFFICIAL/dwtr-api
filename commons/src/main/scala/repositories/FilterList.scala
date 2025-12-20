package org.aulune.commons
package repositories


import filter.Filter
import repositories.RepositoryError.InvalidArgument


/** List operation for repository with support of filter expressions.
 *  @tparam F effect type.
 *  @tparam E element type.
 *  @tparam Fields type of fields used in filter expressions.
 */
trait FilterList[F[_], E, Fields]:
  /** List contained elements.
   *
   *  [[InvalidArgument]] will be returned when token or count are invalid.
   *
   *  @param count number of elements to return.
   *  @param filter optional filter expression.
   *  @return list of elements.
   */
  def list(count: Int, filter: Option[Filter[Fields]]): F[List[E]]
