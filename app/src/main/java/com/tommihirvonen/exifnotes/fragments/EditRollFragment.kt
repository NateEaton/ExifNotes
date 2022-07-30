/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.FragmentEditRollBinding
import com.tommihirvonen.exifnotes.datastructures.Camera
import com.tommihirvonen.exifnotes.datastructures.DateTime
import com.tommihirvonen.exifnotes.datastructures.FilmStock
import com.tommihirvonen.exifnotes.datastructures.Roll
import com.tommihirvonen.exifnotes.dialogs.EditCameraDialog
import com.tommihirvonen.exifnotes.dialogs.EditFilmStockDialog
import com.tommihirvonen.exifnotes.dialogs.SelectFilmStockDialog
import com.tommihirvonen.exifnotes.utilities.*
import com.tommihirvonen.exifnotes.viewmodels.RollViewModel

/**
 * Dialog to edit Roll's information
 */
class EditRollFragment : Fragment() {

    private val model by activityViewModels<RollViewModel>()
    private var cameras = emptyList<Camera>()
    
    private lateinit var binding: FragmentEditRollBinding

    private val roll by lazy { requireArguments().getParcelable(ExtraKeys.ROLL) ?: Roll() }
    private val newRoll by lazy { roll.copy() }

    private lateinit var dateLoadedManager: DateTimeLayoutManager
    private lateinit var dateUnloadedManager: DateTimeLayoutManager
    private lateinit var dateDevelopedManager: DateTimeLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        model.cameras.observe(viewLifecycleOwner) { cameras ->
            this.cameras = cameras
        }

        binding = FragmentEditRollBinding.inflate(inflater, container, false)

        val transitionName = requireArguments().getString(ExtraKeys.TRANSITION_NAME)
        binding.root.transitionName = transitionName
        binding.title.titleTextView.text = requireArguments().getString(ExtraKeys.TITLE)

        // NAME EDIT TEXT
        binding.nameEditText.addTextChangedListener { binding.nameLayout.error = null }
        binding.nameEditText.setText(roll.name)
        // Place the cursor at the end of the input field
        binding.nameEditText.setSelection(binding.nameEditText.text?.length ?: 0)
        binding.nameEditText.isSingleLine = false


        // NOTE EDIT TEXT
        binding.noteEditText.isSingleLine = false
        binding.noteEditText.setText(roll.note)
        binding.noteEditText.setSelection(binding.noteEditText.text?.length ?: 0)


