package com.kizitonwose.remindr.view.internal

import android.content.Context
import android.util.Log
import android.view.ViewGroup

internal inline fun customViewOrRoot(
    customViewClass: String?,
    rootLayout: ViewGroup,
    setupRoot: (ViewGroup) -> Unit,
): ViewGroup {
    return customViewClass?.let {
        val customLayout = runCatching {
            Class.forName(it)
                .getDeclaredConstructor(Context::class.java)
                .newInstance(rootLayout.context) as ViewGroup
        }.onFailure {
            Log.e(
                "Calendar",
                "Failure loading custom class $customViewClass, " +
                    "check that $customViewClass is a ViewGroup and the " +
                    "single argument context constructor is available. " +
                    "For an example on how to use a custom class, see: $EXAMPLE_CUSTOM_CLASS_URL",
                it,
            )
        }.getOrNull()

        customLayout?.apply {
            setupRoot(this)
            addView(rootLayout)
        }
    } ?: rootLayout.apply { setupRoot(this) }
}

private const val EXAMPLE_CUSTOM_CLASS_URL =
    "https://github.com/wave/remindr/blob/main/sample/src/main/java/com/kizitonwose/remindr/sample/view/Example6Fragment.kt#L29"
