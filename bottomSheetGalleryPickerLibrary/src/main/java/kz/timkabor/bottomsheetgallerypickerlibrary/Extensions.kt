package kz.timkabor.bottomsheetgallerypickerlibrary

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.net.Uri
import android.widget.Toast
import java.net.URLConnection
import java.util.concurrent.TimeUnit

fun Any.toast(context: Context, duration: Int = Toast.LENGTH_LONG): Toast {
    return Toast.makeText(context, this.toString(), duration).apply { show() }
}

fun Uri.getMediaDuration(context: Context): Long {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, Uri.parse(this.path))
    val duration = retriever.extractMetadata(METADATA_KEY_DURATION)
    retriever.release()

    return duration.toLongOrNull() ?: 0
}

fun Long.asData(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) -
            TimeUnit.MINUTES.toSeconds(minutes)
    return if (seconds < 10)
        "$minutes:0${seconds}"
    else "$minutes:${seconds}"
}

fun Uri.isVideo(): Boolean {
    val mimeType = URLConnection.guessContentTypeFromName(this.path)
    return (mimeType.startsWith("video"))
}
