package com.wavky.memorycard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode.Reverse
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wavky.memorycard.R
import com.wavky.memorycard.app.Config.LevelLimit
import com.wavky.memorycard.app.Config.StartButton
import com.wavky.memorycard.app.Config.Welcome
import com.wavky.memorycard.app.common.res.Colors
import com.wavky.memorycard.app.common.res.Fonts
import com.wavky.memorycard.app.ui.PlayCard
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

const val DEV_MODE = false
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      Image(
        painterResource(R.drawable.bg),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
      )
      Content()
    }
  }
}

@Composable
fun Content() {
  val config = remember {
    Config(
      row = 4,
      column = 4,
      welcome = Welcome(
        headerFadeInDelay = 500,
        headerFadeInDuration = 1000
      ),
      levelLimit = generateSequence(
        LevelLimit(
          level = 1,
          isCountdown = false,
          countdownMs = 13_000,
          isCountMaxMiss = false,
          maxMissCount = 4,
          previewTimeMs = 5_000
        )
      ) { last ->
        val minCountdownMs = 3000L
        val nextCountdownMs = if (last.countdownMs < minCountdownMs) {
          minCountdownMs
        } else {
          // 取整数秒
          (last.countdownMs * 0.8).toLong() / 1000 * 1000
        }
        val nextMaxMissCount = when {
          last.maxMissCount > 6 -> last.maxMissCount - 2
          last.maxMissCount > 1 -> last.maxMissCount - 1
          else -> 0
        }
        val nextPreviewTimeMs = when {
          last.level < 6 -> last.previewTimeMs
          last.previewTimeMs > 0 -> last.previewTimeMs - 500
          else -> 0L
        }
        LevelLimit(
          level = last.level + 1,
          isCountdown = true,
          countdownMs = if (nextCountdownMs < minCountdownMs) minCountdownMs else nextCountdownMs,
          isCountMaxMiss = true,
          maxMissCount = nextMaxMissCount,
          previewTimeMs = nextPreviewTimeMs,
        )
      },
      startButton = StartButton(
        size = 200.dp,
        fontSize = 40.sp,
        fadeDuration = 1000,
        scaleStart = 0.9f,
        scaleEnd = 1.1f,
        scaleDuration = 1000,
        scaleInitialDelay = 500,
      ),
      messageButton = Config.MessageButton(
        size = 200.dp,
        fontSize = 35.sp,
        fadeDuration = 1000,
      ),
      goButton = Config.GoButton(
        size = 200.dp,
        fontSize = 80.sp,
        showDuration = 500,
        fadeDuration = 300,
      ),
      card = Config.PlayCard(
        height = 100.dp,
        firstTimeFlipInterval = 300L,
        firstTimeFlipDuration = 800L,
        flipInterval = 200L,
        flipDuration = 500L,
      ),
      gameOver = Config.GameOver(
        fadeDuration = 1500
      )
    )
  }

  var showHeader by remember { mutableStateOf(false) }
  var showCards by remember { mutableStateOf(false) }

  var restartGame by remember { mutableIntStateOf(0) }
  var gameOver by remember { mutableIntStateOf(0) }
  val levelIterator by remember(gameOver) { mutableStateOf(config.levelLimit.iterator()) }
  var level by remember(gameOver) { mutableStateOf(levelIterator.next()) }

  val flipToFrontList =
    remember(restartGame) { mutableStateListOf<Boolean>().apply { repeat(config.count) { add(false) } } }
  val staticFlipList by rememberUpdatedState(flipToFrontList)
  val clickList =
    remember(restartGame) { mutableStateListOf<Int>().apply { repeat(config.count) { add(0) } } }
  val staticClickList by rememberUpdatedState(clickList)
  val elementList =
    remember(restartGame) { mutableStateListOf<Element>().apply { repeat(2) { addAll(Element.entries) }; shuffle() } }
  val staticElementList by rememberUpdatedState(elementList)
  val isPlayCardClickableList =
    remember(restartGame) { mutableStateListOf<Boolean>().apply { repeat(config.count) { add(true) } } }
  val staticIsPlayCardClickableList by rememberUpdatedState(isPlayCardClickableList)
  var globalPlayCardClickable by remember { mutableStateOf(false) }

  // 初始开始按钮
  var isStartButtonVisible by remember { mutableStateOf(false) }
  // 下一关按钮
  var isContinueButtonVisible by remember { mutableStateOf(false) }
  // 重新开始按钮
  var isRestartButtonVisible by remember { mutableStateOf(false) }
  var showGameOver by remember { mutableStateOf(false) }

  var startGame by remember { mutableStateOf(false) }
  var showPauseMessage by remember { mutableStateOf(false) }
  var showPreviewMessage by remember { mutableStateOf(false) }
  var showGoMessage by remember { mutableStateOf(false) }
  var complete by remember { mutableStateOf(false) }
  var timeCount by remember(restartGame) { mutableIntStateOf(0) }
  var timeText by remember(restartGame) { mutableStateOf("00:00") }
  var lastFlipIndex by remember(restartGame) { mutableIntStateOf(-1) }
  var miss by remember(restartGame) { mutableIntStateOf(0) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
      LaunchedEffect(Unit) {
        delay(config.welcome.headerFadeInDelay.toLong())
        showHeader = true
        delay(config.welcome.headerFadeInDuration.toLong())
        showCards = true
      }

      AnimatedVisibility(showHeader, enter = fadeIn(tween(config.welcome.headerFadeInDuration))) {
        HeaderRow(
          miss, timeText, level, modifier = Modifier
            .background(Colors.HeaderBackground)
            .fillMaxWidth()
        )
      }
      if (showCards) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
          (1..config.row).mapIndexed { y, _ ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceEvenly,
              modifier = Modifier.fillMaxWidth(),
            ) {
              (1..config.column).mapIndexed { x, _ ->
                val i = y * config.column + x
                PlayCard(
                  element = staticElementList[i],
                  cardFace = R.drawable.cardface,
                  cardBack = R.drawable.cardback,
                  flipToFront = staticFlipList[i],
                  height = config.card.height,
                  modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures {
                      if (globalPlayCardClickable && staticIsPlayCardClickableList[i]) {
                        staticClickList[i]++
                      }
                    }
                  },
                  initialDelay = i * config.card.firstTimeFlipInterval,
                  initialFlipDuration = config.card.firstTimeFlipDuration,
                  flipDuration = config.card.flipDuration
                )

                // 点击卡牌：翻转卡牌牌面，判断卡牌是否相同
                LaunchedEffect(staticClickList[i]) {
                  // 避免第一次加载（初始化）时触发运行
                  if (staticClickList[i] > 0) {
                    staticFlipList[i] = !staticFlipList[i]

                    // 判断卡片图案是否相同
                    // 同一张卡牌
                    if (lastFlipIndex == i) {
                      lastFlipIndex = -1
                      return@LaunchedEffect
                    }
                    // 不同卡牌
                    val cacheLastFlipIndex = lastFlipIndex
                    when {
                      // 上一张牌已经翻开
                      cacheLastFlipIndex >= 0 -> {
                        // 卡牌牌面相同
                        if (staticElementList[cacheLastFlipIndex] == staticElementList[i]) {
                          // 设置为不可继续点击
                          staticIsPlayCardClickableList[cacheLastFlipIndex] = false
                          staticIsPlayCardClickableList[i] = false
                          // 确保卡牌牌面朝上
                          staticFlipList[i] = true
                          staticFlipList[cacheLastFlipIndex] = true
                        } else {
                          // 卡牌牌面不同，等待当前卡牌牌面显示完毕后再翻转回去
                          // 等待期间临时禁用全局点击
                          globalPlayCardClickable = false
                          delay(config.card.flipDuration)
                          miss++
                          staticFlipList[cacheLastFlipIndex] = false
                          staticFlipList[i] = false
                          globalPlayCardClickable = true
                        }
                        lastFlipIndex = -1
                      }

                      staticFlipList[i] -> {
                        // lastFlipIndex == -1 并且当前牌为正面时，设置为当前牌序号
                        lastFlipIndex = i
                      }

                      else -> {
                        // lastFlipIndex == -1 并且当前牌为背面（事实上不存在）
                        lastFlipIndex = -1
                      }
                    }

                    // 判断游戏是否结束
                    if (staticFlipList.all { it }) {
                      globalPlayCardClickable = false
                      startGame = false
                      complete = true
                    }

                    if (level.isCountMaxMiss && miss >= level.maxMissCount) {
                      globalPlayCardClickable = false
                      startGame = false
                    }
                  }
                }

                // 游戏第一次加载时，显示开始按钮
                LaunchedEffect(Unit) {
                  delay(config.count * config.card.firstTimeFlipInterval + config.card.firstTimeFlipDuration)
                  isStartButtonVisible = true
                }
              }
            }
          }
        }
      }
    }

    StartButton(
      config = config,
      isVisible = isStartButtonVisible,
      text = "START",
    ) {
      isStartButtonVisible = false
      restartGame++
    }

    val scope = rememberCoroutineScope()
    StartButton(
      config = config,
      isVisible = isContinueButtonVisible,
      text = "LEVEL\nUP",
    ) {
      isContinueButtonVisible = false
      scope.launch {
        staticFlipList.fill(false)
        delay(config.card.flipDuration)

        complete = false
        level = levelIterator.next()
        restartGame++
      }
    }

    GameOver(
      config,
      isVisible = showGameOver
    ) {
      // 结束当前局，准备下一局(level1)
      isRestartButtonVisible = true
    }

    StartButton(
      config = config,
      isVisible = isRestartButtonVisible,
      text = "RE\nSTART",
    ) {
      isRestartButtonVisible = false
      showGameOver = false

      scope.launch {
        staticFlipList.fill(false)
        delay(config.card.flipDuration)

        gameOver++
        restartGame++
      }
    }

    MessageButton(
      config = config,
      isVisible = showPauseMessage,
      text = "GAME\nOVER?",
    ) {
      showPauseMessage = false
      startGame = false
    }

    MessageButton(
      config = config,
      isVisible = showPreviewMessage,
      text = "PREVIEW",
    ) {}

    GoButton(
      config = config,
      isVisible = showGoMessage,
    )

    // 重新开始游戏后预览卡牌：逐张翻开卡牌牌面，等待预览时间，逐张翻回背面，游戏正式开始
    LaunchedEffect(restartGame) {
      if (restartGame > 0) {

        // 显示【预览】提示
        showPreviewMessage = true
        delay(config.messageButton.fadeDuration.toLong() + 500)
        showPreviewMessage = false

        (1..config.count).forEachIndexed { i, _ ->
          delay(config.card.flipInterval)
          staticFlipList[i] = true
        }

        // 预览倒计时
        var countDown = level.previewTimeMs
        val step = 100L
        while (countDown > 0) {
          timeText = countDown.msToTimeText()
          delay(step)
          countDown -= step
        }
        timeText = 0L.msToTimeText()

        (1..config.count).forEachIndexed { i, _ ->
          delay(config.card.flipInterval)
          staticFlipList[i] = false
        }
        delay(config.card.flipDuration)

        // 显示【开始翻牌】提示
        showGoMessage = true
        delay(config.goButton.fadeDuration.toLong() + config.goButton.showDuration)
        showGoMessage = false
        delay(config.goButton.fadeDuration.toLong())

        startGame = true
        globalPlayCardClickable = true
      }
    }

    // 游戏开始，结束
    LaunchedEffect(startGame) {
      if (restartGame == 0) return@LaunchedEffect

      if (DEV_MODE) {
        if (startGame) {
          delay(1000)
          staticFlipList.fill(true)
          delay(config.card.flipDuration)
          startGame = false
          isContinueButtonVisible = true
        }
        return@LaunchedEffect
      }

      when {
        // 游戏开始计时
        startGame -> {
          if (level.isCountdown) {
            var countDown = level.countdownMs
            val step = 100L
            while (countDown > 0) {
              timeText = countDown.msToTimeText()
              delay(step)
              countDown -= step
            }
            timeText = 0L.msToTimeText()
            startGame = false
            globalPlayCardClickable = false
          } else {
            while (true) {
              delay(1000)
              if (!showPauseMessage) {
                timeCount++
                timeText = timeCount.secToTimeText()
              }
            }
          }
        }

        // 挑战成功
        // 结束当前局，准备下一局(level+1)
        complete -> {
          // 等待最后一张卡牌翻转完毕
          delay(config.card.flipDuration)
          // 挑战成功动画效果
          val flipToBack = async {
            staticFlipList.forEachIndexed { i, _ ->
              delay(config.card.flipInterval)
              staticFlipList[i] = false
            }
          }
          delay(config.card.flipDuration + config.card.flipInterval * config.count)
          flipToBack.await()
          staticFlipList.fill(true)
          delay(config.card.flipDuration)
          isContinueButtonVisible = true
        }

        // 挑战失败
        else -> {
          showGameOver = true
        }
      }
    }
  }

  BackHandler(startGame) {
    if (showPauseMessage) {
      showPauseMessage = false
      globalPlayCardClickable = true
    } else {
      showPauseMessage = true
      globalPlayCardClickable = false
    }
  }
}

