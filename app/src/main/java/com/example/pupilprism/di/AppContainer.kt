package com.example.pupilprism.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import com.example.pupilprism.data.db.AppDatabase
import com.example.pupilprism.data.db.SVE_MIGRACIJE

interface AppContainer {
    val database: AppDatabase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "speedreader-db"
        )
            .addMigrations(*SVE_MIGRACIJE)
            .build()
    }
}


val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer није постављен. Недостаје CompositionLocalProvider у MainActivity.")
}
