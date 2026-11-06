package org.futo.inputmethod.latin.uix.actions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action
import java.io.IOException

private const val EXTRA_SELECTED_TEXT = "org.futo.inputmethod.latin.SELECTED_TEXT"

val SaveAction = Action(
    icon = R.drawable.save,
    name = R.string.action_save_title,
    simplePressImpl = { manager, _ ->
        val selectedText = manager.getSelectedText()
        val context = manager.getContext()

        if (selectedText.isNullOrEmpty()) {
            Toast.makeText(context, R.string.action_save_no_selection, Toast.LENGTH_SHORT).show()
        } else {
            // A service cannot receive an Activity result directly. Use an internal activity
            // to launch the system document picker and write the selected text to its URI.
            val intent = Intent(context, SaveSelectedTextActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra(EXTRA_SELECTED_TEXT, selectedText)
            }
            try {
                context.startActivity(intent)
            } catch (_: RuntimeException) {
                Toast.makeText(context, R.string.action_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    },
    windowImpl = null,
)

/**
 * Receives a document URI from Android's Storage Access Framework.
 * No storage permission or clipboard access is required.
 */
class SaveSelectedTextActivity : ComponentActivity() {
    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val text = intent?.getStringExtra(EXTRA_SELECTED_TEXT)
        if (uri == null || text == null) {
            finish()
            return@registerForActivityResult
        }

        lifecycleScope.launch {
            val succeeded = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri, "w")?.use { output ->
                        output.write(text.toByteArray(Charsets.UTF_8))
                    } ?: throw IOException("Could not open selected document for writing")
                }.isSuccess
            }

            Toast.makeText(
                this@SaveSelectedTextActivity,
                if (succeeded) R.string.action_save_success else R.string.action_save_failed,
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.getStringExtra(EXTRA_SELECTED_TEXT).isNullOrEmpty()) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            try {
                createDocument.launch("selection.txt")
            } catch (_: RuntimeException) {
                Toast.makeText(this, R.string.action_save_failed, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
