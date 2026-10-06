package com.example.ai

import com.example.BuildConfig
import com.example.model.BarisAbsensiKaryawan
import com.example.model.DataKaryawan
import com.example.model.KonfigurasiAdminDashboard
import com.example.model.RiwayatAbsensiItem
import com.example.model.StatusAbsensi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * =========================================================================
 * LAYANAN OLAH DATA OTOMATIS & ANALISIS KEHADIRAN CERDAS
 * MENGGUNAKAN GEMINI 3.1 PRO (HIGH THINKING MODE)
 * =========================================================================
 * Membaca seluruh isi database absensi lokal kantor dan melakukan penalaran mendalam
 * (High Thinking Level) untuk menyusun rekapitulasi, evaluasi ketepatan waktu,
 * deteksi anomali, serta rekomendasi kebijakan HRD kantor secara otomatis.
 */
object GeminiAiService {
  private const val MODEL_NAME = "gemini-3.1-pro-preview"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  /**
   * Menjalankan kueri penalaran tingkat tinggi (High Thinking Mode) ke Gemini 3.1 Pro.
   */
  suspend fun callThinkingModel(prompt: String): Result<String> = withContext(Dispatchers.IO) {
    val apiKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      // Jika API Key belum disetel di Secrets Panel, berikan hasil analisis lokal yang komprehensif
      return@withContext Result.failure(
        IllegalStateException("API Key Gemini belum disetel. Anda dapat memasukkannya di panel Secrets AI Studio.")
      )
    }

    try {
      val requestJson = JSONObject().apply {
        // Contents
        val partsArray = JSONArray().apply {
          put(JSONObject().apply { put("text", prompt) })
        }
        val contentsArray = JSONArray().apply {
          put(JSONObject().apply { put("parts", partsArray) })
        }
        put("contents", contentsArray)

        // Generation Config dengan High Thinking Mode (tanpa maxOutputTokens sesuai instruksi)
        val thinkingConfig = JSONObject().apply {
          put("thinkingLevel", "HIGH")
        }
        val generationConfig = JSONObject().apply {
          put("thinkingConfig", thinkingConfig)
          put("temperature", 0.6)
        }
        put("generationConfig", generationConfig)
      }

      val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url("$BASE_URL?key=$apiKey")
        .post(requestBody)
        .build()

      val response = httpClient.newCall(request).execute()
      val responseString = response.body?.string().orEmpty()

      if (!response.isSuccessful) {
        val errorMsg = runCatching {
          JSONObject(responseString).getJSONObject("error").getString("message")
        }.getOrDefault("HTTP ${response.code}: $responseString")
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      if (candidates != null && candidates.length() > 0) {
        val firstCandidate = candidates.getJSONObject(0)
        val contentObj = firstCandidate.optJSONObject("content")
        val parts = contentObj?.optJSONArray("parts")
        if (parts != null && parts.length() > 0) {
          // Cari part bertipe text (abaikan part thinking jika terpisah atau ambil gabungan)
          val textBuilder = StringBuilder()
          for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
              textBuilder.append(part.getString("text"))
            }
          }
          val hasil = textBuilder.toString().trim()
          if (hasil.isNotEmpty()) {
            return@withContext Result.success(hasil)
          }
        }
      }
      Result.failure(Exception("Model tidak mengembalikan respon teks."))
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Olah Data Otomatis untuk Administrator:
   * Menganalisis seluruh baris presensi hari ini, metrik kehadiran, jam kerja kantor, dan memberikan rekomendasi operasional.
   */
  suspend fun olahDataAbsensiAdmin(
    daftarAbsensi: List<BarisAbsensiKaryawan>,
    config: KonfigurasiAdminDashboard,
    customQuery: String? = null
  ): String {
    val stringData = StringBuilder()
    stringData.appendLine("=== DATA MASTER & ABSENSI KANTOR HARI INI ===")
    stringData.appendLine("Jam Masuk Kerja: ${config.jamMasukKerja} WIB")
    stringData.appendLine("Batas Toleransi Terlambat: ${config.batasTerlambat} WIB")
    stringData.appendLine("Jam Pulang Kerja: ${config.jamPulangKerja} WIB")
    stringData.appendLine("Total Karyawan Terdaftar: ${if (config.totalKaryawan > 0) config.totalKaryawan else daftarAbsensi.size}")
    stringData.appendLine("Jumlah Data Masuk: ${daftarAbsensi.size}")
    stringData.appendLine("\n--- Rincian Karyawan Presensi ---")
    if (daftarAbsensi.isEmpty()) {
      stringData.appendLine("(Belum ada karyawan yang melakukan presensi hari ini)")
    } else {
      daftarAbsensi.forEachIndexed { index, row ->
        stringData.appendLine("${index + 1}. [${row.idKaryawan}] ${row.nama} | Dept: ${row.departemen} | Masuk: ${row.jamMasuk} | Pulang: ${row.jamPulang} | Status: ${row.status.label}")
      }
    }

    val prompt = """
Anda adalah Sistem Kecerdasan Buatan Tingkat Tinggi (AI Enterprise HR Analyst) untuk Aplikasi Absensi Kantor.
Tugas Anda adalah membaca dan mengolah data absensi berikut secara otomatis dengan penalaran mendalam (High Thinking Mode):

$stringData

${if (!customQuery.isNullOrBlank()) "Permintaan/Pertanyaan Khusus Admin:\n$customQuery\n" else ""}

Instruksi Analisis:
1. Rekapitulasi & Statistik Kehadiran: Hitung persentase kehadiran tepat waktu, keterlambatan, dan yang belum absen.
2. Analisis Per Departemen: Tinjau kedisiplinan dan sebaran per divisi.
3. Deteksi Anomali & Temuan Penting: Karyawan yang terlambat atau belum absen pulang saat sudah jam kerja usai.
4. Rekomendasi Tindakan Admin: Berikan saran praktis untuk pimpinan kantor (penegakan disiplin, apresiasi, atau tindak lanjut).
Sajikan dalam format yang rapi, profesional, berbobot, dan mudah dipahami.
    """.trimIndent()

    val result = callThinkingModel(prompt)
    return result.getOrElse {
      // Fallback analisis lokal otomatis jika API key belum aktif
      buatAnalisisLokalAdmin(daftarAbsensi, config, it.message)
    }
  }

