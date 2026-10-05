package com.nutriai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.nutriai.BuildConfig
import com.nutriai.data.ai.AndroidNetworkMonitor
import com.nutriai.data.ai.BackendFoodAnalysisService
import com.nutriai.data.ai.MockFoodAnalysisService
import com.nutriai.data.ai.NetworkMonitor
import com.nutriai.data.local.FoodDao
import com.nutriai.data.local.MealDao
import com.nutriai.data.local.Migrations
import com.nutriai.data.local.NutriAiDatabase
import com.nutriai.data.local.ProfileDao
import com.nutriai.data.local.WeightDao
import com.nutriai.data.repository.Clock
import com.nutriai.data.repository.FoodRepositoryImpl
import com.nutriai.data.repository.MealRepositoryImpl
import com.nutriai.data.repository.ProfileRepositoryImpl
import com.nutriai.data.repository.SystemClock
import com.nutriai.data.repository.WeightRepositoryImpl
import com.nutriai.domain.ai.FoodAnalysisService
import com.nutriai.domain.repository.FoodRepository
import com.nutriai.domain.repository.MealRepository
import com.nutriai.domain.repository.ProfileRepository
import com.nutriai.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NutriAiDatabase =
        Room.databaseBuilder(context, NutriAiDatabase::class.java, NutriAiDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun provideProfileDao(db: NutriAiDatabase): ProfileDao = db.profileDao()
    @Provides fun provideFoodDao(db: NutriAiDatabase): FoodDao = db.foodDao()
    @Provides fun provideMealDao(db: NutriAiDatabase): MealDao = db.mealDao()
    @Provides fun provideWeightDao(db: NutriAiDatabase): WeightDao = db.weightDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Sin URL de backend configurada se usa el MOCK, y la interfaz lo indica.
     * Cambiar de proveedor de IA solo requiere cambiar el backend, no la app.
     */
    @Provides
    @Singleton
    fun provideFoodAnalysisService(
        client: OkHttpClient,
        network: NetworkMonitor,
        @IoDispatcher io: CoroutineDispatcher,
    ): FoodAnalysisService =
        if (BuildConfig.AI_BACKEND_URL.isBlank()) {
            MockFoodAnalysisService()
        } else {
            BackendFoodAnalysisService(BuildConfig.AI_BACKEND_URL, client, network, io)
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository
    @Binds abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository
    @Binds abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository
    @Binds abstract fun bindNetworkMonitor(impl: AndroidNetworkMonitor): NetworkMonitor
    @Binds abstract fun bindClock(impl: SystemClock): Clock
}