        // FILM STOCK PICK DIALOG
        roll.filmStock?.let {
            binding.filmStockLayout.text = it.name
            binding.addFilmStock.visibility = View.GONE
            binding.clearFilmStock.visibility = View.VISIBLE
        } ?: run {
            binding.filmStockLayout.text = null
            binding.clearFilmStock.visibility = View.GONE
            binding.addFilmStock.visibility = View.VISIBLE
        }
        binding.clearFilmStock.setOnClickListener {
            newRoll.filmStock = null
            binding.filmStockLayout.text = null
            binding.clearFilmStock.visibility = View.GONE
            binding.addFilmStock.visibility = View.VISIBLE
        }
        binding.addFilmStock.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
            val dialog = EditFilmStockDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewFilmStock))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), null)
            dialog.setFragmentResultListener("EditFilmStockDialog") { _, bundle ->
                val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                    ?: return@setFragmentResultListener
                database.addFilmStock(filmStock)
                setFilmStock(filmStock)
            }
        }
        binding.filmStockLayout.setOnClickListener {
            val dialog = SelectFilmStockDialog()
            dialog.show(parentFragmentManager.beginTransaction(), null)
            dialog.setFragmentResultListener("SelectFilmStockDialog") { _, bundle ->
                val filmStock: FilmStock = bundle.getParcelable(ExtraKeys.FILM_STOCK)
                    ?: return@setFragmentResultListener
                setFilmStock(filmStock)
            }
        }


        // CAMERA PICK DIALOG
        binding.cameraLayout.text = roll.camera?.name
        binding.cameraLayout.setOnClickListener {
            val listItems = listOf(resources.getString(R.string.NoCamera))
                    .plus(cameras.map { it.name }).toTypedArray()

            val index = cameras.indexOfFirst { it == newRoll.camera }
            val checkedItem = if (index == -1) 0 else index + 1

            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setTitle(R.string.UsedCamera)
            builder.setSingleChoiceItems(listItems, checkedItem) { dialogInterface: DialogInterface, which: Int ->
                // listItems also contains the No camera option
                newRoll.camera = if (which > 0) {
                    binding.cameraLayout.text = listItems[which]
                    cameras[which - 1]
                } else {
                    binding.cameraLayout.text = null
                    null
                }
                dialogInterface.dismiss()
            }
            builder.setNegativeButton(R.string.Cancel) { _: DialogInterface?, _: Int -> }
            val alert1 = builder.create()
            alert1.show()
        }


        // CAMERA ADD DIALOG
        binding.addCamera.isClickable = true
        binding.addCamera.setOnClickListener {
            binding.noteEditText.clearFocus()
            binding.nameEditText.clearFocus()
            val dialog = EditCameraDialog()
            val arguments = Bundle()
            arguments.putString(ExtraKeys.TITLE, resources.getString(R.string.AddNewCamera))
            arguments.putString(ExtraKeys.POSITIVE_BUTTON, resources.getString(R.string.Add))
            dialog.arguments = arguments
            dialog.show(parentFragmentManager.beginTransaction(), EditCameraDialog.TAG)
            dialog.setFragmentResultListener("EditCameraDialog") { _, bundle ->
                val camera: Camera = bundle.getParcelable(ExtraKeys.CAMERA)
                    ?: return@setFragmentResultListener
                model.addCamera(camera)
                binding.cameraLayout.text = camera.name
                newRoll.camera = camera
            }
        }

        // DATE & TIME LOADED PICK DIALOG

        // DATE
        if (roll.date == null) {
            roll.date = DateTime.fromCurrentTime()
        }

        dateLoadedManager = DateTimeLayoutManager(
            requireActivity(),
            binding.dateLoadedLayout,
            roll.date,
            null)

        // DATE & TIME UNLOADED PICK DIALOG
        dateUnloadedManager = DateTimeLayoutManager(
            requireActivity(),
            binding.dateUnloadedLayout,
            roll.unloaded,
            binding.clearDateUnloaded)

        // DATE & TIME DEVELOPED PICK DIALOG
        dateDevelopedManager = DateTimeLayoutManager(
            requireActivity(),
            binding.dateDevelopedLayout,
            roll.developed,
            binding.clearDateDeveloped)


        //ISO PICKER
        binding.isoText.text = if (roll.iso == 0) "" else roll.iso.toString()
        binding.isoLayout.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            val inflater1 = requireActivity().layoutInflater
            @SuppressLint("InflateParams")
            val dialogView = inflater1.inflate(R.layout.dialog_single_numberpicker, null)
            val isoPicker = dialogView.findViewById<NumberPicker>(R.id.number_picker)
            val isoValues = requireActivity().resources.getStringArray(R.array.ISOValues)
            isoPicker.minValue = 0
            isoPicker.maxValue = isoValues.size - 1
            isoPicker.displayedValues = isoValues
            isoPicker.value = 0
            val initialValue = isoValues.indexOfFirst { it.toInt() == newRoll.iso }
            if (initialValue != -1) isoPicker.value = initialValue

            //To prevent text edit
            isoPicker.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            builder.setView(dialogView)
            builder.setTitle(resources.getString(R.string.ChooseISO))
            builder.setPositiveButton(resources.getString(R.string.OK)) { _: DialogInterface?, _: Int ->
                newRoll.iso = isoValues[isoPicker.value].toInt()
                binding.isoText.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
            }
            builder.setNegativeButton(resources.getString(R.string.Cancel)) { _: DialogInterface?, _: Int -> }
            val dialog = builder.create()
            dialog.show()
        }


        //PUSH PULL PICKER
        try {
            if (newRoll.pushPull != null) {
                val values = resources.getStringArray(R.array.CompValues)
                binding.pushPullSpinner.setSelection(values.indexOf(newRoll.pushPull))
            } else {
                binding.pushPullSpinner.setSelection(9)
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }


        //FORMAT PICKER
        try {
            binding.formatSpinner.setSelection(newRoll.format)
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }

        binding.title.negativeButton.setOnClickListener { requireActivity().onBackPressed() }
        binding.title.positiveButton.setOnClickListener {
            if (commitChanges()) {
                val bundle = Bundle()
                bundle.putParcelable(ExtraKeys.ROLL, roll)
                setFragmentResult("EditRollDialog", bundle)
                requireActivity().onBackPressed()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        // Start the transition once all views have been measured and laid out.
        (view.parent as ViewGroup).doOnPreDraw {
            startPostponedEnterTransition()
        }
    }

    private fun commitChanges(): Boolean {
        val name = binding.nameEditText.text.toString()
        if (name.isNotEmpty()) {
            roll.name = name
            roll.note = binding.noteEditText.text.toString()
            roll.camera = newRoll.camera
            roll.date = dateLoadedManager.dateTime
            roll.unloaded = dateUnloadedManager.dateTime
            roll.developed = dateDevelopedManager.dateTime
            roll.iso = newRoll.iso
            roll.pushPull = binding.pushPullSpinner.selectedItem as String?
            roll.format = binding.formatSpinner.selectedItemPosition
            roll.filmStock = newRoll.filmStock
            return true
        } else {
            binding.nameLayout.error = getString(R.string.NoName)
            Toast.makeText(activity, resources.getString(R.string.NoName),
                    Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun setFilmStock(filmStock: FilmStock) {
        binding.filmStockLayout.text = filmStock.name
        newRoll.filmStock = filmStock
        binding.addFilmStock.visibility = View.GONE
        binding.clearFilmStock.visibility = View.VISIBLE
        // If the film stock ISO is defined, set the ISO
        if (filmStock.iso != 0) {
            newRoll.iso = filmStock.iso
            binding.isoText.text = if (newRoll.iso == 0) "" else newRoll.iso.toString()
        }
    }

}