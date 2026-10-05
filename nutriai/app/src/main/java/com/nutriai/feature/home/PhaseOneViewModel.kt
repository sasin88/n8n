package com.nutriai.feature.home

import androidx.lifecycle.ViewModel
import com.nutriai.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** ViewModel temporal: comprueba de punta a punta la cadena Compose → ViewModel → Hilt. */
@HiltViewModel
class PhaseOneViewModel @Inject constructor() : ViewModel() {
    val versionName: String = BuildConfig.VERSION_NAME
}
