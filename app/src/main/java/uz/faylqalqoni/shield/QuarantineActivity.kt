package uz.faylqalqoni.shield

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import uz.faylqalqoni.shield.databinding.ActivityQuarantineBinding
import uz.faylqalqoni.shield.quarantine.QuarantineManager
import uz.faylqalqoni.shield.ui.QuarantineAdapter

/**
 * Karantinga olingan fayllar ro'yxatini ko'rsatadi. Foydalanuvchi bu yerdan
 * faylni butunlay o'chirib tashlashi mumkin (fayl allaqachon ilovaning
 * xavfsiz ichki papkasida bo'lgani uchun, asl joyidagi tahdid allaqachon
 * bartaraf etilgan).
 */
class QuarantineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuarantineBinding
    private lateinit var quarantineManager: QuarantineManager
    private lateinit var adapter: QuarantineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuarantineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        quarantineManager = QuarantineManager(applicationContext)
        adapter = QuarantineAdapter { item ->
            quarantineManager.deletePermanently(item.id)
            refreshList()
        }

        binding.quarantineRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.quarantineRecyclerView.adapter = adapter

        refreshList()
    }

    private fun refreshList() {
        val items = quarantineManager.listQuarantined().sortedByDescending { it.quarantinedAt }
        adapter.submitList(items)
        binding.quarantineEmptyText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.quarantineRecyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
    }
}
