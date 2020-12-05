package foatto.core.link

class FormResponse(
    val tab: String,
    val alHeader: List<Pair<String,String>>,
    //--- кол-во столбцов для GRID-формы (  > 0  )
    //--- или режим отображения для обычной формы (  == 0 или -1  )
    //--- (в настольной версии узкий режим отображения игнорируется - отображается как обычный, т.к. "достигнуть" узкого экрана на компе/ноуте проблематично)
    val columnCount: Int,
    val alFormColumn: List<String>,
    val alFormCell: List<FormCell>,
    val alFormButton: List<FormButton>
)

class FormCell( val cellType: FormCellType ) {

    //--- ДЛЯ ВСЕХ ТИПОВ (значения устанавливаются извне)
    var caption = ""         // empty caption == hidden cell
    var itEditable = false
    var formPinMode = FormPinMode.AUTO

    //--- TYPE_STRING, TYPE_INT, TYPE_DOUBLE
    var name = ""
    var value = ""
    var column = 0
    val alComboString = mutableListOf<String>()

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

    //--- TYPE_DATE, TYPE_TIME, TYPE_DATE_TIME
    val alDateTimeField = mutableListOf<Pair<String,String>>()    // name, value

    //--- TYPE_TIME, TYPE_DATE_TIME
    var withSecond = false

    //--- TYPE_COMBO, TYPE_RADIO
    var comboName = ""
    var comboValue = 0
    val alComboData = mutableListOf<Pair<Int,String>>()         // value, descr

    //--- TYPE_FILE
    var fileName = ""
    var fileID = 0
    val alFile = mutableListOf<Triple<Int,String,String>>()             // id, url, text (который на самом деле просто имя файла)

    var selectorSetURL = ""
    var selectorClearURL = ""
    var itAutoStartSelector = false // запускать ли селектор сразу после открытия формы

    //--- TYPE_STRING, TYPE_TEXT, TYPE_BOOLEAN, TYPE_INT, TYPE_DOUBLE,
    //--- TYPE_DATE, TYPE_TIME, TYPE_DATE_TIME, TYPE_COMBO
    //--- (значения устанавливаются извне)
    var errorMessage = ""

    //--- значения устанавливаются извне
    val alVisible = mutableListOf<Triple<String, Boolean, Set<Int>>>()  // name, state, set<value>

    //--- значения устанавливаются извне
    val alCaption = mutableListOf<Triple<String, String, Set<Int>>>()  // name, string, set<value>

//-----------------------------------------------------------------------------------------------------

    constructor( aBooleanName: String, aBooleanValue: Boolean ) : this( FormCellType.BOOLEAN ) {
        booleanName = aBooleanName
        booleanValue = aBooleanValue
    }

}

enum class FormCellType { STRING, INT, DOUBLE, TEXT, BOOLEAN, DATE, TIME, DATE_TIME, COMBO, RADIO, FILE }

enum class FormPinMode { OFF, AUTO, ON }

class FormButton(
    val url: String,
    val caption: String,
    val iconName: String,
    val withNewData: Boolean,
    val key: Int
)

