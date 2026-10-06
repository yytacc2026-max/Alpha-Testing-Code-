package com.example.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**manajemen keamanan jaringan kantor lokal (INTRANET & GEOFENCING VALIDATION) 
*/
class JaringanKantorManager(private val context: Context) {
  // Batas toleransi radius kantor (meter)
  val radiusMaksimalKantorMeter: Int = 100

  // Nama SSID Wi-Fi kantor resmi
  val namaWifiKantor: String = "WIFI-KANTOR-LOCAL"

  // IP Server lokal komputer kantor
  val ipServerKantor: String = "192.168.1.100:8080"

  // Status apakah saat ini berada di dalam jaringan kantor
  // Default: true (berada di kantor) agar pengguna langsung dapat mencoba fitur absensi,
  // dan disediakan toggle simulasi satu sentuhan untuk menguji kondisi terkunci di luar kantor.
  var isDalamJaringanKantor by mutableStateOf(true)
    private set

  // Jarak perkiraan dari kantor dalam meter (15m saat di kantor, 1.200m saat jauh)
  var jarakDariKantorMeter by mutableIntStateOf(15)
    private set

  // Status koneksi fisik perangkat ke Wi-Fi / jaringan aktif
  fun isTerhubungKoneksi(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
           caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
  }

  // Set simulasi: Karyawan telah tiba di area kantor & terhubung ke Wi-Fi kantor
  fun setMasukKantor() {
    isDalamJaringanKantor = true
    jarakDariKantorMeter = 15
  }

  // Set simulasi: Karyawan berada jauh di luar kantor (aplikasi terkunci)
  fun setJauhDariKantor(jarakMeter: Int = 1200) {
    isDalamJaringanKantor = false
    jarakDariKantorMeter = jarakMeter
  }

  // Toggle mode simulasi secara instan (untuk pengujian di emulator)
  fun toggleModeSimulasi() {
    if (isDalamJaringanKantor) {
      setJauhDariKantor(1200)
    } else {
      setMasukKantor()
    }
  }
}
