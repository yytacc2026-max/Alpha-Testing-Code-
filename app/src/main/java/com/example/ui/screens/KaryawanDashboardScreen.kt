package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAiService
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 
// coding bagian dasboard untuk karyawan 
// 
enum class TabKaryawan(val label: String, val icon: ImageVector) {
  BERANDA("Beranda", Icons.Default.Home),
  RIWAYAT("Riwayat", Icons.Default.History),
  IZIN("Izin", Icons.Default.Description),
  PROFIL("Profil", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaryawanDashboardScreen(
  karyawan: DataKaryawan,
  onLogout: () -> Unit,
  onResetApp: () -> Unit = onLogout,
  jaringanManager: JaringanKantorManager? = null
) {
  val context = LocalContext.current
  val sessionManager = remember { SessionManager(context) }
  val jManager = jaringanManager ?: remember { JaringanKantorManager(context) }
  val coroutineScope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(TabKaryawan.BERANDA) }

  // State Absensi Hari Ini
  var absensiHariIni by remember {
    mutableStateOf(sessionManager.getAbsensiHariIni())
  }

  // Riwayat Absensi
  var daftarRiwayat by remember {
    mutableStateOf(sessionManager.getRiwayat())
  }

  // Pesan Notifikasi Local
  var bannerPesanLokal by remember { mutableStateOf<String?>(null) }

  // State AI Analisis Riwayat Karyawan 
  var isAiAnalisisOpen by remember { mutableStateOf(false) }
  var isAiLoading by remember { mutableStateOf(false) }
  var hasilAnalisisAi by remember { mutableStateOf<String?>(null) }

  val isDalamJaringanKantor = jManager.isDalamJaringanKantor
  val jarakDariKantorMeter = jManager.jarakDariKantorMeter
  val radiusMaksimalKantor = jManager.radiusMaksimalKantorMeter

  // Sapaan dihitung sekali per sesi layar
  val sapaanWaktu = remember {
    val jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when (jam) {
      in 4..10 -> "Selamat Pagi,"
      in 11..14 -> "Selamat Siang,"
      in 15..17 -> "Selamat Sore,"
      else -> "Selamat Malam,"
    }
  }

  Scaffold(
    containerColor = BgLight,
    bottomBar = {
      NavigationBar(
        containerColor = CardWhite,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("karyawan_bottom_nav")
      ) {
        TabKaryawan.values().forEach { tab ->
          val isSelected = selectedTab == tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTab = tab },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label
              )
            },
            label = {
              Text(
                text = tab.label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TealPrimary,
              selectedTextColor = TealPrimary,
              indicatorColor = TealContainer,
              unselectedIconColor = TextMuted,
              unselectedTextColor = TextMuted
            )
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
      ) {
        when (selectedTab) {
          TabKaryawan.BERANDA -> {
            KontenBerandaKaryawan(
              karyawan = karyawan,
              sapaanWaktu = sapaanWaktu,
              absensiHariIni = absensiHariIni,
              daftarRiwayat = daftarRiwayat,
              isDalamJaringanKantor = isDalamJaringanKantor,
              jarakDariKantorMeter = jarakDariKantorMeter,
              radiusMaksimalKantor = radiusMaksimalKantor,
              onToggleSimulasiJaringan = {
                jManager.toggleModeSimulasi()
              },
              onOlahDataAi = {
                isAiAnalisisOpen = true
                isAiLoading = true
                hasilAnalisisAi = null
                coroutineScope.launch {
                  val res = GeminiAiService.olahDataRiwayatKaryawan(karyawan, daftarRiwayat)
                  hasilAnalisisAi = res
                  isAiLoading = false
                }
              },
              onAbsenMasuk = {
                if (!isDalamJaringanKantor) return@KontenBerandaKaryawan
                val cal = Calendar.getInstance()
                val jamSekarangFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).format(cal.time)
                val jamInt = cal.get(Calendar.HOUR_OF_DAY)
                val menitInt = cal.get(Calendar.MINUTE)
                val isTerlambat = jamInt > 8 || (jamInt == 8 && menitInt > 0)
                val status = if (isTerlambat) StatusAbsensi.TERLAMBAT else StatusAbsensi.TEPAT_WAKTU

                val updatedAbsensi = absensiHariIni.copy(
                  jamMasuk = jamSekarangFormat,
                  status = status,
                  sudahAbsenMasuk = true
                )
                absensiHariIni = updatedAbsensi
                sessionManager.simpanAbsensiHariIni(updatedAbsensi)

                sessionManager.tambahAtauUpdateAbsensiKaryawan(
                  BarisAbsensiKaryawan(
                    idKaryawan = karyawan.id,
                    nama = karyawan.nama,
                    departemen = karyawan.departemen,
                    jamMasuk = jamSekarangFormat,
                    jamPulang = "-",
                    status = status
                  )
                )

                bannerPesanLokal = "Data Absen Masuk ($jamSekarangFormat) terkirim ke Komputer Kantor (Local)! Status: ${status.label}"
              },
              onAbsenPulang = {
                if (!isDalamJaringanKantor) return@KontenBerandaKaryawan
                val cal = Calendar.getInstance()
                val jamPulangFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).format(cal.time)
                val updatedAbsensi = absensiHariIni.copy(
                  jamPulang = jamPulangFormat,
                  sudahAbsenPulang = true
                )
                absensiHariIni = updatedAbsensi
                sessionManager.simpanAbsensiHariIni(updatedAbsensi)

                sessionManager.tambahAtauUpdateAbsensiKaryawan(
                  BarisAbsensiKaryawan(
                    idKaryawan = karyawan.id,
                    nama = karyawan.nama,
                    departemen = karyawan.departemen,
                    jamMasuk = absensiHariIni.jamMasuk ?: "08:00",
                    jamPulang = jamPulangFormat,
                    status = absensiHariIni.status ?: StatusAbsensi.TEPAT_WAKTU
                  )
                )

                val tanggalSingkat = SimpleDateFormat("EEEE, d MMM", Locale("id", "ID")).format(cal.time)
                val riwayatBaru = RiwayatAbsensiItem(
                  id = UUID.randomUUID().toString(),
                  hariTanggal = tanggalSingkat,
                  jamMasuk = absensiHariIni.jamMasuk ?: "08:00",
                  jamPulang = jamPulangFormat,
                  status = absensiHariIni.status ?: StatusAbsensi.TEPAT_WAKTU
                )
                val newRiwayat = listOf(riwayatBaru) + daftarRiwayat
                daftarRiwayat = newRiwayat
                sessionManager.simpanRiwayat(newRiwayat)

                bannerPesanLokal = "Data Absen Pulang ($jamPulangFormat) terkirim ke Komputer Kantor (Local)! Absensi hari ini selesai."
              },
              onDismissBanner = { bannerPesanLokal = null },
              bannerPesan = bannerPesanLokal
            )
          }
          TabKaryawan.RIWAYAT -> {
            KontenRiwayatKaryawan(
              daftarRiwayat = daftarRiwayat,
              onOlahDataAi = {
                isAiAnalisisOpen = true
                isAiLoading = true
                hasilAnalisisAi = null
                coroutineScope.launch {
                  val res = GeminiAiService.olahDataRiwayatKaryawan(karyawan, daftarRiwayat)
                  hasilAnalisisAi = res
                  isAiLoading = false
                }
              }
            )
          }
          TabKaryawan.IZIN -> {
            KontenPengajuanIzinKaryawan(
              karyawan = karyawan,
              isDalamJaringanKantor = isDalamJaringanKantor,
              onSubmitIzin = { jenis, alasan ->
                val cal = Calendar.getInstance()
                val tanggalSingkat = SimpleDateFormat("EEEE, d MMM", Locale("id", "ID")).format(cal.time)
                val itemIzin = RiwayatAbsensiItem(
                  id = UUID.randomUUID().toString(),
                  hariTanggal = tanggalSingkat,
                  jamMasuk = "-",
                  jamPulang = "-",
                  status = StatusAbsensi.IZIN,
                  catatan = "$jenis: $alasan"
                )
                val newRiwayat = listOf(itemIzin) + daftarRiwayat
                daftarRiwayat = newRiwayat
                sessionManager.simpanRiwayat(newRiwayat)

                sessionManager.tambahAtauUpdateAbsensiKaryawan(
                  BarisAbsensiKaryawan(
                    idKaryawan = karyawan.id,
                    nama = karyawan.nama,
                    departemen = karyawan.departemen,
                    jamMasuk = "-",
                    jamPulang = "-",
                    status = StatusAbsensi.IZIN
                  )
                )

                selectedTab = TabKaryawan.BERANDA
                bannerPesanLokal = "Pengajuan izin berhasil dicatat ke komputer kantor lokal!"
              }
            )
          }
          TabKaryawan.PROFIL -> {
            KontenProfilKaryawan(
              karyawan = karyawan,
              isDalamJaringanKantor = isDalamJaringanKantor,
              jarakDariKantorMeter = jarakDariKantorMeter,
              onLogout = onLogout,
              onResetApp = {
                sessionManager.hapusPendaftaran()
                sessionManager.resetAdminAbsensiHariIni()
                absensiHariIni = AbsensiHariIni()
                daftarRiwayat = emptyList()
                onResetApp()
              }
            )
          }
        }
      }
    }
  }

  // DIALOG AI OLAH DATA REKAP KEHADIRAN KARYAWAN
  if (isAiAnalisisOpen) {
    val scrollDialog = rememberScrollState()
    AlertDialog(
      containerColor = CardWhite,
      onDismissRequest = { isAiAnalisisOpen = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Analisis Rekap Presensi AI",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Text(
              text = "Gemini 3.1 Pro (High Thinking)",
              fontSize = 11.sp,
              color = TealPrimary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 440.dp)
            .verticalScroll(scrollDialog),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (isAiLoading) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              CircularProgressIndicator(color = TealPrimary)
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Sedang Menganalisis Pola Kehadiran...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
              )
            }
          } else {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = BgLight),
              border = BorderStroke(1.dp, BorderMuted),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = hasilAnalisisAi ?: "Belum ada hasil analisis.",
                fontSize = 13.sp,
                color = TextDark,
                lineHeight = 18.sp,
                modifier = Modifier.padding(14.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { isAiAnalisisOpen = false },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Tutup", color = Color.White)
        }
      }
    )
  }
}

