package app.khom.pavlo.crypto.di

import javax.inject.Scope

@Scope
@Retention(value = AnnotationRetention.RUNTIME)
annotation class PerActivity

@Scope
@Retention(value = AnnotationRetention.RUNTIME)
annotation class PerFragment