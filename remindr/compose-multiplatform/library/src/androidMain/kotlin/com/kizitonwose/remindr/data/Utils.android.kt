package com.kizitonwose.remindr.data

import android.util.Log

internal actual fun log(tag: String, message: String) = Log.w(tag, message).asUnit()
