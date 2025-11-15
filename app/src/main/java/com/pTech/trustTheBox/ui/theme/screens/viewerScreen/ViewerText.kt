import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pTech.trustTheBox.model.MItem

@Composable
fun ViewerText(item: MItem) {
    val context = LocalContext.current
    val text = remember(item.uri) {
        try {
            context.contentResolver.openInputStream(item.uri)?.bufferedReader()
                ?.use { it.readText() } ?: "Помилка завантаження тексту"
        } catch (e: Exception) {
            "Помилка: ${e.message}"
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    )
                ),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(0.8f)
            )
        )
    }
}