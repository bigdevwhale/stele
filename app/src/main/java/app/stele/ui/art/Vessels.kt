package app.stele.ui.art

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import app.stele.domain.Shape
import app.stele.domain.Stage
import app.stele.ui.theme.Palette
import app.stele.ui.theme.U
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** One shard of a vessel: the stage of the find it stands for, and how much dust lies on it. */
@Immutable
data class ShardLook(val stage: Stage, val patina: Float = 0f)

/**
 * A vessel drawn from its shards. Each find is one shard: missing while still in the ground, dirty as a
 * shard, clean terracotta once cleaned, painted with its ornament once restored, glinting on show.
 * A mended vessel has its seams laid in gold, the way kintsugi mends a bowl.
 */
@Composable
fun VesselArt(shape: Shape, shards: List<ShardLook>, mended: Boolean, modifier: Modifier = Modifier, seed: Int = 0) {
    val p = U.c
    Box(modifier.drawWithCache {
        val box = fit(shape, size)
        val body = silhouette(shape, box)
        val pieces = cut(shards.size.coerceAtLeast(1), box, seed).map { piece -> Path().apply { op(piece, body, PathOperation.Intersect) } }
        val handle = handles(shape, box)
        onDrawBehind {
            drawVessel(p, shape, box, body, handle, pieces, shards, mended, seed)
        }
    })
}

/** The box a shape sits in within [size], keeping its proportions. */
fun fit(shape: Shape, size: Size): Rect {
    val aspect = when (shape) {
        Shape.AMPHORA -> 0.62f
        Shape.KRATER -> 0.95f
        Shape.JUG -> 0.7f
        Shape.BOWL -> 1.6f
        Shape.LAMP -> 1.5f
        Shape.MASK -> 0.78f
        Shape.COIN -> 1f
        Shape.TABLET -> 0.78f
    }
    var w = size.width
    var h = w / aspect
    if (h > size.height) { h = size.height; w = h * aspect }
    // Room for handles that reach out of the body.
    val k = if (shape == Shape.BOWL || shape == Shape.KRATER) 0.84f else 0.92f
    w *= k; h *= k
    return Rect(Offset((size.width - w) / 2, (size.height - h) / 2), Size(w, h))
}

private fun profile(shape: Shape): List<Pair<Float, Float>>? = when (shape) {
    Shape.AMPHORA -> listOf(0f to .15f, .035f to .15f, .06f to .1f, .2f to .095f, .3f to .3f, .45f to .4f, .62f to .35f, .8f to .19f, .9f to .085f, .94f to .12f, 1f to .13f)
    Shape.KRATER -> listOf(0f to .5f, .06f to .47f, .3f to .37f, .55f to .33f, .74f to .24f, .82f to .12f, .9f to .11f, .95f to .22f, 1f to .23f)
    Shape.JUG -> listOf(0f to .17f, .06f to .17f, .12f to .09f, .26f to .09f, .38f to .3f, .56f to .37f, .76f to .3f, .9f to .17f, .95f to .15f, 1f to .17f)
    Shape.BOWL -> listOf(.22f to .5f, .3f to .49f, .52f to .4f, .64f to .2f, .7f to .07f, .86f to .06f, .94f to .2f, 1f to .22f)
    else -> null
}

/** A smooth outline through the half-profile and its mirror. */
private fun lathe(prof: List<Pair<Float, Float>>, r: Rect): Path {
    val right = prof.map { (y, hw) -> Offset(r.left + r.width * (0.5f + hw), r.top + r.height * y) }
    val left = prof.reversed().map { (y, hw) -> Offset(r.left + r.width * (0.5f - hw), r.top + r.height * y) }
    val pts = right + left
    return Path().apply {
        moveTo((pts.last().x + pts[0].x) / 2, (pts.last().y + pts[0].y) / 2)
        for (i in pts.indices) {
            val a = pts[i]
            val b = pts[(i + 1) % pts.size]
            quadraticTo(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2)
        }
        close()
    }
}

