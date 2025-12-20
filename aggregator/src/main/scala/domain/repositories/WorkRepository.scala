package org.aulune.aggregator
package domain.repositories


import domain.model.work.{Work, WorkField}

import org.aulune.commons.repositories.{
  FilterList,
  GenericRepository,
  TextSearch,
}
import org.aulune.commons.types.Uuid


/** Repository for [[Work]] objects.
 *
 *  @tparam F effect type.
 */
trait WorkRepository[F[_]]
    extends GenericRepository[F, Work, Uuid[Work]]
    with FilterList[F, Work, WorkField]
    with TextSearch[F, Work]
