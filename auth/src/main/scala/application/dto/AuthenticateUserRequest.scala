package org.aulune.auth
package application.dto


/** Authentication request body. */
enum AuthenticateUserRequest:
  /** Authentication via username and password.
   *  @param username username of user trying to log in.
   *  @param password user's password.
   */
  case BasicAuthentication(username: String, password: String)
