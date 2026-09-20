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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlbumEntity::class,
        MemoryEntity::class,
        MediaObjectEntity::class,
        UploadJobEntity::class,
    ],
    version = 3,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE memories ADD COLUMN remoteUrl TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE upload_jobs ADD COLUMN fileName TEXT")
                db.execSQL("ALTER TABLE upload_jobs ADD COLUMN totalBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE upload_jobs ADD COLUMN bytesTransferred INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE upload_jobs ADD COLUMN progressPercent INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
