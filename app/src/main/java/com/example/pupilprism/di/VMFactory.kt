package com.example.pupilprism.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pupilprism.data.model.RSVPViewModel
import com.example.pupilprism.ui.reader.CalibrationViewModel
import com.example.pupilprism.ui.reader.QuizViewModel

object VMFactory {
    fun create(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            RSVPViewModel(container.database.assessmentDao())
        }
        initializer {
            QuizViewModel(container.database.assessmentDao())
        }
        initializer {
            CalibrationViewModel(
                container.database.assessmentDao(),
                container.database.userStatsDao()
            )
        }
    }
}
