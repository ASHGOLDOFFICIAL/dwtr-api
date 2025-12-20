package org.aulune.aggregator
package domain.repositories


import domain.model.person.Person
import domain.repositories.PersonRepository.Cursor

import org.aulune.commons.pagination.cursor.{CursorDecoder, CursorEncoder}
import org.aulune.commons.repositories.{
  BatchGet,
  GenericRepository,
  PaginatedList,
  TextSearch,
}
import org.aulune.commons.types.Uuid


/** Repository for [[Person]] objects.
 *  @tparam F effect type.
 */
trait PersonRepository[F[_]]
    extends GenericRepository[F, Person, Uuid[Person]]
    with BatchGet[F, Person, Uuid[Person]]
    with PaginatedList[F, Person, Cursor]
    with TextSearch[F, Person]


object PersonRepository:
  /** Cursor to resume pagination.
   *  @param id identity of last entry.
   */
  final case class Cursor(id: Uuid[Person]) derives CursorEncoder, CursorDecoder
