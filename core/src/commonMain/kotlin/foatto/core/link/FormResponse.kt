package foatto.core.link

import kotlinx.serialization.Serializable

@Serializable
class FormResponse(
    val tab: String,
    val alHeader: List<Pair<String, String>>,
    //--- number of columns for GRID-form (> 0)
    //--- or display mode for normal form (== 0 or -1)
    //--- (in the desktop version, the narrow display mode is ignored - it is displayed as normal, because "reaching" a narrow screen on a computer / laptop is problematic)
    val columnCount: Int,
    val alFormColumn: List<String>,
    val alFormCell: List<FormCell>,
    val alFormButton: List<FormButton>
)

@Serializable
class FormCell(val cellType: FormCellType) {

    //--- FOR ALL TYPES (values set externally)
    var caption = ""         // empty caption == hidden cell
    var isEditable = false
    var formPinMode = FormPinMode.AUTO
    var isAutoFocus = false

    //--- TYPE_STRING, TYPE_INT, TYPE_DOUBLE
    var name = ""
    var value = ""
    var column = 0
    var alComboString = listOf<String>()

    //--- TYPE_STRING
    var isPassword = false

    //--- TYPE_TEXT
    var textName = ""
    var textValue = ""
    var textRow = 0
    var textColumn = 0

    //--- TYPE_BOOLEAN
    var booleanName = ""
    var booleanValue = false
    var alSwitchText = listOf<String>()

    //--- TYPE_DATE, TYPE_TIME, TYPE_DATE_TIME
    var alDateTimeField = listOf<Pair<String, String>>()    // name, value

    //--- TYPE_TIME, TYPE_DATE_TIME
    var withSecond = false

    //--- TYPE_COMBO, TYPE_RADIO
    var comboName = ""
    var comboValue = 0
    var alComboData = listOf<Pair<Int, String>>()         // value, descr

    //--- TYPE_FILE
    var fileName = ""
    var fileID = 0
    var alFile = listOf<Triple<Int, String, String>>()             // id, url, text (which is actually just a filename)

    var selectorSetURL = ""
    var selectorClearURL = ""
    var isAutoStartSelector = false // whether to run the selector immediately after opening the form

    //--- TYPE_STRING, TYPE_TEXT, TYPE_BOOLEAN, TYPE_INT, TYPE_DOUBLE,
    //--- TYPE_DATE, TYPE_TIME, TYPE_DATE_TIME, TYPE_COMBO
    //--- (значения устанавливаются извне)
    var errorMessage = ""

    //--- values are set externally
    var alVisible = listOf<Triple<String, Boolean, Array<Int>>>()  // name, state, set<value>

    //--- values are set externally
    var alCaption = listOf<Triple<String, String, Array<Int>>>()  // name, string, set<value>

//-----------------------------------------------------------------------------------------------------

    constructor(aBooleanName: String, aBooleanValue: Boolean, aAlSwitch: List<String>) : this(FormCellType.BOOLEAN) {
        booleanName = aBooleanName
        booleanValue = aBooleanValue
        alSwitchText = aAlSwitch
    }

}

enum class FormCellType { STRING, INT, DOUBLE, TEXT, BOOLEAN, DATE, TIME, DATE_TIME, COMBO, RADIO, FILE }

enum class FormPinMode { OFF, AUTO, ON }

@Serializable
class FormButton(
    val url: String,
    val caption: String,
    val iconName: String,
    val withNewData: Boolean,
    val key: Int,
    val question: String? = null,
)