@Composable
private fun StartButton(
  config: Config,
  isVisible: Boolean,
  text: String,
  onClick: () -> Unit
) {
  fun <T> spec() = tween<T>(config.startButton.fadeDuration)
  AnimatedVisibility(
    isVisible,
    enter = fadeIn(spec()) + scaleIn(spec()),
    exit = fadeOut(spec()) + scaleOut(spec())
  ) {
    var scaleTo by remember { mutableFloatStateOf(config.startButton.scaleStart) }
    val scaleAnimate = animateFloatAsState(
      targetValue = scaleTo,
      animationSpec = InfiniteRepeatableSpec(
        tween(config.startButton.scaleDuration),
        repeatMode = Reverse,
        initialStartOffset = StartOffset(config.startButton.fadeDuration + config.startButton.scaleInitialDelay)
      )
    )
    Box(
      contentAlignment = Alignment.Center, modifier = Modifier
        .scale(scaleAnimate.value)
        .pointerInput(Unit) {
          detectTapGestures {
            onClick()
          }
        }) {
      Image(
        painterResource(R.drawable.start_bg),
        contentDescription = null,
        modifier = Modifier.size(config.startButton.size),
        contentScale = ContentScale.FillBounds
      )
      Text(
        text,
        fontSize = config.startButton.fontSize,
        fontFamily = Fonts.bb77sd,
        textAlign = TextAlign.Center
      )
    }
    DisposableEffect(Unit) {
      scaleTo = config.startButton.scaleEnd

      onDispose {
        scaleTo = config.startButton.scaleStart
      }
    }
  }
}

