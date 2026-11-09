package com.redmemory.app

import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.VideoView
import android.widget.MediaController
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
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
    var selectedCategory by remember { mutableStateOf("ALL") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var memoryToEdit by remember { mutableStateOf<Memory?>(null) }
    var memoryToDelete by remember { mutableStateOf<Memory?>(null) }
    val filtered = remember(memories, search, selectedCategory, favoritesOnly) {
        memories.mapNotNull { memory ->
            val score = memorySearchScore(memory, search)
            val matchesCategory = selectedCategory == "ALL" || memory.category == selectedCategory
            if (score > 0 && matchesCategory && (!favoritesOnly || memory.isFavorite)) memory to score else null
        }.sortedWith(compareByDescending<Pair<Memory, Int>> { it.second }.thenByDescending { it.first.createdAt })
         .map { it.first }
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
                    item {
                        if (selectedTab == "HOME") MemoryCore(memories.size)
                        else ModuleHero(selectedTab, memories.size)
                    }
                    if (selectedTab == "HOME" || selectedTab == "MEMORIES") {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("ALL", "GENERAL", "PERSONAL", "WORK", "IDEAS", "STUDY").forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(when(category) {
                                        "ALL" -> "TODO"; "GENERAL" -> "GENERAL"; "PERSONAL" -> "PERSONAL"
                                        "WORK" -> "TRABAJO"; "IDEAS" -> "IDEAS"; else -> "ESTUDIO"
                                    }, fontSize = 9.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Red.copy(alpha = .18f),
                                        selectedLabelColor = Red,
                                        labelColor = Muted
                                    )
                                )
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = favoritesOnly,
                                onClick = { favoritesOnly = !favoritesOnly },
                                label = { Text(if (favoritesOnly) "MOSTRANDO FAVORITOS" else "SOLO FAVORITOS", fontSize = 10.sp) },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp)) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red.copy(alpha = .18f), selectedLabelColor = Red, labelColor = Muted)
                            )
                        }
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
                        items(filtered, key = { it.id }) { memory ->
                            MemoryCard(memory,
                                onToggleFavorite = { scope.launch { dao.setFavorite(memory.id, !memory.isFavorite) } },
                                onEdit = { memoryToEdit = memory },
                                onDelete = { memoryToDelete = memory })
                        }
                    }
                    }
                    if (selectedTab == "CONNECTIONS") {
                        item { ConnectionsPreview(memories.size) }
                    }
                    if (selectedTab == "VAULT") {
                        item { VaultPreview() }
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
                    onSave = { title, body, imageUris, documentUris, videoUris, category ->
                        scope.launch {
                            val allAttachments = withContext(Dispatchers.IO) {
                                val savedImages = copyUrisToPrivateStorage(context, imageUris, "images")
                                val savedDocuments = copyUrisToPrivateStorage(context, documentUris, "documents")
                                val savedVideos = copyUrisToPrivateStorage(context, videoUris, "videos")
                                listOf(savedImages, savedDocuments, savedVideos).filter { it.isNotBlank() }.joinToString("\n")
                            }
                            dao.insert(Memory(title = title, content = body, attachments = allAttachments, category = category))
                        }
                        showCapture = false
                    }
                )
            }
            memoryToEdit?.let { memory ->
                EditMemoryDialog(
                    memory = memory,
                    onDismiss = { memoryToEdit = null },
                    onSave = { title, content, category ->
                        scope.launch { dao.updateDetails(memory.id, title, content, category) }
                        memoryToEdit = null
                    }
                )
            }
            memoryToDelete?.let { memory ->
                AlertDialog(
                    onDismissRequest = { memoryToDelete = null },
                    containerColor = Color(0xFF171317),
                    title = { Text("ELIMINAR RECUERDO", color = Color.White) },
                    text = { Text("Se eliminará este recuerdo y sus archivos adjuntos del dispositivo. Esta acción no se puede deshacer.", color = Muted) },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    memory.attachments.split("\n").filter { it.isNotBlank() }.forEach { File(it).delete() }
                                }
                                dao.delete(memory.id)
                            }
                            memoryToDelete = null
                        }) { Text("ELIMINAR", color = Red) }
                    },
                    dismissButton = { TextButton(onClick = { memoryToDelete = null }) { Text("CANCELAR", color = Muted) } }
                )
            }
        }
    }
}

