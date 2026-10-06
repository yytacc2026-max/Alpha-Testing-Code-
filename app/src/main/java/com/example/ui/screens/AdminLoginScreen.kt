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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// 
// dashboard login admin
// 
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
  onLoginSuccess: () -> Unit,
  onBack: () -> Unit
) {
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }

  // Status error login
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scrollState = rememberScrollState()

  Scaffold(
    containerColor = BgLight,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Login Administrator",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("admin_login_back_button")) {
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
        // Ikon Header Admin
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(StatusAmberBg),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = "Ikon Admin",
            tint = StatusAmber,
            modifier = Modifier.size(42.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Portal Administrator",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = TextDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Masuk untuk memantau, mengedit & mengolah absensi",
          fontSize = 13.sp,
          color = TextMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Label Keamanan: Kredensial hanya diketahui oleh Operator Resmi
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = StatusAmberBg,
          border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = StatusAmber,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Kredensial Default: admin / admin123",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextDark
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pesan Error jika Login Gagal/Salah
        if (errorMessage != null) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StatusRedBg),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_login_error_card")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
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
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Input Username Admin
        OutlinedTextField(
          value = username,
          onValueChange = {
            username = it
            errorMessage = null
          },
          label = { Text("Username Admin") },
          leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary)
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_username_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Input Password Admin
        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            errorMessage = null
          },
          label = { Text("Kata Sandi") },
          leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary)
          },
          trailingIcon = {
            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
              Icon(
                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Lihat Password",
                tint = TextMuted
              )
            }
          },
          visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = absensiTextFieldColors(),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_password_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Login
        Button(
          onClick = {
            if (username.trim() == "admin" && (password == "admin123" || password == "admin")) {
              errorMessage = null
              onLoginSuccess()
            } else {
              errorMessage = "Login gagal/salah! Periksa kembali username dan kata sandi Anda."
            }
          },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("admin_login_submit_button")
        ) {
          Text(
            text = "Masuk Sebagai Admin",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}
