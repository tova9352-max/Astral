package com.redmemory.app

import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.redmemory.app.data.Memory
import com.redmemory.app.data.MemoryDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Void = Color(0xFF080809)
private val Panel = Color(0xFF151316)
private val Red = Color(0xFFFF3347)
private val Muted = Color(0xFF969096)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = MemoryDatabase.get(this).memoryDao()
        setContent { RedMemoryApp(dao) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedMemoryApp(dao: com.redmemory.app.data.MemoryDao) {
    val memories by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCapture by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("HOME") }
    val filtered = remember(memories, search) {
        if (search.isBlank()) memories else memories.filter {
            it.title.contains(search, true) || it.content.contains(search, true)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Void, surface = Panel, primary = Red,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(Void)) {
            Box(Modifier.align(Alignment.TopCenter).offset(y = (-100).dp).size(290.dp)
                .blur(100.dp).background(Red.copy(alpha = .17f), CircleShape))
            Scaffold(
                containerColor = Color.Transparent,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showCapture = true },
                        containerColor = Red,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(62.dp).shadow(18.dp, CircleShape, ambientColor = Red, spotColor = Red)
                    ) { Icon(Icons.Default.Add, contentDescription = "Guardar recuerdo", modifier = Modifier.size(30.dp)) }
                },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xF20D0C0F), tonalElevation = 0.dp) {
                        listOf("HOME", "MEMORIES", "CONNECTIONS", "VAULT").forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(when(tab) {
                                    "MEMORIES" -> Icons.Default.Archive
                                    "CONNECTIONS" -> Icons.Default.Hub
                                    "VAULT" -> Icons.Default.Lock
                                    else -> Icons.Default.Search
                                }, contentDescription = tab) },
                                label = { Text(tab, fontSize = 9.sp, letterSpacing = 1.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Red, selectedTextColor = Red,
                                    indicatorColor = Red.copy(alpha = .12f),
                                    unselectedIconColor = Muted, unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("RED MEMORY", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                Text("PERSONAL INTELLIGENCE SYSTEM", color = Muted, fontSize = 9.sp, letterSpacing = 1.7.sp)
                            }
                            Box(Modifier.clip(CircleShape).border(1.dp, Red.copy(alpha = .35f), CircleShape).padding(horizontal = 11.dp, vertical = 7.dp)) {
                                Text("LOCAL CORE  •  ON", color = Red, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                    item { MemoryCore(memories.size) }
                    item {
                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Busca cualquier recuerdo...", color = Muted, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Red) },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Red.copy(alpha = .65f), unfocusedBorderColor = Color.White.copy(alpha = .10f),
                                focusedContainerColor = Panel.copy(alpha = .82f), unfocusedContainerColor = Panel.copy(alpha = .72f)
                            )
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (search.isBlank()) "RECENT MEMORIES" else "SEARCH RESULTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp)
                            Text("${filtered.size} ITEMS", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                    if (filtered.isEmpty()) {
                        item { EmptyState(onCapture = { showCapture = true }) }
                    } else {
                        items(filtered, key = { it.id }) { memory -> MemoryCard(memory) }
                    }
                    item {
                        GlassPanel(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(Color(0xFF48D597), CircleShape))
                                Spacer(Modifier.width(9.dp))
                                Text("LOCAL STORAGE READY", color = Color(0xFFB9B3B9), fontSize = 9.sp, letterSpacing = 1.2.sp)
                                Spacer(Modifier.weight(1f))
                                Text("CLOUD OPTIONAL", color = Muted, fontSize = 9.sp, letterSpacing = .8.sp)
                            }
                        }
                    }
                }
            }
            if (showCapture) {
                CaptureDialog(
                    onDismiss = { showCapture = false },
                    onSave = { title, body, imageUris ->
                        scope.launch {
                            val savedPaths = copyImagesToPrivateStorage(context, imageUris)
                            dao.insert(Memory(title = title, content = body, attachments = savedPaths))
                        }
                        showCapture = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MemoryCore(count: Int) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(0.86f, 1.08f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = androidx.compose.animation.core.LinearEasing)), label = "rotation")
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("MEMORY CORE", color = Muted, fontSize = 10.sp, letterSpacing = 2.4.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(184.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val c = center
                    val radius = size.minDimension * .37f
                    drawCircle(Brush.radialGradient(listOf(Red.copy(alpha = .18f), Color.Transparent), c, radius * 1.5f), radius * 1.5f, c)
                    drawCircle(Red.copy(alpha = .24f), radius, c, style = Stroke(1.2.dp.toPx()))
                    drawArc(Red.copy(alpha = .75f), rotation, 105f, false, topLeft = Offset(c.x-radius, c.y-radius), size = androidx.compose.ui.geometry.Size(radius*2, radius*2), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Color.White.copy(alpha = .28f), rotation + 155f, 65f, false, topLeft = Offset(c.x-radius*1.12f, c.y-radius*1.12f), size = androidx.compose.ui.geometry.Size(radius*2.24f, radius*2.24f), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
                }
                Box(Modifier.size((82 * pulse).dp).shadow(22.dp, CircleShape, ambientColor = Red, spotColor = Red)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFA0A8), Red, Color(0xFF5B0715))), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = .55f), CircleShape))
                Box(Modifier.size(54.dp).background(Brush.radialGradient(listOf(Color(0xFFFFE3E5).copy(alpha=.9f), Color(0xFFFF5364), Color(0xFFB10D28))), CircleShape))
            }
            Text("${count.toString().padStart(3, '0')} MEMORIES STORED", color = Color.White, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            Text("YOUR DIGITAL MEMORY IS ONLINE", color = Muted, fontSize = 9.sp, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(Color(0xCC21191E), Color(0xB30F0E12), Color(0xCC1B1117))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = .17f), Red.copy(alpha = .20f), Color.White.copy(alpha = .04f))), RoundedCornerShape(24.dp))
        .padding(18.dp), content = content)
}

