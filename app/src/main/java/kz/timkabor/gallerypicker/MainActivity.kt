package kz.timkabor.gallerypicker

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kz.timkabor.bottomsheetgallerypickerlibrary.BottomSheetGalleryPicker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            BottomSheetGalleryPicker
                .Builder(onImagesSelectedListener)
                .peekHeight(R.dimen.peekHeight)
                .columnSize(R.dimen.columnSize)
                .selectionRange(1, 6)
                .selectionTitles(
                    R.plurals.imagePickerMulti,           //"you have selected <count> images
                    R.string.imagePickerMultiLimit       //"You cannot select more than <max> images"
                )
                .show(supportFragmentManager)
        }
    }

    private val onImagesSelectedListener =
        object : BottomSheetGalleryPicker.OnImagesSelectedListener {
            override fun onImagesSelected(uris: List<Uri>, fromCamera: Boolean) {
            }
        }

    override fun onBackPressed() {
        super.onBackPressed()
        println("back pressed")
    }
}