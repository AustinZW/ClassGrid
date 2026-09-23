package net.yeeren.classgrid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CourseEntity::class, CourseSlotEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    companion object {
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses RENAME TO courses_legacy")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS courses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        teacher TEXT NOT NULL,
                        colorArgb INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO courses (id, name, teacher, colorArgb, note)
                    SELECT id, name, teacher, colorArgb, note FROM courses_legacy
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS course_slots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        courseId INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        startSection INTEGER NOT NULL,
                        endSection INTEGER NOT NULL,
                        activeWeeks TEXT NOT NULL,
                        FOREIGN KEY(courseId) REFERENCES courses(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO course_slots (courseId, location, dayOfWeek, startSection, endSection, activeWeeks)
                    SELECT id, location, dayOfWeek, startSection, endSection, activeWeeks FROM courses_legacy
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_course_slots_courseId ON course_slots(courseId)")
                db.execSQL("DROP TABLE courses_legacy")
            }
        }

        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "class-table.db"
        ).addMigrations(migration1To2).build()
    }
}
