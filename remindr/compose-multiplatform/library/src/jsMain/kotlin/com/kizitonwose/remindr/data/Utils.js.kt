package com.kizitonwose.remindr.data

internal actual fun log(tag: String, message: String) =
    console.log("$tag : $message")
