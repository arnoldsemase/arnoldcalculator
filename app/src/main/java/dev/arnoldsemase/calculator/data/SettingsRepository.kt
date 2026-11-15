package dev.arnoldsemase.calculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.arnoldsemase.calculator.engine.DecimalMark
import dev.arnoldsemase.calculator.engine.GroupingStyle
import dev.arnoldsemase.calculator.engine.NumberFormatSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "arnold_settings")

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val haptics: Boolean = true,
    val numberFormat: NumberFormatSettings = NumberFormatSettings(),
)

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            AppSettings(
                theme = p[THEME].toEnum(ThemeMode.DARK),
                haptics = p[HAPTICS] ?: true,
                numberFormat = NumberFormatSettings(
                    grouping = p[GROUPING].toEnum(GroupingStyle.COMMA),
                    decimalMark = p[DECIMAL].toEnum(DecimalMark.POINT),
                    maxDecimals = p[MAX_DECIMALS] ?: NumberFormatSettings.AUTO,
                ),
            )
        }

    suspend fun setTheme(mode: ThemeMode) = store.edit { it[THEME] = mode.name }
    suspend fun setHaptics(enabled: Boolean) = store.edit { it[HAPTICS] = enabled }
    suspend fun setGrouping(style: GroupingStyle) = store.edit { it[GROUPING] = style.name }
    suspend fun setDecimalMark(mark: DecimalMark) = store.edit { it[DECIMAL] = mark.name }
    suspend fun setMaxDecimals(places: Int) = store.edit { it[MAX_DECIMALS] = places }

    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val HAPTICS = booleanPreferencesKey("haptics")
        val GROUPING = stringPreferencesKey("grouping")
        val DECIMAL = stringPreferencesKey("decimal_mark")
        val MAX_DECIMALS = intPreferencesKey("max_decimals")
    }
}
