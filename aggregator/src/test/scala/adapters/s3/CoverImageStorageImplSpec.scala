package org.aulune.aggregator
package adapters.s3


import domain.model.shared.ImageUri

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import cats.syntax.all.given
import fs2.Stream
import org.aulune.commons.storages.StorageError
import org.aulune.commons.testing.MinIOTestContainer
import org.aulune.commons.testing.syntax.getLeft
import org.aulune.commons.types.NonEmptyString
import org.http4s.Method.HEAD
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{Request, Uri}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpFactory


/** Tests for [[CoverImageStorageImpl]]. */
final class CoverImageStorageImplSpec
    extends AsyncFlatSpec
    with AsyncIOSpec
    with Matchers
    with MinIOTestContainer:

  private given LoggerFactory[IO] = NoOpFactory.impl[IO]

  private def stand = makeStand { (client, endpoint) =>
    CoverImageStorageImpl.build[IO](
      client = client,
      publicUrl = endpoint,
      bucketName = "objects",
      partSize = CoverImageStorageImpl.MinPartSize,
    )
  }

  private val name = NonEmptyString.unsafe("test")
  private val bytes = List[Byte](1, 2, 3)
  private val stream = Stream.emits(bytes)

  behavior of "contains"

  it should "return true for existing objects" in stand { storage =>
    for
      _ <- storage.put(stream, name, None)
      exists <- storage.contains(name)
    yield exists shouldBe true
  }

  it should "return false for non-existent objects" in stand { storage =>
    for exists <- storage.contains(name)
    yield exists shouldBe false
  }

  behavior of "put"

  it should "put objects" in stand { storage =>
    for
      _ <- storage.put(stream, name, None)
      resultO <- storage.get(name)
      result <- resultO match
        case Some(s) => s.compile.toList
        case None    => fail("Expected stream.")
    yield result shouldBe bytes
  }

  it should "throw error when object name is taken" in stand { storage =>
    for
      _ <- storage.put(stream, name, None)
      result <- storage.put(stream, name, None).attempt
    yield result.getLeft shouldBe StorageError.AlreadyExists
  }

  behavior of "get"

  it should "return None for non-existing objects" in stand { storage =>
    for result <- storage.get(name)
    yield result shouldBe None
  }

  behavior of "delete"

  it should "delete objects" in stand { storage =>
    for
      _ <- storage.put(stream, name, None)
      _ <- storage.delete(name)
      result <- storage.get(name)
    yield result shouldBe None
  }

  it should "be idempotent" in stand { storage =>
    for
      _ <- storage.delete(name)
      _ <- storage.delete(name)
      result <- storage.get(name)
    yield result shouldBe None
  }

  behavior of "issueURI"

  it should "return a URI that points to existing resource" in stand { storage =>
    clientResource.use { client =>
      for
        _ <- storage.put(stream, name, None)
        uriO <- storage.issueURI(name)
        uri = uriO.getOrElse(fail("URI was expected."))
        response <- makeHeadRequest(client, uri)
      yield response shouldBe 200
    }
  }

  it should "return nothing if objects doesn't exist" in stand { storage =>
    clientResource.use { client =>
      for result <- storage.issueURI(name)
      yield result shouldBe None
    }
  }

  val clientResource: Resource[IO, Client[IO]] =
    EmberClientBuilder.default[IO].build

  /** Makes HEAD request to given URI and returns status code. */
  def makeHeadRequest(client: Client[IO], uri: ImageUri): IO[Int] =
    val http4sUri = Uri
      .fromString(uri.toString)
      .getOrElse(fail("Invalid URI."))
    client.run(Request[IO](HEAD, http4sUri)).use {
      _.status.code.pure
    }
