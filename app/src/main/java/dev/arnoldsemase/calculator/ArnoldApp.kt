package dev.arnoldsemase.calculator

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.arnoldsemase.calculator.data.AppDatabase
import dev.arnoldsemase.calculator.data.HistoryRepository
import dev.arnoldsemase.calculator.data.SettingsRepository
import dev.arnoldsemase.calculator.data.settingsDataStore

/** Tiny manual DI container: everything is local, so no framework is needed. */
class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "arnold_calculator.db").build()
    val historyRepository = HistoryRepository(db.historyDao())
    val settingsRepository = SettingsRepository(context.settingsDataStore)
}

class ArnoldApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}
