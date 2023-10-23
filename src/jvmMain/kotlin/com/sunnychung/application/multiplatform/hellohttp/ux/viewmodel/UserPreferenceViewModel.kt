package com.sunnychung.application.multiplatform.hellohttp.ux.viewmodel

import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserPreferenceViewModel {
    private val colourThemeStateFlow = MutableStateFlow(ColourTheme.Dark)
    val colourTheme: StateFlow<ColourTheme> = colourThemeStateFlow

    fun setColorTheme(value: ColourTheme) {
        colourThemeStateFlow.value = value
    }
}
