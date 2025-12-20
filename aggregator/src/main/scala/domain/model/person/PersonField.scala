package org.aulune.aggregator
package domain.model.person


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[Person]] that are allowed to be filtered on. */
enum PersonField:
  case Id


object PersonField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[PersonField]:
    override def fromName(
        name: NonEmptyString,
    ): Option[PersonField] = name match
      case s if s == "id" => Some(Id)
      case _              => None

    override def allows(
        a: PersonField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case PersonField.Id => FilterField.stringAllows(operation, value)

  given FilterField[PersonField] = Field
