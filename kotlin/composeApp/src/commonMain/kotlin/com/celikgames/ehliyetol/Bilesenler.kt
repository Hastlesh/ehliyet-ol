package com.celikgames.ehliyetol

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.celikgames.ehliyetol.kaynak.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

val DERS_IKON: List<ImageVector> = listOf(Icons.Rounded.MedicalServices, Icons.Rounded.DirectionsCar, Icons.Rounded.Build, Icons.Rounded.Groups)

@Composable fun dersRenk(d: Int): Color = LocalRenk.current.dersler[d.coerceIn(0, 3)]

/**
 * Sayfa iskeleti: üst çubuk + kaydırılan içerik + (isteğe bağlı) alt çubuk + şerit reklam + sekmeler.
 * Reklam yalnız izinli ekranlarda (ana, konular, istatistik, sonuç, ders) görünür.
 */
@Composable
fun Sayfa(
    d: UygulamaDurum, baslik: String?, geri: Boolean = false,
    eylemler: @Composable RowScope.() -> Unit = {},
    alt: (@Composable () -> Unit)? = null,
    sekme: Ekran? = null,
    kaydirma: androidx.compose.foundation.ScrollState = rememberScrollState(),
    icerik: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (baslik != null) UstCubuk(baslik, if (geri) ({ d.geri() }) else null, eylemler)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(kaydirma).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = icerik,
        )
        alt?.invoke()
        if (sekme != null) Sekmeler(d, sekme)
    }
}

@Composable
fun UstCubuk(baslik: String, geri: (() -> Unit)?, eylemler: @Composable RowScope.() -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (geri != null) IconButton(onClick = geri) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
            else Spacer(Modifier.width(12.dp))
            Text(baslik, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            eylemler()
        }
    }
}

@Composable
fun Sekmeler(d: UygulamaDurum, secili: Ekran) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        listOf(
            Triple(Ekran.Ana, Icons.Rounded.Home, "Ana sayfa"),
            Triple(Ekran.Konular, Icons.Rounded.Headphones, "Konular"),
            Triple(Ekran.Istatistik, Icons.Rounded.BarChart, "İstatistik"),
        ).forEach { (e, ikon, ad) ->
            NavigationBarItem(selected = secili == e, onClick = { d.git(e) }, icon = { Icon(ikon, null) }, label = { Text(ad) })
        }
    }
}

@Composable
fun Kart(modifier: Modifier = Modifier, kenar: Color? = null, tikla: (() -> Unit)? = null, icerik: @Composable ColumnScope.() -> Unit) {
    val s = RoundedCornerShape(16.dp)
    val m = modifier.fillMaxWidth().clip(s).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, kenar ?: MaterialTheme.colorScheme.outline, s)
        .let { if (tikla != null) it.clickable(onClick = tikla) else it }.padding(16.dp)
    Column(m, verticalArrangement = Arrangement.spacedBy(8.dp), content = icerik)
}

@Composable
fun Bolum(metin: String, renk: Color = MaterialTheme.colorScheme.onSurfaceVariant, ikon: ImageVector? = null) {
    Row(Modifier.padding(top = 6.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ikon != null) Icon(ikon, null, Modifier.size(18.dp), tint = renk)
        Text(metin.uppercase(), color = renk, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

@Composable
fun Soluk(metin: String, modifier: Modifier = Modifier, boyut: Int = 13) =
    Text(metin, modifier, fontSize = boyut.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = (boyut + 5).sp)

@Composable
fun Cubuk(oran: Float, renk: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(oran.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(renk))
    }
}

/** Yüzde halkası (hazırlık, sınav puanı) */
@Composable
fun Halka(oran: Float, renk: Color, boyut: Dp, kalinlik: Dp = 10.dp, orta: @Composable () -> Unit) {
    val arka = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(boyut), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val k = kalinlik.toPx(); val s = Size(size.width - k, size.height - k); val o = Offset(k / 2, k / 2)
            drawArc(arka, 0f, 360f, false, o, s, style = Stroke(k))
            if (oran > 0f) drawArc(renk, -90f, 360f * oran.coerceIn(0f, 1f), false, o, s, style = Stroke(k, cap = StrokeCap.Round))
        }
        orta()
    }
}

