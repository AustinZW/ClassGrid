package net.yeeren.classgrid.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.yeeren.classgrid.MainViewModel
import net.yeeren.classgrid.data.CourseSlotEntity
import net.yeeren.classgrid.data.CourseWithSlots
import net.yeeren.classgrid.data.ScheduleDefaults
import net.yeeren.classgrid.data.SectionTime
import net.yeeren.classgrid.data.SemesterSettings
import net.yeeren.classgrid.data.WeekRules
import net.yeeren.classgrid.data.asIndependentCopy
import net.yeeren.classgrid.data.ClassGridBackup
import net.yeeren.classgrid.data.ClassGridBackupCodec
import net.yeeren.classgrid.data.DhuImportResult
import net.yeeren.classgrid.data.DhuConflictPlan
import net.yeeren.classgrid.data.DhuImportConflictPlanner
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val courseColors = listOf(
    0xFF6750A4uL.toLong(), 0xFF006A6AuL.toLong(), 0xFF8C5000uL.toLong(),
    0xFF984061uL.toLong(), 0xFF3F6375uL.toLong(), 0xFF496727uL.toLong(),
    0xFF7E5260uL.toLong(), 0xFF5C5D72uL.toLong()
)

private data class ScheduledCourse(
    val course: CourseWithSlots,
    val slot: CourseSlotEntity
)

private data class CourseAdjustment(
    val course: CourseWithSlots,
    val originalSlot: CourseSlotEntity,
    val day: Int,
    val startSection: Int,
    val endSection: Int,
    val canApplyAllWeeks: Boolean
)

private enum class SettingsPage { SCHEDULE, ADVANCED, ABOUT }

private data class NewTimetableRequest(
    val suggestedName: String = "新课表",
    val importResult: DhuImportResult? = null
)

private data class DhuConflictRequest(
    val result: DhuImportResult,
    val existingCourses: List<CourseWithSlots>,
    val plan: DhuConflictPlan,
    val targetNewTimetable: Boolean
)

