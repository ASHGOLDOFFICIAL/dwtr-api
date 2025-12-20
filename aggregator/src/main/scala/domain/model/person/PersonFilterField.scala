package org.aulune.aggregator
package domain.model.person


import org.aulune.commons.filter.{Filter, FilterField}
import org.aulune.commons.types.NonEmptyString


/** Fields of [[Person]] that are allowed to be filtered on. */
enum PersonFilterField:
  case Id


object PersonFilterField:
  /** [[FilterField]] instance for filtering. */
  private object Field extends FilterField[PersonFilterField]:
    override def fromName(
        name: NonEmptyString,
    ): Option[PersonFilterField] = name match
      case s if s == "id" => Some(Id)
      case _              => None

    override def allows(
        a: PersonFilterField,
    )(operation: Filter.Operator, value: Filter.Literal): Boolean = a match
      case PersonFilterField.Id => FilterField.stringAllows(operation, value)

  given FilterField[PersonFilterField] = Field
