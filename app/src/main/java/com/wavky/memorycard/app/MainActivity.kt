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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wavky.memorycard.R
import com.wavky.memorycard.app.Config.StartButton
import com.wavky.memorycard.app.ui.PlayCard
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
      startButton = StartButton(
        size = 200.dp,
        fontSize = 50.sp,
        fadeDuration = 1000,
        scaleStart = 0.9f,
        scaleEnd = 1.1f,
        scaleDuration = 1000,
        scaleInitialDelay = 500,
      ),
      endButton = Config.EndButton(
        size = 200.dp,
        fontSize = 45.sp,
        fadeDuration = 1000,
      ),
      card = Config.PlayCard(
        height = 100.dp,
        previewTime = 5000,
        flipInterval = 300L,
        flipDuration = 800L
      )
    )
  }

  var restartGame by remember { mutableIntStateOf(0) }
  var level by remember { mutableIntStateOf(1) }

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

  var startGame by remember { mutableStateOf(false) }
  var showPause by remember { mutableStateOf(false) }
  var complete by remember { mutableStateOf(false) }
  var timeCount by remember(restartGame) { mutableIntStateOf(0) }
  var lastFlipIndex by remember(restartGame) { mutableIntStateOf(-1) }
  var miss by remember(restartGame) { mutableIntStateOf(0) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(
      Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
      HeaderRow(miss, timeCount, level, modifier = Modifier.fillMaxWidth())
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
                initialDelay = i * config.card.flipInterval,
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
                }
              }
              LaunchedEffect(Unit) {
                delay(config.count * config.card.flipInterval + config.card.flipDuration)
                isStartButtonVisible = true
              }
            }
          }
        }
      }
    }

    StartButton(
      config = config,
      isVisible = isStartButtonVisible,
      text = "開始",
    ) {
      isStartButtonVisible = false
      restartGame++
    }

    val scope = rememberCoroutineScope()
    StartButton(
      config = config,
      isVisible = isContinueButtonVisible,
      text = "晉級",
    ) {
      isContinueButtonVisible = false
      scope.launch {
        staticFlipList.fill(false)
        delay(config.card.flipDuration)

        complete = false
        level++
        restartGame++
      }
    }

    StartButton(
      config = config,
      isVisible = isRestartButtonVisible,
      text = "重新\n開始",
    ) {
      isRestartButtonVisible = false

      scope.launch {
        staticFlipList.fill(false)
        delay(config.card.flipDuration)

        level = 1
        restartGame++
      }
    }

    EndButton(
      config = config,
      isVisible = showPause,
      text = "結束?",
    ) {
      showPause = false
      startGame = false
    }

    // 重新开始游戏后预览卡牌：逐张翻开卡牌牌面，等待预览时间，逐张翻回背面，游戏正式开始
    LaunchedEffect(restartGame) {
      if (restartGame > 0) {
        (1..config.count).forEachIndexed { i, _ ->
          delay(config.card.flipInterval)
          staticFlipList[i] = true
        }
        delay(config.card.previewTime)
        (1..config.count).forEachIndexed { i, _ ->
          delay(config.card.flipInterval)
          staticFlipList[i] = false
        }
        delay(config.card.flipDuration)
        startGame = true
        globalPlayCardClickable = true
      }
    }

    // 游戏开始，结束
    LaunchedEffect(startGame) {
      if (restartGame == 0) return@LaunchedEffect

      when {
        // 游戏开始计时
        startGame -> {
          while (true) {
            delay(1000)
            if (!showPause) {
              timeCount++
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
          delay(config.card.flipDuration + config.card.flipInterval * (config.count - 2))
          val flipToFront = async {
            staticFlipList.forEachIndexed { i, _ ->
              delay(config.card.flipInterval)
              staticFlipList[i] = true
            }
          }
          flipToBack.await()
          flipToFront.await()
          isContinueButtonVisible = true
        }

        // 挑战失败
        else -> {
          // 结束当前局，准备下一局(level1)
          isRestartButtonVisible = true
        }
      }
    }
  }

  BackHandler(startGame) {
    if (showPause) {
      showPause = false
      globalPlayCardClickable = true
    } else {
      showPause = true
      globalPlayCardClickable = false
    }
  }
}

@Composable
fun StartButton(
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
      Text(text, fontSize = config.startButton.fontSize)
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
fun EndButton(
  config: Config,
  isVisible: Boolean,
  text: String,
  onClick: () -> Unit
) {
  fun <T> spec() = tween<T>(config.endButton.fadeDuration)
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
        painterResource(R.drawable.pause_bg),
        contentDescription = null,
        modifier = Modifier.size(config.startButton.size),
        contentScale = ContentScale.FillBounds
      )
      Text(text, fontSize = config.startButton.fontSize)
    }
  }
}

@Composable
private fun HeaderRow(
  miss: Int,
  timeCount: Int,
  level: Int,
  modifier: Modifier = Modifier
) {
  ConstraintLayout(modifier = modifier.padding(20.dp, 20.dp, 20.dp, 0.dp)) {
    val (missTextRef, timeTextRef, levelTextRef) = createRefs()

    Text("失手: $miss", fontSize = 20.sp, modifier = Modifier.constrainAs(missTextRef) {
      start.linkTo(parent.start)
      baseline.linkTo(timeTextRef.baseline)
    })

    // timeCount 秒数转时间显示
    val minute = timeCount / 60
    val second = timeCount % 60
    val timeText = String.format(Locale.getDefault(), "%02d:%02d", minute, second)
    Text(timeText, fontSize = 40.sp, modifier = Modifier.constrainAs(timeTextRef) {
      top.linkTo(parent.top)
      start.linkTo(parent.start)
      end.linkTo(parent.end)
    })

    Text("Level: $level", fontSize = 20.sp, modifier = Modifier.constrainAs(levelTextRef) {
      end.linkTo(parent.end)
      baseline.linkTo(timeTextRef.baseline)
    })
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewHeaderRow() {
  HeaderRow(
    miss = 3,
    timeCount = 65,
    level = 1,
    modifier = Modifier.fillMaxWidth()
  )
}

@Preview(showBackground = true)
@Composable
private fun PreviewStartButton() {
  StartButton(
    config = Config(
      row = 4,
      column = 4,
      startButton = StartButton(
        size = 200.dp,
        fontSize = 50.sp,
        fadeDuration = 1000,
        scaleStart = 0.9f,
        scaleEnd = 1.1f,
        scaleDuration = 1000,
        scaleInitialDelay = 500,
      ),
      endButton = Config.EndButton(
        size = 200.dp,
        fontSize = 50.sp,
        fadeDuration = 1000,
      ),
      card = Config.PlayCard(
        height = 100.dp,
        previewTime = 5000,
        flipInterval = 300L,
        flipDuration = 800L
      )
    ),
    isVisible = true,
    text = "開始",
    onClick = {}
  )
}

@Preview(showBackground = true)
@Composable
private fun PreviewEndButton() {
  EndButton(
    config = Config(
      row = 4,
      column = 4,
      startButton = StartButton(
        size = 200.dp,
        fontSize = 45.sp,
        fadeDuration = 1000,
        scaleStart = 0.9f,
        scaleEnd = 1.1f,
        scaleDuration = 1000,
        scaleInitialDelay = 500,
      ),
      endButton = Config.EndButton(
        size = 200.dp,
        fontSize = 50.sp,
        fadeDuration = 1000,
      ),
      card = Config.PlayCard(
        height = 100.dp,
        previewTime = 5000,
        flipInterval = 300L,
        flipDuration = 800L
      )
    ),
    isVisible = true,
    text = "結束?",
    onClick = {}
  )
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
