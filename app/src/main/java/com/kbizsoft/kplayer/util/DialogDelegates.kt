package com.kbizsoft.KPlayer.util

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.kbizsoft.libkplayer.Dialog
import com.kbizsoft.KPlayer.gui.dialogs.KplayerLoginDialog
import com.kbizsoft.KPlayer.gui.dialogs.KplayerProgressDialog
import com.kbizsoft.KPlayer.gui.dialogs.KplayerQuestionDialog
import kbizsoft.com.commontools.LiveEvent

private const val TAG = "DialogDelegate"

interface IDialogHandler

interface IDialogDelegate {
    fun observeDialogs(lco: LifecycleOwner, manager: IDialogManager)
}

interface IDialogManager {
    fun fireDialog(dialog: Dialog)
    fun dialogCanceled(dialog: Dialog?)
}

class DialogDelegate : IDialogDelegate {

    override fun observeDialogs(lco: LifecycleOwner, manager: IDialogManager) {
        dialogEvt.observe(lco) {
            when (it) {
                is Show -> manager.fireDialog(it.dialog)
                is Cancel -> manager.dialogCanceled(it.dialog)
            }
        }
    }

    companion object DialogsListener : Dialog.Callbacks {
        private val dialogEvt: LiveEvent<DialogEvt> = LiveEvent()
        var dialogCounter = 0

        override fun onProgressUpdate(dialog: Dialog.ProgressDialog) {
            val kplayerProgressDialog = dialog.context as? KplayerProgressDialog ?: return
            if (kplayerProgressDialog.isVisible) kplayerProgressDialog.updateProgress()
        }

        override fun onDisplay(dialog: Dialog.ErrorMessage) {
            dialogEvt.value = Cancel(dialog)
        }

        override fun onDisplay(dialog: Dialog.LoginDialog) {
            dialogEvt.value = Show(dialog)
        }

        override fun onDisplay(dialog: Dialog.QuestionDialog) {
            dialogEvt.value = Show(dialog)
        }

        override fun onDisplay(dialog: Dialog.ProgressDialog) {
            dialogEvt.value = Show(dialog)
        }

        override fun onCanceled(dialog: Dialog?) {
            (dialog?.context as? DialogFragment)?.dismiss()
            dialogEvt.value = Cancel(dialog)
        }
    }
}

fun Fragment.showKplayerDialog(dialog: Dialog) {
    activity?.showKplayerDialog(dialog)
}

@Suppress("INACCESSIBLE_TYPE")
fun FragmentActivity.showKplayerDialog(dialog: Dialog) {
    val dialogFragment = when (dialog) {
        is Dialog.LoginDialog -> KplayerLoginDialog().apply {
            kplayerDialog = dialog
        }
        is Dialog.QuestionDialog -> KplayerQuestionDialog().apply {
            kplayerDialog = dialog
        }
        is Dialog.ProgressDialog -> KplayerProgressDialog().apply {
            kplayerDialog = dialog
        }
        else -> null
    } ?: return
    val fm = supportFragmentManager
    dialogFragment.show(fm, "kplayer_dialog_${++DialogDelegate.dialogCounter}")
}

private sealed class DialogEvt
private class Show(val dialog: Dialog) : DialogEvt()
private class Cancel(val dialog: Dialog?) : DialogEvt()