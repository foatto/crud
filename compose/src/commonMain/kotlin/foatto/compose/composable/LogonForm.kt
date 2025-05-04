package foatto.compose.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import foatto.compose.colorCheckBox
import foatto.compose.colorOutlinedTextInput
import foatto.compose.colorTextButton
import foatto.compose.singleButtonShape
import foatto.compose.utils.getFullUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun LogonForm(
    modifier: Modifier,
    errorText: String?,
    loginError: String?,
    passwordError: String?,
    login: String,
    password: String,
    isRememberMe: Boolean,
    onLoginInput: (String) -> Unit,
    onPasswordInput: (String) -> Unit,
    onIsRememberMeChange: (Boolean) -> Unit,
    logon: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var isFocusRequesterDefined = false
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
    ) {
        KamelImage(
            modifier = Modifier.size(128.dp).align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
            resource = asyncPainterResource(data = getFullUrl("/images/logo.png")),
            contentDescription = null,
        )
        errorText?.let {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = errorText,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
            )
        }
        OutlinedTextField(
            modifier = Modifier
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter) {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    } else {
                        false
                    }
                }
                .then(
                    run {
                        isFocusRequesterDefined = true
                        Modifier.focusRequester(focusRequester)
                    }
                ),
            colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
            value = login,
            onValueChange = { newText ->
                onLoginInput(newText)
            },
            label = { Text("Имя") },
            isError = loginError != null,
            supportingText = loginError?.let {
                {
                    Text(
                        text = loginError,
                        color = Color.Red,
                    )
                }
            },
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter) {
                        logon()
                        true
                    } else {
                        false
                    }
                },
            colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
            value = password,
            onValueChange = { newText ->
                onPasswordInput(newText)
            },
            label = { Text("Пароль") },
            isError = passwordError != null,
            supportingText = passwordError?.let {
                {
                    Text(
                        text = passwordError,
                        color = Color.Red,
                    )
                }
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        Row {
            Checkbox(
                modifier = Modifier
                    .background(Color.Transparent)
                    .align(Alignment.CenterVertically),
                colors = colorCheckBox ?: CheckboxDefaults.colors(),
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
            shape = singleButtonShape,
            colors = colorTextButton ?: ButtonDefaults.buttonColors(),
            onClick = logon,
        ) {
            Text(
                text = "Вход",
            )
        }
    }

//!!! TODO: ждём исправления бага с автофокусом (сейчас курсов визуально в поле, но ввод не происходит)
//    LaunchedEffect(Unit) {
//        if (isFocusRequesterDefined) {
//            focusRequester.requestFocus()
//        }
//    }
}