fun silhouette(shape: Shape, r: Rect): Path {
    profile(shape)?.let { return lathe(it, r) }
    val w = r.width
    val h = r.height
    return when (shape) {
        Shape.COIN -> Path().apply { addOval(r) }
        Shape.TABLET -> Path().apply {
            moveTo(r.left + w * .06f, r.top + h * .03f)
            quadraticTo(r.left + w * .5f, r.top - h * .02f, r.right - w * .05f, r.top + h * .05f)
            quadraticTo(r.right + w * .02f, r.top + h * .5f, r.right - w * .04f, r.bottom - h * .04f)
            quadraticTo(r.left + w * .5f, r.bottom + h * .015f, r.left + w * .05f, r.bottom - h * .03f)
            quadraticTo(r.left - w * .02f, r.top + h * .5f, r.left + w * .06f, r.top + h * .03f)
            close()
        }
        Shape.LAMP -> Path().apply {
            addOval(Rect(r.left + w * .08f, r.top + h * .28f, r.left + w * .7f, r.bottom - h * .1f))
            val nozzle = Path().apply {
                moveTo(r.left + w * .55f, r.top + h * .38f)
                quadraticTo(r.left + w * .85f, r.top + h * .36f, r.right - w * .02f, r.top + h * .5f)
                quadraticTo(r.right, r.top + h * .62f, r.right - w * .06f, r.top + h * .66f)
                quadraticTo(r.left + w * .8f, r.top + h * .72f, r.left + w * .55f, r.bottom - h * .16f)
                close()
            }
            op(this, nozzle, PathOperation.Union)
        }
        Shape.MASK -> {
            val face = Path().apply {
                moveTo(r.left + w * .5f, r.top)
                cubicTo(r.left + w * .9f, r.top, r.right, r.top + h * .2f, r.right - w * .02f, r.top + h * .42f)
                cubicTo(r.right - w * .04f, r.top + h * .72f, r.left + w * .72f, r.bottom, r.left + w * .5f, r.bottom)
                cubicTo(r.left + w * .28f, r.bottom, r.left + w * .04f, r.top + h * .72f, r.left + w * .02f, r.top + h * .42f)
                cubicTo(r.left, r.top + h * .2f, r.left + w * .1f, r.top, r.left + w * .5f, r.top)
                close()
            }
            val holes = Path().apply {
                addOval(Rect(r.left + w * .2f, r.top + h * .33f, r.left + w * .42f, r.top + h * .43f))
                addOval(Rect(r.left + w * .58f, r.top + h * .33f, r.left + w * .8f, r.top + h * .43f))
                addOval(Rect(r.left + w * .36f, r.top + h * .7f, r.left + w * .64f, r.top + h * .78f))
            }
            Path().apply { op(face, holes, PathOperation.Difference) }
        }
        else -> Path().apply { addRect(r) }
    }
}

/** Handles stand apart from the body: drawn as thick strokes of the same clay, not cut into shards. */
private fun handles(shape: Shape, r: Rect): Path? {
    val w = r.width
    val h = r.height
    fun x(f: Float) = r.left + w * f
    fun y(f: Float) = r.top + h * f
    return when (shape) {
        Shape.AMPHORA -> Path().apply {
            for (s in listOf(-1f, 1f)) {
                moveTo(x(.5f + s * .09f), y(.09f))
                cubicTo(x(.5f + s * .34f), y(.06f), x(.5f + s * .36f), y(.2f), x(.5f + s * .25f), y(.31f))
            }
        }
        Shape.KRATER -> Path().apply {
            for (s in listOf(-1f, 1f)) {
                moveTo(x(.5f + s * .36f), y(.34f))
                cubicTo(x(.5f + s * .56f), y(.3f), x(.5f + s * .56f), y(.14f), x(.5f + s * .4f), y(.2f))
            }
        }
        Shape.JUG -> Path().apply {
            moveTo(x(.6f), y(.05f))
            cubicTo(x(.88f), y(.0f), x(.92f), y(.3f), x(.82f), y(.42f))
        }
        Shape.BOWL -> Path().apply {
            for (s in listOf(-1f, 1f)) {
                moveTo(x(.5f + s * .42f), y(.42f))
                quadraticTo(x(.5f + s * .6f), y(.38f), x(.5f + s * .48f), y(.27f))
            }
        }
        Shape.LAMP -> Path().apply {
            moveTo(x(.12f), y(.42f))
            cubicTo(x(-.04f), y(.3f), x(.0f), y(.62f), x(.14f), y(.66f))
        }
        else -> null
    }
}

/**
 * Cuts the box into [n] pieces along jagged seams: rows of shards whose top and bottom edges are shared,
 * so the pieces fit together like a broken pot.
 */