@Composable
private fun ModuleHero(tab: String, count: Int) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(if (tab == "MEMORIES") "MEMORY ARCHIVE" else if (tab == "CONNECTIONS") "NEURAL CONNECTIONS" else "PRIVATE VAULT",
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(7.dp))
            Text(
                when (tab) {
                    "MEMORIES" -> "Tu colección personal, organizada en un solo lugar."
                    "CONNECTIONS" -> "Un espacio para descubrir cómo se relacionan tus recuerdos."
                    else -> "Tu espacio privado está preparado para la siguiente fase de seguridad."
                },
                color = Muted, fontSize = 11.sp, lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill(if (tab == "MEMORIES") "$count RECUERDOS" else if (tab == "CONNECTIONS") "RED PERSONAL" else "LOCAL FIRST")
                StatPill(if (tab == "VAULT") "CLOUD OPCIONAL" else "CORE ACTIVO")
            }
        }
    }
}

@Composable
private fun StatPill(label: String) {
    Box(Modifier.clip(CircleShape).background(Red.copy(alpha = .10f))
        .border(1.dp, Red.copy(alpha = .25f), CircleShape).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label, color = Red, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConnectionsPreview(count: Int) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Text("MEMORY NETWORK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.7.sp)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val nodes = listOf(
                    Offset(c.x, c.y), Offset(c.x - 92.dp.toPx(), c.y - 48.dp.toPx()),
                    Offset(c.x + 90.dp.toPx(), c.y - 54.dp.toPx()), Offset(c.x - 72.dp.toPx(), c.y + 65.dp.toPx()),
                    Offset(c.x + 76.dp.toPx(), c.y + 68.dp.toPx())
                )
                nodes.drop(1).forEach { drawLine(Red.copy(alpha = .35f), c, it, strokeWidth = 1.5.dp.toPx()) }
                nodes.forEachIndexed { index, point ->
                    val radius = if (index == 0) 17.dp.toPx() else 9.dp.toPx()
                    drawCircle(if (index == 0) Red else Color(0xFFFF7180), radius + 8.dp.toPx(), point, alpha = .12f)
                    drawCircle(if (index == 0) Red else Color(0xFFFF7180), radius, point)
                    drawCircle(Color.White.copy(alpha = .8f), radius * .28f, point)
                }
            }
        }
        Text(if (count == 0) "Guarda recuerdos para comenzar a construir tu red." else "La visualización de relaciones inteligentes llegará en una próxima actualización.",
            color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
        StatPill("$count NODOS DE MEMORIA")
    }
}

