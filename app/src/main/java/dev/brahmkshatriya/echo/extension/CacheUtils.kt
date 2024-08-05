package dev.brahmkshatriya.echo.extension

import android.content.Context
import android.os.Parcel
import java.io.File
import java.io.FileInputStream

fun cacheDir(context: Context, folderName: String): File {
    return File(context.cacheDir, folderName).apply { mkdirs() }
}

fun <T> Context.getFromCache(
    id: String,
    folderName: String,
    creator: (Parcel) -> T?,
): T? {
    val file = File(cacheDir(this, folderName), id.hashCode().toString())
    if (!file.exists()) {
        return null
    }

    return try {
        val bytes = FileInputStream(file).use { it.readBytes() }
        val parcel = Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        val value = creator(parcel)
        parcel.recycle()
        value
    } catch (_: Exception) {
        null
    }
}

fun Context.saveToCache(
    id: String,
    folderName: String,
    writer: (Parcel) -> Unit,
) {
    val fileName = id.hashCode().toString()
    val cacheDir = cacheDir(this, folderName)
    val parcel = Parcel.obtain()
    writer(parcel)
    val bytes = parcel.marshall()
    parcel.recycle()
    File(cacheDir, fileName).outputStream().use { it.write(bytes) }
}
