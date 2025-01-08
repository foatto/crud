package foatto.compose.control.composable

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@Composable
fun Modifier.onPointerEvents(
    withInteractive: Boolean,
    onPointerDown: (PointerInputChange) -> Unit,
    onPointerUp: (PointerInputChange) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: suspend () -> Unit,
    onDragCancel: () -> Unit,
): Modifier {
    val coroutineScope = rememberCoroutineScope()

    return this
        .pointerInput(Unit) {
            if (withInteractive) {
                awaitEachGesture {
                    awaitFirstDown().also { pointerInputChange: PointerInputChange ->
                        pointerInputChange.consume()
                        onPointerDown(pointerInputChange)
                    }
                    while (true) {
                        val pointerInputChange = waitForUpOrCancellation()
                        if (pointerInputChange != null) {
                            pointerInputChange.consume()
                            onPointerUp(pointerInputChange)
                            break
                        }
                    }
                }
            }
        }
        .pointerInput(Unit) {
            if (withInteractive) {
                detectDragGestures(
                    onDragStart = { offset: Offset -> onDragStart(offset) },
                    onDragEnd = { coroutineScope.launch { onDragEnd() } },
                    onDragCancel = { onDragCancel() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(change, dragAmount)
                    },
                )
            }
        }
        .pointerInput(Unit) {
            if (withInteractive) {
                //awaitPointerEventScope { - не работает
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val change = event.changes.first()
                        val scrollDelta = change.scrollDelta
                        onDrag(change, scrollDelta.copy(y = -scrollDelta.y))
                    }
                }
            }
        }
}
