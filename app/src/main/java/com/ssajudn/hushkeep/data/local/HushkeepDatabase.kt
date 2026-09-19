package com.ssajudn.hushkeep.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ssajudn.hushkeep.data.local.dao.AlbumDao
import com.ssajudn.hushkeep.data.local.dao.MediaObjectDao
import com.ssajudn.hushkeep.data.local.dao.MemoryDao
import com.ssajudn.hushkeep.data.local.dao.UploadJobDao
import com.ssajudn.hushkeep.data.local.entity.AlbumEntity
import com.ssajudn.hushkeep.data.local.entity.MediaObjectEntity
import com.ssajudn.hushkeep.data.local.entity.MemoryEntity
import com.ssajudn.hushkeep.data.local.entity.UploadJobEntity

@Database(
    entities = [
        AlbumEntity::class,
        MemoryEntity::class,
        MediaObjectEntity::class,
        UploadJobEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class HushkeepDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun memoryDao(): MemoryDao
    abstract fun mediaObjectDao(): MediaObjectDao
    abstract fun uploadJobDao(): UploadJobDao

    companion object {
        private const val DATABASE_NAME = "hushkeep.db"

        @Volatile
        private var instance: HushkeepDatabase? = null

        fun getInstance(context: Context): HushkeepDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HushkeepDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
        }
    }
}
