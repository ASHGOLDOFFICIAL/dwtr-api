package org.aulune.aggregator
package adapters.service


import application.AggregatorPermission.Modify
import application.PersonService
import application.dto.person.{BatchGetPersonsRequest, CreatePersonRequest, DeletePersonRequest, GetPersonRequest, PersonResource}
import application.errors.PersonServiceError.{InvalidPerson, PersonNotFound}
import domain.model.person.Person
import domain.repositories.PersonRepository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.given
import org.aulune.aggregator.adapters.service.testdata.Persons
import org.aulune.commons.errors.ErrorStatus.PermissionDenied
import org.aulune.commons.errors.{ErrorResponse, ErrorStatus}
import org.aulune.commons.service.auth.User
import org.aulune.commons.service.permission.{Permission, PermissionClientService}
import org.aulune.commons.testing.ErrorAssertions.{assertDomainError, assertErrorStatus, assertInternalError}
import org.aulune.commons.testing.instances.UUIDGenInstances
import org.aulune.commons.testing.syntax.{getRight, getValid}
import org.aulune.commons.typeclasses.SortableUUIDGen
import org.aulune.commons.types.Uuid
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.Assertion
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory

import java.util.UUID


/** Tests for [[PersonServiceImpl]]. */
final class PersonServiceImplSpec
    extends AsyncFlatSpec
    with AsyncIOSpec
    with Matchers
    with AsyncMockFactory:

  import Helpers.*
  import Samples.*

  private given LoggerFactory[IO] = NoOpFactory.impl[IO]
  private given SortableUUIDGen[IO] = UUIDGenInstances.makeFixedUuidGen()

  private val mockRepo = mock[PersonRepository[IO]]
  private val mockPermissions = mock[PermissionClientService[IO]]

  private def stand(
      testCase: PersonService[IO] => IO[Assertion],
  ): IO[Assertion] =
    val _ = (mockPermissions.registerPermission _)
      .expects(*)
      .returning(().asRight.pure)
    PersonServiceImpl
      .build(
        2,
        AggregatorConfig.PaginationParams(2, 1),
        AggregatorConfig.SearchParams(2, 1),
        mockRepo,
        mockPermissions)
      .flatMap(testCase)
  end stand

  behavior of "get"

  it should "find persons if they're present in repository" in stand {
    service =>
      val _ = mockGet(person.some.pure)
      for result <- service.get(getRequest)
      yield result shouldBe personResource.asRight
  }

  it should "result in PersonNotFound if person doesn't exist" in stand {
    service =>
      val _ = mockGet(None.pure)
      val find = service.get(getRequest)
      assertDomainError(find)(PersonNotFound)
  }

  it should "handle errors from repository gracefully" in stand { service =>
    val _ = mockGet(IO.raiseError(new Throwable()))
    val find = service.get(getRequest)
    assertInternalError(find)
  }

  behavior of "batchGet"

  it should "return batch of elements when all are found" in stand { service =>
    val ids = people.map(_.id)
    val _ = (mockRepo.batchGet _)
      .expects(ids)
      .returning(people.toList.pure)

    for result <- service.batchGet(batchGetRequest)
    yield result.getRight.persons.map(_.id) shouldBe ids.toList
  }

  it should "return PersonNotFound if at least one person is not found" in stand {
    service =>
      val _ = (mockRepo.batchGet _)
        .expects(people.map(_.id))
        .returning(people.take(1).pure)

      val batchGet = service.batchGet(batchGetRequest)
      assertDomainError(batchGet)(PersonNotFound)
  }

  behavior of "create"

  it should "allow users with permissions to create persons if none exist" in stand {
    service =>
      val newPerson = person
        .update(id = Uuid[Person](UUIDGenInstances.fixedUuid))
        .getValid
      val _ = mockHasPermission(Modify, true.asRight.pure)
      val _ = (mockRepo.persist _).expects(newPerson).returning(newPerson.pure)

      for result <- service.create(user, createRequest)
      yield result shouldBe personResource
        .copy(id = UUIDGenInstances.fixedUuid)
        .asRight
  }

  it should "result in InvalidArgument when creating person with empty name" in stand {
    service =>
      val emptyNameRequest = CreatePersonRequest(name = "")
      val _ = mockHasPermission(Modify, true.asRight.pure)
      val find = service.create(user, emptyNameRequest)
      assertDomainError(find)(InvalidPerson)
  }

  it should "result in PermissionDenied for unauthorized users" in stand {
    service =>
      val _ = mockHasPermission(Modify, false.asRight.pure)
      val find = service.create(user, createRequest)
      assertErrorStatus(find)(PermissionDenied)
  }

  it should "handle exceptions from hasPermission gracefully" in stand {
    service =>
      val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
      val find = service.create(user, createRequest)
      assertInternalError(find)
  }

  it should "handle exceptions from persist gracefully" in stand { service =>
    val newPerson = person
      .update(id = Uuid[Person](UUIDGenInstances.fixedUuid))
      .getValid
    val _ = mockHasPermission(Modify, true.asRight.pure)
    val _ = (mockRepo.persist _)
      .expects(newPerson)
      .returning(IO.raiseError(new Throwable()))

    val find = service.create(user, createRequest)
    assertInternalError(find)
  }

  behavior of "delete"

  it should "allow users with permissions to delete existing persons" in stand {
    service =>
      val _ = mockHasPermission(Modify, true.asRight.pure)
      val _ = mockDelete(().pure)
      for result <- service.delete(user, deleteRequest)
      yield result shouldBe ().asRight
  }

  it should "result in PermissionDenied for unauthorized users" in stand {
    service =>
      val _ = mockHasPermission(Modify, false.asRight.pure)
      val delete = service.delete(user, deleteRequest)
      assertErrorStatus(delete)(PermissionDenied)
  }

  it should "handle exceptions from hasPermission gracefully" in stand {
    service =>
      val _ = mockHasPermission(Modify, IO.raiseError(new Throwable()))
      val delete = service.delete(user, deleteRequest)
      assertInternalError(delete)
  }

  it should "handle exceptions from delete gracefully" in stand { service =>
    val _ = mockHasPermission(Modify, true.asRight.pure)
    val _ = mockDelete(IO.raiseError(new Throwable()))
    val delete = service.delete(user, deleteRequest)
    assertInternalError(delete)
  }

  object Helpers:
    def mockGet(returning: IO[Option[Person]]): Any =
      (mockRepo.get _).expects(person.id).returning(returning)

    def mockDelete(returning: IO[Unit]): Any =
      (mockRepo.delete _).expects(person.id).returning(returning)

    def mockHasPermission(
        permission: Permission,
        returning: IO[Either[ErrorResponse, Boolean]],
    ): Any = (mockPermissions.hasPermission _)
      .expects(user, permission)
      .returning(returning)

  end Helpers

  object Samples:
    val user: User = User(
      id = UUID.fromString("f04eb510-229c-4cdd-bd7b-9691c3b28ae1"),
      username = "username",
    )
    val people: NonEmptyList[Person] =
      NonEmptyList.of(Persons.person1, Persons.person2)
    val person: Person = people.head

    val personResource: PersonResource =
      PersonResource(id = person.id, name = person.name)
    val getRequest: GetPersonRequest = GetPersonRequest(person.id)
    val batchGetRequest: BatchGetPersonsRequest =
      BatchGetPersonsRequest(names = people.map(_.id).toList)
    val createRequest: CreatePersonRequest =
      CreatePersonRequest(name = person.name)
    val deleteRequest: DeletePersonRequest = DeletePersonRequest(person.id)
