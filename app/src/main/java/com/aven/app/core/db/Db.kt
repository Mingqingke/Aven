package com.aven.app.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExamEntity::class,
        SubjectScoreEntity::class,
        QuestionEntity::class,
        GoalEntity::class,
        PlanEntity::class,
        TaskEntity::class,
        ReportEntity::class,
        ChatEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exams(): ExamDao
    abstract fun subjects(): SubjectDao
    abstract fun questions(): QuestionDao
    abstract fun goals(): GoalDao
    abstract fun plans(): PlanDao
    abstract fun tasks(): TaskDao
    abstract fun reports(): ReportDao
    abstract fun chats(): ChatDao
    abstract fun backup(): BackupDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "aven.db",
            ).build().also { instance = it }
        }
    }
}
