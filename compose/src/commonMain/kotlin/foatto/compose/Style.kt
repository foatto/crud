package foatto.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

//--- по умолчанию - тёмные иконки на светлом фоне кнопок,
//--- но в material 3 design (да и в прочих дизайнах) - контраст к основному фону - светлые иконки/текст на тёмном фоне иконок
var styleToolbarDarkIcon: Boolean = true
var styleOtherDarkIcon: Boolean = true

//--- по умолчанию - иконки размером 36dp (пока только на toolbar'ах, в остальных местах 48)
var styleToolbarIconSize: Int = 36
var styleOtherIconSize: Int = 48

//--- суффикс наименовани типовой иконки material design
fun getStyleToolbarIconNameSuffix(): String = (if (styleToolbarDarkIcon) "black" else "white") + "_" + styleToolbarIconSize + "dp"
fun getStyleOtherIconNameSuffix(): String = (if (styleOtherDarkIcon) "black" else "white") + "_" + styleOtherIconSize + "dp"

//--- different gray tones by default
private const val MAIN_BACK_LIGHTNESS_0 = 0.97f
private const val MAIN_BACK_LIGHTNESS_1 = 0.94f
private const val MAIN_BACK_LIGHTNESS_2 = 0.88f
private const val MAIN_BACK_LIGHTNESS_3 = 0.82f

val colorMainBack0: Color = Color.hsl(0.0f, 0.0f, 1.0f)                             // input field, etc.
private val colorMainBack1: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_0)    // form panel
private val colorMainBack2: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_1)    // active tab + header + toolbar
private val colorMainBack3: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_2)    // non-active tab
val colorMainBack4: Color = Color.hsl(0.0f, 0.0f, MAIN_BACK_LIGHTNESS_3)    // ?

val colorControlBack: Color = colorMainBack0
val colorTableSelectorBack: Color = colorMainBack0  //.copy(alpha = 0.9f)
val colorFormBack: Color = colorMainBack1
val colorTabActive: Color = colorMainBack2
val colorTabInactive: Color = colorMainBack3
val colorHeader: Color = colorTabActive
val colorToolBar: Color = colorHeader
val colorBottomBar: Color = colorToolBar
val colorScrollBarBack: Color = colorMainBack2
val colorScrollBarFore: Color = Color.hsl(0f, 0f, 0.85f)

var colorWait: Color? = null
var colorWaitTrack: Color? = null

var colorTabSelected: Color? = null
var colorTabUnselected: Color? = null

var colorTableCellBorder: Color = Color.hsl(0f, 0f, 0.9f)
var colorTableGroupBack0: Color = Color.hsl(0f, 0f, 0.85f)
var colorTableGroupBack1: Color = Color.hsl(0f, 0f, 0.80f)
var colorTableRowBack0: Color = Color.hsl(0f, 0f, 1.0f)
var colorTableRowBack1: Color = Color.hsl(0f, 0f, 0.95f)
var colorTableCurrentRow: Color = colorMainBack4    // по дефолту - максимальмо мрачный цвет, чтобы сразу переделать захотелось

val colorMainText: Color = Color.Black

var colorTextButton: ButtonColors? = null
var colorTextButtonDefault: ButtonColors? = null
var colorIconButton: IconButtonColors? = null
var colorOutlinedTextInput: TextFieldColors? = null
var colorCheckBox: CheckboxColors? = null
var colorRadioButton: RadioButtonColors? = null

@OptIn(ExperimentalMaterial3Api::class)
var colorDatePicker: DatePickerColors? = null
@OptIn(ExperimentalMaterial3Api::class)
var colorTimePicker: TimePickerColors? = null

val alertDialogShape: Shape = RoundedCornerShape(8.dp)
val singleButtonShape: Shape = RoundedCornerShape(4.dp)

var colorCompositeMovedBlockBack: Color = Color.White