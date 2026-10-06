package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DataKaryawan
import com.example.model.DummyDataKantor
import com.example.model.JaringanKantorManager
import com.example.model.SessionManager
import com.example.ui.theme.*

// =========================================================================
// 3. HALAMAN REGISTER KARYAWAN (DILENGKAPI FITUR ANTI-KECURANGAN)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaryawanRegisterScreen(
  onRegisterSuccess: (DataKaryawan) -> Unit,
  onBack: () -> Unit,
  jaringanManager: JaringanKantorManager? = null
) {
  val context = LocalContext.current
  val sessionManager = remember { SessionManager(context) }
  val karyawanSudahTerdaftar = remember { sessionManager.getKaryawanTerdaftar() }
  var inputTerakhir by remember { mutableStateOf(sessionManager.getInputTerakhir()) }

  // Fitur isi otomatis: nama/NIP terakhir yang ditulis di perangkat ini
  var nipInput by remember { mutableStateOf(inputTerakhir?.nip ?: "") }
  var namaInput by remember { mutableStateOf(inputTerakhir?.nama ?: "") }
  var departemenInput by remember { mutableStateOf(inputTerakhir?.departemen ?: "") }

  // Status feedback
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scrollState = rememberScrollState()

  Scaffold(
    containerColor = BgLight,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (karyawanSudahTerdaftar != null) "Perangkat Terikat" else "Registrasi Karyawan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("register_back_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Kembali ke Pilih Peran",
              tint = TextDark
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
        .padding(paddingValues)
        .imePadding(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 500.dp)
          .verticalScroll(scrollState)
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // JIKA PERANGKAT SUDAH TERDAFTAR: BLOKIR PENDAFTARAN LAIN (ANTI-DUPLIKASI & KECURANGAN)
        if (karyawanSudahTerdaftar != null) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(TealContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Terkunci",
              tint = TealDark,
              modifier = Modifier.size(42.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Perangkat Sudah Terdaftar",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Untuk mencegah kecurangan absensi (titip absen) dan duplikasi, 1 perangkat smartphone hanya diperuntukkan bagi 1 karyawan resmi.",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(20.dp))
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "Akun yang Terdaftar di HP Ini:",
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = karyawanSudahTerdaftar.nama,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
              )
              Text(
                text = "NIP: ${karyawanSudahTerdaftar.nip} • ${karyawanSudahTerdaftar.departemen}",
                fontSize = 13.sp,
                color = TealDark,
                fontWeight = FontWeight.Medium
              )
            }
          }
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { onRegisterSuccess(karyawanSudahTerdaftar) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("open_my_dashboard_button")
          ) {
            Text(
              text = "Buka Dashboard Saya",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
          ) {
            Text(text = "Kembali ke Beranda", color = TextDark)
          }
          return@Column
        }

        // Header Icon
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(TealContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Badge,
            contentDescription = "Ikon Registrasi",
            tint = TealPrimary,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Pendaftaran Karyawan Baru",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Verifikasi NIP dengan data komputer kantor lokal",
          fontSize = 13.sp,
          color = TextMuted
        )

        if (jaringanManager != null) {
          Spacer(modifier = Modifier.height(14.dp))
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (jaringanManager.isDalamJaringanKantor) TealContainer.copy(alpha = 0.5f) else StatusRedBg
            ),
            border = BorderStroke(
              1.dp,
              if (jaringanManager.isDalamJaringanKantor) TealPrimary.copy(alpha = 0.3f) else StatusRed.copy(alpha = 0.4f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("register_network_status_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (jaringanManager.isDalamJaringanKantor) Icons.Default.Wifi else Icons.Default.WifiOff,
                  contentDescription = null,
                  tint = if (jaringanManager.isDalamJaringanKantor) TealDark else StatusRed,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = if (jaringanManager.isDalamJaringanKantor) "Terhubung Jaringan Kantor" else "Di Luar Jaringan Kantor",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (jaringanManager.isDalamJaringanKantor) TealDark else StatusRed
                  )
                  Text(
                    text = if (jaringanManager.isDalamJaringanKantor) "Jarak: ${jaringanManager.jarakDariKantorMeter}m • Siap Verifikasi" else "Jarak: ${jaringanManager.jarakDariKantorMeter}m • Registrasi Terkunci",
                    fontSize = 11.sp,
                    color = TextDark
                  )
                }
              }

              OutlinedButton(
                onClick = {
                  jaringanManager.toggleModeSimulasi()
                  errorMessage = null
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = if (jaringanManager.isDalamJaringanKantor) "Uji: Jauh" else "Uji: Di Kantor",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (jaringanManager.isDalamJaringanKantor) TealDark else StatusRed
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pesan Error jika Register tidak sesuai
        if (errorMessage != null) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StatusRedBg),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("register_error_card")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Register tidak sesuai",
                tint = StatusRed,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = errorMessage ?: "",
                fontSize = 13.sp,
                color = StatusRed,
                fontWeight = FontWeight.Medium
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        // Input NIP Karyawan
        OutlinedTextField(
          value = nipInput,
          onValueChange = {
            nipInput = it
            errorMessage = null
          },
          label = { Text("Nomor Induk Pegawai (NIP)") },
          placeholder = { Text("Contoh: 19940115001") },
          leadingIcon = {
            Icon(Icons.Default.Badge, contentDescription = null, tint = TealPrimary)
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_nip_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Nama Lengkap
        OutlinedTextField(
          value = namaInput,
          onValueChange = {
            namaInput = it
            errorMessage = null
          },
          label = { Text("Nama Lengkap Karyawan") },
          placeholder = { Text("Contoh: Ahmad Fauzi") },
          leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary)
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_nama_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Departemen
        OutlinedTextField(
          value = departemenInput,
          onValueChange = {
            departemenInput = it
            errorMessage = null
          },
          label = { Text("Departemen / Divisi") },
          placeholder = { Text("Contoh: Teknologi") },
          leadingIcon = {
            Icon(Icons.Default.Business, contentDescription = null, tint = TealPrimary)
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("register_departemen_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Fitur Isi Otomatis berdasarkan nama terakhir yang ditulis di perangkat masing-masing
        val lastInput = inputTerakhir
        if (lastInput != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Terakhir di HP ini: ${lastInput.nama.ifBlank { lastInput.nip }}",
              fontSize = 11.sp,
              color = TextMuted,
              modifier = Modifier.weight(1f)
            )
            Row {
              TextButton(
                onClick = {
                  nipInput = ""
                  namaInput = ""
                  departemenInput = ""
                  errorMessage = null
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "Kosongkan",
                  fontSize = 11.sp,
                  color = TextMuted
                )
              }
              TextButton(
                onClick = {
                  nipInput = lastInput.nip
                  if (lastInput.nama.isNotBlank()) namaInput = lastInput.nama
                  if (lastInput.departemen.isNotBlank()) departemenInput = lastInput.departemen
                  errorMessage = null
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.testTag("register_autofill_last_user")
              ) {
                Text(
                  text = "Isi Otomatis",
                  fontSize = 11.sp,
                  color = TealPrimary,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tombol Submit Register
        Button(
          onClick = {
            // Validasi keamanan: Registrasi hanya bisa di jaringan komputer kantor lokal
            if (jaringanManager != null && !jaringanManager.isDalamJaringanKantor) {
              errorMessage = "Pendaftaran Ditolak: Anda berada di luar jaringan kantor (${jaringanManager.jarakDariKantorMeter}m). Komputer kantor lokal tidak dapat dijangkau. Mohon hubungkan ke Wi-Fi kantor."
              return@Button
            }

            val cleanNip = nipInput.trim()
            if (cleanNip.isEmpty()) {
              errorMessage = "Silakan masukkan NIP karyawan terlebih dahulu!"
              return@Button
            }

            // Simpan input terakhir yang diketik di perangkat ini
            sessionManager.simpanInputTerakhir(cleanNip, namaInput.trim(), departemenInput.trim())
            inputTerakhir = sessionManager.getInputTerakhir()

            val matchKaryawan = DummyDataKantor.daftarKaryawanResmi.find { it.nip == cleanNip }
            if (matchKaryawan != null) {
              // Jika nama diisi pengguna, pakai nama pengguna; jika tidak, gunakan data master kantor
              val finalKaryawan = matchKaryawan.copy(
                nama = if (namaInput.trim().isNotEmpty()) namaInput.trim() else matchKaryawan.nama,
                departemen = if (departemenInput.trim().isNotEmpty()) departemenInput.trim() else matchKaryawan.departemen,
                avatarInisial = if (namaInput.trim().isNotEmpty()) {
                  namaInput.trim().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                } else matchKaryawan.avatarInisial
              )
              errorMessage = null
              onRegisterSuccess(finalKaryawan)
            } else {
              // Sesuai flowchart: Register tidak sesuai -> kembali ke form Register
              errorMessage = "Register tidak sesuai! NIP '$cleanNip' tidak ditemukan di database komputer kantor."
            }
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("register_submit_button")
        ) {
          Text(
            text = "Daftarkan & Masuk",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}
