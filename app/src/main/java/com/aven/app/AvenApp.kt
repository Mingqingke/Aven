package com.aven.app

import android.app.Application
import com.aven.app.core.Prefs
import com.aven.app.core.Store
import com.aven.app.core.lock.AppLock

class AvenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { Prefs.init(this) }
        runCatching { Store.init(this) }
        runCatching { AppLock.init(this) }
        runCatching { AppLock.attach(this) }
    }
}
