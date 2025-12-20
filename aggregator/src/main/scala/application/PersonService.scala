package org.aulune.aggregator
package application


import application.AggregatorPermission.Modify
import application.dto.person.{
  BatchGetPersonsRequest,
  BatchGetPersonsResponse,
  CreatePersonRequest,
  DeletePersonRequest,
  GetPersonRequest,
  ListPersonsRequest,
  ListPersonsResponse,
  PersonResource,
  SearchPersonsRequest,
  SearchPersonsResponse,
}
import application.errors.PersonServiceError.{InvalidPerson, PersonNotFound}

import org.aulune.commons.errors.ErrorResponse
import org.aulune.commons.service.auth.User


/** Service managing persons.
 *  @tparam F effect type.
 */
trait PersonService[F[_]]:
  /** Get person by given ID.
   *
   *  Domain error [[PersonNotFound]] will be returned if person is not found.
   *
   *  @param request request to get a person.
   *  @return requested person if found.
   */
  def get(request: GetPersonRequest): F[Either[ErrorResponse, PersonResource]]

  /** Gets persons by their identities in batches.
   *
   *  Persons are returned in the same order as in request.
   *
   *  Domain error [[PersonNotFound]] will be returned if any of the persons are
   *  not found.
   *
   *  @param request request with IDs.
   *  @return resources for every given ID or error.
   */
  def batchGet(
      request: BatchGetPersonsRequest,
  ): F[Either[ErrorResponse, BatchGetPersonsResponse]]

  /** Get a portion of persons.
   *
   *  @param request request to list persons.
   *  @return list of persons if success, otherwise error.
   */
  def list(
      request: ListPersonsRequest,
  ): F[Either[ErrorResponse, ListPersonsResponse]]

  /** Search persons by some query.
   *
   *  @param request request with search information.
   *  @return response with matched persons if success, otherwise error.
   */
  def search(
      request: SearchPersonsRequest,
  ): F[Either[ErrorResponse, SearchPersonsResponse]]

  /** Create new person.
   *
   *  Domain error [[InvalidPerson]] will be returned if request contains
   *  invalid person.
   *
   *  @param user user who performs this action.
   *  @param request person creation request.
   *  @return created person if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def create(
      user: User,
      request: CreatePersonRequest,
  ): F[Either[ErrorResponse, PersonResource]]

  /** Deletes existing person.
   *
   *  @param user user who performs this action.
   *  @param request request to delete a person.
   *  @return `Unit` if success, otherwise error.
   *  @note user must have [[Modify]] permission.
   */
  def delete(
      user: User,
      request: DeletePersonRequest,
  ): F[Either[ErrorResponse, Unit]]
