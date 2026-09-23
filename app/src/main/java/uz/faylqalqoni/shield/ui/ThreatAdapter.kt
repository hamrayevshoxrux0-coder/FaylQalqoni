package uz.faylqalqoni.shield.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.faylqalqoni.shield.R
import uz.faylqalqoni.shield.core.ScanResult
import uz.faylqalqoni.shield.core.ThreatLevel
import uz.faylqalqoni.shield.databinding.ItemThreatBinding

class ThreatAdapter : RecyclerView.Adapter<ThreatAdapter.ViewHolder>() {

    private val items = mutableListOf<ScanResult>()

    fun submitList(newItems: List<ScanResult>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addOrUpdate(result: ScanResult) {
        val idx = items.indexOfFirst { it.path == result.path }
        if (idx >= 0) {
            items[idx] = result
            notifyItemChanged(idx)
        } else {
            items.add(0, result)
            notifyItemInserted(0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThreatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemThreatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(result: ScanResult) {
            val context = binding.root.context
            binding.fileNameText.text = result.displayName
            binding.reasonText.text = result.reasons.joinToString(" • ")
            binding.pathText.text = result.path
            binding.pathText.visibility = if (result.isInstalledApp) android.view.View.GONE else android.view.View.VISIBLE

            val color = when (result.level) {
                ThreatLevel.DANGEROUS -> R.color.level_dangerous
                ThreatLevel.SUSPICIOUS -> R.color.level_suspicious
                ThreatLevel.SAFE -> R.color.level_safe
            }
            val colorInt = context.getColor(color)
            binding.levelIndicator.setBackgroundColor(colorInt)
            binding.levelBadge.setBackgroundColor(colorInt)
            binding.levelBadge.text = when (result.level) {
                ThreatLevel.DANGEROUS -> context.getString(R.string.threat_level_dangerous)
                ThreatLevel.SUSPICIOUS -> context.getString(R.string.threat_level_suspicious)
                ThreatLevel.SAFE -> ""
            }
        }
    }
}
