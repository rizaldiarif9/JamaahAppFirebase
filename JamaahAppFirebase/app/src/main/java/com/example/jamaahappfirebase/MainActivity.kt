package com.example.jamaahappfirebase

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.forEach
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jamaahappfirebase.adapter.JamaahAdapter
import com.example.jamaahappfirebase.databinding.ActivityMainBinding
import com.example.jamaahappfirebase.databinding.LayoutAddEditJamaahBinding
import com.example.jamaahappfirebase.entity.Jamaah
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), JamaahAdapter.FirebaseDataListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: JamaahAdapter
    private lateinit var db: FirebaseDatabase
    private lateinit var jamaahRef: DatabaseReference
    private lateinit var listJamaah: ArrayList<Jamaah>
    private  lateinit var mToolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mToolbar = binding.toolbarMain
        setSupportActionBar(mToolbar)
        setChipGroup()

        db = Firebase.database
        binding.rvJamaah.layoutManager = LinearLayoutManager(this)
        binding.rvJamaah.setHasFixedSize(true)
        jamaahRef = db.reference.child(JAMAAH_CHILD)
        jamaahRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                listJamaah = ArrayList()
                Log.d("Snapshot", "$snapshot")
                for (dataSnapshoot in snapshot.children) {
                    val jamaah = dataSnapshoot.getValue(Jamaah::class.java)
                    jamaah?.id = dataSnapshoot.key.toString()
                    if (jamaah != null) {
                        listJamaah.add(jamaah)
                    }
                }
                Log.d("List Jamaah", "$listJamaah")
                adapter = JamaahAdapter(this@MainActivity, listJamaah)
                binding.rvJamaah.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity, "${error.details} ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
        binding.fabAdd.setOnClickListener {
            dialogTambahJamaah()
        }

    }
    override fun onDataClick(jamaah: Jamaah?, position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
        alertDialogBuilder.setTitle("Pilih Aksi")
        alertDialogBuilder
            .setPositiveButton("Update") { _, _ ->
                dialogUpdateJamaah(jamaah)
            }
            .setNegativeButton("Hapus") { _, _ ->
                hapusJamaah(jamaah)
            }
            .setNeutralButton("Batal") { dialog, _ -> dialog.dismiss()}
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)

        mToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.app_bar_search -> {
                    val menuItemSearch = menu?.findItem(R.id.app_bar_search)
                    val searchView: SearchView = menuItemSearch?.actionView as SearchView
                    searchView.queryHint = "Cari jamaah"
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            searchJamaah(newText)
                            return true
                        }
                    })
                    true
                }
                else -> false
            }
        }
        return true
    }
    private fun setChipGroup(){
        binding.chipGroupFilter.forEach { Child ->
            (Child as? Chip)?.setOnCheckedChangeListener { _, _ -> registerFilterChanged()}
        }
    }
    private fun registerFilterChanged() {
        val ids = binding.chipGroupFilter.checkedChipIds
        val jamaahList = mutableListOf<Jamaah>()

        ids.forEach{id ->
            val text = binding.chipGroupFilter.findViewById<Chip>(id).text
            val jamaahFiltered = listJamaah.filter { it.jenisKelamin == text }
            jamaahFiltered.forEach { jamaah ->
                jamaahList.add(jamaah)
            }
        }
        if (jamaahList.isNotEmpty()){
            adapter.setSearchedList(jamaahList as ArrayList<Jamaah>)
        } else {
            adapter.setSearchedList(listJamaah)
        }
    }
    private fun searchJamaah(query: String?){
        if (query != null){
            val searchedJamaah = listJamaah.filter {
                it.nama?.contains(query, true) == true
                        || it.tglLahir?.contains(query, ignoreCase = true) == true
                        || it.alamat?.contains(query, true) == true
            }
            if (searchedJamaah.isEmpty()){
                Toast.makeText(
                    this@MainActivity, "Tidak ada data ditemukan", Toast.LENGTH_LONG
                ).show()
            } else {
                adapter.setSearchedList(searchedJamaah as ArrayList<Jamaah>)
            }
        }
    }
    private fun dialogTambahJamaah() {
        val tambahDialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_add_edit_jamaah,
            null, false)
        val binding = LayoutAddEditJamaahBinding.bind(view)
        binding.btnTglLahir.setOnClickListener {
            setTglLahir(binding)
        }
        tambahDialogBuilder
            .setTitle("Tambah Jamaah")
            .setView(view)
            .setPositiveButton("Simpan") {_, _ ->
                val nama = binding.edNamaJamaah.text.toString()
                val alamat = binding.edAlamatJamaah.text.toString()
                val tglInput = getCurrentDate()
                if (nama.isNotEmpty() && alamat.isNotEmpty()) {
                    val jamaah = Jamaah()
                    jamaah.nama = nama
                    jamaah.alamat = alamat
                    jamaah.tglInput = tglInput
                    Log.d("Submit Jamaah", "$jamaah")
                    submitDataJamaah(jamaah)
                } else {

                    if (nama.isEmpty()) {
                        binding.edNamaJamaah.error = "Form tidak boleh kosong"
                    }
                    if (alamat.isEmpty()) {
                        binding.edNamaJamaah.error = "Form tidak boleh kosong"
                    }
                }
            }
            .setNegativeButton("Batal") {dialog, _ -> dialog.dismiss()}
        val tambahDialog = tambahDialogBuilder.create()
        tambahDialog.show()
        tambahDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nip = binding.edNipJamaah.text.toString()
            val nama = binding.edNamaJamaah.text.toString()
            var jenisKelamin = "Laki-Laki"
            val checkedRadioButton = binding.radioGroupJkel.checkedRadioButtonId
            val tglLahir = binding.tvTglLahir.text.toString()
            if (checkedRadioButton == R.id.radio_perempuan) jenisKelamin = "Perempuan"
            val alamat = binding.edAlamatJamaah.text.toString()
            val tglInput = getCurrentDate()
            if (nama.isEmpty() || alamat.isEmpty() || nip.isEmpty() || tglLahir ==
                getString(R.string.tanggal_lahir)) {
                Toast.makeText(this@MainActivity, "Form tidak boleh kosong!",
                    Toast.LENGTH_LONG)
                    .show()
            } else {
                val jamaah = Jamaah()
                jamaah.nip = nip.toInt()
                jamaah.nama = nama
                jamaah.jenisKelamin = jenisKelamin
                jamaah.tglLahir = tglLahir
                jamaah.alamat = alamat
                jamaah.tglInput = tglInput
                Log.d("Submit Jamaah", "$jamaah")
                submitDataJamaah(jamaah)
                tambahDialog.dismiss()
            }
        }
    }
    private fun setTglLahir(binding: LayoutAddEditJamaahBinding){
        val myCalendar = Calendar.getInstance()
        val tglLahirFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(tglLahirFormat, Locale.getDefault())
        DatePickerDialog( this@MainActivity, { _, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            binding.tvTglLahir.text = sdf.format(myCalendar.time)
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show()}

    private fun dialogUpdateJamaah(jamaah: Jamaah?) {
        val updateDialogBuilder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_add_edit_jamaah,
            null, false)
        val binding = LayoutAddEditJamaahBinding.bind(view)
        binding.btnTglLahir.setOnClickListener {
            setTglLahir(binding)
        }
        binding.edNipJamaah.setText(jamaah?.nip.toString())
        binding.edNamaJamaah.setText(jamaah?.nama)
        binding.tvTglLahir.text = jamaah?.tglLahir
        if (jamaah?.jenisKelamin == "Perempuan")
            binding.radioGroupJkel.check(R.id.radio_perempuan)
        binding.edAlamatJamaah.setText(jamaah?.alamat)
        updateDialogBuilder
            .setTitle("Update Jamaah")
            .setView(view)
            .setPositiveButton("Simpan") {_, _ ->
                val nama = binding.edNamaJamaah.text.toString()
                val alamat = binding.edAlamatJamaah.text.toString()
                if (nama.isNotEmpty() && alamat.isNotEmpty()) {
                    jamaah?.nama = nama
                    jamaah?.alamat = alamat
                    updateDataJamaah(jamaah)
                } else {
                    if (nama.isEmpty()) {
                        binding.edNamaJamaah.error = "Form tidak boleh kosong"
                    }
                    if (alamat.isEmpty()) {
                        binding.edNamaJamaah.error = "Form tidak boleh kosong"
                    }
                }
            }
            .setNegativeButton("Batal") {dialog, _ -> dialog.dismiss()}
        val updateDialog = updateDialogBuilder.create()
        updateDialog.show()

        updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nip = binding.edNipJamaah.text.toString()
            val nama = binding.edNamaJamaah.text.toString()
            var jenisKelamin = "Laki-Laki"
            val checkedRadioButton = binding.radioGroupJkel.checkedRadioButtonId
            if (checkedRadioButton == R.id.radio_perempuan) jenisKelamin = "Perempuan"
            val tglLahir = binding.tvTglLahir.text.toString()
            val alamat = binding.edAlamatJamaah.text.toString()
            if (nama.isEmpty() || alamat.isEmpty() || nip.isEmpty() || tglLahir ==
                getString(R.string.tanggal_lahir)) {
                Toast.makeText(this@MainActivity, "Form tidak boleh kosong!",
                    Toast.LENGTH_LONG)
                    .show()
            } else {
                jamaah?.nip = nip.toInt()
                jamaah?.nama = nama
                jamaah?.jenisKelamin = jenisKelamin
                jamaah?.tglLahir = tglLahir
                jamaah?.alamat = alamat
                Log.d("Update Jamaah", "$jamaah")
                updateDataJamaah(jamaah)
                updateDialog.dismiss()
            }
        }
    }

    private fun submitDataJamaah(jamaah: Jamaah?) {
        jamaahRef.push()
            .setValue(jamaah).addOnSuccessListener {
                Toast.makeText(this@MainActivity, "Data jamaah berhasil disimpan!",
                    Toast.LENGTH_LONG).show()
            }
    }
    private fun updateDataJamaah(jamaah: Jamaah?) {
        jamaah?.id?.let {
            jamaahRef.child(it)
                .setValue(jamaah).addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Data jamaah berhasil diupdate!",
                        Toast.LENGTH_LONG).show()
                }
        }
    }
    private fun hapusJamaah(jamaah: Jamaah?) {
        jamaah?.id?.let {
            jamaahRef.child(it)
                .removeValue().addOnSuccessListener {
                    Toast.makeText(this@MainActivity, "Data berhasil dihapus!",
                        Toast.LENGTH_LONG).show()
                }
        }
    }
    // Mengambil tanggal dan jam
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy/M/dd HH:mm:ss", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    companion object {
        const val JAMAAH_CHILD = "jamaah"
    }
}