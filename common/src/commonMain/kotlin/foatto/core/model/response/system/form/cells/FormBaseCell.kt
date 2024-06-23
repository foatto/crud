package foatto.core.model.response.system.form.cells

import kotlinx.serialization.Serializable
import foatto.core.model.response.system.form.FormPinMode

@Serializable
sealed class FormBaseCell {
    abstract val name: String
    abstract val caption: String
    abstract val isEditable: Boolean
    abstract val isAutoFocus: Boolean
    abstract val formPinMode: FormPinMode
}

//    //--- STRING, INT, DOUBLE
//    var alComboString = listOf<String>()
//
//    //--- values are set externally
//    var alVisible = listOf<Triple<String, Boolean, Array<Int>>>()  // name, state, set<value>
//
//    //--- values are set externally
//    var alCaption = listOf<Triple<String, String, Array<Int>>>()  // name, string, set<value>