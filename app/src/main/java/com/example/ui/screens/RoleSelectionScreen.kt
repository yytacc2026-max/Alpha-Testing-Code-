package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DataKaryawan
import com.example.model.JaringanKantorManager
import com.example.ui.theme.*

// =========================================================================
// 1. LAYAR UTAMA (ABSENSI KARYAWAN & AKSES ADMIN TERSEMBUNYI)
// =========================================================================
// Sesuai aturan flowchart:
// "Karyawan yang sudah terdaftar langsung masuk ke Halaman Karyawan (tanpa register ulang)".
// - Jika belum terdaftar: kartu menampilkan "Daftar Karyawan Baru"
// - Jika sudah terdaftar: kartu menampilkan "Masuk Dashboard Karyawan" dengan nama karyawan terdaftar
// - Dilengkapi status validasi Jaringan Kantor Lokal (Intranet & Geofencing)
@Composable
fun RoleSelectionScreen(
  karyawanTerdaftar: DataKaryawan?,
  onOpenDashboard: () -> Unit,
  onRegisterNew: () -> Unit,
  onSelectAdmin: () -> Unit,
  jaringanManager: JaringanKantorManager? = null
) {
  val scrollState = rememberScrollState()

  Scaffold(
    containerColor = BgLight
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
          .verticalScroll(scrollState)
          .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Header & Branding Aplikasi
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
        ) {
          // Logo Aplikasi
          Box(
            modifier = Modifier
              .size(86.dp)
              .clip(RoundedCornerShape(24.dp))
              .background(TealPrimary),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_absensi_logo),
              contentDescription = "Logo Absensi Kantor",
              modifier = Modifier.size(70.dp)
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          Text(
            text = "Absensi Kantor",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.testTag("app_title")
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Sistem Presensi Karyawan Berbasis Jaringan Kantor",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Label Info Server Lokal (Komputer Kantor)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(TealContainer)
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Computer,
              contentDescription = null,
              tint = TealDark,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Penyimpanan: Komputer Lokal Kantor",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = TealDark
            )
          }

          if (jaringanManager != null) {
            Spacer(modifier = Modifier.height(10.dp))
            // Status Validasi Jaringan Kantor (Wajib di Jaringan Kantor)
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (jaringanManager.isDalamJaringanKantor) TealContainer.copy(alpha = 0.5f) else StatusRedBg
              ),
              border = BorderStroke(
                1.dp,
                if (jaringanManager.isDalamJaringanKantor) TealPrimary.copy(alpha = 0.35f) else StatusRed.copy(alpha = 0.4f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("role_network_card")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
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
                      text = if (jaringanManager.isDalamJaringanKantor) "Jarak: ${jaringanManager.jarakDariKantorMeter}m • Siap Absensi" else "Jarak: ${jaringanManager.jarakDariKantorMeter}m • Aplikasi Terkunci",
                      fontSize = 11.sp,
                      color = TextDark
                    )
                  }
                }

                OutlinedButton(
                  onClick = { jaringanManager.toggleModeSimulasi() },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  modifier = Modifier.testTag("role_toggle_network_button")
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
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Bagian Aksi Karyawan
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "Akses Presensi Karyawan:",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
          )

          if (karyawanTerdaftar != null) {
            // KARYAWAN SUDAH TERDAFTAR: Langsung Masuk Dashboard tanpa register ulang
            KaryawanMainCard(
              testTag = "open_registered_dashboard_card",
              title = "Buka Dashboard Karyawan",
              subtitle = "Akun Aktif: ${karyawanTerdaftar.nama} (${karyawanTerdaftar.id} • ${karyawanTerdaftar.departemen})",
              icon = Icons.Default.Person,
              badgeColor = TealPrimary,
              onClick = onOpenDashboard
            )

            // Keamanan Anti-Kecurangan / Anti-Duplikasi:
            // 1 Perangkat terkunci untuk 1 Karyawan. Pendaftaran NIP lain dinonaktifkan.
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = TealContainer.copy(alpha = 0.45f)),
              border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("device_binding_security_card")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = TealDark,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Perangkat Terikat: ${karyawanTerdaftar.nama}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealDark
                  )
                  Text(
                    text = "NIP: ${karyawanTerdaftar.nip} • Pendaftaran NIP lain dinonaktifkan di perangkat ini untuk mencegah kecurangan & duplikasi.",
                    fontSize = 11.sp,
                    color = TextDark,
                    lineHeight = 15.sp
                  )
                }
              }
            }
          } else {
            // KARYAWAN BELUM TERDAFTAR: Harus Register Dulu
            KaryawanMainCard(
              testTag = "role_karyawan_card",
              title = "Daftar Karyawan Baru",
              subtitle = "Registrasi NIP baru untuk membuka & mengakses Dashboard Absensi",
              icon = Icons.Default.Badge,
              badgeColor = TealPrimary,
              onClick = onRegisterNew
            )
          }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Akses Administrator (Tautan Halus di Bagian Bawah)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          HorizontalDivider(
            color = BorderMuted,
            modifier = Modifier.padding(horizontal = 24.dp)
          )
          Spacer(modifier = Modifier.height(14.dp))
          TextButton(
            onClick = onSelectAdmin,
            modifier = Modifier.testTag("role_admin_text_button")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Masuk sebagai Administrator",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun KaryawanMainCard(
  testTag: String,
  title: String,
  subtitle: String,
  icon: ImageVector,
  badgeColor: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
      .clickable { onClick() },
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = CardWhite),
    border = BorderStroke(1.dp, BorderMuted),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(badgeColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = badgeColor,
          modifier = Modifier.size(28.dp)
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = TextMuted,
          lineHeight = 16.sp
        )
      }
    }
  }
}
