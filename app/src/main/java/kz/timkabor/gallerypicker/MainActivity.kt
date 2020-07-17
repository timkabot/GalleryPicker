package kz.timkabor.gallerypicker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kz.timkabor.bottomsheetgallerypickerlibrary.BottomSheetGalleryPicker

class MainActivity : AppCompatActivity() {
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
}