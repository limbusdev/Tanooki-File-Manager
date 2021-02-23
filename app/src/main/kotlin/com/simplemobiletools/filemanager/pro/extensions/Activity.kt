package com.simplemobiletools.filemanager.pro.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_STORAGE
import com.simplemobiletools.commons.helpers.isNougatPlus
import com.simplemobiletools.filemanager.pro.BuildConfig
import com.simplemobiletools.filemanager.pro.helpers.*
import kotlinx.android.synthetic.main.activity_read_text.*
import java.io.*
import java.util.*

fun Activity.sharePaths(paths: ArrayList<String>) {
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}

fun Activity.tryOpenPathIntent(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT) {
    if (!forceChooser && path.endsWith(".apk", true)) {
        val uri = if (isNougatPlus()) {
            FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", File(path))
        } else {
            Uri.fromFile(File(path))
        }

        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, getMimeTypeFromUri(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (resolveActivity(packageManager) != null) {
                startActivity(this)
            } else {
                toast(R.string.no_app_found)
            }
        }
    } else {
        openPath(path, forceChooser, openAsType)
    }
}

fun Activity.openPath(path: String, forceChooser: Boolean, openAsType: Int = OPEN_AS_DEFAULT) {
    openPathIntent(path, forceChooser, BuildConfig.APPLICATION_ID, getMimeType(openAsType))
}

private fun getMimeType(type: Int) = when (type) {
    OPEN_AS_DEFAULT -> ""
    OPEN_AS_TEXT -> "text/*"
    OPEN_AS_IMAGE -> "image/*"
    OPEN_AS_AUDIO -> "audio/*"
    OPEN_AS_VIDEO -> "video/*"
    else -> "*/*"
}

fun Activity.setAs(path: String) {
    setAsIntent(path, BuildConfig.APPLICATION_ID)
}

private fun saveTextContent(outputStream: OutputStream?, content: String) {
    outputStream?.bufferedWriter()?.use { it.write(content) }
}

fun BaseSimpleActivity.toggleItemVisibility(oldPath: String, hide: Boolean, callback: ((newPath: String) -> Unit)? = null) {
    val path = oldPath.getParentPath()
    var filename = oldPath.getFilenameFromPath()
    if (hide && filename.startsWith('.')) {
        callback?.invoke(oldPath)
        return
    }

    val hiddenEntries = getDotHiddenEntries(path);

    if (hide) {
        // Add filename to .hidden
        if (hiddenEntries.contains(filename))
            return;

    } else {
        // Remove filename from .hidden
        if (!hiddenEntries.contains(filename))
            return;
    }

    val newHiddenEntries = arrayListOf<String>()

    for (entry in hiddenEntries) {
        if (entry != filename) {
            newHiddenEntries.add(entry)
        }
    }

    if (hide) {
        newHiddenEntries.add(filename)
    }

    val stringBuilder = StringBuilder()
    for (entry in newHiddenEntries) {
        stringBuilder.append(entry)
        stringBuilder.append("\n")
    }

    // Write new .hidden
    val hiddenFileName = "$path/.hidden"

    handlePermission(PERMISSION_WRITE_STORAGE) {
        if (it) {
            val dotHidden = File(hiddenFileName)
            getFileOutputStream(dotHidden.toFileDirItem(this), true) { outputStream ->
                saveTextContent(outputStream, stringBuilder.toString())
            }
        }
    }
}

private fun getDotHiddenEntries( path: String ) : List<String> {
    val hiddenEntries = arrayListOf<String?>();
    val filename = "$path/.hidden"
    val dotHidden = File( filename )
    if ( dotHidden.exists() && filename != null && filename.trim() != "") {
        var fileInputStream = FileInputStream( filename )
        var inputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader = BufferedReader(inputStreamReader)
        var text: String? = null
        while ({ text = bufferedReader.readLine(); text }() != null) {
            if (text != null)
                hiddenEntries.add(text);
        }
        fileInputStream.close()
    }

    return hiddenEntries.filterNotNull();
}