// 
// SUB-KOMPOSABLE: BERANDA KARYAWAN
// 
@Composable
fun KontenBerandaKaryawan(
  karyawan: DataKaryawan,
  sapaanWaktu: String,
  absensiHariIni: AbsensiHariIni,
  daftarRiwayat: List<RiwayatAbsensiItem>,
  isDalamJaringanKantor: Boolean,
  jarakDariKantorMeter: Int,
  radiusMaksimalKantor: Int,
  onToggleSimulasiJaringan: () -> Unit,
  onOlahDataAi: () -> Unit,
  onAbsenMasuk: () -> Unit,
  onAbsenPulang: () -> Unit,
  onDismissBanner: () -> Unit,
  bannerPesan: String?
) {
  val totalHadir = remember(daftarRiwayat) { daftarRiwayat.count { it.status == StatusAbsensi.TEPAT_WAKTU } }
  val totalTerlambat = remember(daftarRiwayat) { daftarRiwayat.count { it.status == StatusAbsensi.TERLAMBAT } }
  val totalIzin = remember(daftarRiwayat) { daftarRiwayat.count { it.status == StatusAbsensi.IZIN } }
  var showDialogTerkunci by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header Profil & Sapaan
    item(key = "header_profil") {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = sapaanWaktu,
            fontSize = 14.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = karyawan.nama,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.testTag("karyawan_greeting_name")
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "ID: ${karyawan.id}",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TealPrimary
            )
            Text(
              text = " • ${karyawan.departemen}",
              fontSize = 12.sp,
              color = TextMuted
            )
          }
        }

        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(TealContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = karyawan.avatarInisial,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TealDark
          )
        }
      }
    }

    // 2. KARTU STATUS JARINGAN KANTOR & SIMULASI LOKASI
    item(key = "network_status_card") {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isDalamJaringanKantor) TealContainer.copy(alpha = 0.45f) else StatusRedBg
        ),
        border = BorderStroke(
          1.dp,
          if (isDalamJaringanKantor) TealPrimary.copy(alpha = 0.3f) else StatusRed.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("network_status_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isDalamJaringanKantor) TealPrimary else StatusRed),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isDalamJaringanKantor) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = if (isDalamJaringanKantor) "Terhubung Jaringan Kantor" else "Di Luar Jaringan Kantor",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDalamJaringanKantor) TealDark else StatusRed
              )
              Text(
                text = if (isDalamJaringanKantor) "Jarak: ${jarakDariKantorMeter}m • IP LAN Kantor" else "Jarak: ${jarakDariKantorMeter}m • Tidak Terjangkau",
                fontSize = 11.sp,
                color = TextDark
              )
            }
          }

          OutlinedButton(
            onClick = onToggleSimulasiJaringan,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = if (isDalamJaringanKantor) TealDark else StatusRed
            ),
            modifier = Modifier.testTag("toggle_network_simulation_button")
          ) {
            Text(
              text = if (isDalamJaringanKantor) "Tes: Jauh" else "Tes: Di Kantor",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Banner Peringatan Keras Jika Jauh Dari Kantor
    if (!isDalamJaringanKantor) {
      item(key = "out_of_range_warning") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StatusRedBg),
          border = BorderStroke(1.5.dp, StatusRed.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("out_of_range_warning_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(StatusRed),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOff,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Aplikasi Tidak Dapat Digunakan",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = StatusRed
                )
                Text(
                  text = "Perangkat berada di luar kantor (${jarakDariKantorMeter}m)",
                  fontSize = 12.sp,
                  color = TextDark
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Sesuai sistem absensi kantor, aplikasi HANYA dapat digunakan jika Anda terhubung langsung ke Wi-Fi / LAN kantor (radius maks $radiusMaksimalKantor meter). Seluruh fitur absensi dikunci.",
              fontSize = 12.sp,
              color = TextDark,
              lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = onToggleSimulasiJaringan,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Simulasi: Masuk Jaringan Kantor (Uji Buka Kunci)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
      }
    }

    // Banner Notifikasi Local Sukses
    if (bannerPesan != null) {
      item(key = "banner_pesan") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = StatusGreenBg),
          border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("karyawan_local_banner")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = StatusGreen,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Data Berhasil Disimpan ke Local",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = StatusGreen
              )
              Text(
                text = bannerPesan,
                fontSize = 12.sp,
                color = TextDark
              )
            }
            IconButton(onClick = onDismissBanner, modifier = Modifier.size(28.dp)) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Tutup",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }

    // 3. KARTU GRADIENT UTAMA
    item(key = "gradient_card") {
      KartuGradientAbsensi(
        absensiHariIni = absensiHariIni,
        isDalamJaringanKantor = isDalamJaringanKantor,
        jarakDariKantorMeter = jarakDariKantorMeter
      )
    }

    // 4. SATU TOMBOL ABSEN BESAR DINAMIS
    item(key = "tombol_absen_utama") {
      when {
        !isDalamJaringanKantor -> {
          Button(
            onClick = { showDialogTerkunci = true },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = StatusRedBg,
              contentColor = StatusRed
            ),
            border = BorderStroke(1.5.dp, StatusRed.copy(alpha = 0.5f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .testTag("button_absen_locked")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = StatusRed,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Aplikasi Terkunci (Di Luar Kantor)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StatusRed
              )
            }
          }
        }
        !absensiHariIni.sudahAbsenMasuk -> {
          Button(
            onClick = onAbsenMasuk,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .testTag("button_absen_masuk")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Login,
                contentDescription = "Absen Masuk",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Absen Masuk",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
        !absensiHariIni.sudahAbsenPulang -> {
          Button(
            onClick = onAbsenPulang,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .testTag("button_absen_pulang")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Absen Pulang",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Absen Pulang",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
        else -> {
          Button(
            onClick = { },
            enabled = false,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
              disabledContainerColor = Color(0xFFDDE6E8),
              disabledContentColor = TextMuted
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(60.dp)
              .testTag("button_absen_selesai")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selesai",
                tint = StatusGreen,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Absensi Hari Ini Selesai",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
              )
            }
          }
        }
      }
    }

    // 5. TIGA KARTU RINGKASAN & TOMBOL AI
    item(key = "ringkasan_metrics") {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          KartuRingkasanItem(
            judul = "Hadir",
            jumlah = "$totalHadir",
            warnaBadge = StatusGreen,
            warnaBg = StatusGreenBg,
            modifier = Modifier.weight(1f)
          )
          KartuRingkasanItem(
            judul = "Terlambat",
            jumlah = "$totalTerlambat",
            warnaBadge = StatusAmber,
            warnaBg = StatusAmberBg,
            modifier = Modifier.weight(1f)
          )
          KartuRingkasanItem(
            judul = "Izin",
            jumlah = "$totalIzin",
            warnaBadge = StatusBlue,
            warnaBg = StatusBlueBg,
            modifier = Modifier.weight(1f)
          )
        }

        // Tombol Olah Data Pribadi AI
        OutlinedButton(
          onClick = onOlahDataAi,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TealDark),
          border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Olah Data & Evaluasi Kehadiran Pribadi (AI Thinking)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 6. RIWAYAT TERBARU
    item(key = "riwayat_title") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Riwayat Absensi Terbaru",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )
        Text(
          text = "${daftarRiwayat.size} Catatan",
          fontSize = 12.sp,
          color = TextMuted
        )
      }
    }

    if (daftarRiwayat.isEmpty()) {
      item(key = "riwayat_empty_card") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardWhite),
          border = BorderStroke(1.dp, BorderMuted),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(TealContainer.copy(alpha = 0.5f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Belum Ada Riwayat Absensi",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Riwayat presensi Anda akan otomatis tercatat di sini setelah Anda melakukan Absen Masuk & Pulang di jaringan kantor.",
              fontSize = 12.sp,
              color = TextMuted,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )
          }
        }
      }
    } else {
      items(items = daftarRiwayat, key = { it.id }) { item ->
        RiwayatCardRow(item = item)
      }
    }

    item(key = "bottom_spacer") {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showDialogTerkunci) {
    AlertDialog(
      onDismissRequest = { showDialogTerkunci = false },
      icon = {
        Icon(
          imageVector = Icons.Default.LocationOff,
          contentDescription = null,
          tint = StatusRed,
          modifier = Modifier.size(36.dp)
        )
      },
      title = {
        Text(
          text = "Aplikasi Terkunci (Di Luar Kantor)",
          fontWeight = FontWeight.Bold,
          color = TextDark,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Anda terdeteksi berada di luar area kantor (jarak ${jarakDariKantorMeter}m dari kantor, batas toleransi maksimal $radiusMaksimalKantor meter).",
            fontSize = 13.sp,
            color = TextDark
          )
          Text(
            text = "Sesuai sistem absensi kantor lokal, absensi masuk dan pulang HANYA dapat dilakukan jika perangkat terhubung ke Wi-Fi / jaringan kantor.",
            fontSize = 12.sp,
            color = TextMuted
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showDialogTerkunci = false
            onToggleSimulasiJaringan()
          },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Uji Simulasi: Tiba di Kantor")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDialogTerkunci = false }) {
          Text("Tutup", color = TextMuted)
        }
      }
    )
  }
}

