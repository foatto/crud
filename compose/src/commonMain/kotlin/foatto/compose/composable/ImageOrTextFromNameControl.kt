package foatto.compose.composable

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.compose.utils.getFullUrl
import foatto.compose.utils.hmNameToImageVector
import foatto.compose.utils.hmNameToUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
internal fun ImageOrTextFromNameControl(
    name: String,
    iconSize: Int,
    imageButton: @Composable (@Composable (() -> Unit)) -> Unit,
    textButton: @Composable (caption: String) -> Unit,
) {
    hmNameToImageVector[name]?.let { imageVector ->
        imageButton {
            Icon(
                imageVector = imageVector,
                contentDescription = name,
            )
        }
    } ?: run {
        hmNameToUrl[name]?.let { url ->
            imageButton {
                KamelImage(
                    modifier = Modifier.size(iconSize.dp),
                    resource = asyncPainterResource(data = getFullUrl(url)),
                    contentDescription = name,
                )
            }
        } ?: run {
            if (name.startsWith('/')) {
                imageButton {
                    KamelImage(
                        modifier = Modifier.size(iconSize.dp),
                        resource = asyncPainterResource(data = getFullUrl(name)),
                        contentDescription = name,
                    )
                }
            } else {
                textButton(name)
            }
        }
    }
}
