package com.kbizsoft.KPlayer.repository

import android.content.Context
import kotlinx.coroutines.*
import com.kbizsoft.libkplayer.util.AndroidUtil
import com.kbizsoft.medialibrary.MLServiceLocator
import com.kbizsoft.medialibrary.interfaces.media.MediaWrapper
import com.kbizsoft.resources.AndroidDevices
import com.kbizsoft.resources.AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY
import com.kbizsoft.tools.IOScopedObject
import com.kbizsoft.tools.SingletonHolder
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.database.CustomDirectoryDao
import com.kbizsoft.KPlayer.database.MediaDatabase
import com.kbizsoft.KPlayer.util.FileUtils
import java.io.File

class DirectoryRepository (private val customDirectoryDao: CustomDirectoryDao) : IOScopedObject() {

    fun addCustomDirectory(path: String): Job = launch {
        customDirectoryDao.insert(com.kbizsoft.KPlayer.mediadb.models.CustomDirectory(path))
    }

    suspend fun getCustomDirectories() = withContext(coroutineContext) {
        try {
            customDirectoryDao.getAll()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteCustomDirectory(path: String) = launch { customDirectoryDao.delete(com.kbizsoft.KPlayer.mediadb.models.CustomDirectory(path)) }

    suspend fun customDirectoryExists(path: String) = withContext(coroutineContext) { customDirectoryDao.get(path).isNotEmpty() }

    suspend fun getMediaDirectoriesList(context: Context) = getMediaDirectories().filter {
        File(it).exists()
    }.map { createDirectory(it, context) }

    suspend fun getMediaDirectories() = mutableListOf<String>().apply {
        add(EXTERNAL_PUBLIC_DIRECTORY)
        addAll(AndroidDevices.externalStorageDirectories)
        addAll(getCustomDirectories().map { it.path })
    }

    companion object : SingletonHolder<DirectoryRepository, Context>({ DirectoryRepository(MediaDatabase.getInstance(it).customDirectoryDao()) })
}

fun createDirectory(it: String, context: Context): MediaWrapper {
    val directory = MLServiceLocator.getAbstractMediaWrapper(AndroidUtil.PathToUri(it))
    directory.type = MediaWrapper.TYPE_DIR
    if (EXTERNAL_PUBLIC_DIRECTORY == it) {
        directory.setDisplayTitle(context.resources.getString(R.string.internal_memory))
    } else {
        val deviceName = FileUtils.getStorageTag(directory.title)
        if (deviceName != null) directory.setDisplayTitle(deviceName)
    }
    return directory
}