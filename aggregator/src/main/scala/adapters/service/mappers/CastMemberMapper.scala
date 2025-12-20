package org.aulune.aggregator
package adapters.service.mappers


import application.dto.work.WorkResource.CastMemberResource
import application.dto.work.CastMemberDTO
import application.dto.person.PersonResource
import domain.model.work.{ActorRole, CastMember}
import domain.model.person.Person

import cats.syntax.all.given
import org.aulune.commons.types.Uuid


/** Mapper between external [[CastMemberResource]] and domain's [[CastMember]].
 *
 *  @note Should not be used outside `service` package to not expose domain
 *    type.
 */
private[service] object CastMemberMapper:
  /** Converts request to domain object and verifies it.
   *  @param dto cast member DTO.
   *  @return created domain object if valid.
   */
  def fromDTO(dto: CastMemberDTO): Option[CastMember] =
    for
      roles <- dto.roles.traverse(ActorRole.apply)
      cast <- CastMember(
        actor = Uuid[Person](dto.actor),
        roles = roles,
        main = dto.main)
    yield cast

  /** Converts domain object to response object.
   *  @param domain entity to use as a base.
   */
  def toResource(
      domain: CastMember,
      person: PersonResource,
  ): CastMemberResource = CastMemberResource(
    actor = person,
    roles = domain.roles,
    main = domain.main,
  )
