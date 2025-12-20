package org.aulune.aggregator
package domain.repositories


import domain.model.translation.{Translation, TranslationField}

import org.aulune.commons.repositories.{FilterList, GenericRepository}
import org.aulune.commons.types.Uuid


/** Repository for [[Translation]] objects.
 *
 *  @tparam F effect type.
 */
trait TranslationRepository[F[_]]
    extends GenericRepository[
      F,
      Translation,
      Uuid[Translation],
    ]
    with FilterList[F, Translation, TranslationField]
