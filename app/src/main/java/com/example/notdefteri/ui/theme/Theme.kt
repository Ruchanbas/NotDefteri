package com.example.notdefteri.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Define your dark color scheme
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // A purple accent for primary elements
    onPrimary = Color.Black,     // Text on primary color
    secondary = Color(0xFF03DAC5), // A teal accent for secondary elements
    background = Color(0xFF121212), // Dark background
    onBackground = Color(0xFFE0E0E0), // Light text on dark background
    surface = Color(0xFF1E1E1E),     // Dark surface for cards, etc.
    onSurface = Color(0xFFE0E0E0)    // Light text on dark surface
)

// Define your light color scheme
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // A strong purple for primary elements
    onPrimary = Color.White,     // Text on primary color
    secondary = Color(0xFF03DAC5), // A teal accent for secondary elements
    background = Color(0xFFFFFFFF), // White background
    onBackground = Color(0xFF1C1B1F), // Dark text on light background
    surface = Color(0xFFFFFFFF),     // White surface
    onSurface = Color(0xFF1C1B1F)    // Dark text on light surface
)