@Composable
private fun VaultPreview() {
    GlassPanel(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(112.dp).background(Brush.radialGradient(listOf(Red.copy(alpha = .28f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Red, modifier = Modifier.size(48.dp))
            }
        }
        Text("PRIVATE SPACE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(7.dp))
        Text("El módulo Vault está en desarrollo. La protección con biometría y el cifrado de archivos se incorporarán antes de habilitar el almacenamiento privado.",
            color = Muted, fontSize = 11.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(14.dp))
        StatPill("SEGURIDAD EN DESARROLLO")
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
private fun MemoryCard(memory: Memory, onToggleFavorite: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val attachmentFiles = remember(memory.attachments) {
        memory.attachments.split("\n").filter { it.isNotBlank() }.map { File(it) }.filter { it.exists() }
    }
    val imageFiles = attachmentFiles.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "heic") }
    val videoFiles = attachmentFiles.filter { it.extension.lowercase() in listOf("mp4", "mkv", "webm", "3gp", "mov", "avi", "m4v") }
    val documentFiles = attachmentFiles - imageFiles.toSet() - videoFiles.toSet()
    var playingVideo by remember { mutableStateOf<String?>(null) }
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Red.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(if (imageFiles.isNotEmpty()) Icons.Default.Image else if (videoFiles.isNotEmpty()) Icons.Default.VideoLibrary else if (documentFiles.isNotEmpty()) Icons.Default.Description else Icons.Default.Archive, null, tint = Red, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(memory.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                StatPill(when(memory.category) {
                    "PERSONAL" -> "PERSONAL"; "WORK" -> "TRABAJO"; "IDEAS" -> "IDEAS"; "STUDY" -> "ESTUDIO"; else -> "GENERAL"
                })
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
                if (videoFiles.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    videoFiles.forEach { file ->
                        GlassPanel(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Red.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Red, modifier = Modifier.size(25.dp))
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(file.name, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatVideoDuration(file) + "  •  VIDEO", color = Muted, fontSize = 9.sp)
                                }
                                TextButton(onClick = { playingVideo = if (playingVideo == file.absolutePath) null else file.absolutePath }) {
                                    Text(if (playingVideo == file.absolutePath) "CERRAR" else "VER", color = Red, fontSize = 10.sp)
                                }
                            }
                            if (playingVideo == file.absolutePath) {
                                Spacer(Modifier.height(8.dp))
                                AndroidView(
                                    factory = { viewContext ->
                                        VideoView(viewContext).apply {
                                            val controller = MediaController(viewContext)
                                            controller.setAnchorView(this)
                                            setMediaController(controller)
                                            setVideoPath(file.absolutePath)
                                            setOnPreparedListener { player -> player.isLooping = false; start() }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(190.dp)
                                )
                            }
                        }
                    }
                }
                if (documentFiles.isNotEmpty()) {
                    Spacer(Modifier.height(9.dp))
                    documentFiles.forEach { file ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Red, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(file.name, color = Color(0xFFD7CDD2), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val attachmentCount = attachmentFiles.size
                Text(SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(memory.createdAt)).uppercase() + if (attachmentCount > 0) "  •  $attachmentCount ATTACHMENTS" else "", color = Red.copy(alpha = .85f), fontSize = 9.sp, letterSpacing = 1.sp)
            }
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
                Icon(if (memory.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = if (memory.isFavorite) "Quitar de favoritos" else "Añadir a favoritos", tint = if (memory.isFavorite) Red else Muted)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Editar recuerdo", tint = Muted, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar recuerdo", tint = Muted, modifier = Modifier.size(17.dp))
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
private fun EditMemoryDialog(memory: Memory, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember(memory.id) { mutableStateOf(memory.title) }
    var content by remember(memory.id) { mutableStateOf(memory.content) }
    var category by remember(memory.id) { mutableStateOf(memory.category) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171317),
        title = { Text("EDITAR RECUERDO", color = Color.White, letterSpacing = 1.3.sp, fontSize = 14.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Contenido") }, minLines = 3)
                Text("CATEGORÍA", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("GENERAL", "PERSONAL", "WORK", "IDEAS", "STUDY").chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(when(option) { "PERSONAL" -> "Personal"; "WORK" -> "Trabajo"; "IDEAS" -> "Ideas"; "STUDY" -> "Estudio"; else -> "General" }, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red.copy(alpha = .18f), selectedLabelColor = Red, labelColor = Muted)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title.trim(), content.trim(), category) }) {
                Text("GUARDAR CAMBIOS", color = Red)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Muted) } }
    )
}

@Composable
private fun CaptureDialog(onDismiss: () -> Unit, onSave: (String, String, List<Uri>, List<Uri>, List<Uri>, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedDocuments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedVideos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var category by remember { mutableStateOf("GENERAL") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isReadingImages by remember { mutableStateOf(false) }
    var ocrMessage by remember { mutableStateOf("") }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        selectedDocuments = (selectedDocuments + uris).distinct()
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImages = (selectedImages + uris).distinct()
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        selectedVideos = (selectedVideos + uris).distinct()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171317),
        title = { Text("CAPTURE MEMORY", color = Color.White, letterSpacing = 1.5.sp, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("¿Qué quieres recordar?") }, minLines = 3)
                Text("CATEGORÍA", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("GENERAL", "PERSONAL", "WORK", "IDEAS", "STUDY").chunked(3).forEach { rowCategories ->
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            rowCategories.forEach { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(when(option) {
                                        "PERSONAL" -> "Personal"; "WORK" -> "Trabajo"; "IDEAS" -> "Ideas"; "STUDY" -> "Estudio"; else -> "General"
                                    }, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Red.copy(alpha = .18f),
                                        selectedLabelColor = Red,
                                        labelColor = Muted
                                    )
                                )
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedImages.isEmpty()) "AÑADIR IMÁGENES" else "${selectedImages.size} IMÁGENES SELECCIONADAS", color = Color.White)
                }
                if (selectedImages.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            if (!isReadingImages) scope.launch {
                                isReadingImages = true
                                ocrMessage = ""
                                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                try {
                                    val extracted = withContext(Dispatchers.IO) {
                                        selectedImages.mapNotNull { uri ->
                                            runCatching {
                                                recognizer.process(InputImage.fromFilePath(context, uri)).await().text
                                                    .takeIf { it.isNotBlank() }
                                            }.getOrNull()
                                        }
                                    }
                                    if (extracted.isNotEmpty()) {
                                        body = listOf(body.trim(), extracted.joinToString("\n\n"))
                                            .filter { it.isNotBlank() }.joinToString("\n\n")
                                        ocrMessage = "TEXTO EXTRAÍDO Y AÑADIDO"
                                    } else {
                                        ocrMessage = "NO SE ENCONTRÓ TEXTO LEGIBLE"
                                    }
                                } catch (_: Exception) {
                                    ocrMessage = "NO SE PUDO LEER UNA O MÁS IMÁGENES"
                                } finally {
                                    recognizer.close()
                                    isReadingImages = false
                                }
                            }
                        },
                        enabled = !isReadingImages,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isReadingImages) "LEYENDO TEXTO..." else "EXTRAER TEXTO DE IMÁGENES", color = Red)
                    }
                    if (ocrMessage.isNotBlank()) {
                        Text(ocrMessage, color = if (ocrMessage.contains("AÑADIDO")) Color(0xFF48D597) else Muted, fontSize = 10.sp)
                    }
                }
                OutlinedButton(onClick = { documentPicker.launch(arrayOf("application/pdf", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedDocuments.isEmpty()) "AÑADIR PDF / DOCUMENTOS" else "${selectedDocuments.size} DOCUMENTOS SELECCIONADOS", color = Color.White)
                }
                OutlinedButton(onClick = { videoPicker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedVideos.isEmpty()) "AÑADIR VIDEOS" else selectedVideos.size.toString() + " VIDEOS SELECCIONADOS", color = Color.White)
                }
                if (selectedImages.isNotEmpty() || selectedDocuments.isNotEmpty() || selectedVideos.isNotEmpty()) Text("Los archivos se guardarán de forma privada en este dispositivo.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank() && (body.isNotBlank() || selectedImages.isNotEmpty() || selectedDocuments.isNotEmpty() || selectedVideos.isNotEmpty())) onSave(title.trim(), body.trim(), selectedImages, selectedDocuments, selectedVideos, category) }) {
                Text("GUARDAR", color = Red, letterSpacing = 1.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Muted) } }
    )
}


