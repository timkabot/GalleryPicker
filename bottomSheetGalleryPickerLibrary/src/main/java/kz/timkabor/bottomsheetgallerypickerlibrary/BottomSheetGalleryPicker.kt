package kz.timkabor.bottomsheetgallerypickerlibrary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DimenRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.gallery_picker.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BottomSheetGalleryPicker : BottomSheetDialogFragment(),
    LoaderManager.LoaderCallbacks<Cursor> {
    private val adapter by lazy {
        GalleryAdapter(::onItemClick, ::selectionCountChanged)
    }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private var onImagesSelectedListener: OnImagesSelectedListener? = null
    private var multiSelectMin = 1
    private var multiSelectMax = 6
    private var currentPhotoUri: Uri? = null

    private var screenHeight: Float = Resources.getSystem().displayMetrics.heightPixels.toFloat()
    private var collapsedTopMarginInDp: Float = 0F

    @DimenRes
    private var peekHeight = R.dimen.imagePickerPeekHeight

    @DimenRes
    private var columnSizeRes = R.dimen.imagePickerColumnSize


    @PluralsRes
    private var resTitleMulti = R.plurals.imagePickerMulti

    @PluralsRes
    private var resTitleMultiMore = R.plurals.imagePickerMultiMore

    @StringRes
    private var resTitleMultiLimit = R.string.imagePickerMultiLimit

    @StringRes
    private var fewFilesChosen = R.string.fewFilesChosen

    private var state: Int = BottomSheetBehavior.STATE_COLLAPSED
        set(value) {
            when (value) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    if (fabExpanded.visibility == View.VISIBLE) {
                        fabExpanded.visibility = View.INVISIBLE
                        fabCollapsed.visibility = View.VISIBLE
                    }
                }
                BottomSheetBehavior.STATE_EXPANDED -> {
                    if (fabCollapsed.visibility == View.VISIBLE) {
                        fabExpanded.visibility = View.VISIBLE
                        fabCollapsed.visibility = View.INVISIBLE
                    }
                }
            }
            field = value
        }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                view?.alpha = if (slideOffset < 0f) 1f + slideOffset else 1f
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                state = newState
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismissAllowingStateLoss()
                }

            }
        }
    }

    private fun initDimensions() {
        collapsedTopMarginInDp =
            (resources.getDimension(peekHeight) / resources.displayMetrics.density)
        screenHeight = resources.pxToDp(screenHeight.toInt())
    }

    private fun changeToCollapsedFab() {
        fabCollapsed.visibility = View.VISIBLE
        fabExpanded.visibility = View.INVISIBLE
    }

    private fun changeToExpandedFab() {
        fabCollapsed.visibility = View.INVISIBLE
        fabExpanded.visibility = View.VISIBLE
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadArguments()
        if (requireContext().hasReadStoragePermission) initLoader()
        else requestReadStoragePermission(REQUEST_PERMISSION_READ_STORAGE)

        if (savedInstanceState != null) {
            currentPhotoUri = savedInstanceState.getParcelable(STATE_CURRENT_URI)
        }
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
    ): View = inflater.inflate(R.layout.gallery_picker, container, false).also {
        (parentFragment as? OnImagesSelectedListener)?.let { onImagesSelectedListener = it }
    }

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

    val attachMediaOnClickListener = View.OnClickListener {
        onImagesSelectedListener?.onImagesSelected(adapter.getSelectedImages())
        dismissAllowingStateLoss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDimensions()
        fabCollapsed.margin(top = collapsedTopMarginInDp - 64)

        tvHeader.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                recycler.smoothScrollToPosition(0)
            }
        }

        with(attachMediaOnClickListener) {
            fabCollapsed.setOnClickListener(this)
            fabExpanded.setOnClickListener(this)
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
            count < multiSelectMin -> {
                val delta = multiSelectMin - count
                tvHeader.text = resources.getQuantityString(resTitleMultiMore, delta, delta)
            }
            count > multiSelectMax -> tvHeader.text = getString(resTitleMultiLimit, multiSelectMax)
            count in listOf(2, 3, 4) -> {
                tvHeader.text = resources.getString(fewFilesChosen, count)
            }
            else -> {
                tvHeader.text = resources.getQuantityString(resTitleMulti, count, count)
            }
        }
        if (count in multiSelectMin..multiSelectMax) {
            if (state == BottomSheetBehavior.STATE_EXPANDED) {
                fabExpanded.visibility = View.VISIBLE
                fabCollapsed.visibility = View.INVISIBLE
                fabExpanded.animate().alpha(1f)

            } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                fabExpanded.visibility = View.INVISIBLE
                fabCollapsed.visibility = View.VISIBLE
                fabCollapsed.animate().alpha(1f)

            }
        } else {
            fabExpanded.visibility = View.INVISIBLE
            fabCollapsed.visibility = View.INVISIBLE
        }
    }

    private fun initRecycler() {
        recycler.layoutManager = AutofitLayoutManager(requireContext(), columnSizeRes)
        (recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        recycler.adapter = adapter
    }

    private fun onItemClick(item: ClickedGalleryItem) {
        when (item) {
            is ClickedGalleryItem.CameraItem -> {
                launchCamera()
            }
            is ClickedGalleryItem.ImageItem -> {
                onImagesSelectedListener?.onImagesSelected(listOf(item.uri))
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
            adapter.itemList = items
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_CURRENT_URI, currentPhotoUri)
        outState.putIntArray(STATE_SELECTION, adapter.selection.toIntArray())
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.itemList = emptyList()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnImagesSelectedListener) {
            onImagesSelectedListener = context
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_READ_STORAGE ->
                if (grantResults.isPermissionGranted) initLoader()
                else dismissAllowingStateLoss()

            REQUEST_PERMISSION_WRITE_STORAGE ->
                if (grantResults.isPermissionGranted) launchCamera()
                else getString(R.string.toastGalleryPickerNoWritePermission).toast(requireContext())
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun notifyGallery() {
        context?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = currentPhotoUri
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        when (requestCode) {
            REQUEST_PHOTO -> {
                notifyGallery()
                currentPhotoUri?.let { uri ->
                    onImagesSelectedListener?.onImagesSelected(listOf(uri))
                }
                dismissAllowingStateLoss()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    interface OnImagesSelectedListener {
        fun onImagesSelected(uris: List<Uri>)
    }

    private fun launchCamera() {
        if (!requireContext().hasWriteStoragePermission) {
            requestWriteStoragePermission(REQUEST_PERMISSION_WRITE_STORAGE)
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) == null) return
        val photoUri = try {
            getPhotoUri()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "could not prepare image file", e)
            return
        }
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoUri = photoUri

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        requireContext().packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        ).forEach { info ->
            val packageName = info.activityInfo.packageName
            requireContext().grantUriPermission(
                packageName,
                photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        startActivityForResult(intent, REQUEST_PHOTO)
    }


    private fun getPhotoUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val contentVals = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, getImageFileName() + ".jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                //put images in pictures folder
                put(MediaStore.MediaColumns.RELATIVE_PATH, "pictures/${requireContext().applicationContext.packageName}")
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentVals)
        } else {
            val imageFileName = getImageFileName()
            val storageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            storageDir.mkdirs()
            val image = File.createTempFile(imageFileName + "_", ".jpg", storageDir)

            //no need to create empty file; camera app will create it on success
            val success = image.delete()
            if (!success && BuildConfig.DEBUG) {
                Log.d(TAG, "Failed to delete temp file: $image")
            }
            FileProvider.getUriForFile(
                requireContext(),
                requireContext().applicationContext.packageName + ".provider",
                image
            )
        }

    @SuppressLint("SimpleDateFormat")
    private fun getImageFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().time)
        return "IMG_$timeStamp"
    }

    private fun loadArguments() {
        val args = arguments ?: return
        columnSizeRes = args.getInt(KEY_COLUMN_SIZE_RES, columnSizeRes)

        resTitleMulti = args.getInt(KEY_TITLE_RES_MULTI, resTitleMulti)
        resTitleMultiMore = args.getInt(KEY_TITLE_RES_MULTI_MORE, resTitleMultiMore)
        resTitleMultiLimit = args.getInt(KEY_TITLE_RES_MULTI_LIMIT, resTitleMultiLimit)

        peekHeight = args.getInt(KEY_PEEK_HEIGHT, peekHeight)

        multiSelectMin = args.getInt(KEY_MULTI_SELECT_MIN, multiSelectMin)
        multiSelectMax = args.getInt(KEY_MULTI_SELECT_MAX, multiSelectMax)
    }

    class Builder() {
        private val args = Bundle().apply {}

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
            @PluralsRes titleNeedMore: Int,
            @StringRes titleLimit: Int
        ) = args.run {
            putInt(KEY_TITLE_RES_MULTI, titleCount)
            putInt(KEY_TITLE_RES_MULTI_MORE, titleNeedMore)
            putInt(KEY_TITLE_RES_MULTI_LIMIT, titleLimit)
            this@Builder
        }

        private fun build() = BottomSheetGalleryPicker().apply { arguments = args }

        fun show(fm: FragmentManager, tag: String? = null) = build().show(fm, tag)
    }
}