// 
// KOMPONEN UI: KARTU GRADIENT
// 
@Composable
fun KartuGradientAbsensi(
  absensiHariIni: AbsensiHariIni,
  isDalamJaringanKantor: Boolean,
  jarakDariKantorMeter: Int
) {
  var jamDigitalText by remember { mutableStateOf("") }
  var tanggalText by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    val formatJam = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("id-ID"))
    val formatTanggal = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))
    while (true) {
      val now = Calendar.getInstance().time
      jamDigitalText = formatJam.format(now) + " WIB"
      tanggalText = formatTanggal.format(now)
      delay(1000)
    }
  }

  val gradientBrush = remember {
    Brush.linearGradient(colors = listOf(TealDark, TealLight))
  }

  Card(
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("gradient_attendance_card")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(gradientBrush)
        .padding(20.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (isDalamJaringanKantor) Color.White.copy(alpha = 0.18f) else Color(0xFFC0392B).copy(alpha = 0.7f)
              )
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isDalamJaringanKantor) Color(0xFF4ADE80) else Color(0xFFFF6B6B))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isDalamJaringanKantor) "Jaringan Kantor (${jarakDariKantorMeter}m)" else "Di Luar Jaringan (${jarakDariKantorMeter}m)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = Color.White
            )
          }

          Text(
            text = if (isDalamJaringanKantor) "Batas: 100m • 08:00" else "Status: Terkunci",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Jam Digital Berjalan
        Text(
          text = if (jamDigitalText.isNotEmpty()) jamDigitalText else "--:--:-- WIB",
          fontSize = 30.sp,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          letterSpacing = 1.sp,
          modifier = Modifier.testTag("digital_clock_text")
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = if (tanggalText.isNotEmpty()) tanggalText else "Hari ini",
          fontSize = 13.sp,
          color = Color.White.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          KotakStatusAbsenHeader(
            judul = "Masuk",
            waktu = absensiHariIni.jamMasuk?.let { "$it WIB" } ?: "--:--",
            keterangan = when (absensiHariIni.status) {
              StatusAbsensi.TEPAT_WAKTU -> "Tepat Waktu"
              StatusAbsensi.TERLAMBAT -> "Terlambat"
              else -> if (absensiHariIni.sudahAbsenMasuk) "Tercatat" else "Belum Absen"
            },
            warnaKeterangan = when (absensiHariIni.status) {
              StatusAbsensi.TEPAT_WAKTU -> Color(0xFF6EE7B7)
              StatusAbsensi.TERLAMBAT -> Color(0xFFFDE68A)
              else -> Color.White.copy(alpha = 0.7f)
            },
            modifier = Modifier.weight(1f)
          )

          KotakStatusAbsenHeader(
            judul = "Pulang",
            waktu = absensiHariIni.jamPulang?.let { "$it WIB" } ?: "--:--",
            keterangan = if (absensiHariIni.sudahAbsenPulang) "Selesai" else "Belum Pulang",
            warnaKeterangan = if (absensiHariIni.sudahAbsenPulang) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
fun KotakStatusAbsenHeader(
  judul: String,
  waktu: String,
  keterangan: String,
  warnaKeterangan: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(Color.White.copy(alpha = 0.14f))
      .padding(12.dp)
  ) {
    Column {
      Text(
        text = judul,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.85f)
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = waktu,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = keterangan,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = warnaKeterangan
      )
    }
  }
}

@Composable
fun KartuRingkasanItem(
  judul: String,
  jumlah: String,
  warnaBadge: Color,
  warnaBg: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = BorderStroke(1.dp, BorderMuted),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(warnaBg),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = jumlah,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = warnaBadge,
          textAlign = TextAlign.Center
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = judul,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextDark,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun RiwayatCardRow(item: RiwayatAbsensiItem) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = BorderStroke(1.dp, BorderMuted),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = item.hariTanggal,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${item.jamMasuk} - ${item.jamPulang}",
            fontSize = 12.sp,
            color = TextMuted
          )
          if (item.catatan.isNotEmpty()) {
            Text(
              text = " • ${item.catatan}",
              fontSize = 11.sp,
              color = TextMuted
            )
          }
        }
      }

      StatusPillBadge(status = item.status)
    }
  }
}

