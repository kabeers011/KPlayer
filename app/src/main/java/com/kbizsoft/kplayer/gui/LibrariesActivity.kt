package com.kbizsoft.KPlayer.gui

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.squareup.moshi.*
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kbizsoft.resources.AndroidDevices
import com.kbizsoft.resources.AppContextProvider
import com.kbizsoft.KPlayer.R
import com.kbizsoft.KPlayer.databinding.LibraryItemBinding
import com.kbizsoft.KPlayer.databinding.LicenseActivityBinding
import com.kbizsoft.KPlayer.gui.dialogs.LicenseDialog
import com.kbizsoft.KPlayer.gui.helpers.SelectorViewHolder
import com.kbizsoft.resources.util.applyOverscanMargin

/**
 * Activity showing the different libraries used by KPlayer for Android and their licenses
 */
class LibrariesActivity : BaseActivity() {

    private lateinit var adapter: LibrariesAdapter
    private lateinit var model: LicenseModel

    internal lateinit var binding: LicenseActivityBinding
    override fun getSnackAnchorView(overAudioPlayer:Boolean) = binding.root
    override val displayTitle = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.license_activity)
        val toolbar = findViewById<MaterialToolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_up)
        title = getString(R.string.libraries)

        binding.licenses.layoutManager = LinearLayoutManager(this)
        adapter = LibrariesAdapter {
            LicenseDialog.newInstance(it).show(supportFragmentManager, "LicenseDialog")
        }
        binding.licenses.adapter = adapter

        model = ViewModelProvider(this)[LicenseModel::class.java]
        model.licenses.observe(this, Observer {
            adapter.update(it)
        })
        lifecycleScope.launchWhenStarted { model.refresh() }
        if (AndroidDevices.isTv) applyOverscanMargin(this)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}

class LibrariesAdapter(private val itemClickHandler: (license: LibraryWithLicense) -> Unit) : DiffUtilAdapter<LibraryWithLicense, LibrariesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LibraryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.library = dataset[position]
    }

    override fun getItemCount() = dataset.size

    inner class ViewHolder(vdb: LibraryItemBinding) : SelectorViewHolder<LibraryItemBinding>(vdb) {
        init {
            vdb.root.setOnClickListener { itemClickHandler.invoke(dataset[layoutPosition]) }
        }
    }
}

class LicenseModel : ViewModel() {

    /**
     * Load libraries the json asset file
     */
    suspend fun refresh() {
        val parsedLicenses = withContext(Dispatchers.IO) {
            val jsonData = AppContextProvider.appResources.openRawResource(R.raw.libraries).bufferedReader().use {
                it.readText()
            }

            val moshi = Moshi.Builder().build()

            val jsonAdapter: JsonAdapter<Licenses> = moshi.adapter(Licenses::class.java)

            val rawLibraries = jsonAdapter.fromJson(jsonData)!!
            mutableListOf<LibraryWithLicense>().apply {
                rawLibraries.libraries.forEach { library ->
                    rawLibraries.licenses.forEach {
                        if (it.id == library.license) {
                            add(LibraryWithLicense(library.title, library.copyright, it.name, it.description, it.link))
                        }
                    }

                }
            }
        }
        licenses.value = parsedLicenses
    }

    val licenses = MutableLiveData<List<LibraryWithLicense>>()
}

data class Licenses(
        @Json(name = "libraries")
        val libraries: List<Library>,
        @Json(name = "licenses")
        val licenses: List<License>
)

data class License(
        @Json(name = "description")
        val description: String,
        @Json(name = "id")
        val id: String,
        @Json(name = "link")
        val link: String,
        @Json(name = "name")
        val name: String
)

data class Library(
        @Json(name = "copyright")
        val copyright: String,
        @Json(name = "license")
        val license: String,
        @Json(name = "title")
        val title: String
)

@Parcelize
data class LibraryWithLicense(
        val title: String,
        val copyright: String,
        val licenseTitle: String,
        val licenseDescription: String,
        val licenseLink: String
) : Parcelable