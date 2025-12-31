import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Calendar Sample",
    ) {
        val driverFactory = com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory()
        App(driverFactory)
    }
}
