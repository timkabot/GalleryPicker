package kz.timkabor.bottomsheetgallerypickerlibrary

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

sealed class ClickedGalleryItem {
    data class ImageItem(val uri: Uri) : ClickedGalleryItem()
    data class VideoItem(val uri: Uri) : ClickedGalleryItem()
}

class GalleryAdapter(private val clickListener: (ClickedGalleryItem) -> Unit,
                     private val numOfSelectedItemsChanged: (Int) -> Unit) :
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
        selection.forEach { add(itemList.getOrNull(it) ?: return@forEach) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseIconViewHolder =
        LayoutInflater
            .from(parent.context)
            .inflate(getLayoutForSpecificViewType(viewType), parent, false)
            .let { view ->
                when (viewType) {
                    VIEW_TYPE_IMAGE -> BaseIconViewHolder.ImageIconViewHolder(view)
                    else -> throw IllegalStateException("viewType $viewType not allowed")
                }
            }

    private fun getLayoutForSpecificViewType(viewType: Int) = when (viewType) {
        VIEW_TYPE_IMAGE -> R.layout.card_image
        else -> R.layout.card_special
    }

    override fun getItemCount() = itemList.size

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_IMAGE
    }
    override fun onBindViewHolder(holder: BaseIconViewHolder, position: Int) {
        (holder as? BaseIconViewHolder.ImageIconViewHolder)?.update(
            itemList[position],
            selection.contains(position),
            ::onImageItemClick
        )
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
        private const val VIEW_TYPE_IMAGE = 0x1002
        private const val VIEW_TYPE_VIDEO = 0x1003
    }
}

sealed class BaseIconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    class ImageIconViewHolder(view: View) : BaseIconViewHolder(view) {
        private val ivImage = view.findViewById<ImageView>(R.id.ivImage)
        private val ivSelect = view.findViewById<View>(R.id.ivSelect)

        private var clickListener: ((selectView: View, position: Int) -> Unit)? = null

        init {
            view.setOnClickListener { clickListener?.invoke(ivSelect, adapterPosition) }
        }

        fun update(
            uri: Uri,
            selected: Boolean,
            clickListener: (selectView: View, position: Int) -> Unit
        ){
            this.clickListener = clickListener

            if(selected) ivSelect.visibility = View.VISIBLE
            else  ivSelect.visibility = View.INVISIBLE

            Glide
                .with(ivImage)
                .load(uri)
                .error(R.drawable.ic_broken_image)
                .into(ivImage)
        }
    }

    class VideoIconViewHolder(view: View) : BaseIconViewHolder(view)
}