/** Liste satırı: ikon · başlık/alt yazı · rozet · ok */
@Composable
fun Satir(baslik: String, alt: String? = null, ikon: ImageVector? = null, ikonRenk: Color = MaterialTheme.colorScheme.primary,
          rozet: String? = null, rozetRenk: Color = LocalRenk.current.uyari, ok: Boolean = true, baslikRenk: Color = Color.Unspecified,
          sag: (@Composable () -> Unit)? = null, tikla: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().let { if (tikla != null) it.clickable(onClick = tikla) else it }.padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (ikon != null) Icon(ikon, null, Modifier.size(24.dp), tint = ikonRenk)
        Column(Modifier.weight(1f)) {
            Text(baslik, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = baslikRenk)
            if (alt != null) Soluk(alt)
        }
        if (rozet != null) Text(rozet, Modifier.clip(RoundedCornerShape(50)).background(rozetRenk).padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        sag?.invoke()
        if (ok && tikla != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Kart içinde satır listesi (aralarında çizgi) */
@Composable
fun Liste(icerik: @Composable ColumnScope.() -> Unit) {
    val s = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxWidth().clip(s).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, s), content = icerik)
}

@Composable fun Ayrac() = HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 16.dp))

@Composable
fun Etiket(metin: String, renk: Color) {
    Text(metin, Modifier.clip(RoundedCornerShape(50)).background(renk.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
        color = renk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

/** Soru resmi (composeResources/files/r/…) — bir kez çözülür, bellekte küçük önbellek */
private val resimOnbellek = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 12
}

@Composable
fun SoruResmi(ad: String, aciklama: String?, modifier: Modifier = Modifier) {
    var resim by remember(ad) { mutableStateOf(resimOnbellek[ad]) }
    LaunchedEffect(ad) {
        if (resim == null) runCatching { Res.readBytes("files/r/$ad").decodeToImageBitmap() }.getOrNull()?.let { resimOnbellek[ad] = it; resim = it }
    }
    val s = RoundedCornerShape(12.dp)
    Box(modifier.fillMaxWidth().clip(s).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outline, s).padding(8.dp), contentAlignment = Alignment.Center) {
        val r = resim
        if (r == null) Box(Modifier.fillMaxWidth().height(160.dp))
        else Image(r, aciklama, Modifier.fillMaxWidth().heightIn(max = 460.dp), contentScale = ContentScale.Fit)
    }
}

/** Konfeti: tetik arttığında ekranın üstünden renkli parçalar dökülür (sınavı geçince). */
@Composable
fun Konfeti(tetik: Int) {
    if (tetik == 0) return
    val zaman = remember(tetik) { Animatable(0f) }
    LaunchedEffect(tetik) { zaman.snapTo(0f); zaman.animateTo(1f, tween(2400, easing = LinearEasing)) }
    if (zaman.value >= 1f) return
    val renkler = listOf(Color(0xFFF4B400), Color(0xFF1D5BD6), Color(0xFF0F7A4A), Color(0xFFD6334A), Color(0xFF6D4BD1))
    val parcalar = remember(tetik) {
        val r = kotlin.random.Random(tetik)
        List(60) { floatArrayOf(r.nextFloat(), r.nextFloat() * .35f, .6f + r.nextFloat() * .8f, r.nextFloat() * 360f, (r.nextFloat() - .5f) * .25f, r.nextInt(5).toFloat()) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = zaman.value
        parcalar.forEach { p ->
            val x = (p[0] + p[4] * t) * size.width
            val y = (-p[1] + p[2] * t * 1.4f) * size.height
            rotate(p[3] + t * 540f, Offset(x, y)) {
                drawRect(renkler[p[5].toInt()].copy(alpha = (1f - t).coerceIn(0f, 1f)), Offset(x - 7f, y - 4f), Size(14f, 8f))
            }
        }
    }
}
