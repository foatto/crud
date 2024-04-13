package foatto.core_compose.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import foatto.core.link.ResponseCode
import foatto.core_compose.utils.getFullUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun LogonForm(
    modifier: Modifier,
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
        modifier = modifier
    ) {
        KamelImage(
            modifier = Modifier.size(128.dp).align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
            resource = asyncPainterResource(data = getFullUrl("/web/images/logo.png")),
            contentDescription = null,
        )
        if (responseCode == ResponseCode.LOGON_FAILED) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "Неправильное имя или пароль!",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedTextField(
            value = login,
            onValueChange = { newText ->
                onLoginInput(newText)
            },
            label = { Text("Имя") },
            singleLine = true,
        )
        OutlinedTextField(
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
                modifier = Modifier.align(Alignment.CenterVertically),
                checked = isRememberMe,
                onCheckedChange = { newState ->
                    onIsRememberMeChange(newState)
                },
            )
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = "Запомнить меня",
            )
        }
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = logon,
        ) {
            Text(
                text = "Вход",
            )
        }
    }
}