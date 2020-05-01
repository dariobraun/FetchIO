package de.hsbo.fetch

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment


class ItemInfoDialogFragment : DialogFragment() {
    private var mListener: OnAddInfoButtonClick? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = context as OnAddInfoButtonClick
    }

    // Override to show soft keyboard on opening dialog and focusing "additional_info_dialog" text input
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val inflater = requireActivity().layoutInflater
            // get reference to EditText for accessing user input on positive button click
            val dialogView = inflater.inflate(R.layout.additional_info_dialog, null)
            val userInput = dialogView.findViewById<EditText>(R.id.pt_additional_text)
            // Request focus for "additional_info_dialog" text input
            userInput.requestFocus()

            val builder = AlertDialog.Builder(it)
            builder.setView(dialogView)
            builder.setMessage(R.string.item_info_dialog_heading)
                .setPositiveButton(
                    R.string.add_btn
                ) { dialog, id ->
                    mListener?.onAddItemInfoClicked(userInput.text.toString())
                }
                .setNegativeButton(
                    R.string.cancel_btn
                ) { dialog, id ->
                    // User cancelled the dialog
                }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

interface OnAddInfoButtonClick {
    fun onAddItemInfoClicked(input: String)
}