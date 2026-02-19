package com.remindr.app.ui.components

import android.app.DatePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.remindr.app.util.parseUsOrIsoLocalDateOrNull
import kotlinx.datetime.LocalDate
import java.util.Calendar
import java.util.Locale

@Composable
actual fun PlatformDateInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val initialDate = parseUsOrIsoLocalDateOrNull(value)
                ?: run {
                    val now = Calendar.getInstance()
                    LocalDate(
                        year = now.get(Calendar.YEAR),
                        monthNumber = now.get(Calendar.MONTH) + 1,
                        dayOfMonth = now.get(Calendar.DAY_OF_MONTH),
                    )
                }
            DatePickerDialog(
                context,
                { _, year, monthOfYear, dayOfMonth ->
                    onValueChange(
                        String.format(
                            Locale.US,
                            "%02d/%02d/%04d",
                            monthOfYear + 1,
                            dayOfMonth,
                            year,
                        )
                    )
                },
                initialDate.year,
                initialDate.monthNumber - 1,
                initialDate.day,
            ).show()
        },
        modifier = modifier,
    ) {
        Icon(Icons.Default.DateRange, contentDescription = null)
        Text(
            text = if (value.isBlank()) label else value,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
