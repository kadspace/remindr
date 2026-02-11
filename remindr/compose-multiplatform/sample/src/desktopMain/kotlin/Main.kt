import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.remindr.app.RemindrApp
import com.remindr.app.data.db.DatabaseDriverFactory
import com.remindr.app.ui.theme.RemindrTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Remindr",
    ) {
        val driverFactory = DatabaseDriverFactory()
        MaterialTheme(RemindrTheme) {
            RemindrApp(
                driverFactory = driverFactory,
                onRequestNotificationTest = {},
                onRequestRichNotificationTest = {},
            )
        }
    }
}
