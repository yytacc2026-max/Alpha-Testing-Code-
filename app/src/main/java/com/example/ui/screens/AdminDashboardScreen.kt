package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

// =========================================================================
// 5. DASHBOARD ADMIN (LENGKAP DENGAN FITUR OLAH DATA OTOMATIS AI THINKING)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  onLogout: () -> Unit
) {
  val context = LocalContext.current
  val sessionManager = remember { SessionManager(context) }
  val coroutineScope = rememberCoroutineScope()

  var configAdmin by remember { mutableStateOf(sessionManager.getKonfigurasiAdmin()) }
  var isEditModeOpen by remember { mutableStateOf(false) }
  var hasBeenEdited by remember { mutableStateOf(false) }
  var pesanLokalAdmin by remember { mutableStateOf<String?>(null) }

  // State untuk Dialog Olah Data Otomatis AI (High Thinking Mode)
  var isAiDialogVisible by remember { mutableStateOf(false) }
  var isAiProcessing by remember { mutableStateOf(false) }
  var hasilAnalisisAi by remember { mutableStateOf<String?>(null) }
  var kueriKhususAdmin by remember { mutableStateOf("") }

  // Daftar absensi yang masuk hari ini (dimulai dari 0/kosong)
  var daftarAbsensi by remember {
    mutableStateOf(sessionManager.getAdminDaftarAbsensi())
  }

  // Menghitung metrik kehadiran secara real-time & akurat dari data absensi yang masuk
  val totalHadir = remember(daftarAbsensi) {
    daftarAbsensi.count { it.status == StatusAbsensi.TEPAT_WAKTU || it.status == StatusAbsensi.TERLAMBAT }
  }
  val totalTerlambat = remember(daftarAbsensi) {
    daftarAbsensi.count { it.status == StatusAbsensi.TERLAMBAT }
  }

  // Total karyawan dihitung akurat dari konfigurasi atau data absensi yang masuk
  val totalKaryawanTampil = remember(configAdmin.totalKaryawan, daftarAbsensi) {
    if (configAdmin.totalKaryawan > 0) {
      configAdmin.totalKaryawan
    } else {
      daftarAbsensi.size
    }
  }

  val totalBelumAbsen = remember(totalKaryawanTampil, totalHadir) {
    maxOf(0, totalKaryawanTampil - totalHadir)
  }

  Scaffold(
    containerColor = BgLight,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Dashboard Admin",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Text(
              text = if (hasBeenEdited) "Versi Setelah Diedit (Local)" else "Versi Standar",
              fontSize = 11.sp,
              color = if (hasBeenEdited) StatusGreen else TextMuted,
              fontWeight = FontWeight.SemiBold
            )
          }
        },
        actions = {
          if (daftarAbsensi.isNotEmpty()) {
            IconButton(
              onClick = {
                sessionManager.resetAdminAbsensiHariIni()
                daftarAbsensi = emptyList()
                pesanLokalAdmin = "Semua data absensi hari ini telah di-reset ke 0."
              },
              modifier = Modifier.testTag("admin_reset_data_button")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Data ke 0",
                tint = TealPrimary
              )
            }
          }
          IconButton(onClick = onLogout, modifier = Modifier.testTag("admin_logout_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Keluar",
              tint = StatusRed
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Banner Notifikasi Local jika data baru saja diedit
        if (pesanLokalAdmin != null) {
          item(key = "admin_banner") {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = StatusGreenBg),
              border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
              elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_local_banner")
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
                    text = "Data berubah di local (komputer kantor)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                  )
                  Text(
                    text = pesanLokalAdmin ?: "",
                    fontSize = 12.sp,
                    color = TextDark
                  )
                }
                IconButton(onClick = { pesanLokalAdmin = null }) {
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

        // 1. KARTU RINGKASAN ADMIN (4 KARTU)
        item(key = "admin_metrics") {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              AdminMetricCard(
                judul = "Total Karyawan",
                nilai = "$totalKaryawanTampil",
                icon = Icons.Default.Groups,
                warna = TealPrimary,
                warnaBg = TealContainer,
                modifier = Modifier.weight(1f)
              )
              AdminMetricCard(
                judul = "Hadir",
                nilai = "$totalHadir",
                icon = Icons.Default.CheckCircle,
                warna = StatusGreen,
                warnaBg = StatusGreenBg,
                modifier = Modifier.weight(1f)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              AdminMetricCard(
                judul = "Terlambat",
                nilai = "$totalTerlambat",
                icon = Icons.Default.Warning,
                warna = StatusAmber,
                warnaBg = StatusAmberBg,
                modifier = Modifier.weight(1f)
              )
              AdminMetricCard(
                judul = "Belum Absen",
                nilai = "$totalBelumAbsen",
                icon = Icons.Default.AccessTime,
                warna = StatusRed,
                warnaBg = StatusRedBg,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }

        // 2. KARTU OLAH DATA OTOMATIS & ANALISIS AI (THINKING MODE)
        item(key = "ai_olah_data_card") {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.5.dp, TealPrimary.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_ai_thinking_card")
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TealContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "Olah Data Otomatis",
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = TealPrimary,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "AI Thinking",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "Membaca & menganalisis seluruh data absensi dengan penalaran tinggi (Gemini 3.1 Pro)",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              Button(
                onClick = {
                  isAiDialogVisible = true
                  isAiProcessing = true
                  hasilAnalisisAi = null
                  coroutineScope.launch {
                    val hasil = GeminiAiService.olahDataAbsensiAdmin(daftarAbsensi, configAdmin, null)
                    hasilAnalisisAi = hasil
                    isAiProcessing = false
                  }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_run_ai_analysis_button")
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Mulai Analisis & Olah Data Otomatis",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }
        }

        // 3. KARTU PENGUMUMAN & JAM KERJA
        item(key = "admin_config_card") {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, BorderMuted),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_config_card")
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Pengaturan Jam Kerja Kantor",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextDark
                )
                if (hasBeenEdited) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(TealContainer)
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = "Diedit",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = TealDark
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(text = "Jam Masuk", fontSize = 11.sp, color = TextMuted)
                  Text(text = configAdmin.jamMasukKerja, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
                Column {
                  Text(text = "Batas Terlambat", fontSize = 11.sp, color = TextMuted)
                  Text(text = configAdmin.batasTerlambat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                }
                Column {
                  Text(text = "Jam Pulang", fontSize = 11.sp, color = TextMuted)
                  Text(text = configAdmin.jamPulangKerja, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              HorizontalDivider(color = BorderMuted)
              Spacer(modifier = Modifier.height(10.dp))
              Text(text = "Pesan Pengumuman Kantor:", fontSize = 11.sp, color = TextMuted)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = configAdmin.pengumuman,
                fontSize = 13.sp,
                color = TextDark,
                lineHeight = 18.sp
              )
            }
          }
        }

        // 4. TOMBOL AKSI: "EDIT DASHBOARD"
        item(key = "admin_edit_button") {
          Button(
            onClick = { isEditModeOpen = true },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("button_edit_dashboard")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Dashboard",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Edit Dashboard (Ubah Data Kantor)",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        // 5. HEADER TABEL ABSENSI
        item(key = "admin_table_header") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Daftar Absensi Karyawan Hari Ini",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Text(
              text = "${daftarAbsensi.size} Data Masuk",
              fontSize = 12.sp,
              color = if (daftarAbsensi.isEmpty()) StatusAmber else TealPrimary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        if (daftarAbsensi.isEmpty()) {
          item(key = "admin_empty_table") {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = CardWhite),
              border = BorderStroke(1.dp, BorderMuted),
              elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_empty_table_card")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 32.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(TealContainer.copy(alpha = 0.5f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(26.dp)
                  )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Belum Ada Data Absensi Masuk (0 Data)",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Seluruh data absensi dimulai dari 0 agar pencatatan akurat. Data kehadiran akan otomatis masuk saat karyawan melakukan Absen Masuk di jaringan kantor.",
                  fontSize = 12.sp,
                  color = TextMuted,
                  textAlign = TextAlign.Center,
                  lineHeight = 16.sp
                )
              }
            }
          }
        } else {
          // BARIS TABEL DENGAN STABLE KEY UNTUK SCROLLING TANPA JANK
          items(items = daftarAbsensi, key = { it.idKaryawan }) { row ->
            BarisTabelAbsensiCard(row = row)
          }
        }

        item(key = "admin_bottom_spacer") {
          Spacer(modifier = Modifier.height(20.dp))
        }
      }
    }
  }

  // DIALOG OLAH DATA OTOMATIS & PENALARAN TINGKAT TINGGI (GEMINI 3.1 PRO)
  if (isAiDialogVisible) {
    val scrollDialog = rememberScrollState()
    AlertDialog(
      containerColor = CardWhite,
      onDismissRequest = { isAiDialogVisible = false },
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
              text = "Hasil Olah Data Otomatis",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = TextDark
            )
            Text(
              text = "Model: Gemini 3.1 Pro (High Thinking)",
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
            .heightIn(max = 480.dp)
            .verticalScroll(scrollDialog),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (isAiProcessing) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              CircularProgressIndicator(color = TealPrimary)
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Sedang Membaca & Mengolah Data...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
              )
              Text(
                text = "Melakukan penalaran mendalam (High Thinking Mode) atas catatan presensi kantor.",
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
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
                lineHeight = 19.sp,
                modifier = Modifier
                  .padding(14.dp)
                  .testTag("admin_ai_result_text")
              )
            }

            // Kolom Pertanyaan Lanjutan Khusus Admin
            Text(
              text = "Tanya / Analisis Khusus:",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextDark
            )
            OutlinedTextField(
              value = kueriKhususAdmin,
              onValueChange = { kueriKhususAdmin = it },
              placeholder = { Text("Contoh: Berikan saran penertiban jam masuk...") },
              singleLine = false,
              maxLines = 3,
              shape = RoundedCornerShape(12.dp),
              colors = absensiTextFieldColors(),
              modifier = Modifier.fillMaxWidth()
            )

            Button(
              onClick = {
                val query = kueriKhususAdmin.trim()
                if (query.isNotEmpty()) {
                  isAiProcessing = true
                  coroutineScope.launch {
                    val hasil = GeminiAiService.olahDataAbsensiAdmin(daftarAbsensi, configAdmin, query)
                    hasilAnalisisAi = hasil
                    kueriKhususAdmin = ""
                    isAiProcessing = false
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Kirim Kueri ke Gemini 3.1 Pro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { isAiDialogVisible = false },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Selesai", color = Color.White)
        }
      }
    )
  }

  // DIALOG EDIT DASHBOARD
  if (isEditModeOpen) {
    EditDashboardDialog(
      configSaatIni = configAdmin,
      onSimpan = { configBaru ->
        configAdmin = configBaru
        sessionManager.simpanKonfigurasiAdmin(configBaru)
        hasBeenEdited = true
        pesanLokalAdmin = "Pengaturan dashboard berhasil diperbarui dan disimpan ke komputer lokal kantor."
        isEditModeOpen = false
      },
      onBatal = {
        isEditModeOpen = false
      }
    )
  }
}

