package com.wavky.memorycard.app

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Config(
  val row: Int,
  val column: Int,
  val welcome: Welcome,
  val levelLimit: Sequence<LevelLimit>,
  val startButton: StartButton,
  val messageButton: MessageButton,
  val goButton: GoButton,
  val card: PlayCard,
  val gameOver: GameOver,
) {
  val count = row * column

  data class Welcome(
    val headerFadeInDelay: Int,
    val headerFadeInDuration: Int,
  )

  data class LevelLimit(
    val level: Int,
    val isCountdown: Boolean, // 倒计时或正向计时
    val countdownMs: Long,
    val isCountMaxMiss: Boolean, // 是否计算最大连续错误次数
    val maxMissCount: Int,
    val previewTimeMs: Long,
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

  data class MessageButton(
    val size: Dp,
    val fontSize: TextUnit,
    val fadeDuration: Int,
  )

  data class GoButton(
    val size: Dp,
    val fontSize: TextUnit,
    val showDuration: Int,
    val fadeDuration: Int,
  )

  data class PlayCard(
    val height: Dp,
    val firstTimeFlipInterval: Long,
    val firstTimeFlipDuration: Long,
    val flipInterval: Long,
    val flipDuration: Long,
  )

  data class GameOver(
    val fadeDuration: Long,
  )

  companion object {
    val preview: Config = Config(
      row = 4,
      column = 4,
      welcome = Welcome(
        headerFadeInDelay = 500,
        headerFadeInDuration = 1000
      ),
      levelLimit = sequenceOf(),
      startButton = StartButton(
        size = 200.dp,
        fontSize = 40.sp,
        fadeDuration = 1000,
        scaleStart = 0.9f,
        scaleEnd = 1.1f,
        scaleDuration = 1000,
        scaleInitialDelay = 500,
      ),
      messageButton = MessageButton(
        size = 200.dp,
        fontSize = 40.sp,
        fadeDuration = 1000,
      ),
      goButton = GoButton(
        size = 200.dp,
        fontSize = 40.sp,
        showDuration = 1000,
        fadeDuration = 1000,
      ),
      card = PlayCard(
        height = 100.dp,
        firstTimeFlipInterval = 300L,
        firstTimeFlipDuration = 800L,
        flipInterval = 200L,
        flipDuration = 600L,
      ),
      gameOver = GameOver(
        fadeDuration = 3000
      )
    )
  }
}
