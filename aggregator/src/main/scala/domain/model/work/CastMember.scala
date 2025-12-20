package org.aulune.aggregator
package domain.model.work


import domain.model.person.Person

import org.aulune.commons.types.Uuid


/** Cast member representation.
 *
 *  Following conditions must be true:
 *    - roles should be non-empty.
 *    - roles should not have duplicated.
 *
 *  @param actor ID of actor (cast member) as a person.
 *  @param roles roles this actor performed.
 *  @param main is this cast member considered part of main cast.
 */
final case class CastMember private (
    actor: Uuid[Person],
    roles: List[ActorRole],
    main: Boolean,
)


object CastMember:
  /** Returns [[CastMember]] if arguments are valid, i.e.:
   *    - roles should be non-empty.
   *    - roles should not have duplicated.
   *  @param actor ID of actor (cast member) as a person.
   *  @param roles roles this actor performed.
   *  @param main is this cast member considered part of main cast.
   */
  def apply(
      actor: Uuid[Person],
      roles: List[ActorRole],
      main: Boolean,
  ): Option[CastMember] = validateState(
    new CastMember(actor = actor, roles = roles, main = main),
  )

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param actor ID of actor (cast member) as a person.
   *  @param roles roles this actor performed.
   *  @param main is this cast member considered part of main cast.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(
      actor: Uuid[Person],
      roles: List[ActorRole],
      main: Boolean,
  ): CastMember = CastMember(actor = actor, roles = roles, main = main) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()

  /** Validates cast member state, i.e.:
   *    - roles should be non-empty.
   *    - roles should not have duplicated.
   *  @param member cast member to be validated.
   *  @return `None` if invalid.
   */
  private def validateState(member: CastMember): Option[CastMember] =
    val roleSet = member.roles.toSet
    val noDuplicates = roleSet.size == member.roles.size
    Option.when(member.roles.nonEmpty && noDuplicates)(member)