// =========================================================================
// KOMPONEN UI: KARTU METRIK ADMIN (OPTIMIZED FLAT)
// =========================================================================
@Composable
fun AdminMetricCard(
  judul: String,
  nilai: String,
  icon: ImageVector,
  warna: Color,
  warnaBg: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = BorderStroke(1.dp, BorderMuted),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(warnaBg),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = warna,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = nilai,
          fontSize = 19.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextDark
        )
        Text(
          text = judul,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = TextMuted
        )
      }
    }
  }
}

// =========================================================================
// KOMPONEN UI: BARIS TABEL ABSENSI (OPTIMIZED SCROLL)
// =========================================================================
@Composable
fun BarisTabelAbsensiCard(row: BarisAbsensiKaryawan) {
  val inisial = remember(row.nama) {
    row.nama.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
  }

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
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(TealContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = inisial,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TealDark
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = row.nama,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
          )
          Text(
            text = "${row.idKaryawan} • ${row.departemen}",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        StatusPillBadge(status = row.status)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Masuk: ${row.jamMasuk} | Pulang: ${row.jamPulang}",
          fontSize = 11.sp,
          color = TextMuted
        )
      }
    }
  }
}

// =========================================================================
// DIALOG: EDIT DASHBOARD ADMIN
// =========================================================================
@Composable
fun EditDashboardDialog(
  configSaatIni: KonfigurasiAdminDashboard,
  onSimpan: (KonfigurasiAdminDashboard) -> Unit,
  onBatal: () -> Unit
) {
  var jamMasuk by remember { mutableStateOf(configSaatIni.jamMasukKerja) }
  var batasTerlambat by remember { mutableStateOf(configSaatIni.batasTerlambat) }
  var jamPulang by remember { mutableStateOf(configSaatIni.jamPulangKerja) }
  var pengumuman by remember { mutableStateOf(configSaatIni.pengumuman) }
  var totalKaryawan by remember { mutableStateOf(configSaatIni.totalKaryawan.toString()) }
  val scrollState = rememberScrollState()

  AlertDialog(
    containerColor = CardWhite,
    onDismissRequest = onBatal,
    title = {
      Text(
        text = "Edit Dashboard Admin",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Perubahan akan disimpan langsung ke komputer kantor (Local).",
          fontSize = 12.sp,
          color = TextMuted
        )

        OutlinedTextField(
          value = jamMasuk,
          onValueChange = { jamMasuk = it },
          label = { Text("Jam Masuk Kerja (WIB)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = batasTerlambat,
          onValueChange = { batasTerlambat = it },
          label = { Text("Batas Toleransi Terlambat") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = jamPulang,
          onValueChange = { jamPulang = it },
          label = { Text("Jam Pulang Kerja (WIB)") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = totalKaryawan,
          onValueChange = { totalKaryawan = it },
          label = { Text("Total Karyawan Terdaftar") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = pengumuman,
          onValueChange = { pengumuman = it },
          label = { Text("Pengumuman Kantor") },
          minLines = 2,
          shape = RoundedCornerShape(12.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val baru = configSaatIni.copy(
            jamMasukKerja = jamMasuk.trim(),
            batasTerlambat = batasTerlambat.trim(),
            jamPulangKerja = jamPulang.trim(),
            pengumuman = pengumuman.trim(),
            totalKaryawan = totalKaryawan.toIntOrNull() ?: configSaatIni.totalKaryawan
          )
          onSimpan(baru)
        },
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
      ) {
        Text("Simpan (Data Berubah di Local)", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onBatal) {
        Text("Batal", color = TextMuted)
      }
    }
  )
}
