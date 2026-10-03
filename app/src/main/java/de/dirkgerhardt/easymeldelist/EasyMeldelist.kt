package de.dirkgerhardt.easymeldelist

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class EasyMeldelist : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}