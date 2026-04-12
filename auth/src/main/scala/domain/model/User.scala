package org.aulune.auth
package domain.model


import domain.errors.UserValidationError
import domain.model.User.ValidationResult
import domain.model.Username

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given
import org.aulune.commons.types.Uuid


/** User representation.
 *  @param id user's unique UUID.
 *  @param username unique username.
 *  @param hashedPassword password hash (if user has it).
 *  @param googleId ID in Google's services (if user linked accounts).
 */
final case class User private (
    id: Uuid[User],
    username: Username,
    hashedPassword: Option[String],
):
  /** Copies with validation. */
  def update(
      id: Uuid[User] = id,
      username: Username = username,
      hashedPassword: Option[String] = hashedPassword,
  ): ValidationResult[User] = User(
    id = id,
    username = username,
    hashedPassword = hashedPassword,
  )


object User:
  private type ValidationResult[A] = ValidatedNec[UserValidationError, A]

  /** Creates user using only required fields. Other fields are initialized as
   *  `None`.
   *  @param id user's UUID.
   *  @param username unique username.
   *  @return user validation result.
   */
  def create(
      id: Uuid[User],
      username: Username,
  ): ValidationResult[User] = User(
    id = id,
    username = username,
    hashedPassword = None,
  )

  /** Returns a user if all given arguments are valid.
   *  @param id UUID assigned to user.
   *  @param username unique username.
   *  @return user validation result.
   */
  def apply(
      id: Uuid[User],
      username: Username,
      hashedPassword: Option[String],
  ): ValidationResult[User] = validateState(
    new User(id = id, username = username, hashedPassword = hashedPassword))

  /** Unsafe constructor for always valid boundary.
   *  @throws UserValidationError if arguments are invalid.
   */
  def unsafe(
      id: Uuid[User],
      username: Username,
      hashedPassword: Option[String],
  ): User =
    User(id = id, username = username, hashedPassword = hashedPassword) match
      case Validated.Valid(a)   => a
      case Validated.Invalid(e) => throw e.head

  /** Validates object state.
   *  @param user user to validate.
   *  @return validation result.
   */
  private def validateState(user: User): ValidationResult[User] = user.validNec
