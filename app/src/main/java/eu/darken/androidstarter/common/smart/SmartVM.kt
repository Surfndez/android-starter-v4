package eu.darken.androidstarter.common.smart

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import eu.darken.androidstarter.common.coroutine.DefaultDispatcherProvider
import eu.darken.androidstarter.common.coroutine.DispatcherProvider
import eu.darken.androidstarter.common.debug.logging.Logging.Priority.WARN
import eu.darken.androidstarter.common.debug.logging.asLog
import eu.darken.androidstarter.common.debug.logging.log
import eu.darken.androidstarter.common.error.ErrorEventSource
import eu.darken.androidstarter.common.flow.DynamicStateFlow
import eu.darken.androidstarter.common.viewmodel.VM
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlin.coroutines.CoroutineContext


abstract class SmartVM(
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
) : VM() {

    val vmScope = viewModelScope + dispatcherProvider.Default

    var launchErrorHandler: CoroutineExceptionHandler? = null

    private fun getVDCContext(): CoroutineContext {
        val dispatcher = dispatcherProvider.Default
        return getErrorHandler()?.let { dispatcher + it } ?: dispatcher
    }

    private fun getErrorHandler(): CoroutineExceptionHandler? {
        val handler = launchErrorHandler
        if (handler != null) return handler

        if (this is ErrorEventSource) {
            return CoroutineExceptionHandler { _, ex ->
                log(WARN) { "Error during launch: ${ex.asLog()}" }
                errorEvents.postValue(ex)
            }
        }

        return null
    }

    fun <T : Any> DynamicStateFlow<T>.asLiveData2() = flow.asLiveData2()

    fun <T> Flow<T>.asLiveData2() = this.asLiveData(context = getVDCContext())

    fun launch(
        scope: CoroutineScope = viewModelScope,
        context: CoroutineContext = getVDCContext(),
        block: suspend CoroutineScope.() -> Unit
    ) {
        try {
            scope.launch(context = context, block = block)
        } catch (e: CancellationException) {
            log(TAG, WARN) { "launch()ed coroutine was canceled (scope=$scope): ${e.asLog()}" }
        }
    }

    open fun <T> Flow<T>.launchInViewModel() = this.launchIn(vmScope)

}