@Composable
private fun MessageButton(
  config: Config,
  isVisible: Boolean,
  text: String,
  fontSize: TextUnit = config.messageButton.fontSize,
  onClick: () -> Unit
) {
  fun <T> spec() = tween<T>(config.messageButton.fadeDuration)
  AnimatedVisibility(
    isVisible,
    enter = fadeIn(spec()) + scaleIn(spec()),
    exit = fadeOut(spec()) + scaleOut(spec())
  ) {
    Box(
      contentAlignment = Alignment.Center, modifier = Modifier
        .pointerInput(Unit) {
          detectTapGestures {
            onClick()
          }
        }) {
      Image(
        painterResource(R.drawable.message_bg),
        contentDescription = null,
        modifier = Modifier.size(config.messageButton.size),
        contentScale = ContentScale.FillBounds
      )
      Text(
        text,
        fontSize = fontSize,
        fontFamily = Fonts.bb77sd,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun GoButton(
  config: Config,
  isVisible: Boolean,
) {
  fun <T> spec() = tween<T>(config.goButton.fadeDuration)
  AnimatedVisibility(
    isVisible,
    enter = fadeIn(spec()) + scaleIn(spec()),
    exit = fadeOut(spec()) + scaleOut(spec())
  ) {
    Box(
      contentAlignment = Alignment.Center) {
      Image(
        painterResource(R.drawable.start_bg),
        contentDescription = null,
        modifier = Modifier.size(config.goButton.size),
        contentScale = ContentScale.FillBounds
      )
      Text(
        "GO!",
        fontSize = config.goButton.fontSize,
        fontFamily = Fonts.bb77sd,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun GameOver(
  config: Config,
  isVisible: Boolean,
  onVisible: suspend () -> Unit
) {
  fun <T> spec() = tween<T>(config.gameOver.fadeDuration.toInt())
  AnimatedVisibility(
    isVisible,
    enter = fadeIn(spec()),
    exit = fadeOut(tween(0))
  ) {
    var toOffsetY by remember { mutableFloatStateOf(0f) }
    val offsetY by animateFloatAsState(
      targetValue = toOffsetY,
      animationSpec = tween(config.gameOver.fadeDuration.toInt())
    )

    BoxWithConstraints(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .background(Colors.GameOverBackground)
        .fillMaxSize()) {
      Image(
        painterResource(R.drawable.game_over),
        contentDescription = null,
        modifier = Modifier.offset {
          IntOffset(0, (maxHeight / 2 * offsetY).roundToPx())
        },
      )
    }

    LaunchedEffect (Unit) {
      delay(config.gameOver.fadeDuration)
      toOffsetY = -0.6f
      delay(config.gameOver.fadeDuration)
      onVisible()
    }
  }
}

@Composable
private fun HeaderRow(
  miss: Int,
  timeText: String,
  level: LevelLimit,
  modifier: Modifier = Modifier
) {
  ConstraintLayout(modifier = modifier.padding(20.dp, 20.dp, 20.dp, 5.dp)) {
    val (missTextRef, timeTextRef, levelTextRef) = createRefs()

    val missCount = String.format(Locale.getDefault(), "%02d/%02d", miss, level.maxMissCount)
    val missText = if (level.isCountMaxMiss) {
      "MISS\n$missCount"
    } else {
      "MISS\n$miss"
    }
    Text(
      missText,
      fontSize = 20.sp,
      fontFamily = Fonts.bb77sd,
      modifier = Modifier.constrainAs(missTextRef) {
        start.linkTo(parent.start)
        top.linkTo(timeTextRef.top)
        bottom.linkTo(timeTextRef.bottom)
      })

    Text(
      timeText,
      fontSize = 40.sp,
      fontFamily = Fonts.bb77cd,
      modifier = Modifier.constrainAs(timeTextRef) {
        top.linkTo(parent.top)
        start.linkTo(parent.start)
        end.linkTo(parent.end)
      })

    Text(
      "LV\n${level.level}",
      fontSize = 20.sp,
      fontFamily = Fonts.bb77sd,
      textAlign = TextAlign.End,
      modifier = Modifier.constrainAs(levelTextRef) {
        end.linkTo(parent.end)
        top.linkTo(timeTextRef.top)
        bottom.linkTo(timeTextRef.bottom)
      })
  }
}

// 毫秒数转时间显示
private fun Long.msToTimeText(): String {
  val second = this / 1000
  val millisecond = this % 1000 / 10
  return String.format(Locale.getDefault(), "%02d:%02d", second, millisecond)
}

// 秒数转时间显示
private fun Int.secToTimeText(): String {
  val minute = this / 60
  val second = this % 60
  return String.format(Locale.getDefault(), "%02d:%02d", minute, second)
}

@Preview(showBackground = true)
@Composable
private fun PreviewHeaderRow() {
  HeaderRow(
    miss = 3,
    timeText = 65.secToTimeText(),
    level = LevelLimit(1, false, 0, false, 0, 0),
    modifier = Modifier.fillMaxWidth()
  )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStartButton() {
  StartButton(
    config = Config.preview,
    isVisible = true,
    text = "RE\nSTART",
    onClick = {}
  )
}

@Preview(showBackground = true)
@Composable
private fun PreviewMessageButton() {
  MessageButton(
    config = Config.preview,
    isVisible = true,
    text = "GAME\nOVER?",
    onClick = {}
  )
}

@Preview
@Composable
private fun PreviewGameOver() {
  GameOver(
    config = Config.preview,
    isVisible = true
  ) { }
}

@Preview(showBackground = true)
@Composable
private fun PreviewContent() {
  Image(
    painterResource(R.drawable.bg),
    contentDescription = null,
    modifier = Modifier.fillMaxSize(),
    contentScale = ContentScale.FillBounds
  )
  Content()
}
