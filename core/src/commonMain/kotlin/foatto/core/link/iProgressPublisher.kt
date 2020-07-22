package foatto.core.link

interface iProgressPublisher {

    val isCancel: Boolean

    fun showProgress( curValue: Int, maxValue: Int )
}
