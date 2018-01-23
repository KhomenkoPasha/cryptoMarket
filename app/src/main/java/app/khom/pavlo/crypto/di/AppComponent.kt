package app.khom.pavlo.crypto.di

import android.app.Application
import app.khom.pavlo.crypto.CApp
import app.khom.pavlo.crypto.rest.NetworkModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(AndroidSupportInjectionModule::class,
        AppModule::class, NetworkModule::class, ActivityBuilder::class))

interface AppComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance fun application(application: Application): Builder
        fun build(): AppComponent
    }

    fun inject(app: CApp)
}