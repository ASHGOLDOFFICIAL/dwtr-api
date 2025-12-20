package org.aulune.aggregator
package application.dto.work

import java.util.UUID


/** Request to upload cover for work.
 *  @param name ID of work to attach cover to.
 *  @param cover cover as bytes.
 */
final case class UploadWorkCoverRequest(
    name: UUID,
    cover: IArray[Byte],
)
