package com.chibychibystore.ui.components.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Overrides the ViewModel factory used by [screenViewModel]. Production leaves
 * this null, so screens get their Hilt-provided ViewModel with no behavioural
 * change. It exists so a unit test can render the real `appDestinations` graph:
 * that graph has no Hilt component under Robolectric, so a test supplies a
 * factory that returns mocked ViewModels and can then navigate the actual
 * routes instead of re-declaring which screen each route should show.
 */
val LocalScreenViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory?> { null }

/**
 * Resolves the screen's ViewModel, honouring [LocalScreenViewModelFactory] when a
 * test has provided one and falling back to Hilt otherwise.
 */
@Composable
inline fun <reified VM : ViewModel> screenViewModel(key: String? = null): VM {
    val factory = LocalScreenViewModelFactory.current
    return if (factory == null) {
        hiltViewModel(key = key)
    } else {
        viewModel(factory = factory, key = key)
    }
}
