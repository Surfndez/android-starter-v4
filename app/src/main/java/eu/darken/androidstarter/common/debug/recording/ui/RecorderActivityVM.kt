package eu.darken.androidstarter.common.debug.recording.ui


import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.androidstarter.R
import eu.darken.androidstarter.common.BuildConfigWrap
import eu.darken.androidstarter.common.PrivacyPolicy
import eu.darken.androidstarter.common.WebpageTool
import eu.darken.androidstarter.common.compression.Zipper
import eu.darken.androidstarter.common.coroutine.DispatcherProvider
import eu.darken.androidstarter.common.debug.logging.logTag
import eu.darken.androidstarter.common.flow.DynamicStateFlow
import eu.darken.androidstarter.common.flow.onError
import eu.darken.androidstarter.common.flow.replayingShare
import eu.darken.androidstarter.common.livedata.SingleLiveEvent
import eu.darken.androidstarter.common.uix.ViewModel3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecorderActivityVM @Inject constructor(
    handle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    @ApplicationContext private val context: Context,
    private val webpageTool: WebpageTool,
) : ViewModel3(dispatcherProvider) {

    private val recordedPath = handle.get<String>(RecorderActivity.RECORD_PATH)!!
    private val pathCache = MutableStateFlow(recordedPath)
    private val resultCacheObs = pathCache
        .map { path -> Pair(path, File(path).length()) }
        .replayingShare(vmScope)

    private val resultCacheCompressedObs = resultCacheObs
        .map { uncompressed ->
            val zipped = "${uncompressed.first}.zip"
            Zipper().zip(arrayOf(uncompressed.first), zipped)
            Pair(zipped, File(zipped).length())
        }
        .replayingShare(vmScope + dispatcherProvider.IO)

    private val stater = DynamicStateFlow(TAG, vmScope) { State() }
    val state = stater.asLiveData2()

    val shareEvent = SingleLiveEvent<Intent>()

    init {
        resultCacheObs
            .onEach { (path, size) ->
                stater.updateBlocking { copy(normalPath = path, normalSize = size) }
            }
            .launchInViewModel()

        resultCacheCompressedObs
            .onEach { (path, size) ->
                stater.updateBlocking {
                    copy(
                        compressedPath = path,
                        compressedSize = size,
                        loading = false
                    )
                }
            }
            .onError { errorEvents.postValue(it) }
            .launchInViewModel()

    }

    fun share() = launch {
        val (path, size) = resultCacheCompressedObs.first()

        val intent = Intent(Intent.ACTION_SEND).apply {
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfigWrap.APPLICATION_ID + ".provider",
                File(path)
            )

            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            type = "application/zip"

            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "${BuildConfigWrap.APPLICATION_ID} DebugLog - ${BuildConfigWrap.VERSION_DESCRIPTION})"
            )
            putExtra(Intent.EXTRA_TEXT, "Your text here.")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }


        val chooserIntent = Intent.createChooser(intent, context.getString(R.string.debug_debuglog_file_label))
        shareEvent.postValue(chooserIntent)
    }

    fun goPrivacyPolicy() {
        webpageTool.open(PrivacyPolicy.URL)
    }

    data class State(
        val normalPath: String? = null,
        val normalSize: Long = -1L,
        val compressedPath: String? = null,
        val compressedSize: Long = -1L,
        val loading: Boolean = true
    )

    companion object {
        private val TAG = logTag("Debug", "eu.darken.androidstarter.common.debug.recording.core.Recorder", "VM")
    }
}