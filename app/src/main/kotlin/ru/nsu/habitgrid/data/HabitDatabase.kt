package ru.nsu.habitgrid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// описание бд
// какие таблицы хранятся, exportSchema = сохранить конфигурацию в файле
@Database(
    entities = [HabitEntity::class, CompletionEntity::class],
    version = 1,
    exportSchema = true,
)
// конвертеры типов данных
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): CompletionDao

    companion object {
        fun create(context: Context): HabitDatabase =
            Room.databaseBuilder(context, HabitDatabase::class.java, "habitgrid.db").build()
    }
}