@Composable
private fun MemoryCard(memory: Memory) {
    val imageFiles = remember(memory.attachments) {
        memory.attachments.split("\n").filter { it.isNotBlank() }.map { File(it) }.filter { it.exists() }
    }
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Red.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(if (imageFiles.isNotEmpty()) Icons.Default.Image else Icons.Default.Archive, null, tint = Red, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(memory.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (memory.content.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(memory.content, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (imageFiles.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        imageFiles.take(4).forEach { file ->
                            val bitmap = remember(file.absolutePath) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                            if (bitmap != null) Image(bitmap, contentDescription = "Imagen guardada", modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp)))
                        }
                        if (imageFiles.size > 4) Text("+${imageFiles.size - 4}", color = Red, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(memory.createdAt)).uppercase() + if (imageFiles.isNotEmpty()) "  •  ${imageFiles.size} IMAGES" else "", color = Red.copy(alpha = .85f), fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun EmptyState(onCapture: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("YOUR MEMORY STARTS HERE", color = Color.White, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Text("Guarda tu primera idea, imagen o enlace. RED MEMORY empezará a construir tu memoria digital.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Text("+  CAPTURE FIRST MEMORY", color = Red, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.clickable { onCapture() }.padding(8.dp))
        }
    }
}

@Composable
private fun CaptureDialog(onDismiss: () -> Unit, onSave: (String, String, List<Uri>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImages = (selectedImages + uris).distinct()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171317),
        title = { Text("CAPTURE MEMORY", color = Color.White, letterSpacing = 1.5.sp, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("¿Qué quieres recordar?") }, minLines = 3)
                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedImages.isEmpty()) "AÑADIR IMÁGENES" else "${selectedImages.size} IMÁGENES SELECCIONADAS", color = Color.White)
                }
                if (selectedImages.isNotEmpty()) Text("Las imágenes se guardarán de forma privada en este dispositivo.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank() && (body.isNotBlank() || selectedImages.isNotEmpty())) onSave(title.trim(), body.trim(), selectedImages) }) {
                Text("GUARDAR", color = Red, letterSpacing = 1.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Muted) } }
    )
}

private fun copyImagesToPrivateStorage(context: Context, uris: List<Uri>): String {
    if (uris.isEmpty()) return ""
    val directory = File(context.filesDir, "memories/images").apply { mkdirs() }
    return uris.mapNotNull { uri ->
        runCatching {
            val extension = context.contentResolver.getType(uri)?.substringAfter("/")?.substringBefore(";")
                ?.takeIf { it.matches(Regex("[A-Za-z0-9]{2,5}")) } ?: "jpg"
            val destination = File(directory, "image_${System.currentTimeMillis()}_${(0..9999).random()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination.absolutePath.takeIf { destination.exists() && destination.length() > 0 }
        }.getOrNull()
    }.joinToString("\n")
}
