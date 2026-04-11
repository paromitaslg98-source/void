import androidx.compose.material3.*
import androidx.compose.ui.Modifier
@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun Test() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { androidx.compose.material3.Text("test") } },
        state = rememberTooltipState()
    ) {
        androidx.compose.material3.Text("Anchor", Modifier.tooltipAnchor())
    }
}
