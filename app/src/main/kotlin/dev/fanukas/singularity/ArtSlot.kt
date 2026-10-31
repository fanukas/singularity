package dev.fanukas.singularity

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fixed art holders (docs/decisions.md, Art). Every picture or video in the game sits in one of these shapes and
 * never changes size: custom art only fills a slot. A slot shows `res/drawable/<name>.png|webp` when it exists,
 * otherwise a labelled placeholder.
 */
enum class SlotShape(val ratio: Float, val px: String) {
    ICON(1f, "256×256"),
    PORTRAIT(3f / 4f, "768×1024"),
    BANNER(3f, "1200×400"),
    SCENE(16f / 9f, "1280×720"), // also every video
}

/** Slot names must be valid resource names: lowercase letters, digits and underscores. */
fun slotName(vararg parts: Any): String =
    parts.joinToString("_") { it.toString() }.lowercase().replace(Regex("[^a-z0-9_]"), "_")

@SuppressLint("DiscouragedApi")
@Composable
fun ArtSlot(name: String, shape: SlotShape, label: String, modifier: Modifier = Modifier, video: Boolean = false) {
    val context = LocalContext.current
    val id = context.resources.getIdentifier(name, "drawable", context.packageName)
    val base = (if (shape == SlotShape.ICON) modifier.size(64.dp) else modifier)
        .aspectRatio(shape.ratio)
        .clip(RoundedCornerShape(8.dp))
    if (id != 0) {
        Image(painterResource(id), contentDescription = label, contentScale = ContentScale.Crop, modifier = base)
    } else {
        Box(
            base
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (if (video) "▶ " else "") + if (shape == SlotShape.ICON) label.take(2) else "$label\n$name · ${shape.px}",
                textAlign = TextAlign.Center,
                fontSize = if (shape == SlotShape.ICON) 18.sp else 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

/** A slot as wide as its column. */
@Composable
fun WideSlot(name: String, shape: SlotShape, label: String, video: Boolean = false) =
    ArtSlot(name, shape, label, Modifier.fillMaxWidth(), video)
