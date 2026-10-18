package app.stele.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.stele.domain.Shape
import app.stele.ui.theme.Palette
import app.stele.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** How many glyphs a dig's marker can carry. */
const val GLYPHS = 12

/**
 * A glyph carved on a dig's marker stone: sun, eye, wave, bird, fish, key, star, tree, hand, spiral,
 * moon, mountain. Drawn in strokes in a unit square.
 */
fun DrawScope.glyph(i: Int, color: Color, r: Rect, weight: Float = 0.085f) {
    val u = min(r.width, r.height)
    val s = Stroke(u * weight, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun o(x: Float, y: Float) = Offset(r.left + r.width * x, r.top + r.height * y)
    val path = Path()
    fun line(vararg pts: Pair<Float, Float>) {
        pts.forEachIndexed { k, (x, y) -> val p = o(x, y); if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
    }
    when (Math.floorMod(i, GLYPHS)) {
        0 -> { // sun
            drawCircle(color, u * 0.17f, o(.5f, .5f), style = s)
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4
                line(.5f + cos(a) * .3f to .5f + sin(a) * .3f, .5f + cos(a) * .42f to .5f + sin(a) * .42f)
            }
        }
        1 -> { // eye
            path.moveTo(o(.1f, .5f).x, o(.1f, .5f).y)
            path.quadraticTo(o(.5f, .1f).x, o(.5f, .1f).y, o(.9f, .5f).x, o(.9f, .5f).y)
            path.quadraticTo(o(.5f, .9f).x, o(.5f, .9f).y, o(.1f, .5f).x, o(.1f, .5f).y)
            drawCircle(color, u * 0.1f, o(.5f, .5f))
        }
        2 -> { // waves
            for (row in 0..2) {
                val y = .3f + row * .2f
                for (k in 0..40) {
                    val x = .1f + .8f * k / 40
                    val p = o(x, y + sin(k / 40f * 4 * PI.toFloat()) * .06f)
                    if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
            }
        }
        3 -> { // bird
            line(.12f to .45f, .32f to .38f, .5f to .52f, .68f to .38f, .88f to .45f)
            line(.5f to .52f, .5f to .78f)
            line(.38f to .78f, .62f to .78f)
        }
        4 -> { // fish
            path.moveTo(o(.15f, .5f).x, o(.15f, .5f).y)
            path.quadraticTo(o(.45f, .2f).x, o(.45f, .2f).y, o(.72f, .5f).x, o(.72f, .5f).y)
            path.quadraticTo(o(.45f, .8f).x, o(.45f, .8f).y, o(.15f, .5f).x, o(.15f, .5f).y)
            line(.72f to .5f, .9f to .32f, .9f to .68f, .72f to .5f)
            drawCircle(color, u * 0.04f, o(.32f, .46f))
        }
        5 -> line(.2f to .8f, .2f to .2f, .8f to .2f, .8f to .65f, .4f to .65f, .4f to .4f, .6f to .4f) // key
        6 -> { // star
            for (k in 0 until 4) {
                val a = k * PI.toFloat() / 4
                line(.5f - cos(a) * .38f to .5f - sin(a) * .38f, .5f + cos(a) * .38f to .5f + sin(a) * .38f)
            }
        }
        7 -> { // tree
            line(.5f to .88f, .5f to .15f)
            for (k in 0..2) { val y = .3f + k * .17f; line(.5f to y + .1f, .25f + k * .03f to y - .04f); line(.5f to y + .1f, .75f - k * .03f to y - .04f) }
        }
        8 -> { // hand
            line(.32f to .85f, .3f to .45f)
            line(.68f to .85f, .72f to .45f)
            for (k in 0..3) { val x = .32f + k * .12f; line(x to .5f, x to .16f + if (k == 1 || k == 2) 0f else .08f) }
            line(.3f to .55f, .14f to .4f)
        }
        9 -> { // spiral
            for (k in 0..60) {
                val a = k / 60f * 5 * PI.toFloat()
                val rr = .04f + .36f * k / 60
                val p = o(.5f + cos(a) * rr, .5f + sin(a) * rr)
                if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
        }
        10 -> { // moon
            path.moveTo(o(.62f, .14f).x, o(.62f, .14f).y)
            path.cubicTo(o(.12f, .2f).x, o(.12f, .2f).y, o(.12f, .8f).x, o(.12f, .8f).y, o(.62f, .86f).x, o(.62f, .86f).y)
            path.cubicTo(o(.36f, .7f).x, o(.36f, .7f).y, o(.36f, .3f).x, o(.36f, .3f).y, o(.62f, .14f).x, o(.62f, .14f).y)
        }
        else -> { // mountain
            line(.08f to .8f, .38f to .3f, .52f to .52f, .66f to .38f, .92f to .8f, .08f to .8f)
        }
    }
    drawPath(path, color, style = s)
}

/** A dig's marker: a round clay seal with its glyph. */
@Composable
fun DigSeal(symbol: Int, clay: Color, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val p = U.c
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2
        val c = center
        drawCircle(p.night.copy(alpha = 0.12f), r * 0.98f, c + Offset(0f, r * 0.08f))
        drawCircle(clay, r * 0.94f, c)
        drawCircle(Color.White.copy(alpha = 0.14f), r * 0.94f, c - Offset(r * 0.18f, r * 0.2f))
        drawCircle(clay, r * 0.76f, c)
        drawCircle(p.night.copy(alpha = 0.25f), r * 0.76f, c, style = Stroke(r * 0.05f))
        glyph(symbol, p.limestone, Rect(c.x - r * 0.5f, c.y - r * 0.5f, c.x + r * 0.5f, c.y + r * 0.5f), 0.11f)
    }
}

/**
 * The museum from the outside: a temple front on its steps. Each hall opened behind the sealed door adds
 * to it — more columns, acroteria, a painted frieze, a mosaic floor, statues, a gilded roof.
 */
@Composable
fun MuseumFront(halls: Int, vessels: List<Pair<Shape, List<ShardLook>>>, modifier: Modifier = Modifier) {
    val p = U.c
    Box(modifier.drawWithCache {
        val w = size.width
        val h = size.height
        onDrawBehind { drawFront(p, w, h, halls, vessels) }
    })
}

private fun DrawScope.drawFront(p: Palette, w: Float, h: Float, halls: Int, vessels: List<Pair<Shape, List<ShardLook>>>) {
    // Evening sky and the sun going down behind the hill.
    drawRect(Brush.verticalGradient(listOf(p.ochreSoft, p.claySoft.copy(alpha = 0.7f), p.bg), 0f, h * 0.8f), Offset.Zero, Size(w, h))
    drawCircle(p.goldSoft, h * 0.16f, Offset(w * 0.82f, h * 0.26f))
    drawCircle(p.gold.copy(alpha = 0.35f), h * 0.11f, Offset(w * 0.82f, h * 0.26f))
    val hill = Path().apply {
        moveTo(0f, h * 0.7f)
        cubicTo(w * 0.25f, h * 0.58f, w * 0.55f, h * 0.66f, w, h * 0.56f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(hill, p.surface2)
    val cols = (4 + halls).coerceAtMost(9)
    val left = w * 0.08f
    val right = w * 0.92f
    val base = h * 0.92f
    val stepH = h * 0.035f
    // Steps.
    for (k in 0 until 3) {
        val inset = k * w * 0.015f
        drawRect(if (k % 2 == 0) p.stone else p.limestone, Offset(left - w * 0.03f + inset, base - stepH * (k + 1)), Size(right - left + w * 0.06f - inset * 2, stepH))
    }
    val floorY = base - stepH * 3
    val roofY = h * 0.34f
    val entY = roofY + h * 0.09f
    // Mosaic floor in front of the colonnade.
    if (halls >= 3) {
        val n = 22
        for (k in 0 until n) {
            val x = left + (right - left) * k / n
            drawRect(if (k % 2 == 0) p.clay.copy(alpha = 0.55f) else p.ochre.copy(alpha = 0.5f), Offset(x, base - stepH * 1.6f), Size((right - left) / n * 0.8f, stepH * 0.35f))
        }
    }
    // The cella's dark interior, where the exhibits stand.
    drawRect(p.night.copy(alpha = 0.72f), Offset(left + w * 0.02f, entY), Size(right - left - w * 0.04f, floorY - entY))
    drawRect(Brush.verticalGradient(listOf(p.night.copy(alpha = 0f), p.ochre.copy(alpha = 0.22f)), entY, floorY), Offset(left + w * 0.02f, entY), Size(right - left - w * 0.04f, floorY - entY))
    // Columns, fluted.
    val colW = (right - left) / (cols * 2 - 1)
    val gaps = mutableListOf<Rect>()
    for (k in 0 until cols) {
        val x = left + k * colW * 2
        drawRect(Brush.horizontalGradient(listOf(p.stone, p.limestone, p.limestone, p.stone), x, x + colW), Offset(x, entY), Size(colW, floorY - entY))
        for (f in 1..3) drawLine(p.stone.copy(alpha = 0.8f), Offset(x + colW * f / 4, entY + colW * 0.4f), Offset(x + colW * f / 4, floorY - colW * 0.3f), colW * 0.05f)
        drawRect(p.limestone, Offset(x - colW * 0.15f, entY), Size(colW * 1.3f, colW * 0.32f))
        drawRect(p.stone, Offset(x - colW * 0.1f, floorY - colW * 0.25f), Size(colW * 1.2f, colW * 0.25f))
        if (k < cols - 1) gaps += Rect(x + colW, entY, x + colW * 2, floorY)
    }
    // Exhibits between the columns, on little plinths.
    gaps.forEachIndexed { i, g ->
        val v = vessels.getOrNull(i) ?: return@forEachIndexed
        val ph = (g.height * 0.2f)
        plinth(p, Rect(g.left + g.width * 0.1f, g.bottom - ph, g.right - g.width * 0.1f, g.bottom))
        val vr = Rect(g.left - g.width * 0.05f, g.bottom - ph - g.height * 0.5f, g.right + g.width * 0.05f, g.bottom - ph + g.height * 0.02f)
        drawMiniVessel(p, v.first, v.second, vr, i)
    }
    // Architrave and frieze.
    drawRect(p.limestone, Offset(left - w * 0.02f, roofY + h * 0.04f), Size(right - left + w * 0.04f, h * 0.05f))
    drawLine(p.stone, Offset(left - w * 0.02f, roofY + h * 0.065f), Offset(right + w * 0.02f, roofY + h * 0.065f), h * 0.004f)
    if (halls >= 2) {
        val n = 16
        for (k in 0 until n) {
            val x = left + (right - left) * (k + 0.5f) / n
            if (k % 2 == 0) drawRect(p.clay.copy(alpha = 0.8f), Offset(x - w * 0.012f, roofY + h * 0.047f), Size(w * 0.024f, h * 0.036f))
            else for (t in 0..2) drawLine(p.night.copy(alpha = 0.5f), Offset(x - w * 0.008f + t * w * 0.008f, roofY + h * 0.048f), Offset(x - w * 0.008f + t * w * 0.008f, roofY + h * 0.082f), w * 0.003f)
        }
    }
    // Pediment.
    val ped = Path().apply {
        moveTo(left - w * 0.04f, roofY + h * 0.04f)
        lineTo(w / 2, h * 0.12f)
        lineTo(right + w * 0.04f, roofY + h * 0.04f)
        close()
    }
    drawPath(ped, if (halls >= 7) p.goldSoft else p.limestone)
    drawPath(ped, p.stone, style = Stroke(h * 0.012f, join = StrokeJoin.Miter))
    // A seal in the pediment, and a figure for each hall past the fourth.
    drawCircle(p.clay, h * 0.04f, Offset(w / 2, roofY - h * 0.05f))
    glyph(9, p.limestone, Rect(w / 2 - h * 0.025f, roofY - h * 0.075f, w / 2 + h * 0.025f, roofY - h * 0.025f), 0.14f)
    if (halls >= 1) for (s in listOf(-1f, 1f)) {
        val x = w / 2 + s * (w / 2 - left + w * 0.04f)
        drawCircle(p.gold, h * 0.018f, Offset(x, roofY + h * 0.02f))
    }
    if (halls >= 1) drawCircle(p.gold, h * 0.02f, Offset(w / 2, h * 0.105f))
    if (halls >= 4) for (s in listOf(-1f, 1f)) statue(p, Offset(w / 2 + s * w * 0.47f, base - stepH * 3), h * 0.3f)
    if (halls >= 5) for (s in listOf(-1f, 1f)) {
        val c = Offset(w / 2 + s * w * 0.16f, roofY - h * 0.0f)
        drawCircle(p.bronze.copy(alpha = 0.7f), h * 0.015f, c)
    }
    if (halls >= 6) {
        // Banners between the columns.
        listOf(0, gaps.size - 1).distinct().forEach { i ->
            val g = gaps.getOrNull(i) ?: return@forEach
            drawRect(p.clay, Offset(g.left + g.width * 0.25f, g.top + g.height * 0.02f), Size(g.width * 0.5f, g.height * 0.28f))
            glyph(i * 3, p.goldSoft, Rect(g.left + g.width * 0.3f, g.top + g.height * 0.08f, g.right - g.width * 0.3f, g.top + g.height * 0.24f), 0.12f)
        }
    }
}

private fun DrawScope.statue(p: Palette, foot: Offset, height: Float) {
    val u = height
    drawRect(p.stone, Offset(foot.x - u * 0.12f, foot.y - u * 0.18f), Size(u * 0.24f, u * 0.18f))
    val body = Path().apply {
        moveTo(foot.x - u * 0.08f, foot.y - u * 0.18f)
        lineTo(foot.x - u * 0.06f, foot.y - u * 0.7f)
        quadraticTo(foot.x, foot.y - u * 0.78f, foot.x + u * 0.06f, foot.y - u * 0.7f)
        lineTo(foot.x + u * 0.08f, foot.y - u * 0.18f)
        close()
    }
    drawPath(body, p.limestone)
    drawCircle(p.limestone, u * 0.07f, Offset(foot.x, foot.y - u * 0.84f))
    drawLine(p.stone, Offset(foot.x - u * 0.02f, foot.y - u * 0.6f), Offset(foot.x + u * 0.03f, foot.y - u * 0.22f), u * 0.012f)
}

/** A small vessel inside a scene: no cutting, just its stage colour and how much of it is in. */
fun DrawScope.drawMiniVessel(p: Palette, shape: Shape, shards: List<ShardLook>, r: Rect, seed: Int) {
    val box = fit(shape, r.size).translate(r.topLeft)
    val body = silhouette(shape, box)
    val pieces = cut(shards.size.coerceAtLeast(1), box, seed)
    val dirty = lerp(p.clay, p.earth, 0.55f)
    drawPath(body, p.night.copy(alpha = 0.5f))
    clipPath(body) {
        pieces.forEachIndexed { i, piece ->
            val s = shards.getOrNull(i) ?: return@forEachIndexed
            if (s.stage == app.stele.domain.Stage.GROUND) return@forEachIndexed
            drawPath(piece, if (s.stage == app.stele.domain.Stage.SHARD) dirty else p.clay)
            if (s.stage >= app.stele.domain.Stage.RESTORED) drawPath(piece, p.night.copy(alpha = 0.22f))
            if (s.stage == app.stele.domain.Stage.EXHIBIT) drawPath(piece, p.gold.copy(alpha = 0.35f))
        }
        drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, p.night.copy(alpha = 0.3f)), box.left, box.right), box.topLeft, box.size)
    }
}

/** The sealed door: a stone slab with a clay seal, its halves parting as [open] goes from 0 to 1. */
@Composable
fun SealedDoor(open: Float, modifier: Modifier = Modifier, glyphIndex: Int = 6) {
    val p = U.c
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val arch = Rect(w * 0.14f, h * 0.06f, w * 0.86f, h * 0.96f)
        // Frame of dressed stone.
        drawRect(p.stone, Offset(arch.left - w * 0.07f, arch.top - h * 0.04f), Size(arch.width + w * 0.14f, arch.height + h * 0.04f))
        drawRect(p.limestone, Offset(arch.left - w * 0.1f, arch.top - h * 0.07f), Size(arch.width + w * 0.2f, h * 0.05f))
        // The light behind.
        drawRect(Brush.verticalGradient(listOf(p.goldSoft, p.gold, p.ochre)), arch.topLeft, arch.size)
        if (open > 0f) for (k in 0 until 7) {
            val a = -PI.toFloat() / 2 + (k - 3) * 0.22f
            drawLine(p.goldSoft.copy(alpha = 0.6f * open), arch.center, Offset(arch.center.x + cos(a) * w, arch.center.y + sin(a) * h), w * 0.05f, cap = StrokeCap.Round)
        }
        val half = arch.width / 2
        val shift = half * 0.92f * open
        for (s in listOf(-1, 1)) {
            val x0 = if (s < 0) arch.left - shift else arch.center.x + shift
            clipRect(arch.left, arch.top, arch.right, arch.bottom) {
                drawRect(Brush.horizontalGradient(listOf(p.stone, p.limestone, p.stone), x0, x0 + half), Offset(x0, arch.top), Size(half, arch.height))
                for (k in 1..4) drawLine(p.night.copy(alpha = 0.12f), Offset(x0, arch.top + arch.height * k / 5), Offset(x0 + half, arch.top + arch.height * k / 5), w * 0.006f)
                drawRect(p.night.copy(alpha = 0.18f), Offset(if (s < 0) x0 + half - w * 0.01f else x0, arch.top), Size(w * 0.01f, arch.height))
            }
        }
        // The seal across the seam, breaking as the door opens.
        val c = Offset(arch.center.x, arch.top + arch.height * 0.45f)
        val r = w * 0.13f
        if (open < 1f) {
            val a = 1f - open
            for (s in listOf(-1, 1)) {
                rotate(s * 25f * open, Offset(c.x + s * r * 0.2f, c.y)) {
                    clipRect(if (s < 0) 0f else c.x + s * shift * 0.2f, 0f, if (s < 0) c.x - shift * 0.2f else w, h) {
                        drawCircle(p.clay.copy(alpha = a), r, Offset(c.x + s * shift, c.y))
                        drawCircle(p.night.copy(alpha = 0.25f * a), r * 0.78f, Offset(c.x + s * shift, c.y), style = Stroke(r * 0.07f))
                        glyph(glyphIndex, p.limestone.copy(alpha = a), Rect(c.x + s * shift - r * 0.5f, c.y - r * 0.5f, c.x + s * shift + r * 0.5f, c.y + r * 0.5f), 0.12f)
                    }
                }
            }
        }
    }
}

