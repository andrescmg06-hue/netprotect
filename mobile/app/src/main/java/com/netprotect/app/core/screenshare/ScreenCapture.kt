package com.netprotect.app.core.screenshare

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

/** Sprint 23: the Android screen-capture consent dialog.
 *
 * Kept beside the service rather than in core/permissions with the others because it is not a
 * permission in Android's sense — there is nothing to check for, nothing that stays granted. Each
 * call produces a one-shot authorization for a single capture session, which is precisely why
 * screen sharing here can never be silent or persistent.
 */
object ScreenCapture {

    fun consentIntent(context: Context): Intent =
        (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .createScreenCaptureIntent()
}
