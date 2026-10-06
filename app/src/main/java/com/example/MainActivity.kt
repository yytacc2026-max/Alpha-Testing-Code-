package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.model.DummyDataKantor
import com.example.model.JaringanKantorManager
import com.example.model.SessionManager
import com.example.ui.screens.*
import com.example.ui.theme.AbsensiKantorTheme
import com.example.ui.theme.BgLight

// 
// Rute navigasi aplikasi
// 
enum class LayarNavigasi {
  PILIH_PERAN,            // Layar awal membuka aplikasi
  LOGIN_ADMIN,            // Form Login Admin
  DASHBOARD_ADMIN,        // Halaman Admin (standar & setelah diedit)
  REGISTER_KARYAWAN,      // Form Register Karyawan
  DASHBOARD_KARYAWAN      // Halaman Karyawan (sebelum & sesudah absen)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AbsensiKantorTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = BgLight
        ) {
          AbsensiAppMain()
        }
      }
    }
  }
}

@Composable
fun AbsensiAppMain() {
  val context = LocalContext.current
  val sessionManager = remember { SessionManager(context) }
  val jaringanManager = remember { JaringanKantorManager(context) }

  // Cek data karyawan yang tersimpan di perangkat
  var dataKaryawanAktif by remember {
    mutableStateOf(
      sessionManager.getKaryawanTerdaftar() ?: DummyDataKantor.karyawanAktifDefault
    )
  }

  // Sesuai flowchart: Karyawan yang sudah terdaftar langsung masuk ke Halaman Karyawan (tanpa register ulang)
  var layarAktif by remember {
    mutableStateOf(
      if (sessionManager.isSudahTerdaftar()) LayarNavigasi.DASHBOARD_KARYAWAN
      else LayarNavigasi.PILIH_PERAN
    )
  }

  // Tangani tombol Back fisik/gesture Android
  BackHandler(enabled = layarAktif != LayarNavigasi.PILIH_PERAN) {
    layarAktif = LayarNavigasi.PILIH_PERAN
  }

  when (layarAktif) {
    LayarNavigasi.PILIH_PERAN -> {
      val karyawanTersimpan = sessionManager.getKaryawanTerdaftar()
      RoleSelectionScreen(
        karyawanTerdaftar = karyawanTersimpan,
        jaringanManager = jaringanManager,
        onOpenDashboard = {
          // Buka dashboard langsung untuk akun yang sudah terdaftar
          dataKaryawanAktif = karyawanTersimpan ?: DummyDataKantor.karyawanAktifDefault
          layarAktif = LayarNavigasi.DASHBOARD_KARYAWAN
        },
        onRegisterNew = {
          // Registrasi NIP baru
          layarAktif = LayarNavigasi.REGISTER_KARYAWAN
        },
        onSelectAdmin = {
          // Masuk sebagai Admin
          layarAktif = LayarNavigasi.LOGIN_ADMIN
        }
      )
    }

    LayarNavigasi.LOGIN_ADMIN -> {
      AdminLoginScreen(
        onLoginSuccess = {
          layarAktif = LayarNavigasi.DASHBOARD_ADMIN
        },
        onBack = {
          layarAktif = LayarNavigasi.PILIH_PERAN
        }
      )
    }

    LayarNavigasi.DASHBOARD_ADMIN -> {
      AdminDashboardScreen(
        onLogout = {
          layarAktif = LayarNavigasi.PILIH_PERAN
        }
      )
    }

    LayarNavigasi.REGISTER_KARYAWAN -> {
      KaryawanRegisterScreen(
        jaringanManager = jaringanManager,
        onRegisterSuccess = { karyawanBaru ->
          // Simpan identitas pendaftaran ke session lokal perangkat
          sessionManager.simpanPendaftaran(karyawanBaru)
          dataKaryawanAktif = karyawanBaru
          layarAktif = LayarNavigasi.DASHBOARD_KARYAWAN
        },
        onBack = {
          layarAktif = LayarNavigasi.PILIH_PERAN
        }
      )
    }

    LayarNavigasi.DASHBOARD_KARYAWAN -> {
      KaryawanDashboardScreen(
        karyawan = dataKaryawanAktif,
        jaringanManager = jaringanManager,
        onLogout = {
          layarAktif = LayarNavigasi.PILIH_PERAN
        },
        onResetApp = {
          sessionManager.hapusPendaftaran()
          layarAktif = LayarNavigasi.PILIH_PERAN
        }
      )
    }
  }
}