  /**
   * Olah Data Otomatis untuk Karyawan:
   * Menganalisis riwayat kehadiran personal karyawan, rasio kepatuhan waktu, dan saran performa.
   */
  suspend fun olahDataRiwayatKaryawan(
    karyawan: DataKaryawan,
    daftarRiwayat: List<RiwayatAbsensiItem>
  ): String {
    val sb = StringBuilder()
    sb.appendLine("=== DATA REKAP KEHADIRAN KARYAWAN ===")
    sb.appendLine("Nama: ${karyawan.nama} (ID: ${karyawan.id}, NIP: ${karyawan.nip})")
    sb.appendLine("Departemen: ${karyawan.departemen}")
    sb.appendLine("Total Riwayat Tercatat: ${daftarRiwayat.size} hari")
    sb.appendLine("\n--- Log Riwayat ---")
    if (daftarRiwayat.isEmpty()) {
      sb.appendLine("(Belum ada catatan riwayat presensi)")
    } else {
      daftarRiwayat.forEachIndexed { index, r ->
        sb.appendLine("${index + 1}. ${r.hariTanggal} | Masuk: ${r.jamMasuk} | Pulang: ${r.jamPulang} | Status: ${r.status.label} ${if (r.catatan.isNotBlank()) "(${r.catatan})" else ""}")
      }
    }

    val prompt = """
Anda adalah Asisten Karir & Penasihat HR AI untuk Karyawan.
Lakukan analisis mendalam terhadap riwayat presensi karyawan ini menggunakan High Thinking Mode:

$sb

Instruksi Analisis:
1. Ringkasan Kinerja Kehadiran: Evaluasi konsistensi jam masuk dan kepulangan.
2. Skor Kedisiplinan & Tren: Tinjau tren ketepatan waktu.
3. Rekomendasi Personal: Berikan tips manajemen waktu kerja dan kesehatan kerja.
Sajikan dengan ramah, motivatif, dan profesional.
    """.trimIndent()

    val result = callThinkingModel(prompt)
    return result.getOrElse {
      buatAnalisisLokalKaryawan(karyawan, daftarRiwayat, it.message)
    }
  }

  /**
   * Otomasi Pembuatan Draf Surat Izin / Cuti Resmi oleh AI
   */
  suspend fun buatDrafSuratIzin(
    karyawan: DataKaryawan,
    jenisIzin: String,
    alasan: String
  ): String {
    val prompt = """
Buatkan draf surat permohonan izin kerja resmi formal bahasa Indonesia untuk:
- Nama Karyawan: ${karyawan.nama}
- NIP: ${karyawan.nip}
- Departemen: ${karyawan.departemen}
- Jenis Izin: $jenisIzin
- Alasan / Keterangan: $alasan

Formatkan secara lengkap dengan salam pembuka, rincian identitas, pokok permohonan, penutup, dan tempat tanda tangan formal.
    """.trimIndent()

    val result = callThinkingModel(prompt)
    return result.getOrElse {
      """
SURAT PERMOHONAN IZIN KERJA RESMI

Kepada Yth.
Kepala Bagian HRD / Pimpinan Kantor
Di Tempat

Dengan hormat,
Saya yang bertanda tangan di bawah ini:
Nama        : ${karyawan.nama}
NIP         : ${karyawan.nip}
Departemen  : ${karyawan.departemen}

Bermaksud untuk mengajukan permohonan izin $jenisIzin dengan alasan:
"$alasan"

Demikian surat permohonan ini saya sampaikan dengan sebenar-benarnya. Atas perhatian dan izin yang diberikan, saya ucapkan terima kasih.

Hormat saya,

(${karyawan.nama})
      """.trimIndent()
    }
  }