/** A chisel, for the strikes staked before looking. */
fun DrawScope.chisel(color: Color, r: Rect) {
    val u = min(r.width, r.height)
    rotate(-40f, r.center) {
        val cx = r.center.x
        drawRoundRect(color, Offset(cx - u * 0.1f, r.top + u * 0.04f), Size(u * 0.2f, u * 0.42f), androidx.compose.ui.geometry.CornerRadius(u * 0.06f))
        drawRect(color.copy(alpha = 0.7f), Offset(cx - u * 0.07f, r.top + u * 0.48f), Size(u * 0.14f, u * 0.3f))
        drawPath(Path().apply {
            moveTo(cx - u * 0.07f, r.top + u * 0.78f); lineTo(cx + u * 0.07f, r.top + u * 0.78f); lineTo(cx, r.top + u * 0.96f); close()
        }, color.copy(alpha = 0.7f))
    }
}

/**
 * Dust over whatever lies beneath, swept off by a brush from left to right as [cleared] goes from 0 to 1.
 */
@Composable
fun Dust(cleared: Float, modifier: Modifier = Modifier, seed: Int = 0) {
    val p = U.c
    Box(modifier.drawWithCache {
        val rnd = java.util.Random(seed * 17L + 5)
        val grains = List(520) { Triple(rnd.nextFloat(), rnd.nextFloat(), 0.4f + rnd.nextFloat()) }
        val clumps = List(26) { Triple(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat()) }
        onDrawBehind {
            val edge = size.width * (cleared * 1.15f - 0.05f)
            val u = size.minDimension
            if (cleared < 1f) {
                clipRect(left = edge.coerceAtLeast(0f)) {
                    drawRect(lerp(p.surface2, p.earth, 0.35f))
                    clumps.forEach { (x, y, k) -> drawCircle(p.earth.copy(alpha = 0.35f), u * 0.09f * k, Offset(x * size.width, y * size.height)) }
                    grains.forEach { (x, y, k) -> drawCircle(if (k > 1.1f) p.earth else p.stone, u * 0.012f * k, Offset(x * size.width, y * size.height)) }
                }
            }
            if (cleared in 0.001f..0.999f) {
                // The flying dust at the sweep, and the brush.
                grains.take(80).forEach { (x, y, k) ->
                    val px = edge - u * 0.25f * x
                    drawCircle(p.earth.copy(alpha = 0.4f * (1 - x)), u * 0.01f * k, Offset(px, y * size.height - u * 0.1f * x))
                }
                val bx = edge
                val by = size.height * 0.5f
                drawRoundRect(p.ochre, Offset(bx - u * 0.06f, by - u * 0.32f), Size(u * 0.12f, u * 0.4f), androidx.compose.ui.geometry.CornerRadius(u * 0.04f))
                drawRect(p.goldDeep, Offset(bx - u * 0.065f, by + u * 0.06f), Size(u * 0.13f, u * 0.07f))
                for (k in 0..6) {
                    val x = bx - u * 0.06f + k * u * 0.02f
                    drawLine(p.night.copy(alpha = 0.7f), Offset(x, by + u * 0.13f), Offset(x - u * 0.03f, by + u * 0.3f), u * 0.012f, cap = StrokeCap.Round)
                }
            }
        }
    })
}