private fun memorySearchScore(memory: Memory, rawQuery: String): Int {
    if (rawQuery.isBlank()) return 1
    fun normalize(value: String): String = java.text.Normalizer.normalize(value.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
    val query = normalize(rawQuery)
    if (query.isBlank()) return 1
    val title = normalize(memory.title)
    val content = normalize(memory.content)
    val attachments = normalize(memory.attachments)
    val all = "$title $content $attachments"
    val terms = query.split(Regex("\\s+")).filter { it.length > 1 }.distinct()
    var score = 0
    if (title.contains(query)) score += 12
    if (content.contains(query)) score += 7
    if (attachments.contains(query)) score += 3
    terms.forEach { term ->
        if (title.split(Regex("\\s+")).any { it == term }) score += 6
        else if (title.contains(term)) score += 3
        if (content.contains(term)) score += 2
        if (attachments.contains(term)) score += 1
    }
    if (terms.size > 1 && terms.all { all.contains(it) }) score += 5
    return score
}

private fun formatVideoDuration(file: File): String {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val seconds = duration / 1000
            (seconds / 60).toString() + ":" + (seconds % 60).toString().padStart(2, '0')
        } finally { retriever.release() }
    }.getOrDefault("Video")
}

private fun copyUrisToPrivateStorage(context: Context, uris: List<Uri>, category: String): String {
    if (uris.isEmpty()) return ""
    val directory = File(context.filesDir, "memories/$category").apply { mkdirs() }
    return uris.mapNotNull { uri ->
        runCatching {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val extension = when (mime) {
                "application/pdf" -> "pdf"
                "text/plain" -> "txt"
                "application/msword" -> "doc"
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/jpeg" -> "jpg"
                "video/mp4" -> "mp4"
                "video/webm" -> "webm"
                "video/3gpp" -> "3gp"
                "video/quicktime" -> "mov"
                "video/x-matroska" -> "mkv"
                else -> mime.substringAfter("/", "").substringBefore(";").takeIf { it.matches(Regex("[A-Za-z0-9]{2,5}")) } ?: if (category == "images") "jpg" else "file"
            }
            val destination = File(directory, "${category}_${System.currentTimeMillis()}_${(0..9999).random()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
            destination.absolutePath.takeIf { destination.exists() && destination.length() > 0 }
        }.getOrNull()
    }.joinToString("\n")
}
