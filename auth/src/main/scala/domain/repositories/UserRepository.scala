package org.aulune.auth
package domain.repositories


import domain.model.{User, Username}

import org.aulune.commons.repositories.GenericRepository
import org.aulune.commons.types.Uuid


/** Repository which stores [[User]] objects.
 *  @tparam F effect type.
 */
trait UserRepository[F[_]] extends GenericRepository[F, User, Uuid[User]]:

  /** Finds a user by their unique username.
   *  @param username user's unique username.
   *  @return user if found.
   */
  def getByUsername(username: Username): F[Option[User]]
