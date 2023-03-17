package foatto.core.link

import kotlinx.serialization.Serializable

@Serializable
class FormResponse(
    val tab: String,
    val alHeader: Array<Pair<String, String>>,
    //--- number of columns for GRID-form (> 0)
    //--- or display mode for normal form (== 0 or -1)
    //--- (in the desktop version, the narrow display mode is ignored - it is displayed as normal, because "reaching" a narrow screen on a computer / laptop is problematic)
    val columnCount: Int,
    val alFormColumn: Array<String>,
    val alFormCell: Array<FormCell>,
    val alFormButton: Array<FormButton>
)

@Serializable
class FormCell(val cellType: FormCellType) {

    //--- FOR ALL TYPES (values set externally)
    var caption = ""         // empty caption == hidden cell
    var itEditable = false
    var formPinMode = FormPinMode.AUTO
    var itAutoFocus = false

    //--- TYPE_STRING, TYPE_INT, TYPE_DOUBLE
    var name = ""
    var value = ""
    var column = 0
    var alComboString = arrayOf<String>()

    //--- TYPE_STRING
    var itPassword = false

    //--- TYPE_TEXT
    var textName = ""
    var textValue = ""
    var textRow = 0
    var textColumn = 0

    //--- TYPE_BOOLEAN
    var booleanName = ""
    var booleanValue = false
    var arrSwitchText = arrayOf<String>()

    //--- TYPE_DATE, TYPE_TIME, TYPE_DATE_TIME
    var alDateTimeField = arrayOf<Pair<String, String>>()    // name, value

    //--- TYPE_TIME, TYPE_DATE_TIME
    var withSecond = false

    //--- TYPE_COMBO, TYPE_RADIO
    var comboName = ""
    var comboValue = 0
    var alComboData = arrayOf<Pair<Int, String>>()         // value, descr

    //--- TYPE_FILE
    var fileName = ""
    var fileID = 0
    var alFile = arrayOf<Triple<Int, String, String>>()             // id, url, text (which is actually just a filename)

    var selectorSetURL = ""
    var selectorClearURL = ""
    var itAutoStartSelector = false // whether to run the selector immediately after opening the form

    //--- TYPE_STRING, TYPE_TEXT, TYPE_BOOLEAN, TYPE_INT, TYPE_DOUBLE,
    //--- TYPE_DATE, TYPE_TIME, TYPE_DATE_TIME, TYPE_COMBO
    //--- (значения устанавливаются извне)
    var errorMessage = ""

    //--- values are set externally
    var alVisible = arrayOf<Triple<String, Boolean, Array<Int>>>()  // name, state, set<value>

    //--- values are set externally
    var alCaption = arrayOf<Triple<String, String, Array<Int>>>()  // name, string, set<value>

//-----------------------------------------------------------------------------------------------------

    constructor(aBooleanName: String, aBooleanValue: Boolean, aArrSwitch: Array<String>) : this(FormCellType.BOOLEAN) {
        booleanName = aBooleanName
        booleanValue = aBooleanValue
        arrSwitchText = aArrSwitch
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

