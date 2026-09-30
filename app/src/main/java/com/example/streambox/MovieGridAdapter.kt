package com.example.streambox

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/** Only visible cards and their posters are allocated, even with thousands of titles. */
class MovieGridAdapter : RecyclerView.Adapter<MovieGridAdapter.Holder>() {
    data class Tile(val title: String, val subtitle: String, val poster: String = "", val action: String,
                    val onClick: () -> Unit, val onLongClick: (() -> Unit)? = null, val romance: Boolean = false)
    private val tiles = mutableListOf<Tile>()
    private var rowHeight: Int? = null

    fun fitThreeRows(viewportHeight: Int) {
        if (viewportHeight <= 0) return
        val height = viewportHeight / RomanceCatalog.ROWS
        if (rowHeight == height) return
        rowHeight = height
        if (tiles.isNotEmpty()) notifyItemRangeChanged(0, tiles.size)
    }

    fun normalRows() {
        if (rowHeight == null) return
        rowHeight = null
        if (tiles.isNotEmpty()) notifyItemRangeChanged(0, tiles.size)
    }

    fun replace(items: List<Tile>) {
        tiles.clear()
        tiles.addAll(items)
        notifyDataSetChanged()
    }

    fun append(items: List<Tile>) {
        val start = tiles.size
        tiles.addAll(items)
        if (items.isNotEmpty()) notifyItemRangeInserted(start, items.size)
    }

    override fun getItemCount() = tiles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
            setBackgroundColor(Color.rgb(24, 24, 24))
            layoutParams = RecyclerView.LayoutParams(-1, dp(246)).apply { setMargins(dp(2), dp(2), dp(2), dp(6)) }
            isFocusable = true
        }
        val poster = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        val cover = FrameLayout(context)
        cover.addView(poster, FrameLayout.LayoutParams(-1, -1))
        val coverTitle = TextView(context).apply {
            setTextColor(Color.WHITE); textSize = 13f; gravity = Gravity.CENTER
            setPadding(dp(3), 0, dp(3), 0); maxLines = 4; ellipsize = TextUtils.TruncateAt.END
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        cover.addView(coverTitle, FrameLayout.LayoutParams(-1, -1))
        card.addView(cover, LinearLayout.LayoutParams(-1, 0, 1f))
        val title = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 2
            includeFontPadding = false
            ellipsize = TextUtils.TruncateAt.END
        }
        card.addView(title, LinearLayout.LayoutParams(-1, dp(36)))
        val subtitle = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 11f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        card.addView(subtitle, LinearLayout.LayoutParams(-1, dp(20)))
        val action = TextView(context).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(155, 12, 28))
            textSize = 11f
            gravity = Gravity.CENTER
            maxLines = 1
            isFocusable = true
        }
        card.addView(action, LinearLayout.LayoutParams(-1, dp(48)))
        return Holder(card, poster, title, subtitle, action, coverTitle)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tile = tiles[position]
        val density = holder.itemView.resources.displayMetrics.density
        holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
            // Nine tiles fit the portrait viewport; very short screens scroll within the page.
            height = rowHeight?.let { (it - 8 * density).toInt().coerceAtLeast((142 * density).toInt()) }
                ?: (246 * density).toInt()
        }
        holder.title.text = tile.title
        holder.subtitle.text = tile.subtitle
        holder.action.text = tile.action
        holder.poster.contentDescription = tile.title
        holder.coverTitle.visibility = if (tile.romance && tile.poster.isBlank()) android.view.View.VISIBLE
            else android.view.View.GONE
        holder.coverTitle.text = "♥\n${tile.title}"
        holder.coverTitle.background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.HSVToColor(floatArrayOf((tile.title.hashCode().ushr(1) % 360).toFloat(),
                0.65f, 0.40f)), Color.rgb(45, 12, 28)))
        Glide.with(holder.poster).load(tile.poster.ifBlank { null })
            .placeholder(R.drawable.ic_streambox_logo).error(R.drawable.ic_streambox_logo)
            .centerCrop().into(holder.poster)
        holder.itemView.contentDescription = "${tile.title}, ${tile.subtitle}, ${tile.action}"
        holder.itemView.setOnClickListener { tile.onClick() }
        holder.action.contentDescription = "${tile.action}: ${tile.title}"
        holder.action.setOnClickListener { tile.onClick() }
        holder.itemView.setOnLongClickListener {
            tile.onLongClick?.invoke()
            tile.onLongClick != null
        }
    }

    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.poster).clear(holder.poster)
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
        holder.action.setOnClickListener(null)
        super.onViewRecycled(holder)
    }

    class Holder(view: LinearLayout, val poster: ImageView, val title: TextView,
                 val subtitle: TextView, val action: TextView, val coverTitle: TextView) : RecyclerView.ViewHolder(view)
}
