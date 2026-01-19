import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kizitonwose.remindr.compose.multiplatform.sample.DatabaseDriverFactory

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Remindr",
    ) {
        val driverFactory = com.kizitonwose.remindr.compose.multiplatform.sample.DatabaseDriverFactory()
        App(driverFactory)
    }
}
