package com.sparesapp.register.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sparesapp.register.SparesApp
import com.sparesapp.register.ui.main.MainViewModel
import com.sparesapp.register.ui.picker.GraphBrowserViewModel
import com.sparesapp.register.ui.picker.PickMode

class MainViewModelFactory(private val app: SparesApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(app.inventoryRepository, app.preferencesRepository) as T
}

class GraphBrowserViewModelFactory(
    private val app: SparesApp,
    private val mode: PickMode,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        GraphBrowserViewModel(app.graphRepository, mode) as T
}
