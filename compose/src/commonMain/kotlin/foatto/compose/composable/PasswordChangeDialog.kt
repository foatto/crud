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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import foatto.compose.alertDialogShape
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorTextButton
import foatto.compose.colorTextButtonDefault
import foatto.compose.singleButtonShape

@Composable
fun PasswordChangeDialog(
    onOkClick: (String) -> Unit,
    onCancelClick: () -> Unit,
) {
    var password1 by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    AlertDialog(
        shape = alertDialogShape,
        text = {
            Column {
                Text(text = "Введите новый пароль:")
                OutlinedTextField(
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = password1,
                    onValueChange = { newPass ->
                        password1 = newPass
                        errorMessage = ""
                    },
                    label = { Text("Новый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
                    value = password2,
                    onValueChange = { newPass ->
                        password2 = newPass
                        errorMessage = ""
                    },
                    label = { Text("Ещё раз") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
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
                    if (password1 != password2) {
                        errorMessage = "Вы ввели разные пароли. Попробуйте ввести ещё раз."
                    } else if (password1.length < 3) {
                        errorMessage = "Слишком короткий пароль. Попробуйте ввести ещё раз."
                    } else {
                        errorMessage = ""
                        onOkClick(password1)
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

/*
//--- проверку комплексности пароля пока пропустим. Не любят этого пользователи.
if(  pwd.length() < 8  ) return false;
if(  pwd.equals(  pwd.toUpperCase()  )  ) return false;
if(  pwd.equals(  pwd.toLowerCase()  )  ) return false;
boolean haveDigit = false;
for(  int i = 0; i < 10; i++  )
    if(  pwd.indexOf(  "" + i  ) >= 0  ) {
        haveDigit = true;
        break;
    }
return haveDigit;
 */