package net.yeeren.classgrid.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.yeeren.classgrid.data.DhuImportResult
import net.yeeren.classgrid.data.DhuTimetableImporter
import org.json.JSONTokener

private const val DHU_TIMETABLE_URL = "https://jwgl.dhu.edu.cn/dhu/StudentCourseTable/toPage"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhuImportScreen(
    onClose: () -> Unit,
    onImportCurrent: (DhuImportResult) -> Unit,
    onImportNew: (DhuImportResult) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isRecognizing by remember { mutableStateOf(false) }
    var autoRedirected by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("请完成统一身份认证，登录后将自动进入课表。") }
    var importResult by remember { mutableStateOf<DhuImportResult?>(null) }
    var selectedCourses by remember { mutableStateOf(setOf<Int>()) }

    fun navigateToTimetable() {
        autoRedirected = true
        webView?.loadUrl(DHU_TIMETABLE_URL)
    }

    fun recognize() {
        val view = webView ?: return
        isRecognizing = true
        view.evaluateJavascript(DHU_EXTRACT_SCRIPT) { encoded ->
            runCatching {
                val value = JSONTokener(encoded).nextValue()
                require(value is String && value.isNotBlank()) { "页面未返回可识别的数据" }
                DhuTimetableImporter.parse(value)
            }.onSuccess { result ->
                importResult = result
                selectedCourses = result.courses.indices.toSet()
                message = "已识别 ${result.courses.size} 门课程，请选择需要导入的课程。"
            }.onFailure { error ->
                message = error.message ?: "识别失败，请确认课表已完整显示后重试。"
            }
            isRecognizing = false
        }
    }

    BackHandler {
        when {
            importResult != null -> importResult = null
            webView?.canGoBack() == true -> webView?.goBack()
            else -> onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("从教务系统导入", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (importResult == null) "东华大学本科教务管理系统" else "选择课程",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (importResult != null) importResult = null else onClose()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            },
            floatingActionButton = {
                if (importResult == null) {
                    FloatingActionButton(
                        onClick = ::recognize,
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (isRecognizing) {
                            CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "识别课表")
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoading || isRecognizing) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                        }
                        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (importResult == null && !currentUrl.contains("/StudentCourseTable/")) {
                            TextButton(onClick = ::navigateToTimetable) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Text("前往课表")
                            }
                        }
                    }
                }
                if (importResult == null) {
                    DhuWebView(
                        modifier = Modifier.fillMaxSize(),
                        onCreated = { webView = it },
                        onPageStarted = {
                            currentUrl = it
                            isLoading = true
                        },
                        onPageFinished = { view, url ->
                            currentUrl = url
                            isLoading = false
                            if (url.contains("/StudentCourseTable/")) {
                                message = "课表已打开，点击右下角识别。"
                            } else {
                                view.evaluateJavascript(
                                    "Boolean(document.querySelector('a[href*=\\\"casLogout\\\"]'))"
                                ) { authenticated ->
                                    if (authenticated == "true" && !autoRedirected) {
                                        message = "登录成功，正在进入课表……"
                                        navigateToTimetable()
                                    } else if (authenticated != "true") {
                                        message = "请完成统一身份认证，登录后将自动进入课表。"
                                    }
                                }
                            }
                        },
                        onBlockedNavigation = {
                            message = "为保护登录信息，仅允许打开东华大学域名内的页面。"
                        }
                    )
                } else {
                    CourseSelection(
                        result = importResult!!,
                        selected = selectedCourses,
                        onToggle = { index ->
                            selectedCourses = if (index in selectedCourses) {
                                selectedCourses - index
                            } else {
                                selectedCourses + index
                            }
                        },
                        onSelectAll = {
                            selectedCourses = if (selectedCourses.size == importResult!!.courses.size) {
                                emptySet()
                            } else {
                                importResult!!.courses.indices.toSet()
                            }
                        },
                        onImportCurrent = {
                            onImportCurrent(importResult!!.withSelected(selectedCourses))
                        },
                        onImportNew = {
                            onImportNew(importResult!!.withSelected(selectedCourses))
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun DhuWebView(
    modifier: Modifier,
    onCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (WebView, String) -> Unit,
    onBlockedNavigation: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setSupportMultipleWindows(false)
                CookieManager.getInstance().setAcceptCookie(true)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val uri = request.url
                        val allowed = uri.scheme in setOf("http", "https") && isDhuHost(uri)
                        if (!allowed) onBlockedNavigation()
                        return !allowed
                    }

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        onPageStarted(url)
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        onPageFinished(view, url)
                    }
                }
                loadUrl(DHU_TIMETABLE_URL)
                onCreated(this)
            }
        }
    )
}

