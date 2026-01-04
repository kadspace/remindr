import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.kizitonwose.calendar.compose.multiplatform.sample.CalendarApp
import com.kizitonwose.calendar.compose.multiplatform.sample.DatabaseDriverFactory
import com.kizitonwose.calendar.compose.multiplatform.sample.SampleColorScheme
import com.kizitonwose.calendar.compose.multiplatform.sample.ReminderScheduler
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun App(driverFactory: DatabaseDriverFactory, requestMagicAdd: Boolean = false, scheduler: ReminderScheduler? = null) {
    MaterialTheme(SampleColorScheme) {
        CalendarApp(driverFactory, requestMagicAdd, scheduler)
    }
}
