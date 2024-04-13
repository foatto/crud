package foatto.core_compose.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import foatto.core_compose.di.hmNameToImageVector
import foatto.core_compose.di.hmNameToUrl
import foatto.core_compose.utils.getFullUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
internal fun ImageOrTextFromName(name: String, tooltip: String?) {
    hmNameToImageVector[name]?.let { imageVector ->
        Icon(
            imageVector = imageVector,
            contentDescription = tooltip,
        )
    } ?: run {
        hmNameToUrl[name]?.let { url ->
            KamelImage(
                resource = asyncPainterResource(data = getFullUrl(url)),
                contentDescription = tooltip
            )
        } ?: run {
            if (name.startsWith('/')) {
                KamelImage(
                    resource = asyncPainterResource(data = getFullUrl(name)),
                    contentDescription = tooltip
                )
            } else {
                Text(text = name)
            }
        }
    }
}
