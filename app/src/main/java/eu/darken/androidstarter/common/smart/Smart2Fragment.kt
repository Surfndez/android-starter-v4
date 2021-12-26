package eu.darken.androidstarter.common.smart

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.LiveData
import androidx.viewbinding.ViewBinding
import eu.darken.androidstarter.common.debug.logging.log
import eu.darken.androidstarter.common.error.asErrorDialogBuilder
import eu.darken.androidstarter.common.navigation.doNavigate
import eu.darken.androidstarter.common.navigation.popBackStack


abstract class Smart2Fragment(@LayoutRes layoutRes: Int?) : SmartFragment(layoutRes) {

    constructor() : this(null)

    abstract val ui: ViewBinding?
    abstract val vdc: Smart2VM

    var onErrorEvent: ((Throwable) -> Boolean)? = null

    var onFinishEvent: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vdc.navEvents.observe2(ui) {
            log { "navEvents: $it" }

            it?.run { doNavigate(this) } ?: onFinishEvent?.invoke() ?: popBackStack()
        }

        vdc.errorEvents.observe2(ui) {
            val showDialog = onErrorEvent?.invoke(it) ?: true
            if (showDialog) it.asErrorDialogBuilder(requireContext()).show()
        }
    }

    inline fun <T> LiveData<T>.observe2(
        crossinline callback: (T) -> Unit
    ) {
        observe(viewLifecycleOwner) { callback.invoke(it) }
    }

    inline fun <T, reified VB : ViewBinding?> LiveData<T>.observe2(
        ui: VB,
        crossinline callback: VB.(T) -> Unit
    ) {
        observe(viewLifecycleOwner) { callback.invoke(ui, it) }
    }

}
