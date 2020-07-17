package kz.timkabor.bottomsheetgallerypickerlibrary

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                view?.alpha = if (slideOffset < 0f) 1f + slideOffset else 1f
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadArguments()
        if (requireContext().hasReadStoragePermission) initLoader()
        else requestReadStoragePermission(REQUEST_PERMISSION_READ_STORAGE)
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
                Random().nextInt()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvHeader.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                recycler.smoothScrollToPosition(0)
            }
        }
        tvHeader.text = getString(R.string.imagePickerSingle)

        // btnGallery.setOnClickListener { launchGallery() }
        btnCamera.setOnClickListener { launchCamera() }
        btnDone.setOnClickListener {
            onImagesSelectedListener?.onImagesSelected(adapter.getSelectedImages())
            dismissAllowingStateLoss()
        }

        initRecycler()

        selectionCountChanged(adapter.selection.size)
    }

    private fun selectionCountChanged(count: Int) {
        when {
            count < multiSelectMin -> {
                val delta = multiSelectMin - count
                tvHeader.text = resources.getQuantityString(resTitleMultiMore, delta, delta)
            }
            count > multiSelectMax -> tvHeader.text = getString(resTitleMultiLimit, multiSelectMax)
            else -> tvHeader.text = resources.getQuantityString(resTitleMulti, count, count)
        }
        btnDone.isEnabled = count in multiSelectMin..multiSelectMax
        if (btnDone.isEnabled) btnDone.visibility = View.VISIBLE
        else btnDone.visibility = View.INVISIBLE

        btnDone.animate().alpha(if (btnDone.isEnabled) 1f else .2f)
    }

    private fun initRecycler() {
        recycler.layoutManager = AutofitLayoutManager(requireContext(), columnSizeRes)
        (recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        recycler.adapter = adapter
    }

    private fun onItemClick(item: ClickedGalleryItem) {
        when (item) {
            is ClickedGalleryItem.VideoItem -> {
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

        while (items.size < MAX_CURSOR_IMAGES && data.moveToNext()) {
            val itemLocation: String = data.getString(columnIndex)
            val imageFile = File(itemLocation)
            val videoUri = Uri.fromFile(imageFile)
            items.add(videoUri)
        }
        data.moveToFirst()
        adapter.itemList = items
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

        fun cameraButton(type: ButtonType) = args.run {
            putBoolean(KEY_SHOW_CAMERA_BTN, type == ButtonType.Button)
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

enum class ButtonType {
    None, Button, Tile
}
