package com.mugarutv

import android.app.Application
import com.mugarutv.updates.ContentUpdateScheduler

class MugaruTvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ContentUpdateScheduler.ensureScheduled(this)
    }
}
