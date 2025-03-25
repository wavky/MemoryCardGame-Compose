package com.wavky.memorycard.app.ui

import android.graphics.BitmapFactory
import android.graphics.Camera
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wavky.memorycard.R
import com.wavky.memorycard.app.Element
import com.wavky.memorycard.app.common.res.Colors
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun PlayCard(
  element: Element,
  cardFace: ImageBitmap,
  cardBack: ImageBitmap,
  flipToFront: Boolean,
  height: Dp,
  modifier: Modifier = Modifier,
  initialDelay: Long = 0,
  initialFlipDuration: Long = 1000,
  flipDuration: Long = 1000,
) {
  val camera by remember { mutableStateOf(Camera()) }
  val rotationAnimatable = remember { Animatable(if (flipToFront) -270f else -90f) }
  val scaleAnimatable = remember { Animatable(1f) }
  var showCardFace by remember { mutableStateOf(flipToFront) }
  val cardFaceImageColor = Colors.CardFaceImageTint

  val width = remember { height * 0.6f }
  val contentSize = remember { height * 0.4f }
  val cornerSize = remember { height * 3 / 50f }
  var isFirstTime by remember { mutableStateOf(true) }

  Card(
    modifier = modifier
      .height(height)
      .width(width)
      .drawWithContent {
        drawIntoCanvas { canvas ->
          val nativeCanvas = canvas.nativeCanvas
          canvas.translate(size.width / 2, size.height / 2)
          camera.save()
          camera.rotateY(rotationAnimatable.value)
          camera.applyToCanvas(nativeCanvas)
          camera.restore()
          canvas.translate(-size.width / 2, -size.height / 2)
        }
        drawContent()
      }
      .scale(scaleAnimatable.value),
    shape = RoundedCornerShape(cornerSize),
  ) {
    if (showCardFace) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
          cardFace,
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
        Icon(
          element.image,
          modifier = Modifier.size(contentSize),
          contentDescription = null,
          tint = cardFaceImageColor
        )
      }
    } else {
      Image(
        cardBack,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
      )
    }
  }
  LaunchedEffect(flipToFront) {
    val halfDuration = if (isFirstTime) initialFlipDuration.toInt() / 2 else flipDuration.toInt() / 2
    if (isFirstTime) {
      isFirstTime = false
      delay(initialDelay)
    }
    fun <T> upperTween(): TweenSpec<T> = tween(halfDuration, easing = FastOutLinearInEasing)
    fun <T> lowerTween(): TweenSpec<T> = tween(halfDuration, easing = LinearOutSlowInEasing)
    val upperRotate = async {
      rotationAnimatable.animateTo(
        if (flipToFront) 90f else 270f,
        animationSpec = upperTween()
      )
    }
    val upperScale = async {
      scaleAnimatable.animateTo(
        1.5f,
        animationSpec = upperTween()
      )
    }
    upperRotate.await()
    upperScale.await()
    showCardFace = flipToFront
    val lowerRotate = async {
      rotationAnimatable.animateTo(
        if (flipToFront) 180f else 360f,
        animationSpec = lowerTween()
      )
    }
    val lowerScale = async {
      scaleAnimatable.animateTo(
        1f,
        animationSpec = lowerTween()
      )
    }
    lowerRotate.await()
    lowerScale.await()
    if (!flipToFront) {
      rotationAnimatable.snapTo(0f)
    }
  }
}

@Composable
fun rememberSharedBitmap(resId: Int): ImageBitmap {
  val context = LocalContext.current
  return remember(resId) {
    val bitmap = BitmapFactory.decodeResource(context.resources, resId)
    bitmap.asImageBitmap()
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewPlayCard() {
  val element = remember { Element.entries.random() }
  var click by remember { mutableIntStateOf(0) }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
    modifier = Modifier.fillMaxSize(),
  ) {
    var flipToFront by remember { mutableStateOf(false) }
    val cardFace = rememberSharedBitmap(R.drawable.cardface)
    val cardBack = rememberSharedBitmap(R.drawable.cardback)
    PlayCard(
      element = element,
      cardFace = cardFace,
      cardBack = cardBack,
      flipToFront = flipToFront,
      height = 100.dp,
      modifier = Modifier.clickable { click++ }
    )
    LaunchedEffect(click) {
      if (click > 0) {
        flipToFront = !flipToFront
      }
    }
  }
}
