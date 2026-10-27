package net.yeeren.classgrid.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yeeren.classgrid.data.CourseWithSlots
import net.yeeren.classgrid.data.ScheduleOverrideEntity
import net.yeeren.classgrid.data.ScheduleProjection
import net.yeeren.classgrid.data.SemesterSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    week: Int,
    settings: SemesterSettings,
    courses: List<CourseWithSlots>,
    overrides: List<ScheduleOverrideEntity>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fields by remember { mutableStateOf(ShareFields()) }
    var layout by remember { mutableStateOf(ShareLayout.LIST) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }
    val days = remember(week, settings, courses, overrides) {
        ScheduleProjection.forWeek(week, settings, courses, overrides)
    }

    LaunchedEffect(days, fields, settings, week, layout) {
        bitmap = null
        error = null
        runCatching {
            withContext(Dispatchers.Default) {
                ShareImageRenderer.render(settings, week, days, fields, layout)
            }
        }.onSuccess { bitmap = it }
            .onFailure { error = it.message ?: "图片生成失败" }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("分享第 $week 周课表") },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                    )
                },
                bottomBar = {
                    Button(
                        enabled = bitmap != null && !sharing,
                        onClick = {
                            val image = bitmap ?: return@Button
                            sharing = true
                            scope.launch {
                                runCatching {
                                    val intent = withContext(Dispatchers.IO) {
                                        ShareImageStorage.createShareIntent(context, image)
                                    }
                                    context.startActivity(intent)
                                }.onFailure { error = it.message ?: "分享失败" }
                                sharing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) { Text("分享图片") }
                }
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "图片样式",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShareLayoutButton("列表", layout == ShareLayout.LIST,
                            { layout = ShareLayout.LIST }, Modifier.weight(1f))
                        ShareLayoutButton("课表", layout == ShareLayout.TIMETABLE,
                            { layout = ShareLayout.TIMETABLE }, Modifier.weight(1f))
                    }
                    Text(
                        "选择要显示的信息",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        FieldCheckbox("教师", fields.teacher, { fields = fields.copy(teacher = it) }, Modifier.weight(1f))
                        FieldCheckbox("教室", fields.location, { fields = fields.copy(location = it) }, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        FieldCheckbox("节次时间", fields.time, { fields = fields.copy(time = it) }, Modifier.weight(1f))
                        FieldCheckbox("备注", fields.note, { fields = fields.copy(note = it) }, Modifier.weight(1f))
                    }
                    Text(
                        "预览",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                    when {
                        error != null -> Text(
                            error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        bitmap == null -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                        else -> Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "课表分享图片预览",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareLayoutButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(label) }
    else OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
}

@Composable
private fun FieldCheckbox(
    label: String,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = onSelected)
        Text(label)
    }
}
