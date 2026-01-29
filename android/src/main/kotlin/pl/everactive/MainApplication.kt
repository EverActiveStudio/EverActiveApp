package pl.everactive

import android.app.Application
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
import pl.everactive.config.mainModule
import pl.everactive.services.RuleNotificationService

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(mainModule)

            koin.get<RuleNotificationService>()
                .start()
        }
    }
}
