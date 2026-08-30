package brief.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun ModernDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    optionLabel: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(label, color = ThemeColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) ThemeColors.Background else ThemeColors.SurfaceVariant)
                .border(1.dp, if (expanded) ThemeColors.Primary else ThemeColors.Border, RoundedCornerShape(10.dp))
                .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (selectedOption.isNotEmpty()) optionLabel(selectedOption) else "Select...",
                    color = ThemeColors.TextMain,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text("▼", color = ThemeColors.TextMuted, fontSize = 10.sp)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(ThemeColors.SurfaceVariant).border(1.dp, ThemeColors.Border, RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }) {
                        Text(optionLabel(option), color = ThemeColors.TextMain)
                    }
                }
            }
        }
    }
}

@Composable
fun FilePickerInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDirectory: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(label, color = ThemeColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) ThemeColors.Background else ThemeColors.SurfaceVariant)
                .border(1.dp, ThemeColors.Border, RoundedCornerShape(10.dp))
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = TextStyle(color = if (enabled) ThemeColors.TextMain else ThemeColors.TextMuted, fontSize = 14.sp),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                singleLine = true
            )
            Button(
                onClick = {
                    if (isDirectory) {
                        System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    }
                    val dialog = FileDialog(null as Frame?, if (isDirectory) "Select Directory" else "Select File", FileDialog.LOAD)
                    dialog.isVisible = true
                    if (isDirectory) {
                        System.setProperty("apple.awt.fileDialogForDirectories", "false")
                    }
                    if (dialog.directory != null && dialog.file != null) {
                        onValueChange(File(dialog.directory, dialog.file).absolutePath)
                    }
                },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(backgroundColor = ThemeColors.SurfaceVariant, contentColor = ThemeColors.TextMain),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(if (isDirectory) "📂" else "📄", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun NavButton(icon: String, text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val targetBgColor = when {
        isSelected -> ThemeColors.Primary.copy(alpha = 0.15f)
        isHovered -> ThemeColors.SurfaceVariant
        else -> Color.Transparent
    }

    val bgColor = targetBgColor
    val textColor = if (isSelected || isHovered) ThemeColors.TextMain else ThemeColors.TextMuted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = textColor, fontSize = 15.sp)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ThemeColors.Surface.copy(alpha = 0.6f))
            .border(1.dp, ThemeColors.Border, RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        content()
    }
}

@Composable
fun modernTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = ThemeColors.TextMain,
    backgroundColor = ThemeColors.Background,
    focusedBorderColor = ThemeColors.Primary,
    unfocusedBorderColor = ThemeColors.Border,
    cursorColor = ThemeColors.Primary,
    focusedLabelColor = ThemeColors.Primary,
    unfocusedLabelColor = ThemeColors.TextMuted
)
