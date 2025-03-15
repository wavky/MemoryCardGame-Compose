package com.wavky.memorycard.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.vector.ImageVector

enum class Element(val image: ImageVector) {
  STAR(Icons.Rounded.Star),
  HEART(Icons.Rounded.Favorite),
  FACE(Icons.Rounded.Face),
  HOME(Icons.Rounded.Home),
  PHONE(Icons.Rounded.Call),
  CART(Icons.Rounded.ShoppingCart),
  LOCATION(Icons.Rounded.LocationOn),
  EMAIL(Icons.Rounded.Email),
}
