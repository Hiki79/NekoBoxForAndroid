package io.nekohasekai.sagernet.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.database.GeositeEntry
import io.nekohasekai.sagernet.database.GeositeLibrary
import io.nekohasekai.sagernet.databinding.LayoutRuleLibraryBinding
import io.nekohasekai.sagernet.databinding.LayoutRuleLibraryItemBinding

class RuleLibraryActivity : ThemedActivity() {

    private lateinit var binding: LayoutRuleLibraryBinding
    private lateinit var adapter: LibraryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LayoutRuleLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setTitle(R.string.rule_library_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_navigation_close)
        }

        adapter = LibraryAdapter()
        binding.ruleList.layoutManager = LinearLayoutManager(this)
        binding.ruleList.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString() ?: "")
            }
        })
    }

    private fun fillRoute(entry: GeositeEntry) {
        startActivity(Intent(this, RouteSettingsActivity::class.java).apply {
            putExtra(RouteSettingsActivity.EXTRA_ROUTE_NAME, entry.name)
            putExtra(RouteSettingsActivity.EXTRA_ROUTE_DOMAIN, "geosite:" + entry.geosite)
        })
        finish()
    }

    inner class LibraryAdapter : RecyclerView.Adapter<LibraryHolder>() {

        private val items = GeositeLibrary.All.toMutableList()

        fun filter(query: String) {
            val result = GeositeLibrary.search(query)
            items.clear()
            items.addAll(result)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryHolder {
            return LibraryHolder(
                LayoutRuleLibraryItemBinding.inflate(layoutInflater, parent, false)
            )
        }

        override fun onBindViewHolder(holder: LibraryHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    inner class LibraryHolder(val b: LayoutRuleLibraryItemBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(entry: GeositeEntry) {
            b.entryName.text = entry.name
            b.entryGeosite.text = "geosite:" + entry.geosite
            b.entryDesc.text = entry.description
            b.entryTags.text = entry.tags.joinToString(" · ")
            itemView.setOnClickListener {
                fillRoute(entry)
            }
        }
    }
}
