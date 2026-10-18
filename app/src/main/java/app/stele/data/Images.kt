package app.stele.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID

/** Pictures on finds: taken with the phone's own camera or picked from the gallery, kept small in the app's files. */
object Images {
    private const val MAX = 1280

    fun dir(context: Context): File = File(context.filesDir, "images").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    /** Where the system camera writes a new shot, shared through the file provider. */
    fun captureUri(context: Context): Uri {
        val f = File(context.cacheDir, "capture").apply { mkdirs() }.let { File(it, "shot.jpg") }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    }

    /** Copies a picture in, scaled down and turned upright; returns its name. */
    fun keep(context: Context, uri: Uri): String? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX) sample *= 2
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val turn = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val k = MAX / maxOf(raw.width, raw.height).toFloat()
        val m = Matrix().apply { if (k < 1f) postScale(k, k); postRotate(turn) }
        val bmp = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        val name = UUID.randomUUID().toString() + ".jpg"
        file(context, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 84, it) }
        name
    }.getOrNull()

    fun drop(context: Context, name: String) {
        if (name.isNotBlank()) file(context, name).delete()
    }
}
