package com.aven.app

import android.app.Application
import com.aven.app.core.Prefs
import com.aven.app.core.Store

class AvenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { Prefs.init(this) }
        runCatching { Store.init(this) }
    }
}
