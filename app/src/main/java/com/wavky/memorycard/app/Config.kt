package com.wavky.memorycard.app

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

data class Config(
  val row: Int,
  val column: Int,
  val welcome: Welcome,
  val startButton: StartButton,
  val endButton: EndButton,
  val card: PlayCard
) {
  val count = row * column

  data class Welcome(
    val headerFadeInDelay: Int,
    val headerFadeInDuration: Int,
  )

  data class StartButton(
    val size: Dp,
    val fontSize: TextUnit,
    val fadeDuration: Int,
    val scaleStart: Float,
    val scaleEnd: Float,
    val scaleDuration: Int,
    val scaleInitialDelay: Int,
  )

  data class EndButton(
    val size: Dp,
    val fontSize: TextUnit,
    val fadeDuration: Int,
  )

  data class PlayCard(
    val height: Dp,
    val previewTime: Long,
    val flipInterval: Long,
    val flipDuration: Long
  )
}
