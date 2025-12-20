package org.aulune.aggregator
package domain.model.work

/** Role represented by the actor. */
opaque type ActorRole <: String = String


object ActorRole:
  /** Returns [[ActorRole]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param role actor part.
   */
  def apply(role: String): Option[ActorRole] =
    val stripped = role.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param role actor part.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(role: String): ActorRole = ActorRole(role) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
