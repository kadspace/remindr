import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import com.remindr.app.RemindrApp
import com.remindr.app.ui.theme.RemindrTheme

@Suppress("FunctionName")
fun MainViewController() = ComposeUIViewController {
    // iOS doesn't have DatabaseDriverFactory implemented yet
    // This is a placeholder — iOS support needs a SQLDelight driver
    MaterialTheme(RemindrTheme) {
        // TODO: Implement iOS DatabaseDriverFactory and pass it here
    }
}
