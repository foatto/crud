package foatto.core_compose.composables

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.core_compose.utils.hmNameToImageVector
import foatto.core_compose.utils.hmNameToUrl
import foatto.core_compose.utils.getFullUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
internal fun ImageOrTextFromName(
    name: String,
    tooltip: String?,
    iconSize: Int,
) {
    hmNameToImageVector[name]?.let { imageVector ->
        Icon(
            imageVector = imageVector,
            contentDescription = tooltip,
        )
    } ?: run {
        hmNameToUrl[name]?.let { url ->
            KamelImage(
                modifier = Modifier.size(iconSize.dp),
                resource = asyncPainterResource(data = getFullUrl(url)),
                contentDescription = tooltip,
            )
        } ?: run {
            if (name.startsWith('/')) {
                KamelImage(
                    modifier = Modifier.size(iconSize.dp),
                    resource = asyncPainterResource(data = getFullUrl(name)),
                    contentDescription = tooltip,
                )
            } else {
                Text(
                    text = name,
                )
            }
        }
    }
}
