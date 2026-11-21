package com.bechir.filespro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilesApp(
                persistFolder = { uri ->
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                },
                initialFolder = getSharedPreferences("bechir_files", Context.MODE_PRIVATE).getString("last_folder", null),
                saveFolder = { uri -> getSharedPreferences("bechir_files", Context.MODE_PRIVATE).edit().putString("last_folder", uri.toString()).apply() },
                openFile = { uri ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } catch (_: Exception) { }
                }
            )
        }
    }
}

@Composable
fun FilesApp(persistFolder: (Uri) -> Unit, initialFolder: String?, saveFolder: (Uri) -> Unit, openFile: (Uri) -> Unit) {
    var dark by remember { mutableStateOf(true) }
    val bg = if (dark) Color(0xFF10172B) else Color(0xFFF7F5FD)
    val panel = if (dark) Color(0xFF202B47) else Color.White
    val fg = if (dark) Color.White else Color(0xFF1E2340)
    val accent = Color(0xFF9168F4)
    var root by remember { mutableStateOf<DocumentFile?>(null) }
    var current by remember { mutableStateOf<DocumentFile?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var operation by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<DocumentFile?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var clipboard by remember { mutableStateOf<DocumentFile?>(null) }
    var moving by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(initialFolder) {
        if (!initialFolder.isNullOrBlank()) {
            try {
                val restored = DocumentFile.fromTreeUri(context, Uri.parse(initialFolder))
                if (restored != null && restored.canRead()) { root = restored; current = restored; revision++ }
            } catch (_: Exception) { }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                persistFolder(uri)
                saveFolder(uri)
                root = DocumentFile.fromTreeUri(context, uri)
                current = root
                query = ""
                error = ""
            } catch (e: Exception) {
                error = "تعذر فتح المجلد: ${e.message}"
            }
        }
    }
    val files = remember(current, query, revision) {
        try {
            current?.listFiles()
                ?.filter { it.name.orEmpty().contains(query, ignoreCase = true) }
                ?.sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
                ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = accent) else lightColorScheme(primary = Color(0xFF7545C9))) {
        Column(Modifier.fillMaxSize().background(bg).systemBarsPadding().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ملفاتي | بشير", color = fg, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("BECHIR FILES PRO  •  مدير الملفات", color = fg.copy(alpha = .65f), style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = { dark = !dark }) {
                    Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "المظهر", tint = fg)
                }
            }
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF39245D) else Color(0xFFE9DDFC)), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("مساحة ملفاتك الخاصة", color = fg, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("تصفّح • نظّم • انسخ • انقل", color = fg.copy(alpha = .75f))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { picker.launch(null) }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("اختيار مجلد")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(20.dp), label = { Text("ابحث داخل المجلد") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            Spacer(Modifier.height(12.dp))
            if (current != null) {
                Row {
                    TextButton(onClick = { operation = "create"; nameInput = "" }) { Text("＋ مجلد جديد") }
                    if (clipboard != null) {
                        TextButton(onClick = {
                            val source = clipboard
                            val destination = current
                            val success = if (source != null && destination != null) {
                                if (moving) FileOperations.moveFile(context, source, destination)
                                else FileOperations.copyFile(context, source, destination)
                            } else false
                            if (success) { clipboard = null; error = ""; revision++ }
                            else error = "تعذر اللصق: تحقق من صلاحيات الكتابة أو تكرار الاسم"
                        }) { Text("لصق") }
                        TextButton(onClick = { clipboard = null }) { Text("إلغاء") }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (current?.uri != root?.uri) current = current?.parentFile }) {
                    Icon(Icons.Default.ArrowBack, "رجوع", tint = fg)
                }
                Text(current?.name ?: "لم يتم اختيار مجلد", color = fg, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${files.size} عنصر", color = fg.copy(alpha = .6f))
            }
            if (error.isNotBlank()) Text(error, color = Color(0xFFFF8E8E))
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(files, key = { it.uri.toString() }) { file ->
                    Card(onClick = { if (file.isDirectory) { current = file; query = "" } else openFile(file.uri) }, colors = CardDefaults.cardColors(containerColor = panel), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                        Column(Modifier.fillMaxWidth().height(158.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(when { file.isDirectory -> Icons.Default.Folder; file.type?.startsWith("image/") == true -> Icons.Default.Image; file.type?.startsWith("video/") == true -> Icons.Default.Movie; file.type?.startsWith("audio/") == true -> Icons.Default.MusicNote; file.type == "application/pdf" -> Icons.Default.PictureAsPdf; else -> Icons.Default.Description }, null, tint = if (file.isDirectory) Color(0xFFF2B84D) else accent, modifier = Modifier.size(46.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(file.name ?: "ملف", color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                            Row {
                                if (file.isFile) {
                                    IconButton(onClick = { clipboard = file; moving = false; error = "افتح مجلد الوجهة واضغط لصق" }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.ContentCopy, "نسخ", tint = fg, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { clipboard = file; moving = true; error = "افتح مجلد الوجهة واضغط لصق" }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.DriveFileMove, "نقل", tint = fg, modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { target = file; nameInput = file.name.orEmpty(); operation = "rename" }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Edit, "إعادة تسمية", tint = fg, modifier = Modifier.size(18.dp)) }
                                IconButton(onClick = { target = file; operation = "delete" }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "حذف", tint = fg, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
            }
        }
        if (operation.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { operation = "" },
                title = { Text(when (operation) { "create" -> "إنشاء مجلد"; "rename" -> "إعادة تسمية"; else -> "تأكيد الحذف النهائي" }) },
                text = {
                    if (operation == "delete") Text("هل تريد حذف هذا العنصر نهائيًا؟ لا توجد سلة محذوفات.")
                    else OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("الاسم") }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = {
                        val success = when (operation) {
                            "create" -> current?.let { FileOperations.createFolder(it, nameInput) } == true
                            "rename" -> target?.let { FileOperations.rename(it, nameInput) } == true
                            "delete" -> target?.let { FileOperations.deleteConfirmed(it) } == true
                            else -> false
                        }
                        if (success) { operation = ""; target = null; error = ""; revision++ }
                        else error = "تعذرت العملية. تحقق من صلاحيات المجلد والاسم."
                    }) { Text(if (operation == "delete") "حذف نهائيًا" else "حفظ") }
                },
                dismissButton = { TextButton(onClick = { operation = ""; target = null }) { Text("إلغاء") } }
            )
        }
    }
}