@Composable
fun StatusPillBadge(status: StatusAbsensi) {
  val (bgColor, textColor, label) = when (status) {
    StatusAbsensi.TEPAT_WAKTU -> Triple(StatusGreenBg, StatusGreen, "Tepat Waktu")
    StatusAbsensi.TERLAMBAT -> Triple(StatusAmberBg, StatusAmber, "Terlambat")
    StatusAbsensi.IZIN -> Triple(StatusBlueBg, StatusBlue, "Izin")
    StatusAbsensi.BELUM_ABSEN -> Triple(BgLight, TextMuted, "Belum Absen")
  }

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(bgColor)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = textColor
    )
  }
}

//
// SUB-KOMPOSABLE: TAB RIWAYAT LENGKAP
// 
@Composable
fun KontenRiwayatKaryawan(
  daftarRiwayat: List<RiwayatAbsensiItem>,
  onOlahDataAi: () -> Unit = {}
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item(key = "riwayat_header") {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Riwayat Kehadiran Lengkap",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Data disimpan di komputer kantor lokal",
            fontSize = 13.sp,
            color = TextMuted
          )
        }
        if (daftarRiwayat.isNotEmpty()) {
          IconButton(onClick = onOlahDataAi) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = "Analisis AI",
              tint = TealPrimary
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (daftarRiwayat.isEmpty()) {
      item(key = "riwayat_tab_empty") {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = CardWhite),
          border = BorderStroke(1.dp, BorderMuted),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(TealContainer.copy(alpha = 0.5f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Belum Ada Catatan Kehadiran",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Lakukan Absen Masuk & Pulang di Beranda untuk mulai mencatat riwayat presensi harian.",
              fontSize = 12.sp,
              color = TextMuted,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )
          }
        }
      }
    } else {
      items(items = daftarRiwayat, key = { it.id }) { item ->
        RiwayatCardRow(item = item)
      }
    }

    item(key = "riwayat_footer") {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

// 
// SUB-KOMPOSABLE: TAB PENGAJUAN IZIN (DENGAN DRAFTER AI)
// 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KontenPengajuanIzinKaryawan(
  karyawan: DataKaryawan,
  isDalamJaringanKantor: Boolean,
  onSubmitIzin: (String, String) -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  var jenisIzin by remember { mutableStateOf("Sakit") }
  var alasanText by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var isDraftingAi by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Formulir Pengajuan Izin",
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = TextDark
    )
    Text(
      text = "Kirim izin langsung ke komputer kantor untuk diverifikasi admin",
      fontSize = 13.sp,
      color = TextMuted
    )

    if (!isDalamJaringanKantor) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StatusRedBg),
        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            tint = StatusRed,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Pengajuan Izin Dikunci (Di Luar Kantor)",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = StatusRed
            )
            Text(
              text = "Perangkat harus terhubung ke Wi-Fi kantor untuk mengirim data izin ke komputer kantor lokal.",
              fontSize = 11.sp,
              color = TextDark,
              lineHeight = 15.sp
            )
          }
        }
      }
    }

    Text(
      text = "Jenis Izin:",
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold,
      color = TextDark
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("Sakit", "Cuti", "Keperluan Keluarga").forEach { pilihan ->
        val isSelected = jenisIzin == pilihan
        FilterChip(
          selected = isSelected,
          onClick = { jenisIzin = pilihan },
          label = { Text(pilihan) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TealContainer,
            selectedLabelColor = TealDark
          )
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Keterangan / Alasan:",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextDark
      )

      // Tombol AI untuk membuat draf formal otomatis
      TextButton(
        onClick = {
          isDraftingAi = true
          coroutineScope.launch {
            val draf = GeminiAiService.buatDrafSuratIzin(
              karyawan,
              jenisIzin,
              if (alasanText.isNotBlank()) alasanText else "Sedang dalam kondisi kesehatan kurang baik dan memerlukan istirahat dokter"
            )
            alasanText = draf
            isDraftingAi = false
          }
        },
        enabled = !isDraftingAi
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isDraftingAi) "Merancang..." else "Draf Resmi AI",
          fontSize = 11.sp,
          color = TealPrimary,
          fontWeight = FontWeight.Bold
        )
      }
    }

    OutlinedTextField(
      value = alasanText,
      onValueChange = {
        alasanText = it
        errorText = null
      },
      placeholder = { Text("Contoh: Demam, berobat ke dokter...") },
      minLines = 4,
      shape = RoundedCornerShape(16.dp),
      colors = absensiTextFieldColors(),
      modifier = Modifier.fillMaxWidth()
    )

    if (errorText != null) {
      Text(text = errorText ?: "", color = StatusRed, fontSize = 12.sp)
    }

    Spacer(modifier = Modifier.weight(1f))

    Button(
      onClick = {
        if (!isDalamJaringanKantor) {
          errorText = "Gagal kirim: Perangkat Anda sedang berada di luar jaringan kantor!"
          return@Button
        }
        if (alasanText.trim().isEmpty()) {
          errorText = "Mohon isi alasan pengajuan izin!"
        } else {
          onSubmitIzin(jenisIzin, alasanText.trim())
        }
      },
      enabled = isDalamJaringanKantor,
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = TealPrimary,
        disabledContainerColor = StatusRedBg,
        disabledContentColor = StatusRed
      ),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    ) {
      Text(
        text = if (isDalamJaringanKantor) "Kirimkan ke Komputer Kantor" else "Terkunci (Di Luar Kantor)",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDalamJaringanKantor) Color.White else StatusRed
      )
    }
  }
}