fun cut(n: Int, r: Rect, seed: Int): List<Path> {
    val rnd = java.util.Random(seed * 31L + n)
    val rows = ceil(sqrt(n * r.height / r.width.toDouble() * 0.8)).toInt().coerceIn(1, n)
    val perRow = IntArray(rows) { n / rows }.also { a -> repeat(n % rows) { a[it]++ } }
    val steps = 7
    // Each seam between rows is a function of x; the outer ones lie beyond the box.
    val seams = (0..rows).map { i ->
        val base = r.top + r.height * i / rows
        if (i == 0 || i == rows) FloatArray(steps + 1) { base + (if (i == 0) -2 else 2) * r.height }
        else FloatArray(steps + 1) { base + (rnd.nextFloat() - 0.5f) * r.height / rows * 0.55f }
    }
    fun seamY(i: Int, x: Float): Float {
        val t = ((x - r.left) / r.width * steps).coerceIn(0f, steps.toFloat())
        val k = t.toInt().coerceAtMost(steps - 1)
        val f = t - k
        return seams[i][k] * (1 - f) + seams[i][k + 1] * f
    }
    val out = mutableListOf<Path>()
    for (row in 0 until rows) {
        val cols = perRow[row]
        val offset = rnd.nextFloat() * 0.3f - 0.15f
        // Each upright seam: a few points from the top seam down to the bottom one.
        val cuts = (0..cols).map { j ->
            if (j == 0 || j == cols) {
                val x = if (j == 0) r.left - r.width else r.right + r.width
                listOf(Offset(x, seamY(row, x)), Offset(x, seamY(row + 1, x)))
            } else {
                val x0 = r.left + r.width * ((j + offset * (if (cols > 1) 1 else 0)) / cols)
                val pts = (0..3).map { k ->
                    val x = x0 + (rnd.nextFloat() - 0.5f) * r.width / cols * 0.35f
                    val t = k / 3f
                    Offset(x, seamY(row, x) * (1 - t) + seamY(row + 1, x) * t)
                }
                pts
            }
        }
        for (j in 0 until cols) {
            val left = cuts[j]
            val right = cuts[j + 1]
            out += Path().apply {
                moveTo(left.first().x, left.first().y)
                val xs = left.first().x
                val xe = right.first().x
                for (k in 1..12) { val x = xs + (xe - xs) * k / 12; lineTo(x, seamY(row, x)) }
                right.drop(1).forEach { lineTo(it.x, it.y) }
                val bs = right.last().x
                val be = left.last().x
                for (k in 1..12) { val x = bs + (be - bs) * k / 12; lineTo(x, seamY(row + 1, x)) }
                left.reversed().drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
        }
    }
    return out
}

private fun DrawScope.drawVessel(
    p: Palette, shape: Shape, box: Rect, body: Path, handle: Path?, pieces: List<Path>, shards: List<ShardLook>, mended: Boolean, seed: Int,
) {
    val unit = min(box.width, box.height)
    val clay = p.clay
    val dirty = lerp(p.clay, p.earth, 0.55f)
    val seam = if (mended) p.gold else p.night.copy(alpha = 0.55f)
    // The ground shadow.
    drawOval(p.night.copy(alpha = 0.1f), Offset(box.left + box.width * 0.18f, box.bottom - unit * 0.035f), Size(box.width * 0.64f, unit * 0.07f))
    val anyThere = shards.any { it.stage != Stage.GROUND }
    if (handle != null) {
        drawPath(handle, if (anyThere) clay else p.line, style = Stroke(unit * 0.045f, cap = StrokeCap.Round))
    }
    // The place where the vessel will stand: its outline in the sand.
    drawPath(body, p.surface2.copy(alpha = 0.7f))
    drawPath(body, p.earth.copy(alpha = 0.5f), style = Stroke(unit * 0.008f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(unit * 0.03f, unit * 0.025f))))
    val rnd = java.util.Random(seed * 7L + 3)
    pieces.forEachIndexed { i, piece ->
        val s = shards.getOrNull(i) ?: return@forEachIndexed
        if (s.stage == Stage.GROUND) {
            drawPath(piece, p.earth.copy(alpha = 0.12f))
            return@forEachIndexed
        }
        drawPath(piece, if (s.stage == Stage.SHARD) dirty else clay)
        clipPath(piece) {
            if (s.stage >= Stage.RESTORED) ornament(p, shape, box)
            if (s.stage == Stage.SHARD) speckle(p.earth.copy(alpha = 0.55f), piece.getBounds(), unit * 0.012f, 26, rnd)
            if (s.patina > 0f) {
                drawRect(Verdigris.copy(alpha = 0.35f + 0.4f * s.patina), piece.getBounds().topLeft, piece.getBounds().size)
                speckle(lerp(p.bronze, p.limestone, 0.2f).copy(alpha = 0.85f * s.patina), piece.getBounds(), unit * 0.016f, (60 * s.patina).toInt() + 8, rnd)
            }
            if (s.stage == Stage.EXHIBIT) {
                val b = piece.getBounds()
                drawCircle(p.goldSoft.copy(alpha = 0.85f), unit * 0.012f, Offset(b.left + b.width * (0.3f + rnd.nextFloat() * 0.4f), b.top + b.height * (0.3f + rnd.nextFloat() * 0.4f)))
            }
        }
    }
    // Volume: light from the left, shade to the right.
    clipPath(body) {
        drawRect(
            Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0.0f), 0.22f to Color.White.copy(alpha = 0.2f), 0.45f to Color.White.copy(alpha = 0f),
                0.75f to p.night.copy(alpha = 0.08f), 1f to p.night.copy(alpha = 0.28f),
                startX = box.left, endX = box.right,
            ),
            box.topLeft, box.size,
        )
    }
    // Seams between the shards that are in, gold when the vessel is whole.
    pieces.forEachIndexed { i, piece ->
        val s = shards.getOrNull(i) ?: return@forEachIndexed
        if (s.stage == Stage.GROUND) {
            drawPath(piece, p.earth.copy(alpha = 0.4f), style = Stroke(unit * 0.006f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(unit * 0.02f, unit * 0.02f))))
        } else {
            drawPath(piece, seam, style = Stroke(unit * if (mended) 0.014f else 0.008f, join = StrokeJoin.Round))
        }
    }
    if (anyThere) drawPath(body, p.night.copy(alpha = 0.35f), style = Stroke(unit * 0.008f))
}

