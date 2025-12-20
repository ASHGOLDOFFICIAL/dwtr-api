package org.aulune.aggregator
package application.dto.work

import java.util.UUID


/** Cast member representation.
 *  @param actor ID of an actor (cast member).
 *  @param roles roles this actor performed.
 *  @param main is this cast member considered part of main cast.
 */
final case class CastMemberDTO(
    actor: UUID,
    roles: List[String],
    main: Boolean,
)
