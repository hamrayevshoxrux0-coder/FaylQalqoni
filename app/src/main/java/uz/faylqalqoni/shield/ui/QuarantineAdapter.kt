package uz.faylqalqoni.shield.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.faylqalqoni.shield.R
import uz.faylqalqoni.shield.databinding.ItemQuarantineBinding
import uz.faylqalqoni.shield.quarantine.QuarantineManager

class QuarantineAdapter(
    private val onDelete: (QuarantineManager.QuarantinedItem) -> Unit
) : RecyclerView.Adapter<QuarantineAdapter.ViewHolder>() {

    private val items = mutableListOf<QuarantineManager.QuarantinedItem>()

    fun submitList(newItems: List<QuarantineManager.QuarantinedItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuarantineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.qFileNameText.text = item.originalName
        holder.binding.qPathText.text = holder.binding.root.context.getString(
            R.string.quarantine_item_subtitle, item.originalPath
        )
        holder.binding.qDeleteButton.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(val binding: ItemQuarantineBinding) : RecyclerView.ViewHolder(binding.root)
}
