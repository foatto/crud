package foatto.core_compose.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import foatto.core.link.ResponseCode

@Composable
fun LogonForm(
    responseCode: ResponseCode,
    login: String,
    password: String,
    isRememberMe: Boolean,
    onLoginInput: (String) -> Unit,
    onPasswordInput: (String) -> Unit,
    onIsRememberMeChange: (Boolean) -> Unit,
    logon: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        //!!! Logon Logo
        if (responseCode == ResponseCode.LOGON_FAILED) {
            Text(
                text = "Неправильное имя или пароль",
            )
        }
        TextField(
            value = login,
            onValueChange = { newText ->
                onLoginInput(newText)
            },
            label = { Text("Имя") },
            singleLine = true,
        )
        TextField(
            value = password,
            onValueChange = { newText ->
                onPasswordInput(newText)
            },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            //!!! обязательно ли использование?
            //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Row {
            Checkbox(
                checked = isRememberMe,
                onCheckedChange = { newState ->
                    onIsRememberMeChange(newState)
                },
            )
            Text(
                text = "Запомнить меня",
            )
        }
        Button(
            onClick = logon,
        ) {
            Text(
                text = "Вход",
            )
        }
    }
}