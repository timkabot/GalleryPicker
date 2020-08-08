package kz.timkabor.bottomsheetgallerypickerlibrary

import android.app.Dialog
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.card_special.view.*
import kotlinx.android.synthetic.main.fragment_gallery_picker.*
import java.io.File
import java.util.*

class BottomSheetGalleryPicker : BottomSheetDialogFragment(),
    LoaderManager.LoaderCallbacks<Cursor> {
    private val adapter by lazy {
        GalleryAdapter(
            ::onItemClick,
            ::selectionCountChanged,
            ::bindCameraPreview
        )
    }

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var onImagesSelectedListener: OnImagesSelectedListener
    private var multiSelectMin = 1
    private var multiSelectMax = 6

    private var screenHeight: Float = Resources.getSystem().displayMetrics.heightPixels.toFloat()
    private var collapsedTopMarginInDp: Float = 0F

    @DimenRes
    private var peekHeight = R.dimen.imagePickerPeekHeight

    @DimenRes
    private var columnSizeRes = R.dimen.imagePickerColumnSize

    @PluralsRes
    private var resTitleMulti = R.plurals.imagePickerMulti

    @StringRes
    private var resTitleMultiLimit = R.string.imagePickerMultiLimit

    @StringRes
    private var fewFilesChosen = R.string.fewFilesChosen

    private var state: Int = BottomSheetBehavior.STATE_COLLAPSED
        set(value) {
            when (value) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    if (fab_expanded.visibility == View.VISIBLE) changeToCollapsedFab()
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    if (fab_collapsed.visibility == View.VISIBLE) changeToExpandedFab()
                }
            }
            field = value
        }

    private fun changeToCollapsedFab() {
        fab_collapsed.visibility = View.VISIBLE
        fab_expanded.visibility = View.GONE
        fab_collapsed.animate().alpha(1f)
    }

    private fun changeToExpandedFab() {
        fab_collapsed.visibility = View.GONE
        fab_expanded.visibility = View.VISIBLE
        fab_expanded.animate().alpha(1f)
    }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                view?.alpha = if (slideOffset < 0f) 1f + slideOffset else 1f
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                state = newState
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    dismissAllowingStateLoss()
            }
        }
    }

    private fun initDimensions() {
        collapsedTopMarginInDp =
            (resources.getDimension(peekHeight) / resources.displayMetrics.density)
        screenHeight = resources.pxToDp(screenHeight.toInt())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadArguments()
        when {
            requireContext().hasReadStoragePermission -> initLoader()
            requireContext().hasCameraPermission -> requestReadStoragePermission(
                REQUEST_PERMISSION_READ_STORAGE
            )
            else -> requestReadStorageAndCameraPreviewPermission(
                REQUEST_PERMISSION_READ_STORAGE_AND_CAMERA
            )
        }
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider, itemView: View) {
        cameraProvider.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)

        itemView.camera_preview.preferredImplementationMode =
            (PreviewView.ImplementationMode.TEXTURE_VIEW)
        preview.setSurfaceProvider(itemView.camera_preview.createSurfaceProvider())
    }

    private fun initLoader() {
        LoaderManager
            .getInstance(this)
            .initLoader(LOADER_ID, null, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gallery_picker, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(R.id.design_bottom_sheet)
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(peekHeight)
                bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
            }
        }
    }

    private val attachMediaOnClickListener = View.OnClickListener {
        onImagesSelectedListener.onImagesSelected(adapter.getSelectedImages())
        dismissAllowingStateLoss()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDimensions()
        fab_collapsed.margin(top = collapsedTopMarginInDp - 64)

        header.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                recycler.smoothScrollToPosition(0)
            }
        }

        with(attachMediaOnClickListener) {
            fab_collapsed.setOnClickListener(this)
            fab_expanded.setOnClickListener(this)
        }

        initRecycler()

        val oldSelection = savedInstanceState?.getIntArray(STATE_SELECTION)
        if (oldSelection != null) {
            adapter.selection = oldSelection.toHashSet()
        }
        selectionCountChanged(adapter.selection.size)
    }


    private fun selectionCountChanged(count: Int) {
        when {
            count > multiSelectMax -> header.text = getString(resTitleMultiLimit, multiSelectMax)
            count in listOf(2, 3, 4) -> header.text = resources.getString(fewFilesChosen, count)
            else -> header.text = resources.getQuantityString(resTitleMulti, count, count)
        }

        if (count in multiSelectMin..multiSelectMax) {
            if (state == BottomSheetBehavior.STATE_EXPANDED) {
                changeToExpandedFab()
            } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                changeToCollapsedFab()
            }
        } else {
            fab_expanded.visibility = View.GONE
            fab_collapsed.visibility = View.GONE
        }
    }

    private fun initRecycler() {
        recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        (recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        recycler.adapter = adapter
    }

    private fun onItemClick(item: ClickedGalleryItem) {
        when (item) {
            is ClickedGalleryItem.CameraItem -> {
                if (requireContext().hasWriteStoragePermission) {
                    launchCamera()
                } else {
                    requestWriteStoragePermission(REQUEST_PERMISSION_WRITE_STORAGE)
                }
            }
            is ClickedGalleryItem.ImageItem -> {
                onImagesSelectedListener.onImagesSelected(listOf(item.uri))
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        if (id != LOADER_ID) throw IllegalStateException("illegal loader id: $id")
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Video.VideoColumns.DATA)
        val sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC"

        val selection = VIDEO_AND_IMAGE_SELECTION

        return CursorLoader(requireContext(), uri, projection, selection, null, sortOrder)
    }


    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        data ?: return
        val items = ArrayList<Uri>()

        val columnIndex = data.getColumnIndex(MediaStore.Video.VideoColumns.DATA)
        Thread().run {
            while (items.size < MAX_CURSOR_IMAGES && data.moveToNext()) {
                val itemLocation: String = data.getString(columnIndex)
                val file = File(itemLocation)

                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    items.add(uri)
                }
            }
            data.moveToFirst()
            if (items.isNotEmpty()) {
                adapter.itemList = items
            } else {
                onImagesSelectedListener.onImagesSelected(listOf(), true)
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(STATE_SELECTION, adapter.selection.toIntArray())
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.itemList = emptyList()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_READ_STORAGE_AND_CAMERA -> {
                if (grantResults.isPermissionGranted) {
                    initLoader()
                } else {
                    showToast(getString(R.string.camera_and_storage_permission_is_necessary))
                    dismissAllowingStateLoss()
                }
            }
            REQUEST_PERMISSION_READ_STORAGE ->
                if (grantResults.isPermissionGranted) {
                    initLoader()
                } else {
                    showToast(getString(R.string.read_storage_permission_is_necessary))
                    dismissAllowingStateLoss()
                }

            REQUEST_PERMISSION_WRITE_STORAGE ->
                if (grantResults.isPermissionGranted) {
                    launchCamera()
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    interface OnImagesSelectedListener {
        fun onImagesSelected(uris: List<Uri>, fromCamera: Boolean = false)
    }

    private fun launchCamera() {
        onImagesSelectedListener.onImagesSelected(listOf(), true)
        dismissAllowingStateLoss()
    }

    private fun loadArguments() {
        val args = arguments ?: return
        columnSizeRes = args.getInt(KEY_COLUMN_SIZE_RES, columnSizeRes)

        resTitleMulti = args.getInt(KEY_TITLE_RES_MULTI, resTitleMulti)
        resTitleMultiLimit = args.getInt(KEY_TITLE_RES_MULTI_LIMIT, resTitleMultiLimit)

        peekHeight = args.getInt(KEY_PEEK_HEIGHT, peekHeight)

        multiSelectMin = args.getInt(KEY_MULTI_SELECT_MIN, multiSelectMin)
        multiSelectMax = args.getInt(KEY_MULTI_SELECT_MAX, multiSelectMax)
    }

    class Builder(private val onImagesSelectedListenerTemp: OnImagesSelectedListener) {
        private val args = Bundle()

        fun columnSize(@DimenRes columnSizeRes: Int) = args.run {
            putInt(KEY_COLUMN_SIZE_RES, columnSizeRes)
            this@Builder
        }

        fun peekHeight(@DimenRes peekHeightRes: Int) = args.run {
            putInt(KEY_PEEK_HEIGHT, peekHeightRes)
            this@Builder
        }

        fun selectionRange(min: Int = 1, max: Int = 6) = args.run {
            putInt(KEY_MULTI_SELECT_MIN, min)
            putInt(KEY_MULTI_SELECT_MAX, max)
            this@Builder
        }

        fun selectionTitles(
            @PluralsRes titleCount: Int,
            @StringRes titleLimit: Int
        ) = args.run {
            putInt(KEY_TITLE_RES_MULTI, titleCount)
            putInt(KEY_TITLE_RES_MULTI_LIMIT, titleLimit)
            this@Builder
        }

        private fun build() = BottomSheetGalleryPicker()
            .apply {
                arguments = args
                this.onImagesSelectedListener = onImagesSelectedListenerTemp
            }

        fun show(fm: FragmentManager, tag: String? = null) = build().show(fm, tag)
    }
}
