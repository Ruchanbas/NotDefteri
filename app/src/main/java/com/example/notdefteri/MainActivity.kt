package com.example.notdefteri

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.* // Import all Material3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.notdefteri.ui.theme.DarkColorScheme // Import your custom color schemes
import com.example.notdefteri.ui.theme.LightColorScheme // Import your custom color schemes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

// Data class to represent a single note
data class Note(
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // State to control whether dark theme is active
            var koyuTema by remember { mutableStateOf(false) }

            // Choose the appropriate color scheme based on the koyuTema state
            val renkler = if (koyuTema) DarkColorScheme else LightColorScheme

            // Apply the chosen Material Theme to the entire UI
            MaterialTheme(colorScheme = renkler) {
                // Surface is a standard Material Design container that uses background color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Ensures the main background changes with theme
                ) {
                    NotDefteriEkrani(
                        isDark = koyuTema,
                        onToggleTheme = { koyuTema = !koyuTema }
                    )
                }
            }
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
    // Mutable state for the list of notes, loaded from preferences
    var notlar by remember { mutableStateOf(listOf<Note>()) }

    // Load notes when the composable first enters the composition
    LaunchedEffect(key1 = true) {
        notlar = yukleNotlar(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // The background color is set by the Surface in MainActivity,
            // but if you remove Surface, this background modifier will be essential.
            // .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top bar with app title and theme toggle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically // Center items vertically
        ) {
            Text(
                "Not Defteri",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground // Text color adapts to background
            )
            Button(onClick = onToggleTheme) {
                Text(if (isDark) "Açık Tema" else "Koyu Tema")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field for note title
        OutlinedTextField(
            value = yeniBaslik,
            onValueChange = { yeniBaslik = it },
            label = { Text("Başlık") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, // Use a contrasting color for unfocused label
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                // The background of the text field itself is usually handled by the default
                // MaterialTheme surface color, or you can explicitly set it here if needed.
                // containerColor = MaterialTheme.colorScheme.surface // Example for setting container color
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input field for note content
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

        Spacer(modifier = Modifier.height(10.dp))

        // Save button
        Button(
            onClick = {
                if (yeniBaslik.isNotBlank() || yeniIcerik.isNotBlank()) {
                    notlar = notlar + Note(title = yeniBaslik, content = yeniIcerik)
                    kaydetNotlar(context, notlar) // Save updated list
                    yeniBaslik = "" // Clear input fields
                    yeniIcerik = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            // Button colors will come from the MaterialTheme's primary and onPrimary colors
        ) {
            Text("Kaydet")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section title for notes and "Delete All" button
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
                notlar = emptyList() // Clear all notes
                kaydetNotlar(context, notlar) // Save empty list
            }) {
                Text("Hepsini Sil")
            }
        }

        // List of notes
        LazyColumn(
            modifier = Modifier.fillMaxWidth() // Make LazyColumn fill width
        ) {
            items(notlar) { not ->
                // State for dropdown menu visibility and rename dialog
                var showMenu by remember { mutableStateOf(false) }
                var yeniBaslikDialog by remember { mutableStateOf(not.title) }
                var dialogGoster by remember { mutableStateOf(false) }

                Card( // Using Card for better visual separation and elevation
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = { /* Handle single click if needed */ },
                            onLongClick = { showMenu = true }
                        ),
                    colors = CardDefaults.cardColors( // Set card background based on theme
                        containerColor = MaterialTheme.colorScheme.surfaceVariant // Use surfaceVariant for card background
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) { // Padding inside the card
                        Text(
                            text = not.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium, // Use titleMedium for note title
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Text color on surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = not.content,
                            style = MaterialTheme.typography.bodyMedium, // Use bodyMedium for note content
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🕓 " + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(not.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Slightly faded timestamp
                        )
                        if (not.isFavorite) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "⭐ Favori",
                                color = MaterialTheme.colorScheme.primary, // Favorite star color
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Dropdown menu for note actions
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        // DropdownMenu background will adapt from MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sil") },
                            onClick = {
                                notlar = notlar - not // Remove note from list
                                kaydetNotlar(context, notlar)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Adını Değiştir") },
                            onClick = {
                                dialogGoster = true // Show rename dialog
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Üste Taşı") },
                            onClick = {
                                notlar = listOf(not) + notlar.filter { it != not } // Move note to top
                                kaydetNotlar(context, notlar)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (not.isFavorite) "⭐ Favoriden Kaldır" else "⭐ Favori Olarak İşaretle") },
                            onClick = {
                                // Toggle favorite status
                                notlar = notlar.map {
                                    if (it == not) it.copy(isFavorite = !it.isFavorite) else it
                                }
                                kaydetNotlar(context, notlar)
                                showMenu = false
                            }
                        )
                    }

                    // Dialog for renaming note title
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
                            containerColor = MaterialTheme.colorScheme.surface, // Dialog background
                            titleContentColor = MaterialTheme.colorScheme.onSurface, // Dialog title color
                            textContentColor = MaterialTheme.colorScheme.onSurface // Dialog text color
                        )
                    }
                }
            }
        }
    }
}

// Function to save notes to SharedPreferences
fun kaydetNotlar(context: Context, notlar: List<Note>) {
    val prefs = context.getSharedPreferences("notlarPrefs", Context.MODE_PRIVATE)
    val json = Gson().toJson(notlar)
    prefs.edit().putString("notListesi", json).apply()
}

// Function to load notes from SharedPreferences
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

// Optional: Preview function for your Composable (for design view in Android Studio)
@Preview(showBackground = true)
@Composable
fun NotDefteriPreview() {
    MaterialTheme(colorScheme = LightColorScheme) { // Or DarkColorScheme for a dark preview
        NotDefteriEkrani(isDark = false, onToggleTheme = {})
    }
}