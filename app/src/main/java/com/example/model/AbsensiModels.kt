package com.example.model

/** Model Data dan sumber data */

enum class PeranPengguna {
  BELUM_PILIH,
  ADMIN,
  KARYAWAN
}

enum class StatusAbsensi(val label: String) {
  TEPAT_WAKTU("Tepat Waktu"),
  TERLAMBAT("Terlambat"),
  IZIN("Izin"),
  BELUM_ABSEN("Belum Absen")
}

data class DataKaryawan(
  val id: String,           // Contoh: "K-001"
  val nip: String,          // Nomor Induk Pegawai kantor
  val nama: String,         // Contoh: "Ahmad Fauzi"
  val departemen: String,   // Contoh: "Teknologi"
  val avatarInisial: String = "AF"
)

data class RiwayatAbsensiItem(
  val id: String,
  val hariTanggal: String,
  val jamMasuk: String,
  val jamPulang: String,
  val status: StatusAbsensi,
  val catatan: String = ""
)

data class AbsensiHariIni(
  val jamMasuk: String? = null,
  val jamPulang: String? = null,
  val status: StatusAbsensi? = null,
  val sudahAbsenMasuk: Boolean = false,
  val sudahAbsenPulang: Boolean = false
)

data class BarisAbsensiKaryawan(
  val idKaryawan: String,
  val nama: String,
  val departemen: String,
  val jamMasuk: String,
  val jamPulang: String,
  val status: StatusAbsensi
)

data class KonfigurasiAdminDashboard(
  val jamMasukKerja: String = "08:00",
  val jamPulangKerja: String = "17:00",
  val batasTerlambat: String = "08:00",
  val pengumuman: String = "Pengumuman: Jam kerja resmi kantor dimulai pukul 08:00 WIB.",
  val totalKaryawan: Int = 0,
  val totalHadir: Int = 0,
  val totalTerlambat: Int = 0,
  val totalBelumAbsen: Int = 0
)

object DummyDataKantor {
  // Master data karyawan resmi kantor (untuk verifikasi registrasi)
  val daftarKaryawanResmi = listOf(
    DataKaryawan("K-001", "19940115001", "Ahmad Fauzi", "Teknologi", "AF"),
    DataKaryawan("K-002", "19950320002", "Siti Nurhaliza", "Keuangan", "SN"),
    DataKaryawan("K-003", "19960711003", "Budi Santoso", "Operasional", "BS"),
    DataKaryawan("K-004", "19970822004", "Dewi Lestari", "Sumber Daya Manusia", "DL")
  )

  // Karyawan default yang aktif masuk
  val karyawanAktifDefault = daftarKaryawanResmi[0] // Ahmad Fauzi, K-001

  // Riwayat awal: kosong untuk kondisi penggunaan pertama kali
  val riwayatAwal: List<RiwayatAbsensiItem> = emptyList()

  // Data baris tabel absensi hari ini untuk Admin: Dimulai dari 0 (kosong) agar data yang masuk akurat
  val absensiHariIniAdmin: List<BarisAbsensiKaryawan> = emptyList()
}
