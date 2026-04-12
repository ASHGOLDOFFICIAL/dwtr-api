package org.aulune.auth
package adapters.jdbc.postgres


import domain.model.Username

import doobie.Meta


/** [[Meta]] instances for user object. */
private[postgres] object UserMetas:
  given Meta[Username] = Meta[String].imap(Username.unsafe)(identity)
