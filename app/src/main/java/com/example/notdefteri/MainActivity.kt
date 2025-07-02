package com.example.notdefteri

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.notdefteri.ui.theme.DarkColorScheme
import com.example.notdefteri.ui.theme.LightColorScheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class Note(
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val reminderTime: Long? = null // Hatırlatma zamanı eklendi
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel(this)

        setContent {
            var koyuTema by remember { mutableStateOf(false) }
            val renkler = if (koyuTema) DarkColorScheme else LightColorScheme

            MaterialTheme(colorScheme = renkler) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotDefteriEkrani(
                        isDark = koyuTema,
                        onToggleTheme = { koyuTema = !koyuTema }
                    )
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hatırlatma Kanalı"
            val descriptionText = "Notlar için hatırlatma bildirimleri"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("NOT_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotDefteriEkrani(
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current

    var yeniBaslik by remember { mutableStateOf("") }
    var yeniIcerik by remember { mutableStateOf("") }
    var hatirlatmaZamani by remember { mutableStateOf<Long?>(null) }

    var notlar by remember { mutableStateOf(listOf<Note>()) }

    // Bildirim için date/time picker kontrolleri
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        notlar = yukleNotlar(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "Not Defteri",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = onToggleTheme) {
                Text(if (isDark) "Açık Tema" else "Koyu Tema")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = yeniBaslik,
            onValueChange = { yeniBaslik = it },
            label = { Text("Başlık") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = yeniIcerik,
            onValueChange = { yeniIcerik = it },
            label = { Text("İçerik") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hatırlatma zamanı seçme butonu ve gösterimi
        Button(onClick = { showDatePicker = true }) {
            Text(
                text = hatirlatmaZamani?.let {
                    "Hatırlatma: " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "Hatırlatma Zamanı Seç"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                if (yeniBaslik.isNotBlank() || yeniIcerik.isNotBlank()) {
                    val yeniNot = Note(
                        title = yeniBaslik,
                        content = yeniIcerik,
                        reminderTime = hatirlatmaZamani
                    )
                    notlar = notlar + yeniNot
                    kaydetNotlar(context, notlar)

                    // Bildirim kur
                    if (hatirlatmaZamani != null && hatirlatmaZamani!! > System.currentTimeMillis()) {
                        val intent = Intent(context, ReminderReceiver::class.java).apply {
                            putExtra("title", yeniBaslik)
                            putExtra("content", yeniIcerik)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            yeniBaslik.hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                        )
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, hatirlatmaZamani!!, pendingIntent)
                    }

                    yeniBaslik = ""
                    yeniIcerik = ""
                    hatirlatmaZamani = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kaydet")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "Notlar:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = {
                notlar = emptyList()
                kaydetNotlar(context, notlar)
            }) {
                Text("Hepsini Sil")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(notlar) { not ->
                var showMenu by remember { mutableStateOf(false) }
                var yeniBaslikDialog by remember { mutableStateOf(not.title) }
                var dialogGoster by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = { /* tıklama isteğe bağlı */ },
                            onLongClick = { showMenu = true }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = not.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = not.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🕓 " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(not.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        not.reminderTime?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⏰ " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (not.isFavorite) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "⭐ Favori",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sil") },
                            onClick = {
                                notlar = notlar - not
                                kaydetNotlar(context, notlar)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Adını Değiştir") },
                            onClick = {
                                dialogGoster = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Üste Taşı") },
                            onClick = {
                                notlar = listOf(not) + notlar.filter { it != not }
                                kaydetNotlar(context, notlar)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(if (not.isFavorite) "⭐ Favoriden Kaldır" else "⭐ Favori Olarak İşaretle")
                            },
                            onClick = {
                                notlar = notlar.map {
                                    if (it == not) it.copy(isFavorite = !it.isFavorite) else it
                                }
                                kaydetNotlar(context, notlar)
                                showMenu = false
                            }
                        )
                    }

                    if (dialogGoster) {
                        AlertDialog(
                            onDismissRequest = { dialogGoster = false },
                            title = { Text("Başlığı Değiştir", color = MaterialTheme.colorScheme.onSurface) },
                            text = {
                                OutlinedTextField(
                                    value = yeniBaslikDialog,
                                    onValueChange = { yeniBaslikDialog = it },
                                    label = { Text("Yeni başlık") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    notlar = notlar.map {
                                        if (it == not) it.copy(title = yeniBaslikDialog) else it
                                    }
                                    kaydetNotlar(context, notlar)
                                    dialogGoster = false
                                }) {
                                    Text("Kaydet")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { dialogGoster = false }) {
                                    Text("İptal")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            textContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // DatePickerDialog ve TimePickerDialog çağrıları
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, yil, ay, gun ->
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, yil)
                    set(Calendar.MONTH, ay)
                    set(Calendar.DAY_OF_MONTH, gun)
                }
                hatirlatmaZamani = calendar.timeInMillis
                showDatePicker = false
                showTimePicker = true
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, saat, dakika ->
                hatirlatmaZamani = hatirlatmaZamani?.let {
                    Calendar.getInstance().apply {
                        timeInMillis = it
                        set(Calendar.HOUR_OF_DAY, saat)
                        set(Calendar.MINUTE, dakika)
                    }.timeInMillis
                }
                showTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
}

fun kaydetNotlar(context: Context, notlar: List<Note>) {
    val prefs = context.getSharedPreferences("notlarPrefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(notlar)
    prefs.edit().putString("notListesi", json).apply()
}

fun yukleNotlar(context: Context): List<Note> {
    val prefs = context.getSharedPreferences("notlarPrefs", Context.MODE_PRIVATE)
    val json = prefs.getString("notListesi", null)
    return if (json != null) {
        val type = object : TypeToken<List<Note>>() {}.type
        Gson().fromJson(json, type)
    } else {
        emptyList()
    }
}

// Hatırlatma bildirimini gösteren BroadcastReceiver
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Hatırlatma"
        val content = intent.getStringExtra("content") ?: ""

        val notification = NotificationCompat.Builder(context, "NOT_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // İkon değiştirebilirsin
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            ContextCompat.getSystemService(context, NotificationManager::class.java) as NotificationManager

        notificationManager.notify(title.hashCode(), notification)
    }
}

@Preview(showBackground = true)
@Composable
fun NotDefteriPreview() {
    MaterialTheme(colorScheme = LightColorScheme) {
        NotDefteriEkrani(isDark = false, onToggleTheme = {})
    }
}
