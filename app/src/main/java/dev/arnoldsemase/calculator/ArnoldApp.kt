package dev.arnoldsemase.calculator

import android.app.Application
import android.content.Context
import androidx.room.Room
import dev.arnoldsemase.calculator.data.AppDatabase
import dev.arnoldsemase.calculator.data.HistoryRepository
import dev.arnoldsemase.calculator.data.RatesRepository
import dev.arnoldsemase.calculator.data.SettingsRepository
import dev.arnoldsemase.calculator.data.settingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

/** Tiny manual DI container: everything is local, so no framework is needed. */
class AppContainer(private val context: Context) {
    // Lazy so Application.onCreate stays instant; the real work happens in [warmUp] off the main thread.
    private val db by lazy { Room.databaseBuilder(context, AppDatabase::class.java, "arnold_calculator.db").build() }
    val historyRepository by lazy { HistoryRepository(db.historyDao()) }
    val settingsRepository by lazy { SettingsRepository(context.settingsDataStore) }
    val ratesRepository by lazy {
        RatesRepository(
            cacheFile = File(context.filesDir, "exchange_rates.json"),
            prefs = context.getSharedPreferences("currency", Context.MODE_PRIVATE),
        )
    }

    /** Opens the database and reads settings on a background thread so first use never waits on disk. */
    fun warmUp(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            runCatching { settingsRepository.settings.first() }
            runCatching { db.openHelper.writableDatabase }
        }
    }
}

class ArnoldApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext).also { it.warmUp(appScope) }
    }
}