/** The painted ornament of a vessel, in black-figure umber over the clay. */
private fun DrawScope.ornament(p: Palette, shape: Shape, r: Rect) {
    val ink = p.night.copy(alpha = 0.88f)
    val w = r.width
    val h = r.height
    val unit = min(w, h)
    val line = Stroke(unit * 0.012f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
    fun band(y: Float, height: Float) {
        drawLine(ink, Offset(r.left, y), Offset(r.right, y), unit * 0.012f)
        drawLine(ink, Offset(r.left, y + height), Offset(r.right, y + height), unit * 0.012f)
    }
    fun meander(y: Float, height: Float) {
        band(y, height)
        val c = height * 1.4f
        var x = r.left
        val path = Path()
        val t = y + height * 0.2f
        val b = y + height * 0.8f
        while (x < r.right) {
            path.moveTo(x, b)
            path.lineTo(x, t)
            path.lineTo(x + c * 0.7f, t)
            path.lineTo(x + c * 0.7f, b - height * 0.25f)
            path.lineTo(x + c * 0.3f, b - height * 0.25f)
            path.lineTo(x + c * 0.3f, t + height * 0.28f)
            path.moveTo(x, b)
            path.lineTo(x + c, b)
            x += c
        }
        drawPath(path, ink, style = line)
    }
    fun wave(y: Float, height: Float) {
        band(y - height * 0.1f, height * 1.2f)
        val path = Path()
        val steps = 80
        for (i in 0..steps) {
            val x = r.left + w * i / steps
            val yy = y + height / 2 + sin(i / steps.toFloat() * 2 * PI.toFloat() * 7) * height * 0.32f
            if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
        }
        drawPath(path, ink, style = line)
    }
    fun rays(y: Float, height: Float) {
        val n = 14
        for (i in 0 until n) {
            val x = r.left + w * (i + 0.5f) / n
            drawPath(Path().apply { moveTo(x - w / n * 0.32f, y + height); lineTo(x, y); lineTo(x + w / n * 0.32f, y + height); close() }, ink)
        }
    }
    fun dots(y: Float) {
        val n = 18
        for (i in 0 until n) drawCircle(ink, unit * 0.013f, Offset(r.left + w * (i + 0.5f) / n, y))
    }
    when (shape) {
        Shape.AMPHORA -> { meander(r.top + h * .33f, h * .05f); wave(r.top + h * .5f, h * .07f); dots(r.top + h * .66f); rays(r.top + h * .7f, h * .1f) }
        Shape.KRATER -> { meander(r.top + h * .1f, h * .07f); wave(r.top + h * .4f, h * .08f); rays(r.top + h * .62f, h * .1f) }
        Shape.JUG -> { dots(r.top + h * .3f); meander(r.top + h * .44f, h * .06f); wave(r.top + h * .62f, h * .06f) }
        Shape.BOWL -> { meander(r.top + h * .33f, h * .08f); dots(r.top + h * .52f) }
        Shape.LAMP -> {
            val c = Offset(r.left + w * .39f, r.top + h * .55f)
            for (i in 0 until 8) {
                val a = i * PI.toFloat() / 4
                drawLine(ink, c, Offset(c.x + cos(a) * w * .14f, c.y + sin(a) * h * .16f), unit * 0.02f, cap = StrokeCap.Round)
            }
            drawCircle(p.clay, unit * 0.05f, c)
            drawCircle(ink, unit * 0.05f, c, style = line)
        }
        Shape.MASK -> {
            drawArc(ink, 200f, 140f, false, Offset(r.left + w * .16f, r.top + h * .24f), Size(w * .3f, h * .12f), style = line)
            drawArc(ink, 200f, 140f, false, Offset(r.left + w * .54f, r.top + h * .24f), Size(w * .3f, h * .12f), style = line)
            for (s in listOf(-1f, 1f)) {
                val c = Offset(r.left + w * (0.5f + s * .28f), r.top + h * .6f)
                val sp = Path()
                for (i in 0..40) {
                    val a = i / 40f * 4 * PI.toFloat()
                    val rr = unit * 0.07f * (1 - i / 44f)
                    val pt = Offset(c.x + cos(a) * rr * s, c.y + sin(a) * rr)
                    if (i == 0) sp.moveTo(pt.x, pt.y) else sp.lineTo(pt.x, pt.y)
                }
                drawPath(sp, ink, style = line)
            }
        }
        Shape.COIN -> {
            val c = r.center
            drawCircle(ink, w * .42f, c, style = line)
            drawCircle(ink, w * .36f, c, style = Stroke(unit * 0.008f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(unit * 0.02f, unit * 0.02f))))
            val star = Path()
            for (i in 0 until 16) {
                val a = i * PI.toFloat() / 8 - PI.toFloat() / 2
                val rr = if (i % 2 == 0) w * .26f else w * .1f
                val pt = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr)
                if (i == 0) star.moveTo(pt.x, pt.y) else star.lineTo(pt.x, pt.y)
            }
            star.close()
            drawPath(star, ink)
        }
        Shape.TABLET -> {
            val rnd = java.util.Random(5)
            var y = r.top + h * .1f
            while (y < r.bottom - h * .08f) {
                drawLine(ink.copy(alpha = 0.5f), Offset(r.left + w * .06f, y + h * .07f), Offset(r.right - w * .06f, y + h * .07f), unit * 0.006f)
                var x = r.left + w * .1f
                while (x < r.right - w * .1f) {
                    val k = unit * (0.025f + rnd.nextFloat() * 0.02f)
                    drawPath(Path().apply { moveTo(x, y); lineTo(x + k, y + k * 0.4f); lineTo(x, y + k * 1.1f); close() }, ink)
                    if (rnd.nextBoolean()) drawLine(ink, Offset(x + k * 0.3f, y + k * 0.5f), Offset(x + k * 1.6f, y + k * 0.5f), unit * 0.008f)
                    x += k * 2.2f
                }
                y += h * .11f
            }
        }
    }
}

