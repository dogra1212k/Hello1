package com.example.streambox

import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/** Only visible cards and their posters are allocated, even with thousands of titles. */
class MovieGridAdapter : RecyclerView.Adapter<MovieGridAdapter.Holder>() {
    data class Tile(val title: String, val subtitle: String, val poster: String = "", val action: String,
                    val onClick: () -> Unit, val onLongClick: (() -> Unit)? = null)
    private val tiles = mutableListOf<Tile>()
    private var rowHeight = 0

    /** Fit three rows in the measured grid, retaining readable cards on small screens. */
    fun fitThreeRows(viewportHeight: Int, density: Float, fontScale: Float) {
        if (viewportHeight <= 0) return
        val minimum = ((120 + 36 * fontScale.coerceAtLeast(1f)) * density).toInt()
        val next = (viewportHeight / 3).coerceAtLeast(minimum)
        if (next == rowHeight) return
        rowHeight = next
        notifyItemRangeChanged(0, tiles.size)
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
        card.addView(poster, LinearLayout.LayoutParams(-1, 0, 1f))
        val title = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }
        card.addView(title, LinearLayout.LayoutParams(-1, title.lineHeight * 2 + dp(4)))
        val subtitle = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 11f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        card.addView(subtitle, LinearLayout.LayoutParams(-1, subtitle.lineHeight + dp(4)))
        val action = TextView(context).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(155, 12, 28))
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 1
        }
        card.addView(action, LinearLayout.LayoutParams(-1, dp(34)))
        return Holder(card, poster, title, subtitle, action)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (rowHeight > 0) {
            val params = holder.itemView.layoutParams as RecyclerView.LayoutParams
            val height = rowHeight - params.topMargin - params.bottomMargin
            if (params.height != height) {
                params.height = height
                holder.itemView.layoutParams = params
            }
        }
        val tile = tiles[position]
        holder.title.text = tile.title
        holder.subtitle.text = tile.subtitle
        holder.action.text = tile.action
        holder.poster.contentDescription = tile.title
        Glide.with(holder.poster).load(tile.poster.ifBlank { null })
            .placeholder(R.drawable.ic_streambox_logo).error(R.drawable.ic_streambox_logo)
            .centerCrop().into(holder.poster)
        holder.itemView.contentDescription = "${tile.title}, ${tile.subtitle}, ${tile.action}"
        holder.itemView.setOnClickListener { tile.onClick() }
        holder.itemView.setOnLongClickListener {
            tile.onLongClick?.invoke()
            tile.onLongClick != null
        }
    }

    override fun onViewRecycled(holder: Holder) {
        Glide.with(holder.poster).clear(holder.poster)
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)
        super.onViewRecycled(holder)
    }

    class Holder(view: LinearLayout, val poster: ImageView, val title: TextView,
                 val subtitle: TextView, val action: TextView) : RecyclerView.ViewHolder(view)
}
