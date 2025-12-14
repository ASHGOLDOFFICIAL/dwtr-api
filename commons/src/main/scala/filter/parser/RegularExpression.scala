package org.aulune.commons
package filter.parser

import scala.util.matching.Regex


/** Collection of regular expression used in parser. */
private[parser] object RegularExpression:
  val string: Regex = """(?:[^"\\]*(?:\\.)?)*""".r
  val integer: Regex = """-?\d+""".r
  val boolean: Regex = """true|false""".r
  val fieldName: Regex = """[a-zA-Z_]\w+""".r
