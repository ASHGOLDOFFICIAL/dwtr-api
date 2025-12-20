package org.aulune.commons
package adapters.doobie.queries


import filter.Filter
import filter.instances.DoobieFragmentFilterEvaluator
import repositories.RepositoryError
import types.NonEmptyString

import cats.syntax.either.given
import cats.syntax.traverse.given
import doobie.Fragment
import doobie.syntax.all.given


/** Makes SQL queries for list operations.
 *  @param selectBase SQL fragment with `SELECT {...fields} FROM table`.
 *  @param columnName function that transforms filter field to string to use in
 *    query.
 *  @tparam F filter fields.
 */
final class ListQueryMaker[F](selectBase: Fragment)(
    columnName: F => NonEmptyString,
):
  /** Makes query for given amount of elements and filter expression.
   *  @param count needed amount of elements.
   *  @param filter filter expression tree.
   *  @return query if everything is valid and supported.
   */
  def make(
      count: Int,
      filter: Option[Filter[F]],
  ): Either[RepositoryError, Fragment] = filter
    .traverse(filterEvaluator.eval)
    .map { optFilterFragment =>
      val where = optFilterFragment.fold(Fragment.empty)(fr"WHERE" ++ _)
      selectBase ++ where ++ fr0" LIMIT $count"
    }
    .leftMap(_ => RepositoryError.InvalidArgument)

  private val filterEvaluator: DoobieFragmentFilterEvaluator[F] =
    DoobieFragmentFilterEvaluator[F](columnName)
