package com.example.pupilprism

import android.app.Application
import com.example.pupilprism.di.AppContainer
import com.example.pupilprism.di.DefaultAppContainer
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PupilPrismApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        container = DefaultAppContainer(this)
    }
}
