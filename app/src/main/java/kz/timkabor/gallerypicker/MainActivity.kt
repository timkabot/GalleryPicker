package kz.timkabor.gallerypicker

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kz.timkabor.bottomsheetgallerypickerlibrary.BottomSheetGalleryPicker

class MainActivity : AppCompatActivity(), BottomSheetGalleryPicker.OnImagesSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            BottomSheetGalleryPicker
                .Builder()
                .peekHeight(R.dimen.peekHeight)     //peek height of the bottom sheet
                .columnSize(R.dimen.columnSize)     //size of the columns (will be changed a little to fit)
                .selectionRange(1, 6)
                .selectionTitles(
                    R.plurals.imagePickerMulti,           //"you have selected <count> images
                    R.plurals.imagePickerMultiMore,      //"You have to select <min-count> more images"
                    R.string.imagePickerMultiLimit       //"You cannot select more than <max> images"
                )
                .show(supportFragmentManager)
        }
    }

    override fun onImagesSelected(uris: List<Uri>) {
        println(uris.size)
        uris.forEach{uri ->
            println(uri)
            val type: String? = contentResolver.getType(uri)
            println(type)
        }
    }
}