package org.aulune.aggregator
package domain.model.work


import domain.errors.WorkValidationError
import domain.errors.WorkValidationError.*
import domain.model.person.Person
import domain.model.work.Work.ValidationResult
import domain.model.series.Series
import domain.model.shared.{
  ExternalResource,
  ImageUri,
  ReleaseDate,
  SelfHostedLocation,
  Synopsis,
}

import cats.data.{NonEmptyChain, Validated, ValidatedNec}
import org.aulune.commons.types.Uuid


/** Piece of media representation.
 *  @param id ID.
 *  @param title title.
 *  @param synopsis brief description.
 *  @param releaseDate release date.
 *  @param writers author(s).
 *  @param cast cast.
 *  @param seriesId series ID.
 *  @param seriesSeason season number.
 *  @param seriesNumber series number in season.
 *  @param episodeType type of episode in series.
 *  @param coverUri URL to cover.
 *  @param selfHostedLocation link to self-hosted place where it can be
 *    consumed.
 *  @param externalResources links to different resources.
 */
final case class Work private (
    id: Uuid[Work],
    title: Title,
    synopsis: Synopsis,
    releaseDate: ReleaseDate,
    writers: List[Uuid[Person]],
    cast: List[CastMember],
    seriesId: Option[Uuid[Series]],
    seriesSeason: Option[SeasonNumber],
    seriesNumber: Option[SeriesNumber],
    episodeType: Option[EpisodeType],
    coverUri: Option[ImageUri],
    selfHostedLocation: Option[SelfHostedLocation],
    externalResources: List[ExternalResource],
):
  /** Copies with validation.
   *  @return new state validation result.
   */
  def update(
      id: Uuid[Work] = id,
      title: Title = title,
      synopsis: Synopsis = synopsis,
      releaseDate: ReleaseDate = releaseDate,
      writers: List[Uuid[Person]] = writers,
      cast: List[CastMember] = cast,
      seriesId: Option[Uuid[Series]] = seriesId,
      seriesSeason: Option[SeasonNumber] = seriesSeason,
      seriesNumber: Option[SeriesNumber] = seriesNumber,
      episodeType: Option[EpisodeType] = episodeType,
      coverUrl: Option[ImageUri] = coverUri,
      selfHostedLocation: Option[SelfHostedLocation] = selfHostedLocation,
      externalResources: List[ExternalResource] = externalResources,
  ): ValidationResult[Work] = Work(
    id = id,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    writers = writers,
    cast = cast,
    seriesId = seriesId,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    episodeType = episodeType,
    coverUrl = coverUrl,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
  )


object Work:
  private type ValidationResult[A] = ValidatedNec[WorkValidationError, A]

  /** Creates piece of media with state validation, i.e.:
   *    - writers must not have duplicates.
   *    - cast must not have one person listed more than once.
   *    - episode type and series must both be set or both be empty.
   *    - series must be given, if season, series number or episode type is
   *      given.
   *  @return validation result.
   */
  def apply(
      id: Uuid[Work],
      title: Title,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writers: List[Uuid[Person]],
      cast: List[CastMember],
      seriesId: Option[Uuid[Series]],
      seriesSeason: Option[SeasonNumber],
      seriesNumber: Option[SeriesNumber],
      episodeType: Option[EpisodeType],
      coverUrl: Option[ImageUri],
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): ValidationResult[Work] = validateState(
    new Work(
      id = id,
      title = title,
      synopsis = synopsis,
      releaseDate = releaseDate,
      writers = writers,
      cast = cast,
      seriesId = seriesId,
      seriesSeason = seriesSeason,
      seriesNumber = seriesNumber,
      episodeType = episodeType,
      coverUri = coverUrl,
      selfHostedLocation = selfHostedLocation,
      externalResources = externalResources,
    ))

  /** Unsafe constructor to use only inside always-valid boundary.
   *
   *  @throws WorkValidationError if constructs invalid object.
   */
  def unsafe(
      id: Uuid[Work],
      title: Title,
      synopsis: Synopsis,
      releaseDate: ReleaseDate,
      writers: List[Uuid[Person]],
      cast: List[CastMember],
      seriesId: Option[Uuid[Series]],
      seriesSeason: Option[SeasonNumber],
      seriesNumber: Option[SeriesNumber],
      episodeType: Option[EpisodeType],
      coverUrl: Option[ImageUri],
      selfHostedLocation: Option[SelfHostedLocation],
      externalResources: List[ExternalResource],
  ): Work = Work(
    id = id,
    title = title,
    synopsis = synopsis,
    releaseDate = releaseDate,
    writers = writers,
    cast = cast,
    seriesId = seriesId,
    seriesSeason = seriesSeason,
    seriesNumber = seriesNumber,
    episodeType = episodeType,
    coverUrl = coverUrl,
    selfHostedLocation = selfHostedLocation,
    externalResources = externalResources,
  ) match
    case Validated.Valid(a)   => a
    case Validated.Invalid(e) => throw e.head

  /** Validates state.
   *  @param pom piece of media to validate.
   *  @return validation result.
   */
  private def validateState(
      pom: Work,
  ): ValidationResult[Work] = validateWriters(pom)
    .andThen(validateCast)
    .andThen(validateSeriesPresence)
    .andThen(validateEpisodeTypePresence)

  /** Validates writers. There should not be duplicates.
   *  @param pom piece of media whose writers are being checked.
   *  @return validation result.
   */
  private def validateWriters(
      pom: Work,
  ): ValidationResult[Work] =
    val noDuplicates = pom.writers.toSet.size == pom.writers.size
    Validated.cond(noDuplicates, pom, NonEmptyChain.one(WriterDuplicates))

  /** Validates cast. There should not be duplicate actors.
   *  @param ap piece of media whose cast is being checked.
   *  @return validation result.
   */
  private def validateCast(ap: Work): ValidationResult[Work] =
    val actors = ap.cast.map(_.actor).toSet
    val noDuplicates = actors.size == ap.cast.size
    Validated.cond(noDuplicates, ap, NonEmptyChain.one(CastMemberDuplicates))

  /** Validates episode type presence. Episode type must be specified if series
   *  is given.
   *  @param ap piece of media which is being validated.
   *  @return validation result.
   */
  private def validateEpisodeTypePresence(
      ap: Work,
  ): ValidationResult[Work] =
    val a = ap.episodeType.isDefined == ap.seriesId.isDefined
    Validated.cond(a, ap, NonEmptyChain.one(EpisodeTypeIsMissing))

  /** Validates series presence. Series must be present if season, series number
   *  or episode type is given.
   *  @param ap piece of media whose series info is being validated.
   *  @return validation result.
   */
  private def validateSeriesPresence(
      ap: Work,
  ): ValidationResult[Work] =
    val seriesAlright = ap.seriesId.isDefined ||
      (ap.seriesSeason.isEmpty && ap.seriesNumber.isEmpty)
    Validated.cond(seriesAlright, ap, NonEmptyChain.one(SeriesIsMissing))