private fun DrawScope.speckle(color: Color, b: Rect, r: Float, n: Int, rnd: java.util.Random) {
    repeat(n) {
        drawCircle(color, r * (0.4f + rnd.nextFloat()), Offset(b.left + rnd.nextFloat() * b.width, b.top + rnd.nextFloat() * b.height))
    }
}

/** The pale green bloom old clay and bronze take on when left alone. */
private val Verdigris = Color(0xFF7FAE9C)

fun lerp(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, a.alpha + (b.alpha - a.alpha) * t,
)

/** A single shard, for a find on its own: its outline, colour and condition follow its stage. */
@Composable
fun ShardArt(stage: Stage, modifier: Modifier = Modifier, patina: Float = 0f, cracked: Boolean = false, seed: Int = 0) {
    val p = U.c
    Box(modifier.drawWithCache {
        val rnd = java.util.Random(seed * 13L + 1)
        val c = Offset(size.width / 2, size.height / 2)
        val rad = min(size.width, size.height) * 0.44f
        val n = 6 + rnd.nextInt(3)
        val start = rnd.nextFloat() * 6.28f
        val path = Path().apply {
            for (i in 0 until n) {
                val a = start + i * 2 * PI.toFloat() / n + (rnd.nextFloat() - 0.5f) * 0.5f
                val rr = rad * (0.68f + rnd.nextFloat() * 0.32f)
                val pt = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr * 0.86f)
                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
            close()
            fillType = PathFillType.NonZero
        }
        val b = path.getBounds()
        val crack = Path().apply {
            moveTo(b.left + b.width * 0.3f, b.top + b.height * 0.1f)
            lineTo(b.left + b.width * 0.45f, b.top + b.height * 0.42f)
            lineTo(b.left + b.width * 0.38f, b.top + b.height * 0.6f)
            lineTo(b.left + b.width * 0.58f, b.bottom - b.height * 0.1f)
        }
        onDrawBehind {
            val unit = rad
            when (stage) {
                Stage.GROUND -> {
                    drawOval(p.earth.copy(alpha = 0.35f), Offset(c.x - rad, c.y + rad * 0.15f), Size(rad * 2, rad * 0.8f))
                    drawPath(path, p.earth.copy(alpha = 0.75f), style = Stroke(unit * 0.07f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(unit * 0.16f, unit * 0.12f))))
                }
                else -> {
                    val fill = when (stage) {
                        Stage.SHARD -> lerp(p.clay, p.earth, 0.55f)
                        Stage.EXHIBIT -> p.clay
                        else -> p.clay
                    }
                    drawPath(path, fill)
                    clipPath(path) {
                        val rr = java.util.Random(seed.toLong())
                        if (stage == Stage.SHARD) speckle(p.earth.copy(alpha = 0.6f), b, unit * 0.05f, 12, rr)
                        if (stage >= Stage.RESTORED) {
                            drawLine(p.night.copy(alpha = 0.85f), Offset(b.left, c.y - unit * 0.14f), Offset(b.right, c.y - unit * 0.14f), unit * 0.07f)
                            drawLine(p.night.copy(alpha = 0.85f), Offset(b.left, c.y + unit * 0.18f), Offset(b.right, c.y + unit * 0.18f), unit * 0.07f)
                            for (k in 0..4) drawCircle(p.night.copy(alpha = 0.85f), unit * 0.06f, Offset(b.left + b.width * (0.1f + k * 0.2f), c.y + unit * 0.02f))
                        }
                        if (patina > 0f) {
                            drawRect(Verdigris.copy(alpha = 0.35f + 0.4f * patina), b.topLeft, b.size)
                            speckle(lerp(p.bronze, p.limestone, 0.4f).copy(alpha = 0.8f * patina), b, unit * 0.06f, (14 * patina).toInt() + 1, rr)
                        }
                        drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, p.night.copy(alpha = 0.22f)), b.left, b.right), b.topLeft, b.size)
                    }
                    if (cracked) drawPath(crack, p.night.copy(alpha = 0.8f), style = Stroke(unit * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, if (stage == Stage.EXHIBIT) p.gold else p.night.copy(alpha = 0.4f), style = Stroke(unit * if (stage == Stage.EXHIBIT) 0.1f else 0.04f, join = StrokeJoin.Round))
                    if (stage == Stage.EXHIBIT) drawCircle(p.goldSoft, unit * 0.09f, Offset(b.left + b.width * 0.32f, b.top + b.height * 0.3f))
                }
            }
        }
    })
}

/** A rounded placard plinth's top, used under exhibits. */
fun DrawScope.plinth(p: Palette, r: Rect) {
    val cap = r.height * 0.18f
    drawRoundRect(p.night.copy(alpha = 0.08f), Offset(r.left + r.width * 0.04f, r.bottom - cap * 0.3f), Size(r.width * 0.92f, cap * 0.6f))
    drawRect(Brush.horizontalGradient(listOf(p.limestone, p.stone, p.limestone.copy(alpha = 0.9f), p.stone), r.left, r.right), Offset(r.left + r.width * 0.1f, r.top + cap), Size(r.width * 0.8f, r.height - cap * 2))
    drawPath(Path().apply { addRoundRect(RoundRect(r.left, r.top, r.right, r.top + cap, cap * 0.2f, cap * 0.2f)) }, p.limestone)
    drawRect(p.stone, Offset(r.left, r.bottom - cap), Size(r.width, cap))
    drawLine(p.line, Offset(r.left, r.top + cap), Offset(r.right, r.top + cap), cap * 0.08f)
}
