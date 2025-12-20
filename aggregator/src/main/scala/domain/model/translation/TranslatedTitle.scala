package org.aulune.aggregator
package domain.model.translation

/** Original work's translated title. */
opaque type TranslatedTitle <: String = String


object TranslatedTitle:
  /** Returns [[TranslatedTitle]] if argument is valid.
   *
   *  To be valid string should not be empty and should not consist of
   *  whitespaces only. All whitespaces are being stripped.
   *
   *  @param title title given to work in translation.
   */
  def apply(title: String): Option[TranslatedTitle] =
    val stripped = title.strip()
    Option.when(stripped.nonEmpty)(stripped)

  /** Unsafe constructor to use inside always-valid boundary.
   *  @param title title in translation.
   *  @throws IllegalArgumentException if given params are invalid.
   */
  def unsafe(title: String): TranslatedTitle = TranslatedTitle(title) match
    case Some(value) => value
    case None        => throw new IllegalArgumentException()
