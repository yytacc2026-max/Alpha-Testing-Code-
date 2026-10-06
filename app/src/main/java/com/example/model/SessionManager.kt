package com.example.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** * Menyimpan status pendaftaran, data absensi hari ini, dan riwayat di lokal perangkat.
 */
class SessionManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("absensi_kantor_prefs", Context.MODE_PRIVATE)

  companion object {
    private const val KEY_FRESH_START_V1 = "key_fresh_start_v1"
    private const val KEY_SUDAH_TERDAFTAR = "key_sudah_terdaftar"
    private const val KEY_ID = "key_id"
    private const val KEY_NIP = "key_nip"
    private const val KEY_NAMA = "key_nama"
    private const val KEY_DEPARTEMEN = "key_departemen"
    private const val KEY_AVATAR = "key_avatar"

    // Key status absensi harian
    private const val KEY_JAM_MASUK = "key_jam_masuk"
    private const val KEY_JAM_PULANG = "key_jam_pulang"
    private const val KEY_STATUS_ABSEN = "key_status_absen"
    private const val KEY_SUDAH_MASUK = "key_sudah_masuk"
    private const val KEY_SUDAH_PULANG = "key_sudah_pulang"
    private const val KEY_RIWAYAT_JSON = "key_riwayat_json"

    // Key absensi & konfigurasi Admin
    private const val KEY_ADMIN_ABSENSI_JSON = "key_admin_absensi_json"
    private const val KEY_ADMIN_JAM_MASUK = "key_admin_jam_masuk"
    private const val KEY_ADMIN_JAM_PULANG = "key_admin_jam_pulang"
    private const val KEY_ADMIN_BATAS_TERLAMBAT = "key_admin_batas_terlambat"
    private const val KEY_ADMIN_PENGUMUMAN = "key_admin_pengumuman"
    private const val KEY_ADMIN_TOTAL_KARYAWAN = "key_admin_total_karyawan"

    // Key riwayat input terakhir di perangkat
    private const val KEY_LAST_INPUT_NIP = "key_last_input_nip"
    private const val KEY_LAST_INPUT_NAMA = "key_last_input_nama"
    private const val KEY_LAST_INPUT_DEPARTEMEN = "key_last_input_departemen"
  }

  init {
    if (!prefs.getBoolean(KEY_FRESH_START_V1, false)) {
      prefs.edit()
        .clear()
        .putBoolean(KEY_FRESH_START_V1, true)
        .apply()
    }
  }

  fun simpanPendaftaran(karyawan: DataKaryawan) {
    prefs.edit()
      .putBoolean(KEY_SUDAH_TERDAFTAR, true)
      .putString(KEY_ID, karyawan.id)
      .putString(KEY_NIP, karyawan.nip)
      .putString(KEY_NAMA, karyawan.nama)
      .putString(KEY_DEPARTEMEN, karyawan.departemen)
      .putString(KEY_AVATAR, karyawan.avatarInisial)
      .apply()
    simpanInputTerakhir(karyawan.nip, karyawan.nama, karyawan.departemen)
  }

  fun isSudahTerdaftar(): Boolean {
    return prefs.getBoolean(KEY_SUDAH_TERDAFTAR, false)
  }

  fun getKaryawanTerdaftar(): DataKaryawan? {
    if (!isSudahTerdaftar()) return null
    val id = prefs.getString(KEY_ID, "K-001") ?: "K-001"
    val nip = prefs.getString(KEY_NIP, "19940115001") ?: "19940115001"
    val nama = prefs.getString(KEY_NAMA, "Ahmad Fauzi") ?: "Ahmad Fauzi"
    val departemen = prefs.getString(KEY_DEPARTEMEN, "Teknologi") ?: "Teknologi"
    val avatar = prefs.getString(KEY_AVATAR, "AF") ?: "AF"
    return DataKaryawan(id, nip, nama, departemen, avatar)
  }

  fun simpanAbsensiHariIni(absensi: AbsensiHariIni) {
    prefs.edit()
      .putString(KEY_JAM_MASUK, absensi.jamMasuk)
      .putString(KEY_JAM_PULANG, absensi.jamPulang)
      .putString(KEY_STATUS_ABSEN, absensi.status?.name)
      .putBoolean(KEY_SUDAH_MASUK, absensi.sudahAbsenMasuk)
      .putBoolean(KEY_SUDAH_PULANG, absensi.sudahAbsenPulang)
      .apply()
  }

  fun getAbsensiHariIni(): AbsensiHariIni {
    val masuk = prefs.getString(KEY_JAM_MASUK, null)
    val pulang = prefs.getString(KEY_JAM_PULANG, null)
    val statusStr = prefs.getString(KEY_STATUS_ABSEN, null)
    val status = statusStr?.let { runCatching { StatusAbsensi.valueOf(it) }.getOrNull() }
    val sudahMasuk = prefs.getBoolean(KEY_SUDAH_MASUK, false)
    val sudahPulang = prefs.getBoolean(KEY_SUDAH_PULANG, false)
    return AbsensiHariIni(
      jamMasuk = masuk,
      jamPulang = pulang,
      status = status,
      sudahAbsenMasuk = sudahMasuk,
      sudahAbsenPulang = sudahPulang
    )
  }

  fun simpanRiwayat(list: List<RiwayatAbsensiItem>) {
    val jsonArray = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("hariTanggal", item.hariTanggal)
      obj.put("jamMasuk", item.jamMasuk)
      obj.put("jamPulang", item.jamPulang)
      obj.put("status", item.status.name)
      obj.put("catatan", item.catatan)
      jsonArray.put(obj)
    }
    prefs.edit().putString(KEY_RIWAYAT_JSON, jsonArray.toString()).apply()
  }

  fun getRiwayat(): List<RiwayatAbsensiItem> {
    val jsonStr = prefs.getString(KEY_RIWAYAT_JSON, null) ?: return emptyList()
    val result = mutableListOf<RiwayatAbsensiItem>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val status = runCatching { StatusAbsensi.valueOf(obj.getString("status")) }.getOrDefault(StatusAbsensi.TEPAT_WAKTU)
        result.add(
          RiwayatAbsensiItem(
            id = obj.getString("id"),
            hariTanggal = obj.getString("hariTanggal"),
            jamMasuk = obj.getString("jamMasuk"),
            jamPulang = obj.getString("jamPulang"),
            status = status,
            catatan = obj.optString("catatan", "")
          )
        )
      }
    } catch (_: Exception) {
      return emptyList()
    }
    return result
  }

  fun hapusPendaftaran() {
    prefs.edit()
      .remove(KEY_SUDAH_TERDAFTAR)
      .remove(KEY_ID)
      .remove(KEY_NIP)
      .remove(KEY_NAMA)
      .remove(KEY_DEPARTEMEN)
      .remove(KEY_AVATAR)
      .remove(KEY_JAM_MASUK)
      .remove(KEY_JAM_PULANG)
      .remove(KEY_STATUS_ABSEN)
      .remove(KEY_SUDAH_MASUK)
      .remove(KEY_SUDAH_PULANG)
      .remove(KEY_RIWAYAT_JSON)
      .remove(KEY_ADMIN_ABSENSI_JSON)
      .apply()
  }

  // =========================================================================
  // DATA ABSENSI & METRIK ADMIN (DIMULAI DARI 0, AKURAT SESUAI AKTIVITAS)
  // =========================================================================
  fun getAdminDaftarAbsensi(): List<BarisAbsensiKaryawan> {
    val jsonStr = prefs.getString(KEY_ADMIN_ABSENSI_JSON, null) ?: return emptyList()
    val list = mutableListOf<BarisAbsensiKaryawan>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val status = runCatching { StatusAbsensi.valueOf(obj.getString("status")) }.getOrDefault(StatusAbsensi.TEPAT_WAKTU)
        list.add(
          BarisAbsensiKaryawan(
            idKaryawan = obj.getString("idKaryawan"),
            nama = obj.getString("nama"),
            departemen = obj.getString("departemen"),
            jamMasuk = obj.getString("jamMasuk"),
            jamPulang = obj.getString("jamPulang"),
            status = status
          )
        )
      }
    } catch (_: Exception) {
      return emptyList()
    }
    return list
  }

  fun simpanAdminDaftarAbsensi(list: List<BarisAbsensiKaryawan>) {
    val jsonArray = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("idKaryawan", item.idKaryawan)
      obj.put("nama", item.nama)
      obj.put("departemen", item.departemen)
      obj.put("jamMasuk", item.jamMasuk)
      obj.put("jamPulang", item.jamPulang)
      obj.put("status", item.status.name)
      jsonArray.put(obj)
    }
    prefs.edit().putString(KEY_ADMIN_ABSENSI_JSON, jsonArray.toString()).apply()
  }

  fun tambahAtauUpdateAbsensiKaryawan(baris: BarisAbsensiKaryawan) {
    val currentList = getAdminDaftarAbsensi().toMutableList()
    val index = currentList.indexOfFirst { it.idKaryawan == baris.idKaryawan }
    if (index >= 0) {
      currentList[index] = baris
    } else {
      currentList.add(baris)
    }
    simpanAdminDaftarAbsensi(currentList)
  }

  fun resetAdminAbsensiHariIni() {
    prefs.edit().remove(KEY_ADMIN_ABSENSI_JSON).apply()
  }

  fun getKonfigurasiAdmin(): KonfigurasiAdminDashboard {
    val jamMasuk = prefs.getString(KEY_ADMIN_JAM_MASUK, "08:00") ?: "08:00"
    val jamPulang = prefs.getString(KEY_ADMIN_JAM_PULANG, "17:00") ?: "17:00"
    val batas = prefs.getString(KEY_ADMIN_BATAS_TERLAMBAT, "08:00") ?: "08:00"
    val pengumuman = prefs.getString(KEY_ADMIN_PENGUMUMAN, "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.") ?: "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB."
    val totalKaryawan = prefs.getInt(KEY_ADMIN_TOTAL_KARYAWAN, 0)
    return KonfigurasiAdminDashboard(
      jamMasukKerja = jamMasuk,
      jamPulangKerja = jamPulang,
      batasTerlambat = batas,
      pengumuman = pengumuman,
      totalKaryawan = totalKaryawan,
      totalHadir = 0,
      totalTerlambat = 0,
      totalBelumAbsen = 0
    )
  }

  fun simpanKonfigurasiAdmin(config: KonfigurasiAdminDashboard) {
    prefs.edit()
      .putString(KEY_ADMIN_JAM_MASUK, config.jamMasukKerja)
      .putString(KEY_ADMIN_JAM_PULANG, config.jamPulangKerja)
      .putString(KEY_ADMIN_BATAS_TERLAMBAT, config.batasTerlambat)
      .putString(KEY_ADMIN_PENGUMUMAN, config.pengumuman)
      .putInt(KEY_ADMIN_TOTAL_KARYAWAN, config.totalKaryawan)
      .apply()
  }

  // =========================================================================
  // RIWAYAT INPUT TERAKHIR DI PERANGKAT (FITUR ISI OTOMATIS PERSONAL)
  // =========================================================================
  fun simpanInputTerakhir(nip: String, nama: String, departemen: String) {
    if (nip.isNotBlank() || nama.isNotBlank()) {
      prefs.edit()
        .putString(KEY_LAST_INPUT_NIP, nip.trim())
        .putString(KEY_LAST_INPUT_NAMA, nama.trim())
        .putString(KEY_LAST_INPUT_DEPARTEMEN, departemen.trim())
        .apply()
    }
  }

  fun getInputTerakhir(): DataInputTerakhir? {
    val nip = prefs.getString(KEY_LAST_INPUT_NIP, null)
    val nama = prefs.getString(KEY_LAST_INPUT_NAMA, null)
    val departemen = prefs.getString(KEY_LAST_INPUT_DEPARTEMEN, "") ?: ""
    if (nip.isNullOrBlank() && nama.isNullOrBlank()) return null
    return DataInputTerakhir(
      nip = nip ?: "",
      nama = nama ?: "",
      departemen = departemen
    )
  }
}

/** Data masukan terakhir pengguna di perangkat masing-masing */
data class DataInputTerakhir(
  val nip: String,
  val nama: String,
  val departemen: String
)
