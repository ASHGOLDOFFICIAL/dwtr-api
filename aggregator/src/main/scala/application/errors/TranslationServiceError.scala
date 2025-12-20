package org.aulune.aggregator
package application.errors


import application.TranslationService

import org.aulune.commons.errors.ErrorReason


/** Errors that can occur in [[TranslationService]].
 *
 *  @param reason string representation of error.
 */
enum TranslationServiceError(val reason: String) extends ErrorReason(reason):
  /** Given ID of original work is not associated with any work. */
  case OriginalNotFound extends TranslationServiceError("ORIGINAL_NOT_FOUND")

  /** Specified translation is not found. */
  case TranslationNotFound
      extends TranslationServiceError("TRANSLATION_NOT_FOUND")

  /** Translation is not self-hosted. */
  case NotSelfHosted extends TranslationServiceError("NOT_SELF_HOSTED")

  /** Given translation is not valid translation. */
  case InvalidTranslation extends TranslationServiceError("INVALID_TRANSLATION")