// 
// SUB-KOMPOSABLE: TAB PROFIL KARYAWAN
// 
@Composable
fun KontenProfilKaryawan(
  karyawan: DataKaryawan,
  isDalamJaringanKantor: Boolean,
  jarakDariKantorMeter: Int,
  onLogout: () -> Unit,
  onResetApp: () -> Unit
) {
  var showResetConfirm by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(TealContainer),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = karyawan.avatarInisial,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = TealDark
      )
    }

    Text(
      text = karyawan.nama,
      fontSize = 22.sp,
      fontWeight = FontWeight.Bold,
      color = TextDark
    )

    Text(
      text = "Karyawan Tetap • ${karyawan.departemen}",
      fontSize = 13.sp,
      color = TextMuted
    )

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = CardWhite),
      border = BorderStroke(1.dp, BorderMuted),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        BarisInfoProfil("ID Pegawai", karyawan.id)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderMuted)
        BarisInfoProfil("NIP", karyawan.nip)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderMuted)
        BarisInfoProfil("Jam Kerja Kantor", "08:00 - 17:00 WIB")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderMuted)
        BarisInfoProfil("Status Jaringan", if (isDalamJaringanKantor) "Terhubung ke LAN Kantor ($jarakDariKantorMeter m)" else "Di Luar Jangkauan ($jarakDariKantorMeter m)")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderMuted)
        BarisInfoProfil("Lokasi Server", "Local Office Network (LAN / Subnet Kantor)")
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = BorderMuted)
        BarisInfoProfil("AI Engine", "Gemini 3.1 Pro (High Thinking)")
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    OutlinedButton(
      onClick = onLogout,
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("karyawan_logout_button")
    ) {
      Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Ganti Peran / Keluar")
    }

    TextButton(
      onClick = { showResetConfirm = true },
      modifier = Modifier.testTag("reset_data_button")
    ) {
      Text(
        text = "Reset Data (Ulangi Penggunaan Pertama Kali)",
        color = StatusRed,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("Reset Data Aplikasi?", fontWeight = FontWeight.Bold) },
      text = { Text("Aplikasi akan dikembalikan ke kondisi awal (belum ada akun terdaftar dan riwayat kosong).") },
      confirmButton = {
        Button(
          onClick = {
            showResetConfirm = false
            onResetApp()
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
        ) {
          Text("Ya, Reset ke Awal", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) {
          Text("Batal")
        }
      }
    )
  }
}

@Composable
fun BarisInfoProfil(label: String, nilai: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, fontSize = 13.sp, color = TextMuted)
    Text(text = nilai, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
  }
}
