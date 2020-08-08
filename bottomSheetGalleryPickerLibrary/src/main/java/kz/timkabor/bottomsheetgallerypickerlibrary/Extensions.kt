package kz.timkabor.bottomsheetgallerypickerlibrary

import android.content.Context
import android.content.res.Resources
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.net.URLConnection
import java.util.concurrent.TimeUnit

fun Uri.getMediaDuration(context: Context): Long {
    val retriever = MediaMetadataRetriever()
    var duration = ""
    try {
        retriever.setDataSource(context, Uri.parse(this.path))
        duration = retriever.extractMetadata(METADATA_KEY_DURATION)
    } catch (e: Exception) {
    } finally {
        retriever.release()
    }
    return duration.toLongOrNull() ?: 0
}

fun Long.asData(): String {
    if (this == 0L) return ""
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) -
            TimeUnit.MINUTES.toSeconds(minutes)
    if (minutes > 1000) return ""
    return if (seconds < 10) "$minutes:0${seconds}" else "$minutes:${seconds}"
}

fun Uri.isVideo(): Boolean {
    val mimeType = URLConnection.guessContentTypeFromName(this.path)
    return (mimeType.startsWith("video"))
}

fun View.margin(
    left: Float? = null,
    top: Float? = null,
    right: Float? = null,
    bottom: Float? = null
) {
    layoutParams<ViewGroup.MarginLayoutParams> {
        left?.let { leftMargin = dpToPx(it) }
        top?.let { topMargin = dpToPx(it) }
        right?.let { rightMargin = dpToPx(it) }
        bottom?.let { bottomMargin = dpToPx(it) }
    }
}

fun Fragment.showToast(message: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(requireContext(), message, duration).show()
}

fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}

fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)
fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

fun Resources.pxToDp(pixels: Int) = (pixels / this.displayMetrics.density)