@Composable
fun ClassTableApp(viewModel: MainViewModel) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var viewingCourse by remember { mutableStateOf<CourseWithSlots?>(null) }
    var editingCourse by remember { mutableStateOf<CourseWithSlots?>(null) }
    var copyingCourse by remember { mutableStateOf<CourseWithSlots?>(null) }
    var newCourseSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var settingsPage by remember { mutableStateOf<SettingsPage?>(null) }
    var pendingDelete by remember { mutableStateOf<CourseWithSlots?>(null) }
    var conflictMessage by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<ClassGridBackup?>(null) }
    var exportContent by remember { mutableStateOf<String?>(null) }
    var transferNotice by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDhuImport by remember { mutableStateOf(false) }
    var newTimetableRequest by remember { mutableStateOf<NewTimetableRequest?>(null) }
    var deferredNewTimetable by remember { mutableStateOf<NewTimetableRequest?>(null) }
    var dhuConflictRequest by remember { mutableStateOf<DhuConflictRequest?>(null) }
    var selectedConflictCourses by remember { mutableStateOf(setOf<String>()) }

    fun completeNewTimetable(request: NewTimetableRequest) {
        val onResult: (Result<Unit>) -> Unit = { result ->
            transferNotice = result.fold(
                onSuccess = {
                    "新课表已创建" to if (request.importResult == null) {
                        "可以开始添加课程了。"
                    } else {
                        "已导入 ${request.importResult.courses.size} 门课程。"
                    }
                },
                onFailure = { error ->
                    "创建失败" to (error.message ?: "无法创建新课表")
                }
            )
        }
        request.importResult?.let { imported ->
            viewModel.importDhuCourses(
                result = imported,
                replaceCurrent = true,
                newTimetableName = request.suggestedName,
                onResult = onResult
            )
        } ?: viewModel.createEmptyTimetable(request.suggestedName, onResult)
    }

    fun importIntoCurrent(result: DhuImportResult, finalCourses: List<CourseWithSlots>, importedCount: Int) {
        viewModel.importDhuCourses(
            result = result.copy(courses = finalCourses),
            replaceCurrent = true,
            newTimetableName = state.settings.name
        ) { operation ->
            transferNotice = operation.fold(
                onSuccess = {
                    "导入完成" to if (importedCount > 0) {
                        "已向当前课表导入 $importedCount 门课程。"
                    } else {
                        "已保留当前课表中的课程，没有导入冲突课程。"
                    }
                },
                onFailure = { error ->
                    "导入失败" to (error.message ?: "无法导入课程")
                }
            )
        }
    }

    fun prepareDhuImport(result: DhuImportResult, targetNewTimetable: Boolean) {
        showDhuImport = false
        val existing = if (targetNewTimetable) emptyList() else state.courses
        val plan = DhuImportConflictPlanner.plan(
            existing = existing,
            imported = result.courses,
            includeExisting = !targetNewTimetable
        )
        if (plan != null) {
            dhuConflictRequest = DhuConflictRequest(result, existing, plan, targetNewTimetable)
            selectedConflictCourses = plan.defaultSelectedKeys
        } else if (targetNewTimetable) {
            newTimetableRequest = NewTimetableRequest(result.semesterName, result)
        } else {
            importIntoCurrent(result, state.courses + result.courses, result.courses.size)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ClassGridBackupCodec.MIME_TYPE)
    ) { uri ->
        val content = exportContent
        exportContent = null
        if (uri != null && content != null) {
            coroutineScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                            output.write(content.toByteArray(Charsets.UTF_8))
                        } ?: error("无法写入所选文件")
                    }
                }.onSuccess {
                    transferNotice = "导出完成" to "课格数据已保存为 .cgf 文件。"
                    deferredNewTimetable?.let { request ->
                        deferredNewTimetable = null
                        completeNewTimetable(request)
                    }
                }.onFailure { error ->
                    deferredNewTimetable = null
                    transferNotice = "导出失败" to (error.message ?: "无法写入文件")
                }
            }
        } else {
            deferredNewTimetable = null
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { readBackupFile(context, uri) }
                }.onSuccess { backup ->
                    pendingImport = backup
                }.onFailure { error ->
                    transferNotice = "导入失败" to (error.message ?: "文件内容无效")
                }
            }
        }
    }

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val activeSettingsPage = if (!state.settings.onboardingComplete) {
        SettingsPage.SCHEDULE
    } else {
        settingsPage
    }
    var displayedSettingsPage by remember { mutableStateOf<SettingsPage?>(null) }
    LaunchedEffect(activeSettingsPage) {
        if (activeSettingsPage != null) displayedSettingsPage = activeSettingsPage
    }

    Box(Modifier.fillMaxSize().clearFocusOnTap(focusManager)) {
        ScheduleHome(
            semesterName = state.settings.name,
            selectedWeek = state.selectedWeek,
            totalWeeks = state.settings.totalWeeks,
            settings = state.settings,
            courses = state.courses,
            onWeekSelected = viewModel::selectWeek,
            onCurrentWeek = viewModel::goToCurrentWeek,
            onAdd = { newCourseSlot = (state.settings.visibleDays.minOrNull() ?: 1) to 1 },
            onEmptySlotClick = { day, section -> newCourseSlot = day to section },
            onCourseClick = { viewingCourse = it },
            onCourseCopy = { copyingCourse = it.asIndependentCopy() },
            onCourseAdjust = viewModel::adjustCourseSlot,
            canAdjustCourse = viewModel::canAdjustCourseSlot,
            onCourseConflict = {
                conflictMessage = "该时间已有课程，请选择其他星期、节次或周次。"
            },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onDhuImport = { showDhuImport = true },
            onNewTimetable = { newTimetableRequest = NewTimetableRequest() },
            onExport = {
                runCatching {
                    ClassGridBackupCodec.encode(state.settings, state.courses)
                }.onSuccess { content ->
                    exportContent = content
                    exportLauncher.launch("ClassGrid-${LocalDate.now()}.cgf")
                }.onFailure { error ->
                    transferNotice = "导出失败" to (error.message ?: "无法生成备份")
                }
            },
            onSettingsSelected = { settingsPage = it }
        )
    }

    viewingCourse?.let { course ->
        CourseDetailsDialog(
            course = course,
            onDismiss = { viewingCourse = null },
            onEdit = {
                viewingCourse = null
                editingCourse = course
            }
        )
    }

    if (editingCourse != null || copyingCourse != null || newCourseSlot != null) {
        val initial = editingCourse ?: copyingCourse
        val firstSlot = initial?.slots?.firstOrNull()
        val slot = newCourseSlot ?: ((firstSlot?.dayOfWeek ?: 1) to (firstSlot?.startSection ?: 1))
        CourseEditorDialog(
            course = initial,
            isCopy = copyingCourse != null,
            initialDay = slot.first,
            initialSection = slot.second,
            totalWeeks = state.settings.totalWeeks,
            sectionsPerDay = state.settings.sectionsPerDay,
            visibleDays = state.settings.visibleDays,
            onDismiss = {
                editingCourse = null
                copyingCourse = null
                newCourseSlot = null
            },
            onSave = {
                if (viewModel.saveCourseIfNoConflict(it)) {
                    editingCourse = null
                    copyingCourse = null
                    newCourseSlot = null
                } else {
                    conflictMessage = "该课程与已有课程时间冲突，请修改时段后再保存。"
                }
            },
            onDelete = editingCourse?.let { course ->
                {
                    pendingDelete = course
                    editingCourse = null
                }
            }
        )
    }

    pendingDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除课程？") },
            text = { Text("“${course.course.name}”及其所有时段将被永久删除。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse(course)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }

    conflictMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { conflictMessage = null },
            icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
            title = { Text("课程冲突") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { conflictMessage = null }) { Text("知道了") }
            }
        )
    }

    pendingImport?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            icon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
            title = { Text("导入课格数据？") },
            text = {
                Text(
                    "将使用“${backup.settings.name}”及 ${backup.courses.size} 门课程替换当前全部数据。"
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingImport = null
                    viewModel.importBackup(backup) { result ->
                        transferNotice = result.fold(
                            onSuccess = {
                                "导入完成" to "课程表设置和课程数据已恢复。"
                            },
                            onFailure = { error ->
                                "导入失败" to (error.message ?: "无法替换当前数据")
                            }
                        )
                    }
                }) { Text("替换并导入") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消") }
            }
        )
    }

    newTimetableRequest?.let { request ->
        var timetableName by remember(request) {
            mutableStateOf(request.suggestedName.ifBlank { "新课表" })
        }
        AlertDialog(
            onDismissRequest = { newTimetableRequest = null },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            title = { Text(if (request.importResult == null) "新建课表" else "导入到新课表") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = timetableName,
                        onValueChange = { timetableName = it.take(120) },
                        label = { Text("课表名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.courses.isNotEmpty()) {
                        Text("当前课表已有 ${state.courses.size} 门课程。可以先导出为 .cgf 文件，再创建新课表。")
                    } else if (request.importResult != null) {
                        Text("将创建新课表并导入 ${request.importResult.courses.size} 门课程。")
                    }
                }
            },
            confirmButton = {
                if (state.courses.isNotEmpty()) {
                    Button(
                        enabled = timetableName.isNotBlank(),
                        onClick = {
                            val namedRequest = request.copy(suggestedName = timetableName.trim())
                            newTimetableRequest = null
                            runCatching {
                                ClassGridBackupCodec.encode(state.settings, state.courses)
                            }.onSuccess { content ->
                                deferredNewTimetable = namedRequest
                                exportContent = content
                                exportLauncher.launch("ClassGrid-${LocalDate.now()}.cgf")
                            }.onFailure { error ->
                                transferNotice = "导出失败" to (error.message ?: "无法生成备份")
                            }
                        }
                    ) { Text("保存后新建") }
                } else {
                    Button(
                        enabled = timetableName.isNotBlank(),
                        onClick = {
                            newTimetableRequest = null
                            completeNewTimetable(request.copy(suggestedName = timetableName.trim()))
                        }
                    ) { Text("创建") }
                }
            },
            dismissButton = {
                Row {
                    if (state.courses.isNotEmpty()) {
                        TextButton(
                            enabled = timetableName.isNotBlank(),
                            onClick = {
                                newTimetableRequest = null
                                completeNewTimetable(request.copy(suggestedName = timetableName.trim()))
                            }
                        ) { Text("不保存并新建") }
                    }
                    TextButton(onClick = { newTimetableRequest = null }) { Text("取消") }
                }
            }
        )
    }

    dhuConflictRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { dhuConflictRequest = null },
            icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
            title = { Text("选择要保留的课程") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "检测到时间和周次重叠。勾选一门课程时，会自动取消与它冲突的课程。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        request.plan.candidates.forEach { candidate ->
                            val checked = candidate.key in selectedConflictCourses
                            Surface(
                                onClick = {
                                    selectedConflictCourses = if (checked) {
                                        selectedConflictCourses - candidate.key
                                    } else {
                                        (selectedConflictCourses - request.plan.conflictsWith(candidate.key)) + candidate.key
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (checked) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            selectedConflictCourses = if (checked) {
                                                selectedConflictCourses - candidate.key
                                            } else {
                                                (selectedConflictCourses - request.plan.conflictsWith(candidate.key)) + candidate.key
                                            }
                                        }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(candidate.course.course.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (candidate.isImported) "教务系统课程" else "当前课表课程",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            candidate.course.slots.joinToString("；") { slot ->
                                                "${dayNames.getOrElse(slot.dayOfWeek - 1) { "星期" }} " +
                                                    "${slot.startSection}-${slot.endSection} 节 " +
                                                    "${WeekRules.compactWeeks(slot.activeWeeks)} 周"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val resolved = DhuImportConflictPlanner.resolve(
                        existing = request.existingCourses,
                        imported = request.result.courses,
                        plan = request.plan,
                        selectedKeys = selectedConflictCourses,
                        includeExisting = !request.targetNewTimetable
                    )
                    dhuConflictRequest = null
                    if (request.targetNewTimetable) {
                        val adjusted = request.result.copy(courses = resolved.finalCourses)
                        if (adjusted.courses.isEmpty()) {
                            transferNotice = "未导入课程" to "请至少保留一门教务系统课程。"
                        } else {
                            newTimetableRequest = NewTimetableRequest(adjusted.semesterName, adjusted)
                        }
                    } else {
                        importIntoCurrent(request.result, resolved.finalCourses, resolved.importedCourseCount)
                    }
                }) { Text("确认保留") }
            },
            dismissButton = {
                TextButton(onClick = { dhuConflictRequest = null }) { Text("取消") }
            }
        )
    }

    transferNotice?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { transferNotice = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { transferNotice = null }) { Text("确定") }
            }
        )
    }

    AnimatedVisibility(
        visible = activeSettingsPage != null,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 300),
            initialOffsetX = { it }
        ) + fadeIn(animationSpec = tween(180)),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 250),
            targetOffsetX = { it }
        ) + fadeOut(animationSpec = tween(180))
    ) {
        val page = activeSettingsPage ?: displayedSettingsPage
        if (page != null) {
            SettingsScreen(
                page = page,
                current = state.settings,
                isInitialization = !state.settings.onboardingComplete,
                onBack = { settingsPage = null },
                onAutoSave = viewModel::saveSettings,
                onCompleteInitialization = {
                    viewModel.saveSettings(it.copy(onboardingComplete = true))
                    settingsPage = null
                }
            )
        }
    }

    if (showDhuImport) {
        DhuImportScreen(
            onClose = { showDhuImport = false },
            onImportCurrent = { result ->
                prepareDhuImport(result, targetNewTimetable = false)
            },
            onImportNew = { result ->
                prepareDhuImport(result, targetNewTimetable = true)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleHome(
    semesterName: String,
    selectedWeek: Int,
    totalWeeks: Int,
    settings: SemesterSettings,
    courses: List<CourseWithSlots>,
    onWeekSelected: (Int) -> Unit,
    onCurrentWeek: () -> Unit,
    onAdd: () -> Unit,
    onEmptySlotClick: (Int, Int) -> Unit,
    onCourseClick: (CourseWithSlots) -> Unit,
    onCourseCopy: (CourseWithSlots) -> Unit,
    onCourseAdjust: (CourseWithSlots, CourseSlotEntity, Int, Int, Int, Int?) -> Boolean,
    canAdjustCourse: (CourseWithSlots, CourseSlotEntity, Int, Int, Int, Int?) -> Boolean,
    onCourseConflict: () -> Unit,
    onImport: () -> Unit,
    onDhuImport: () -> Unit,
    onNewTimetable: () -> Unit,
    onExport: () -> Unit,
    onSettingsSelected: (SettingsPage) -> Unit
) {
    val timetableScroll = rememberScrollState()
    val currentWeek = WeekRules.currentWeek(settings.startDate, LocalDate.now(), totalWeeks)
    val pagerState = rememberPagerState(
        initialPage = (selectedWeek - 1).coerceIn(0, totalWeeks - 1),
        pageCount = { totalWeeks }
    )
    LaunchedEffect(selectedWeek) {
        val page = (selectedWeek - 1).coerceIn(0, totalWeeks - 1)
        if (pagerState.currentPage != page) pagerState.animateScrollToPage(page)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { onWeekSelected(it + 1) }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(semesterName, fontWeight = FontWeight.SemiBold)
                        Text(
                            "共 $totalWeeks 周",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedWeek != currentWeek) {
                            TextButton(
                                onClick = onCurrentWeek,
                                modifier = Modifier.height(48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) { Text("本周") }
                        }
                        WeekSelector(
                            week = selectedWeek,
                            totalWeeks = totalWeeks,
                            onWeekSelected = onWeekSelected
                        )
                        SettingsMenuButton(
                            onSelected = onSettingsSelected,
                            onImport = onImport,
                            onDhuImport = onDhuImport,
                            onNewTimetable = onNewTimetable,
                            onExport = onExport
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加课程") }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val pageWeek = page + 1
                val pageCourses = remember(courses, pageWeek, settings.visibleDays) {
                    courses.flatMap { course ->
                        course.slots
                            .filter { it.isActiveIn(pageWeek) && it.dayOfWeek in settings.visibleDays }
                            .map { ScheduledCourse(course, it) }
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().verticalScroll(timetableScroll)) {
                        Timetable(
                            courses = pageCourses,
                            week = pageWeek,
                            startDate = settings.startDate,
                            visibleDays = settings.visibleDays,
                            sectionTimes = settings.sectionTimes,
                            slotHeight = settings.courseCellHeightDp.dp,
                            titleFontScale = settings.courseTitleFontScale,
                            detailFontScale = settings.courseDetailFontScale,
                            onEmptySlotClick = onEmptySlotClick,
                            onCourseClick = onCourseClick,
                            onCourseCopy = onCourseCopy,
                            onCourseAdjust = onCourseAdjust,
                            canAdjustCourse = canAdjustCourse,
                            onCourseConflict = onCourseConflict,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp + settings.courseCellHeightDp.dp * settings.sectionTimes.size + 96.dp)
                        )
                    }
                    if (pageCourses.isEmpty()) {
                        EmptyWeekState(Modifier.fillMaxSize().padding(start = 58.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSelector(
    week: Int,
    totalWeeks: Int,
    onWeekSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.height(48.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("第 $week 周", fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "选择周次")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..totalWeeks).forEach { option ->
                DropdownMenuItem(
                    text = { Text("第 $option 周", fontWeight = if (option == week) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onWeekSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuButton(
    onSelected: (SettingsPage) -> Unit,
    onImport: () -> Unit,
    onDhuImport: () -> Unit,
    onNewTimetable: () -> Unit,
    onExport: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "设置")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                SettingsPage.SCHEDULE to "课程表设置",
                SettingsPage.ADVANCED to "高级设置",
                SettingsPage.ABOUT to "关于"
            ).forEach { (page, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelected(page)
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("新建课表") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    expanded = false
                    onNewTimetable()
                }
            )
            DropdownMenuItem(
                text = { Text("从教务系统导入") },
                leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDhuImport()
                }
            )
            DropdownMenuItem(
                text = { Text("从文件导入") },
                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                onClick = {
                    expanded = false
                    onImport()
                }
            )
            DropdownMenuItem(
                text = { Text("导出到文件") },
                leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) },
                onClick = {
                    expanded = false
                    onExport()
                }
            )
        }
    }
}

private fun readBackupFile(context: android.content.Context, uri: android.net.Uri): ClassGridBackup {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= ClassGridBackupCodec.MAX_FILE_BYTES) { "文件超过 5 MB 限制" }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("无法读取所选文件")
    return ClassGridBackupCodec.decode(bytes.toString(Charsets.UTF_8))
}

@Composable
private fun Timetable(
    courses: List<ScheduledCourse>,
    week: Int,
    startDate: LocalDate,
    visibleDays: Set<Int>,
    sectionTimes: List<SectionTime>,
    slotHeight: Dp,
    titleFontScale: Float,
    detailFontScale: Float,
    onEmptySlotClick: (Int, Int) -> Unit,
    onCourseClick: (CourseWithSlots) -> Unit,
    onCourseCopy: (CourseWithSlots) -> Unit,
    onCourseAdjust: (CourseWithSlots, CourseSlotEntity, Int, Int, Int, Int?) -> Boolean,
    canAdjustCourse: (CourseWithSlots, CourseSlotEntity, Int, Int, Int, Int?) -> Boolean,
    onCourseConflict: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shownDays = visibleDays.sorted()
    var adjustment by remember(week) { mutableStateOf<CourseAdjustment?>(null) }
    var adjustmentSaving by remember(week) { mutableStateOf(false) }

    LaunchedEffect(courses, adjustmentSaving) {
        val pending = adjustment
        if (adjustmentSaving && pending != null) {
            val persisted = courses.any { scheduled ->
                scheduled.course.course.id == pending.course.course.id &&
                    scheduled.slot.dayOfWeek == pending.day &&
                    scheduled.slot.startSection == pending.startSection &&
                    scheduled.slot.endSection == pending.endSection &&
                    scheduled.slot.isActiveIn(week)
            }
            if (persisted) {
                adjustment = null
                adjustmentSaving = false
            }
        }
    }

    if (shownDays.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请在设置中至少选择一个上课日", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    BoxWithConstraints(modifier) {
        val timetableWidth = maxWidth
        val timeWidth = (maxWidth * 0.15f).coerceIn(54.dp, 64.dp)
        val dayWidth = (maxWidth - timeWidth) / shownDays.size.toFloat()
        Column(Modifier.fillMaxSize().padding(bottom = 96.dp)) {
            Row {
                Spacer(Modifier.width(timeWidth).height(54.dp))
                shownDays.forEach { day ->
                    val date = startDate.plusDays(((week - 1) * 7L) + day - 1L)
                    val isToday = date == LocalDate.now()
                    Box(
                        Modifier.width(dayWidth).height(54.dp).padding(horizontal = 2.dp, vertical = 3.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    dayNames[day - 1],
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isToday) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    "${date.monthValue}/${date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isToday) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(slotHeight * sectionTimes.size)
            ) {
                Column {
                    sectionTimes.forEachIndexed { index, time ->
                        val section = index + 1
                        Row {
                            Box(
                                Modifier
                                    .width(timeWidth)
                                    .height(slotHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("第 $section 节", style = MaterialTheme.typography.labelMedium)
                                    Text(time.start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(time.end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            shownDays.forEach { day ->
                                Box(
                                    Modifier
                                        .width(dayWidth)
                                        .height(slotHeight)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .clickable { onEmptySlotClick(day, section) }
                                )
                            }
                        }
                    }
                }
                if (adjustment != null && !adjustmentSaving) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(2f)
                            .pointerInput(week) {
                                detectTapGestures {
                                    adjustment = null
                                    adjustmentSaving = false
                                }
                            }
                    )
                }
                courses.forEach { course ->
                    val activeAdjustment = adjustment?.takeIf {
                        it.course.course.id == course.course.course.id &&
                            it.originalSlot.id == course.slot.id
                    }
                    val displayedSlot = activeAdjustment?.let {
                        course.slot.copy(
                            dayOfWeek = it.day,
                            startSection = it.startSection,
                            endSection = it.endSection
                        )
                    } ?: course.slot
                    val displayedCourse = course.copy(slot = displayedSlot)
                    val dayIndex = shownDays.indexOf(displayedSlot.dayOfWeek)
                    if (dayIndex >= 0) {
                        CourseBlock(
                            scheduled = displayedCourse,
                            dayIndex = dayIndex,
                            timeWidth = timeWidth,
                            dayWidth = dayWidth,
                            slotHeight = slotHeight,
                            titleFontScale = titleFontScale,
                            detailFontScale = detailFontScale,
                            shownDays = shownDays,
                            sectionCount = sectionTimes.size,
                            isAdjusting = activeAdjustment != null,
                            adjustmentControlsVisible = activeAdjustment != null && !adjustmentSaving,
                            onClick = { onCourseClick(course.course) },
                            onMove = { day, section ->
                                val span = course.slot.endSection - course.slot.startSection
                                val endSection = section + span
                                val canApplyCurrentWeek = canAdjustCourse(
                                    course.course, course.slot, day, section, endSection, week
                                )
                                if (!canApplyCurrentWeek) {
                                    adjustment = null
                                    adjustmentSaving = false
                                    onCourseConflict()
                                } else {
                                    adjustment = CourseAdjustment(
                                        course = course.course,
                                        originalSlot = course.slot,
                                        day = day,
                                        startSection = section,
                                        endSection = endSection,
                                        canApplyAllWeeks = canAdjustCourse(
                                            course.course, course.slot, day, section, endSection, null
                                        )
                                    )
                                    adjustmentSaving = false
                                }
                            },
                            onResizeStart = { delta ->
                                adjustment?.let { current ->
                                    val start = (current.startSection + delta)
                                        .coerceIn(1, current.endSection)
                                    val canApplyCurrentWeek = canAdjustCourse(
                                        current.course, current.originalSlot, current.day,
                                        start, current.endSection, week
                                    )
                                    if (canApplyCurrentWeek) {
                                        adjustment = current.copy(
                                            startSection = start,
                                            canApplyAllWeeks = canAdjustCourse(
                                                current.course, current.originalSlot, current.day,
                                                start, current.endSection, null
                                            )
                                        )
                                    } else {
                                        adjustment = null
                                        onCourseConflict()
                                    }
                                }
                            },
                            onResizeEnd = { delta ->
                                adjustment?.let { current ->
                                    val end = (current.endSection + delta)
                                        .coerceIn(current.startSection, sectionTimes.size)
                                    val canApplyCurrentWeek = canAdjustCourse(
                                        current.course, current.originalSlot, current.day,
                                        current.startSection, end, week
                                    )
                                    if (canApplyCurrentWeek) {
                                        adjustment = current.copy(
                                            endSection = end,
                                            canApplyAllWeeks = canAdjustCourse(
                                                current.course, current.originalSlot, current.day,
                                                current.startSection, end, null
                                            )
                                        )
                                    } else {
                                        adjustment = null
                                        onCourseConflict()
                                    }
                                }
                            }
                        )
                    }
                }
                adjustment?.takeIf { !adjustmentSaving }?.let { pending ->
                    val pendingDayIndex = shownDays.indexOf(pending.day)
                    if (pendingDayIndex >= 0) {
                        val panelWidth = 144.dp
                        val panelHeight = 160.dp
                        val gap = 4.dp
                        val courseLeft = timeWidth + dayWidth * pendingDayIndex
                        val courseRight = courseLeft + dayWidth
                        val spaceOnRight = timetableWidth - courseRight
                        val panelX = when {
                            spaceOnRight >= panelWidth + gap -> courseRight + gap
                            courseLeft >= panelWidth + gap -> courseLeft - panelWidth - gap
                            spaceOnRight >= courseLeft ->
                                (timetableWidth - panelWidth).coerceAtLeast(0.dp)
                            else -> 0.dp
                        }
                        val courseTop = slotHeight * (pending.startSection - 1)
                        val courseHeight = slotHeight *
                            (pending.endSection - pending.startSection + 1)
                        val maxPanelY = (slotHeight * sectionTimes.size - panelHeight)
                            .coerceAtLeast(0.dp)
                        val panelY = (courseTop + courseHeight / 2f - panelHeight / 2f)
                            .coerceIn(0.dp, maxPanelY)
                        CourseAdjustmentActions(
                            modifier = Modifier
                                .offset(x = panelX, y = panelY)
                                .width(panelWidth)
                                .zIndex(6f),
                            canApplyAllWeeks = pending.canApplyAllWeeks,
                            onCopy = {
                                adjustment = null
                                adjustmentSaving = false
                                onCourseCopy(pending.course)
                            },
                            onApplyCurrentWeek = {
                                val applied = onCourseAdjust(
                                    pending.course, pending.originalSlot, pending.day,
                                    pending.startSection, pending.endSection, week
                                )
                                if (applied) {
                                    adjustmentSaving = true
                                } else {
                                    adjustment = null
                                    adjustmentSaving = false
                                    onCourseConflict()
                                }
                            },
                            onApplyAllWeeks = {
                                val applied = onCourseAdjust(
                                    pending.course, pending.originalSlot, pending.day,
                                    pending.startSection, pending.endSection, null
                                )
                                if (applied) {
                                    adjustmentSaving = true
                                } else {
                                    adjustment = null
                                    adjustmentSaving = false
                                    onCourseConflict()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWeekState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.offset(y = (-32).dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "本周暂无课程，可点击空白时段或右下角添加课程。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CourseDetailsDialog(
    course: CourseWithSlots,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .width(6.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(course.course.colorArgb.toInt()))
                )
                Text(course.course.name)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (course.course.teacher.isNotBlank()) {
                    Text("教师：${course.course.teacher}")
                }
                course.slots.forEachIndexed { index, slot ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (course.slots.size > 1) {
                                Text("时段 ${index + 1}", fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                "${dayNames[slot.dayOfWeek - 1]} · " +
                                    "第 ${slot.startSection}–${slot.endSection} 节"
                            )
                            Text("教室：${slot.location.ifBlank { "未填写" }}")
                            Text("周次：第 ${WeekRules.compactWeeks(slot.activeWeeks)} 周")
                        }
                    }
                }
                if (course.course.note.isNotBlank()) {
                    Text("备注", fontWeight = FontWeight.SemiBold)
                    Text(course.course.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onEdit) { Text("编辑") }
        }
    )
}

@Composable
private fun CourseBlock(
    scheduled: ScheduledCourse,
    dayIndex: Int,
    timeWidth: Dp,
    dayWidth: Dp,
    slotHeight: Dp,
    titleFontScale: Float,
    detailFontScale: Float,
    shownDays: List<Int>,
    sectionCount: Int,
    isAdjusting: Boolean,
    adjustmentControlsVisible: Boolean,
    onClick: () -> Unit,
    onMove: (day: Int, startSection: Int) -> Unit,
    onResizeStart: (Int) -> Unit,
    onResizeEnd: (Int) -> Unit
) {
    val course = scheduled.course.course
    val slot = scheduled.slot
    val span = (slot.endSection - slot.startSection + 1).coerceAtLeast(1)
    // Stored colors are ordinary 32-bit ARGB values. Passing them as ULong would
    // be interpreted as Compose's internal packed color-space representation.
    val background = Color(course.colorArgb.toInt())
    val density = LocalDensity.current
    val dayWidthPx = with(density) { dayWidth.toPx() }
    val slotHeightPx = with(density) { slotHeight.toPx() }
    val dragThresholdPx = with(density) { 12.dp.toPx() }
    var dragOffset by remember(scheduled.slot.id) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(scheduled.slot.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .offset(
                x = timeWidth + dayWidth * dayIndex,
                y = slotHeight * (slot.startSection - 1)
            )
            .width(dayWidth)
            .height(slotHeight * span)
            .padding(2.dp)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                if (isDragging) {
                    scaleX = 1.03f
                    scaleY = 1.03f
                    shadowElevation = 12.dp.toPx()
                }
            }
            .zIndex(if (isDragging || isAdjusting) 3f else 1f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .pointerInput(
                    scheduled.slot.id,
                    shownDays,
                    sectionCount,
                    dayWidthPx,
                    slotHeightPx
                ) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDragging = true
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = Offset.Zero
                        },
                        onDragEnd = {
                            val moved = dragOffset.getDistance() >= dragThresholdPx
                            val targetDayIndex = if (moved) {
                                (dayIndex + (dragOffset.x / dayWidthPx).roundToInt())
                                    .coerceIn(shownDays.indices)
                            } else dayIndex
                            val maxStart = (sectionCount - span + 1).coerceAtLeast(1)
                            val targetSection = if (moved) {
                                (slot.startSection + (dragOffset.y / slotHeightPx).roundToInt())
                                    .coerceIn(1, maxStart)
                            } else slot.startSection
                            onMove(shownDays[targetDayIndex], targetSection)
                            isDragging = false
                            dragOffset = Offset.Zero
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = background,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(Modifier.padding(6.dp)) {
                Text(
                    course.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = (11f * titleFontScale).sp,
                    lineHeight = (13f * titleFontScale).sp
                )
                if (slot.location.isNotBlank()) {
                    Text(
                        slot.location,
                        fontSize = (9f * detailFontScale).sp,
                        lineHeight = (11f * detailFontScale).sp
                    )
                }
                if (course.teacher.isNotBlank()) {
                    Text(
                        course.teacher,
                        fontSize = (9f * detailFontScale).sp,
                        lineHeight = (11f * detailFontScale).sp
                    )
                }
            }
        }
        if (adjustmentControlsVisible) {
            ResizeHandle(
                iconUp = true,
                stepSize = slotHeight,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp),
                onStep = onResizeStart
            )
            ResizeHandle(
                iconUp = false,
                stepSize = slotHeight,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 16.dp),
                onStep = onResizeEnd
            )
        }
    }
}

@Composable
private fun CourseAdjustmentActions(
    canApplyAllWeeks: Boolean,
    onCopy: () -> Unit,
    onApplyCurrentWeek: () -> Unit,
    onApplyAllWeeks: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            DropdownMenuItem(
                text = { Text("复制课程") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = onCopy
            )
            DropdownMenuItem(
                text = { Text("仅更改本周") },
                onClick = onApplyCurrentWeek
            )
            DropdownMenuItem(
                text = { Text("更改全部周次") },
                onClick = onApplyAllWeeks,
                enabled = canApplyAllWeeks
            )
        }
    }
}

@Composable
private fun ResizeHandle(
    iconUp: Boolean,
    stepSize: Dp,
    modifier: Modifier = Modifier,
    onStep: (Int) -> Unit
) {
    val density = LocalDensity.current
    val stepThreshold = with(density) { stepSize.toPx() }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    Surface(
        modifier = modifier
            .size(40.dp)
            .zIndex(5f)
            .graphicsLayer { translationY = dragOffsetY }
            .clickable { onStep(if (iconUp) -1 else 1) }
            .pointerInput(stepThreshold) {
                detectVerticalDragGestures(
                    onDragStart = { dragOffsetY = 0f },
                    onDragCancel = { dragOffsetY = 0f },
                    onDragEnd = { dragOffsetY = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragOffsetY += amount
                        while (kotlin.math.abs(dragOffsetY) >= stepThreshold) {
                            val direction = if (dragOffsetY > 0) 1 else -1
                            onStep(direction)
                            dragOffsetY -= direction * stepThreshold
                        }
                    }
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 4.dp
    ) {
        Icon(
            imageVector = if (iconUp) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (iconUp) "拖动调整开始节次" else "拖动调整结束节次",
            modifier = Modifier.padding(4.dp)
        )
    }
}

/* Legacy single-slot editor retained in history; replaced by CourseEditorDialog.kt.
private enum class WeekMode { ALL, ODD, CUSTOM }

@Composable
private fun CourseEditorDialog(
    course: CourseEntity?,
    initialDay: Int,
    initialSection: Int,
    totalWeeks: Int,
    sectionsPerDay: Int,
    visibleDays: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(course?.id) { mutableStateOf(course?.name.orEmpty()) }
    var teacher by remember(course?.id) { mutableStateOf(course?.teacher.orEmpty()) }
    var location by remember(course?.id) { mutableStateOf(course?.location.orEmpty()) }
    var day by remember(course?.id) {
        mutableIntStateOf(course?.dayOfWeek?.takeIf { it in visibleDays } ?: initialDay)
    }
    var start by remember(course?.id) { mutableStateOf((course?.startSection ?: initialSection).toString()) }
    var end by remember(course?.id) { mutableStateOf((course?.endSection ?: initialSection).toString()) }
    var mode by remember(course?.id) { mutableStateOf(if (course == null) WeekMode.ALL else WeekMode.CUSTOM) }
    var customWeeks by remember(course?.id) { mutableStateOf(course?.activeWeeks.orEmpty()) }
    var color by remember(course?.id) { mutableLongStateOf(course?.colorArgb ?: courseColors.first()) }
    var note by remember(course?.id) { mutableStateOf(course?.note.orEmpty()) }

    val startNumber = start.toIntOrNull()
    val endNumber = end.toIntOrNull()
    val weeks = when (mode) {
        WeekMode.ALL -> WeekRules.allWeeks(totalWeeks)
        WeekMode.ODD -> WeekRules.oddWeeks(totalWeeks)
        WeekMode.CUSTOM -> WeekRules.normalizeCustomWeeks(customWeeks, totalWeeks)
    }
    val valid = name.isNotBlank() && startNumber != null && endNumber != null &&
        startNumber in 1..sectionsPerDay && endNumber in startNumber..sectionsPerDay && weeks.isNotBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(if (course == null) "添加课程" else "编辑课程", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(name, { name = it }, label = { Text("课程名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(teacher, { teacher = it }, label = { Text("教师") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(location, { location = it }, label = { Text("教室") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("星期", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleDays.sorted().forEach { dayNumber ->
                            FilterChip(
                                selected = day == dayNumber,
                                onClick = { day = dayNumber },
                                label = { Text(dayNames[dayNumber - 1]) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(start, { start = it.filter(Char::isDigit) }, label = { Text("开始节次") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(end, { end = it.filter(Char::isDigit) }, label = { Text("结束节次") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("上课周次", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(WeekMode.ALL to "每周", WeekMode.ODD to "单周", WeekMode.CUSTOM to "自定义").forEach { (item, label) ->
                            FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(label) })
                        }
                    }
                    if (mode == WeekMode.CUSTOM) {
                        OutlinedTextField(
                            customWeeks,
                            { customWeeks = it },
                            label = { Text("周次，例如 1-4,6,8") },
                            supportingText = { Text("有效范围：1–$totalWeeks 周") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("课程颜色", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        courseColors.forEach { option ->
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(option.toInt()))
                                    .then(if (color == option) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                    .clickable { color = option }
                            )
                        }
                    }
                    OutlinedTextField(note, { note = it }, label = { Text("备注") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    } else Spacer(Modifier.width(1.dp))
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Button(
                            onClick = {
                                onSave(
                                    CourseEntity(
                                        id = course?.id ?: 0,
                                        name = name.trim(),
                                        teacher = teacher.trim(),
                                        location = location.trim(),
                                        dayOfWeek = day,
                                        startSection = startNumber!!,
                                        endSection = endNumber!!,
                                        activeWeeks = weeks,
                                        colorArgb = color,
                                        note = note.trim()
                                    )
                                )
                            },
                            enabled = valid
                        ) { Text("保存") }
                    }
                }
            }
        }
    }
}
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    page: SettingsPage,
    current: SemesterSettings,
    isInitialization: Boolean,
    onBack: () -> Unit,
    onAutoSave: (SemesterSettings) -> Unit,
    onCompleteInitialization: (SemesterSettings) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf(current.name) }
    var startDate by remember { mutableStateOf(current.startDate) }
    var totalWeeks by remember { mutableStateOf(current.totalWeeks.toString()) }
    var sections by remember { mutableStateOf(current.sectionsPerDay.toString()) }
    var sectionsFocused by remember { mutableStateOf(false) }
    var times by remember { mutableStateOf(current.sectionTimes) }
    var visibleDays by remember { mutableStateOf(current.visibleDays) }
    var courseCellHeightDp by remember { mutableIntStateOf(current.courseCellHeightDp) }
    var cellHeightInput by remember { mutableStateOf(current.courseCellHeightDp.toString()) }
    var cellHeightInputFocused by remember { mutableStateOf(false) }
    var titleFontScale by remember { mutableFloatStateOf(current.courseTitleFontScale) }
    var detailFontScale by remember { mutableFloatStateOf(current.courseDetailFontScale) }
    var unifiedFontScale by remember { mutableStateOf(current.unifiedCourseFontScale) }
    var unifiedScale by remember {
        mutableFloatStateOf((current.courseTitleFontScale + current.courseDetailFontScale) / 2f)
    }
    var showWeekPicker by remember { mutableStateOf(false) }
    val parsedWeeks = totalWeeks.toIntOrNull()
    val parsedSections = sections.toIntOrNull()
    val timesForSave = parsedSections
        ?.takeIf { it in 1..20 }
        ?.let { ScheduleDefaults.timesForCount(it, times) }
        ?: times
    val timePattern = remember { Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$") }
    val valid = name.isNotBlank() && parsedWeeks != null && parsedWeeks in 1..30 &&
        parsedSections != null && parsedSections in 1..20 && visibleDays.isNotEmpty() &&
        timesForSave.all { timePattern.matches(it.start) && timePattern.matches(it.end) }

    val draftSettings = current.copy(
        name = name.trim(),
        startDate = startDate,
        totalWeeks = parsedWeeks ?: current.totalWeeks,
        sectionsPerDay = parsedSections ?: current.sectionsPerDay,
        sectionTimes = timesForSave,
        visibleDays = visibleDays,
        courseCellHeightDp = courseCellHeightDp,
        courseTitleFontScale = titleFontScale,
        courseDetailFontScale = detailFontScale,
        unifiedCourseFontScale = unifiedFontScale
    )

    val saveAndGoBack = {
        if (page != SettingsPage.ABOUT && valid) onAutoSave(draftSettings)
        onBack()
    }
    BackHandler(enabled = !isInitialization, onBack = saveAndGoBack)
    LaunchedEffect(draftSettings, valid, page) {
        if (page != SettingsPage.ABOUT && valid) {
            delay(350)
            onAutoSave(draftSettings)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isInitialization -> "初始化课程表"
                            page == SettingsPage.SCHEDULE -> "课程表设置"
                            page == SettingsPage.ADVANCED -> "高级设置"
                            else -> "关于"
                        }
                    )
                },
                navigationIcon = {
                    if (!isInitialization) {
                        IconButton(onClick = saveAndGoBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clearFocusOnTap(focusManager)
        ) {
                if (isInitialization) {
                    Text(
                        "设置学期、上课日和每节课时间，之后可随时修改。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (page) {
                    SettingsPage.SCHEDULE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text("学期名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { showWeekPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("第一周：$startDate — ${startDate.plusDays(6)}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            totalWeeks,
                            { totalWeeks = it.filter(Char::isDigit) },
                            label = { Text("总周数") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            sections,
                            {
                                sections = it.filter(Char::isDigit)
                            },
                            label = { Text("每日节数") },
                            supportingText = { Text("1–20") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (sectionsFocused && !focusState.isFocused) {
                                        sections.toIntOrNull()
                                            ?.takeIf { count -> count in 1..20 }
                                            ?.let { count -> times = ScheduleDefaults.timesForCount(count, times) }
                                    }
                                    sectionsFocused = focusState.isFocused
                                }
                        )
                    }
                    Text("单周上课日", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dayNames.forEachIndexed { index, label ->
                            val day = index + 1
                            FilterChip(
                                selected = day in visibleDays,
                                onClick = {
                                    visibleDays = if (day in visibleDays) visibleDays - day else visibleDays + day
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (visibleDays.isEmpty()) {
                        Text("请至少选择一个上课日", color = MaterialTheme.colorScheme.error)
                    }
                    Text("每节课时间", style = MaterialTheme.typography.titleSmall)
                    times.forEachIndexed { index, time ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", modifier = Modifier.width(24.dp), fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                time.start,
                                { value ->
                                    times = times.toMutableList().also { it[index] = time.copy(start = value) }
                                },
                                label = { Text("开始") },
                                placeholder = { Text("00:00") },
                                singleLine = true,
                                isError = !timePattern.matches(time.start),
                                modifier = Modifier.weight(1f)
                            )
                            Text("–")
                            OutlinedTextField(
                                time.end,
                                { value ->
                                    times = times.toMutableList().also { it[index] = time.copy(end = value) }
                                },
                                label = { Text("结束") },
                                placeholder = { Text("00:00") },
                                singleLine = true,
                                isError = !timePattern.matches(time.end),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    }
                    SettingsPage.ADVANCED -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("课程格子高度", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "同时调整时间轴和课程卡片",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = cellHeightInput,
                                onValueChange = { value ->
                                    cellHeightInput = value.filter(Char::isDigit).take(3)
                                    cellHeightInput.toIntOrNull()
                                        ?.takeIf {
                                            it in ScheduleDefaults.MIN_CELL_HEIGHT_DP..
                                                ScheduleDefaults.MAX_CELL_HEIGHT_DP
                                        }
                                        ?.let { courseCellHeightDp = it }
                                },
                                modifier = Modifier
                                    .width(112.dp)
                                    .height(56.dp)
                                    .onFocusChanged { focusState ->
                                        if (cellHeightInputFocused && !focusState.isFocused) {
                                            cellHeightInput = courseCellHeightDp.toString()
                                        }
                                        cellHeightInputFocused = focusState.isFocused
                                    },
                                suffix = { Text("dp") },
                                singleLine = true,
                                isError = cellHeightInput.toIntOrNull()?.let {
                                    it !in ScheduleDefaults.MIN_CELL_HEIGHT_DP..
                                        ScheduleDefaults.MAX_CELL_HEIGHT_DP
                                } ?: true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Slider(
                            value = courseCellHeightDp.toFloat(),
                            onValueChange = {
                                courseCellHeightDp = it.roundToInt()
                                cellHeightInput = courseCellHeightDp.toString()
                            },
                            valueRange = ScheduleDefaults.MIN_CELL_HEIGHT_DP.toFloat()..
                                ScheduleDefaults.MAX_CELL_HEIGHT_DP.toFloat(),
                            steps = 17,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        )
                        TextButton(
                            onClick = {
                                courseCellHeightDp = ScheduleDefaults.DEFAULT_CELL_HEIGHT_DP
                                cellHeightInput = courseCellHeightDp.toString()
                            },
                            modifier = Modifier.align(Alignment.End).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("恢复默认") }
                        Text("字号", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("统一调整字号", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "开启后课程名和教室、教师字号按相同比例变化",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = unifiedFontScale,
                                onCheckedChange = { checked ->
                                    unifiedFontScale = checked
                                    if (checked) {
                                        val average = (titleFontScale + detailFontScale) / 2f
                                        unifiedScale = average
                                        titleFontScale = average
                                        detailFontScale = average
                                    }
                                }
                            )
                        }
                        FontScaleControl(
                            label = "统一字号比例",
                            value = unifiedScale,
                            enabled = unifiedFontScale,
                            onValueChange = {
                                unifiedScale = it
                                titleFontScale = it
                                detailFontScale = it
                            }
                        )
                        FontScaleControl(
                            label = "课程名字号比例",
                            value = titleFontScale,
                            enabled = !unifiedFontScale,
                            onValueChange = { titleFontScale = it }
                        )
                        FontScaleControl(
                            label = "教室和教师字号比例",
                            value = detailFontScale,
                            enabled = !unifiedFontScale,
                            onValueChange = { detailFontScale = it }
                        )
                        TextButton(
                            onClick = {
                                unifiedScale = 1f
                                titleFontScale = 1f
                                detailFontScale = 1f
                            },
                            modifier = Modifier.align(Alignment.End).height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) { Text("恢复默认字号") }
                        Text("实时预览", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        CourseAppearancePreview(
                            cellHeight = courseCellHeightDp.dp,
                            titleFontScale = titleFontScale,
                            detailFontScale = detailFontScale,
                            visibleDayCount = visibleDays.size
                        )
                    }
                    SettingsPage.ABOUT -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("课格 ClassGrid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "一款清晰、灵活且注重本地使用体验的 Material Design 3 课程表。支持整周日期、多时段课程、自定义节次与个性化外观。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "版本 0.1.0 · net.yeeren.classgrid",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "课程数据仅保存在当前设备。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "可通过 .cgf 文件在设备或不同安装版本之间迁移数据。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }
                }
                if (isInitialization) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onCompleteInitialization(draftSettings) },
                            enabled = valid
                        ) { Text("完成设置") }
                    }
                }
            }
        }

        if (showWeekPicker) {
            WeekPickerDialog(
                initialWeekStart = startDate,
                onDismiss = { showWeekPicker = false },
                onConfirm = { selectedWeekStart ->
                    startDate = selectedWeekStart
                    showWeekPicker = false
                }
            )
        }
    }

@Composable
private fun FontScaleControl(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    var inputText by remember { mutableStateOf((value * 100).roundToInt().toString()) }
    var inputFocused by remember { mutableStateOf(false) }
    LaunchedEffect(value, inputFocused) {
        if (!inputFocused) inputText = (value * 100).roundToInt().toString()
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = contentColor)
            OutlinedTextField(
                value = inputText,
                onValueChange = { text ->
                    inputText = text.filter(Char::isDigit).take(3)
                    inputText.toIntOrNull()
                        ?.takeIf { it in 75..150 }
                        ?.let { onValueChange(it / 100f) }
                },
                modifier = Modifier
                    .width(104.dp)
                    .height(56.dp)
                    .onFocusChanged { focusState ->
                        if (inputFocused && !focusState.isFocused) {
                            inputText = (value * 100).roundToInt().toString()
                        }
                        inputFocused = focusState.isFocused
                    },
                suffix = { Text("%") },
                singleLine = true,
                enabled = enabled,
                isError = enabled && (inputText.toIntOrNull()?.let { it !in 75..150 } ?: true),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = 0.75f..1.5f,
            steps = 14,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        )
    }
}

@Composable
private fun CourseAppearancePreview(
    cellHeight: Dp,
    titleFontScale: Float,
    detailFontScale: Float,
    visibleDayCount: Int
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val oneDayWidth = (
            (maxWidth - 24.dp) / visibleDayCount.coerceAtLeast(1).toFloat()
        ).coerceAtLeast(36.dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(oneDayWidth)
                        .height(cellHeight * 2)
                        .padding(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF006A6A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(6.dp)) {
                        Text(
                            "高等数学",
                            fontWeight = FontWeight.Bold,
                            fontSize = (11f * titleFontScale).sp,
                            lineHeight = (13f * titleFontScale).sp
                        )
                        Text(
                            "1教101",
                            fontSize = (9f * detailFontScale).sp,
                            lineHeight = (11f * detailFontScale).sp
                        )
                        Text(
                            "张三",
                            fontSize = (9f * detailFontScale).sp,
                            lineHeight = (11f * detailFontScale).sp
                        )
                    }
                }
            }
        }
    }

}

@Composable
private fun WeekPickerDialog(
    initialWeekStart: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var selectedWeekStart by remember(initialWeekStart) {
        mutableStateOf(ScheduleDefaults.normalizeSemesterStart(initialWeekStart))
    }
    var displayedMonth by remember(initialWeekStart) {
        mutableStateOf(YearMonth.from(initialWeekStart.plusDays(3)))
    }
    val firstOfMonth = displayedMonth.atDay(1)
    val firstGridDate = firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("选择第一周", style = MaterialTheme.typography.headlineSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                    }
                    Text(
                        "${displayedMonth.year} 年 ${displayedMonth.monthValue} 月",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    dayNames.forEach { name ->
                        Text(
                            text = name.removePrefix("周"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                repeat(6) { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val date = firstGridDate.plusDays((row * 7L) + column)
                            val selected = !date.isBefore(selectedWeekStart) &&
                                !date.isAfter(selectedWeekStart.plusDays(6))
                            val selectionShape = when (date.dayOfWeek) {
                                DayOfWeek.MONDAY -> RoundedCornerShape(
                                    topStart = 20.dp,
                                    bottomStart = 20.dp
                                )
                                DayOfWeek.SUNDAY -> RoundedCornerShape(
                                    topEnd = 20.dp,
                                    bottomEnd = 20.dp
                                )
                                else -> RoundedCornerShape(0.dp)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(selectionShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedWeekStart = ScheduleDefaults.normalizeSemesterStart(date)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = when {
                                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                                        date.month == displayedMonth.month -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = { onConfirm(selectedWeekStart) }) { Text("确定") }
                }
            }
        }
    }
}
