package com.sparesapp.register

import android.app.Application
import com.sparesapp.register.auth.AuthManager
import com.sparesapp.register.data.local.AppDatabase
import com.sparesapp.register.data.InventoryRepository
import com.sparesapp.register.data.PreferencesRepository
import com.sparesapp.register.graph.GraphRepository

class SparesApp : Application() {

    lateinit var authManager: AuthManager
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var graphRepository: GraphRepository
        private set

    lateinit var inventoryRepository: InventoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        authManager = AuthManager(this)
        preferencesRepository = PreferencesRepository(this)
        graphRepository = GraphRepository(authManager)
        val db = AppDatabase.getInstance(this)
        inventoryRepository = InventoryRepository(
            graphRepository = graphRepository,
            preferencesRepository = preferencesRepository,
            inventoryDao = db.inventoryDao(),
        )
    }
}
