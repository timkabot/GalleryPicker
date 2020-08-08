package kz.timkabor.bottomsheetgallerypickerlibrary

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.card_image.view.*
import kotlinx.android.synthetic.main.card_special.view.*

sealed class ClickedGalleryItem {
    data class ImageItem(val uri: Uri) : ClickedGalleryItem()
    object CameraItem : ClickedGalleryItem()
}

class GalleryAdapter(
    private val clickListener: (ClickedGalleryItem) -> Unit,
    private val numOfSelectedItemsChanged: (Int) -> Unit,
    private val bindCameraPreview: (cameraProvider: ProcessCameraProvider, itemView: View) -> Unit
) :
    RecyclerView.Adapter<BaseIconViewHolder>() {

    var itemList: List<Uri> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var selection = HashSet<Int>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun clear() {
        selection.clear()
        numOfSelectedItemsChanged(0)
    }

    fun getSelectedImages(): ArrayList<Uri> = ArrayList<Uri>(selection.size).apply {
        selection.forEach {
            add(itemList.getOrNull(it) ?: return@forEach)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseIconViewHolder =
        LayoutInflater
            .from(parent.context)
            .inflate(
                if (viewType == VIEW_TYPE_IMAGE) R.layout.card_image else R.layout.card_special,
                parent,
                false
            )
            .let { view ->
                when (viewType) {
                    VIEW_TYPE_IMAGE -> BaseIconViewHolder.VHImageTile(view)
                    VIEW_TYPE_CAMERA -> BaseIconViewHolder.VHCameraTile(view, bindCameraPreview) {
                        clickListener.invoke(ClickedGalleryItem.CameraItem)
                    }
                    else -> throw IllegalStateException("viewType $viewType not allowed")
                }
            }


    override fun getItemCount() = itemList.size

    override fun onBindViewHolder(holder: BaseIconViewHolder, position: Int) {
        (holder as? BaseIconViewHolder.VHImageTile)?.update(
            itemList[position],
            selection.contains(position),
            ::onImageItemClick
        )
        (holder as? BaseIconViewHolder.VHCameraTile)?.update()
    }

    private fun onImageItemClick(selectView: View, position: Int) {
        if (selection.contains(position)) {
            selectView.visibility = View.INVISIBLE
            selection.remove(position)
        } else {
            selectView.visibility = View.VISIBLE
            selection.add(position)
        }
        numOfSelectedItemsChanged.invoke(selection.size)

    }

    companion object {
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_CAMERA = 2
    }

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> VIEW_TYPE_CAMERA
        else -> VIEW_TYPE_IMAGE
    }
}

sealed class BaseIconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    class VHImageTile(view: View) : BaseIconViewHolder(view) {
        private val ivImage = view.item_image
        private val ivSelect = view.item_select
        private val ivDuration = view.duration_card
        private val ivDurationText = view.duration_text

        private var clickListener: ((selectView: View, position: Int) -> Unit)? = null

        init {
            view.setOnClickListener { clickListener?.invoke(ivSelect, adapterPosition) }
        }

        fun update(
            uri: Uri,
            selected: Boolean,
            clickListener: (selectView: View, position: Int) -> Unit
        ) {
            this.clickListener = clickListener
            if (uri.isVideo()) {
                ivDuration.visibility = View.VISIBLE
                ivDurationText.text = uri.getMediaDuration(ivDurationText.context).asData()
            } else {
                ivDuration.visibility = View.INVISIBLE
            }

            if (selected) {
                ivSelect.visibility = View.VISIBLE
            } else {
                ivSelect.visibility = View.INVISIBLE
            }

            Glide
                .with(ivImage)
                .load(uri)
                .error(R.drawable.ic_broken_image)
                .into(ivImage)
        }

    }

    class VHCameraTile(
        private val view: View,
        private val bindCameraPreview: (cameraProvider: ProcessCameraProvider, itemView: View) -> Unit,
        clickListener: () -> Unit
    ) : BaseIconViewHolder(view) {
        init {
            view.setOnClickListener { clickListener.invoke() }
        }

        fun update() {
            if (view.context.hasCameraPermission) {
                view.camera_preview.visibility = View.VISIBLE
                val cameraProviderFuture = ProcessCameraProvider.getInstance(view.context)
                cameraProviderFuture.addListener(Runnable {
                    val cameraProvider = cameraProviderFuture.get()
                    bindCameraPreview(cameraProvider, view)
                }, ContextCompat.getMainExecutor(view.context))
            }
        }
    }
}

