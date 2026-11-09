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
import androidx.compose.material.icons.filled.Description
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
            it.title.contains(search, true) || it.content.contains(search, true) || it.attachments.contains(search, true)
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
                    onSave = { title, body, imageUris, documentUris ->
                        scope.launch {
                            val savedImages = copyUrisToPrivateStorage(context, imageUris, "images")
                            val savedDocuments = copyUrisToPrivateStorage(context, documentUris, "documents")
                            val allAttachments = listOf(savedImages, savedDocuments).filter { it.isNotBlank() }.joinToString("\n")
                            dao.insert(Memory(title = title, content = body, attachments = allAttachments))
                        }
                        showCapture = false
                    }
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
private fun MemoryCard(memory: Memory) {
    val attachmentFiles = remember(memory.attachments) {
        memory.attachments.split("\n").filter { it.isNotBlank() }.map { File(it) }.filter { it.exists() }
    }
    val imageFiles = attachmentFiles.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "heic") }
    val documentFiles = attachmentFiles - imageFiles.toSet()
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Red.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                Icon(if (imageFiles.isNotEmpty()) Icons.Default.Image else if (documentFiles.isNotEmpty()) Icons.Default.Description else Icons.Default.Archive, null, tint = Red, modifier = Modifier.size(18.dp))
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
private fun CaptureDialog(onDismiss: () -> Unit, onSave: (String, String, List<Uri>, List<Uri>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedDocuments by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        selectedDocuments = (selectedDocuments + uris).distinct()
    }
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
                OutlinedButton(onClick = { documentPicker.launch(arrayOf("application/pdf", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedDocuments.isEmpty()) "AÑADIR PDF / DOCUMENTOS" else "${selectedDocuments.size} DOCUMENTOS SELECCIONADOS", color = Color.White)
                }
                if (selectedImages.isNotEmpty() || selectedDocuments.isNotEmpty()) Text("Los archivos se guardarán de forma privada en este dispositivo.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank() && (body.isNotBlank() || selectedImages.isNotEmpty() || selectedDocuments.isNotEmpty())) onSave(title.trim(), body.trim(), selectedImages, selectedDocuments) }) {
                Text("GUARDAR", color = Red, letterSpacing = 1.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Muted) } }
    )
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
                else -> mime.substringAfter("/", "").substringBefore(";").takeIf { it.matches(Regex("[A-Za-z0-9]{2,5}")) } ?: if (category == "images") "jpg" else "file"
            }
            val destination = File(directory, "${category}_${System.currentTimeMillis()}_${(0..9999).random()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
            destination.absolutePath.takeIf { destination.exists() && destination.length() > 0 }
        }.getOrNull()
    }.joinToString("\n")
}
