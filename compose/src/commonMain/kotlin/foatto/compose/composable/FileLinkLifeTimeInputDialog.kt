package foatto.compose.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import foatto.compose.alertDialogShape
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorTextButton
import foatto.compose.colorTextButtonDefault
import foatto.compose.singleButtonShape

@Composable
fun FileLinkLifeTimeInputDialog(
    onOkClick: (Int) -> Unit,
    onCancelClick: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        shape = alertDialogShape,
        text = {
            Column {
                Text(text = "Введите время жизни короткой ссылки [час]:")
                OutlinedTextField(
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        errorMessage = ""
                    },
                    singleLine = true,
                )
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = errorMessage,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        onDismissRequest = {
            onCancelClick()
        },
        confirmButton = {
            TextButton(
                shape = singleButtonShape,
                colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                onClick = {
                    text.toIntOrNull()?.let { hour ->
                        errorMessage = ""
                        onOkClick(hour)
                    } ?: run {
                        errorMessage = "Введите целое число"
                    }
                }
            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(
                shape = singleButtonShape,
                colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                onClick = {
                    errorMessage = ""
                    onCancelClick()
                }
            ) {
                Text(text = "Отмена")
            }
        },
    )
}
