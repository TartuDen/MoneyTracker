package com.moneytracker.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Blue900 = Color(0xFF0B1D3A)
private val Blue700 = Color(0xFF1C3B6D)
private val Blue500 = Color(0xFF2F63B7)
private val Teal500 = Color(0xFF12A4A0)
private val Amber500 = Color(0xFFF2A33A)
private val Rose500 = Color(0xFFE4586A)

private val Sand50 = Color(0xFFF7F4EF)
private val Sand100 = Color(0xFFF0E9DE)
private val Ink900 = Color(0xFF111827)
private val Ink700 = Color(0xFF374151)

val LightColors = lightColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    primaryContainer = Blue700,
    onPrimaryContainer = Color.White,
    secondary = Teal500,
    onSecondary = Color.White,
    tertiary = Amber500,
    onTertiary = Color(0xFF2F1B00),
    background = Sand50,
    onBackground = Ink900,
    surface = Sand100,
    onSurface = Ink900,
    surfaceVariant = Color(0xFFE5DED2),
    onSurfaceVariant = Ink700,
    error = Rose500,
    onError = Color.White
)