private fun isDhuHost(uri: Uri): Boolean {
    val host = uri.host.orEmpty().lowercase()
    return host == "dhu.edu.cn" || host.endsWith(".dhu.edu.cn")
}

@Composable
private fun CourseSelection(
    result: DhuImportResult,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onImportCurrent: () -> Unit,
    onImportNew: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(result.semesterName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "已选择 ${selected.size}/${result.courses.size} 门",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onSelectAll) {
                Text(if (selected.size == result.courses.size) "取消全选" else "全选")
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(result.courses) { index, item ->
                Card(onClick = { onToggle(index) }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = index in selected, onCheckedChange = { onToggle(index) })
                        Column(Modifier.weight(1f)) {
                            Text(item.course.name, fontWeight = FontWeight.SemiBold)
                            val details = buildList {
                                item.course.teacher.takeIf(String::isNotBlank)?.let(::add)
                                add("${item.slots.size} 个时段")
                            }.joinToString(" · ")
                            Text(
                                details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onImportNew,
                enabled = selected.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { Text("导入新课表") }
            Button(
                onClick = onImportCurrent,
                enabled = selected.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { Text("导入当前课表") }
        }
    }
}

private fun DhuImportResult.withSelected(selected: Set<Int>): DhuImportResult = copy(
    courses = courses.filterIndexed { index, _ -> index in selected }
)

private val DHU_EXTRACT_SCRIPT = """
(() => {
  const tables = Array.from(document.querySelectorAll('table'));
  const table = tables.find(t => {
    const text = (t.rows[0]?.innerText || '').replace(/\s/g, '');
    return text.includes('星期一') && text.includes('星期日');
  });
  if (!table) return '';
  const chinese = {一:1,二:2,三:3,四:4,五:5,六:6,七:7,八:8,九:9,十:10,十一:11,十二:12,十三:13,十四:14,十五:15,十六:16,十七:17,十八:18,十九:19,二十:20};
  const occupancy = {};
  const cells = [];
  Array.from(table.rows).slice(1).forEach(row => {
    const label = (row.cells[0]?.innerText || '').replace(/\s/g, '').replace('节','');
    const section = Number(label) || chinese[label];
    if (!section) return;
    occupancy[section] ||= [];
    let column = 0;
    Array.from(row.cells).forEach(cell => {
      while (occupancy[section][column]) column++;
      const rowSpan = Math.max(1, Number(cell.rowSpan) || 1);
      const colSpan = Math.max(1, Number(cell.colSpan) || 1);
      const lines = (cell.innerText || '').split(/\n+/).map(v => v.trim()).filter(Boolean);
      if (column >= 1 && column <= 7 && lines.some(v => /\d+\s*(?:[-—－]\s*\d+)?\s*周/.test(v))) {
        cells.push({day: column, start: section, end: section + rowSpan - 1, lines});
      }
      for (let r = section; r < section + rowSpan; r++) {
        occupancy[r] ||= [];
        for (let c = column; c < column + colSpan; c++) occupancy[r][c] = true;
      }
      column += colSpan;
    });
  });
  const semester = (document.body.innerText.match(/\d{4}\s*[-—]\s*\d{4}学年[^\n]*?第\s*\d+\s*学期/) || ['教务系统课表'])[0].replace(/\s+/g, ' ');
  return JSON.stringify({semester, cells});
})()
""".trimIndent()
