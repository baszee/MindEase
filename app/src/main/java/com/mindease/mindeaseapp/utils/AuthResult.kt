package com.mindease.mindeaseapp.utils

import java.lang.Exception

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(val exception: Exception) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}