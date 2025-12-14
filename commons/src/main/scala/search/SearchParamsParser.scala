package org.aulune.commons
package search


import search.SearchValidationError.{InvalidLimit, InvalidQuery}
import types.NonEmptyString

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given


/** Parser of search params. */
object SearchParamsParser:
  /** Builds parser with argument validation. It checks that default limit is
   *  positive and doesn't exceed maximum allowed.
   *
   *  @param defaultLimit limit in case of absence.
   *  @param maxLimit maximum allowed limit.
   *  @return parser if given arguments are valid, otherwise `None`.
   */
  def build(
      defaultLimit: Int,
      maxLimit: Int,
  ): Option[SearchParamsParser] =
    val isValid = (0 < defaultLimit) && (defaultLimit <= maxLimit)
    Option.when(isValid)(SearchParamsParser(defaultLimit, maxLimit))


final class SearchParamsParser private (
    defaultLimit: Int,
    maxLimit: Int,
):
  private type ValidationResult[A] = ValidatedNec[SearchValidationError, A]

  /** Parses given search parameters.
   *
   *  For limit:
   *    - If it is not given or is zero, default will be used.
   *    - If it is negative, [[InvalidLimit]] will be returned.
   *    - If it exceeds maximum permitted, it will coerced down to maximum
   *      permitted.
   *    - In other case given limit will be used.
   *
   *  For query: it should be non-empty, otherwise [[InvalidQuery]] will be
   *  returned.
   *
   *  @param query search query.
   *  @param limit number of elements.
   *  @return search params if valid, otherwise errors.
   */
  def parse(
      query: String,
      limit: Option[Int],
  ): ValidationResult[SearchParams] = (
    NonEmptyString.from(query).toValidNec(InvalidQuery),
    validateLimit(limit),
  ).mapN(SearchParams.apply)

  private def validateLimit(pageSize: Option[Int]): ValidationResult[Int] =
    val size = pageSize.getOrElse(defaultLimit)
    if size == 0 then defaultLimit.validNec
    else Validated.condNec(size > 0, size min maxLimit, InvalidLimit)
