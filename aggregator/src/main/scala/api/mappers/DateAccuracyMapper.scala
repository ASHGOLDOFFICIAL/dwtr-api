package org.aulune.aggregator
package api.mappers

import application.dto.shared.ReleaseDateDTO.DateAccuracyDTO


/** Mapper between application layer's [[ReleaseDateDTO]] and its API
 *  representation as strings.
 */
private[api] object DateAccuracyMapper:
  private val fromDtoMapper = DateAccuracyDTO.values.map {
    case t @ DateAccuracyDTO.Unknown => t -> "unknown"
    case t @ DateAccuracyDTO.Day     => t -> "day"
    case t @ DateAccuracyDTO.Month   => t -> "month"
    case t @ DateAccuracyDTO.Year    => t -> "year"
  }.toMap

  private val fromStringMapper = fromDtoMapper.map(_.swap)

  val stringValues: List[String] = fromStringMapper.keys.toList

  /** Returns string representation of [[DateAccuracyDTO]].
   *  @param dto DTO to represent.
   */
  def toString(dto: DateAccuracyDTO): String = fromDtoMapper(dto)

  /** Returns [[DateAccuracyDTO]] for given string if valid.
   *  @param str string.
   *  @return [[DateAccuracyDTO]] or `None` if given string is not mapped to any
   *    DTO object.
   */
  def fromString(str: String): Option[DateAccuracyDTO] =
    fromStringMapper.get(str)
