package org.aulune.commons
package pagination.params


import pagination.cursor.CursorDecoder
import pagination.params.PaginationValidationError.{
  InvalidPageSize,
  InvalidPageToken,
}

import cats.data.{Validated, ValidatedNec}
import cats.syntax.all.given


/** Parser of pagination params.
 *
 *  @see [[https://google.aip.dev/158 Google's AIP]].
 */
object PaginationParamsParser:
  /** Builds parser with argument validation. It checks that default page size
   *  is positive and doesn't exceed maximum allowed.
   *
   *  @param defaultPageSize page size in case of absence.
   *  @param maxPageSize maximum allowed page size.
   *  @tparam Cursor type of cursor.
   *  @return parser if given arguments are valid, otherwise `None`.
   */
  def build[Cursor: CursorDecoder](
      defaultPageSize: Int,
      maxPageSize: Int,
  ): Option[PaginationParamsParser[Cursor]] =
    val isValid = (0 < defaultPageSize) && (defaultPageSize <= maxPageSize)
    Option.when(isValid)(PaginationParamsParser(defaultPageSize, maxPageSize))


final class PaginationParamsParser[Cursor: CursorDecoder] private (
    defaultPageSize: Int,
    maxPageSize: Int,
):
  private type ValidationResult[A] = ValidatedNec[PaginationValidationError, A]

  /** Parses given pagination parameters.
   *
   *  For page size:
   *    - If it is not given or is zero, default will be used.
   *    - If it is negative, [[InvalidPageSize]] will be returned.
   *    - If it exceeds maximum permitted, it will coerced down to maximum
   *      permitted.
   *    - In other case given page size will be used.
   *
   *  For page token: if it can be decoded to cursor, it will be used, otherwise
   *  [[InvalidPageToken]] will be returned.
   *
   *  @param pageSize optional page size.
   *  @param pageToken optional page token.
   *  @return pagination params if valid, otherwise errors.
   */
  def parse(
      pageSize: Option[Int],
      pageToken: Option[String],
  ): ValidationResult[PaginationParams[Cursor]] = (
    validatePageSize(pageSize),
    validatePageToken(pageToken),
  ).mapN(PaginationParams.apply)

  private def validatePageSize(pageSize: Option[Int]): ValidationResult[Int] =
    val size = pageSize.getOrElse(defaultPageSize)
    if size == 0 then defaultPageSize.validNec
    else Validated.condNec(size > 0, size min maxPageSize, InvalidPageSize)

  private def validatePageToken(
      maybeToken: Option[String],
  ): ValidationResult[Option[Cursor]] = maybeToken.traverse { token =>
    CursorDecoder[Cursor].decode(token).toValidNec(InvalidPageToken)
  }
