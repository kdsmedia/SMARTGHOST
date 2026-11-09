package com.altomedia.smartghost.security

import android.util.Log

/**
 * Thin logging wrapper that redacts secrets before anything is written
 * (Bab 3, 9). All app logging must go through this object.
 */
object SafeLogger {

    private const val TAG = "SMARTGHOST"

    fun d(message: String) = Log.d(TAG, SecretRedactor.redact(message))

    fun i(message: String) = Log.i(TAG, SecretRedactor.redact(message))

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, SecretRedactor.redact(message), throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, SecretRedactor.redact(message), throwable)
    }
}
