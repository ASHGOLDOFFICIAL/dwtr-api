package org.aulune.aggregator
package domain.repositories


import domain.model.person.{Person, PersonField}

import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{
  BatchGet,
  FilterList,
  GenericRepository,
  TextSearch,
}
import org.aulune.commons.types.Uuid


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */
trait PersonRepository[F[_]]
    extends GenericRepository[F, Person, Uuid[Person]]
    with BatchGet[F, Person, Uuid[Person]]
    with FilterList[F, Person, PersonField]
    with TextSearch[F, Person]