  // Fallback Analisis Lokal Admin ketika API key belum diset
  private fun buatAnalisisLokalAdmin(
    daftarAbsensi: List<BarisAbsensiKaryawan>,
    config: KonfigurasiAdminDashboard,
    catatanError: String?
  ): String {
    val totalHadir = daftarAbsensi.count { it.status == StatusAbsensi.TEPAT_WAKTU || it.status == StatusAbsensi.TERLAMBAT }
    val totalTepat = daftarAbsensi.count { it.status == StatusAbsensi.TEPAT_WAKTU }
    val totalTerlambat = daftarAbsensi.count { it.status == StatusAbsensi.TERLAMBAT }
    val totalIzin = daftarAbsensi.count { it.status == StatusAbsensi.IZIN }
    val totalPegawai = if (config.totalKaryawan > 0) config.totalKaryawan else maxOf(daftarAbsensi.size, 1)
    val persentaseHadir = (totalHadir.toFloat() / totalPegawai * 100).toInt()

    return buildString {
      appendLine("📊 REKAPITULASI OLAH DATA OTOMATIS (SISTEM CERDAS KANTOR)")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
      appendLine("• Tingkat Kehadiran: $persentaseHadir% ($totalHadir dari $totalPegawai karyawan)")
      appendLine("• Tepat Waktu: $totalTepat karyawan")
      appendLine("• Terlambat: $totalTerlambat karyawan")
      appendLine("• Izin/Sakit: $totalIzin karyawan")
      appendLine()
      appendLine("📌 TEMUAN OPERASIONAL & DISIPLIN:")
      if (totalTerlambat > 0) {
        val yangTerlambat = daftarAbsensi.filter { it.status == StatusAbsensi.TERLAMBAT }.joinToString { it.nama }
        appendLine("- Karyawan melewati batas toleransi ${config.batasTerlambat} WIB: $yangTerlambat.")
      } else {
        appendLine("- Seluruh karyawan yang hadir hari ini mematuhi jam masuk kerja sebelum ${config.batasTerlambat} WIB.")
      }
      appendLine()
      appendLine("💡 REKOMENDASI MANAJEMEN:")
      appendLine("1. Lakukan pemantauan absensi pulang pada pukul ${config.jamPulangKerja} WIB.")
      appendLine("2. Berikan apresiasi bagi divisi dengan kehadiran 100% tepat waktu.")
      if (catatanError != null && catatanError.contains("API Key")) {
        appendLine()
        appendLine("ℹ️ Catatan: Untuk mengaktifkan Gemini 3.1 Pro High Thinking Mode penuh secara cloud, tambahkan GEMINI_API_KEY di Secrets Panel AI Studio.")
      }
    }
  }

  // Fallback Analisis Lokal Karyawan
  private fun buatAnalisisLokalKaryawan(
    karyawan: DataKaryawan,
    daftarRiwayat: List<RiwayatAbsensiItem>,
    catatanError: String?
  ): String {
    val totalTepat = daftarRiwayat.count { it.status == StatusAbsensi.TEPAT_WAKTU }
    val totalTerlambat = daftarRiwayat.count { it.status == StatusAbsensi.TERLAMBAT }
    val totalIzin = daftarRiwayat.count { it.status == StatusAbsensi.IZIN }
    val totalHari = daftarRiwayat.size

    return buildString {
      appendLine("📈 EVALUASI REKAP KEHADIRAN PRIBADI")
      appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
      appendLine("Pegawai: ${karyawan.nama} | ${karyawan.departemen}")
      appendLine("Total Hari Tercatat: $totalHari hari")
      appendLine("• Hadir Tepat Waktu: $totalTepat hari")
      appendLine("• Keterlambatan: $totalTerlambat hari")
      appendLine("• Izin / Sakit: $totalIzin hari")
      appendLine()
      appendLine("🌟 INSIGHT & REKOMENDASI:")
      if (totalTerlambat == 0 && totalHari > 0) {
        appendLine("- Catatan sempurna! Pertahankan disiplin waktu kehadiran Anda.")
      } else if (totalTerlambat > 0) {
        appendLine("- Terdapat $totalTerlambat kali keterlambatan. Pertimbangkan berangkat 15-20 menit lebih awal untuk mengantisipasi kepadatan jalan.")
      } else {
        appendLine("- Lakukan presensi secara konsisten setiap hari kerja.")
      }
      if (catatanError != null && catatanError.contains("API Key")) {
        appendLine()
        appendLine("ℹ️ Mode offline lokal aktif. Konfigurasi GEMINI_API_KEY di Secrets Panel untuk penalaran cloud mendalam.")
      }
    }
  }
}
