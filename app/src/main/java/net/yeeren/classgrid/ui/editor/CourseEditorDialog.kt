package net.yeeren.classgrid.ui

import android.graphics.Color as AndroidColor
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.yeeren.classgrid.data.CourseEntity
import net.yeeren.classgrid.data.CourseSlotEntity
import net.yeeren.classgrid.data.CourseWithSlots
import net.yeeren.classgrid.data.WeekRules
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val editorDayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
private val editorColors = listOf(
    0xFF6750A4uL.toLong(), 0xFF006A6AuL.toLong(), 0xFF8C5000uL.toLong(),
    0xFF984061uL.toLong(), 0xFF3F6375uL.toLong(), 0xFF496727uL.toLong(),
    0xFF7E5260uL.toLong(), 0xFF5C5D72uL.toLong()
)

internal enum class WeekMode { ALL, ODD, CUSTOM }
private enum class ColorMode { RGB, HSB }

internal data class SlotDraft(
    val id: Long = 0,
    val location: String = "",
    val day: Int,
    val startSection: Int,
    val endSection: Int,
    val weekMode: WeekMode,
    val customWeeks: String
)

internal fun insertSlotCopy(slots: List<SlotDraft>, afterIndex: Int): List<SlotDraft> {
    require(afterIndex in slots.indices)
    return slots.toMutableList().also { drafts ->
        drafts.add(afterIndex + 1, slots[afterIndex].copy(id = 0L))
    }
}

private fun SlotDraft.normalizedWeeks(totalWeeks: Int): String = when (weekMode) {
    WeekMode.ALL -> WeekRules.allWeeks(totalWeeks)
    WeekMode.ODD -> WeekRules.oddWeeks(totalWeeks)
    WeekMode.CUSTOM -> WeekRules.normalizeCustomWeeks(customWeeks, totalWeeks)
}

@Composable
internal fun CourseEditorDialog(
    course: CourseWithSlots?,
    isCopy: Boolean = false,
    initialDay: Int,
    initialSection: Int,
    totalWeeks: Int,
    sectionsPerDay: Int,
    visibleDays: Set<Int>,
    onDismiss: () -> Unit,
    onSave: (CourseWithSlots) -> Unit,
    onDelete: (() -> Unit)?
) {
    val focusManager = LocalFocusManager.current
    val courseId = course?.course?.id ?: 0L
    var name by remember(courseId) { mutableStateOf(course?.course?.name.orEmpty()) }
    var teacher by remember(courseId) { mutableStateOf(course?.course?.teacher.orEmpty()) }
    var color by remember(courseId) { mutableLongStateOf(course?.course?.colorArgb ?: editorColors.first()) }
    var note by remember(courseId) { mutableStateOf(course?.course?.note.orEmpty()) }
    var showCustomColor by remember { mutableStateOf(false) }
    var pickerIndex by remember { mutableStateOf<Int?>(null) }
    var slots by remember(courseId, totalWeeks, sectionsPerDay) {
        mutableStateOf(
            course?.slots?.map { slot ->
                val mode = when (slot.activeWeeks) {
                    WeekRules.allWeeks(totalWeeks) -> WeekMode.ALL
                    WeekRules.oddWeeks(totalWeeks) -> WeekMode.ODD
                    else -> WeekMode.CUSTOM
                }
                SlotDraft(
                    id = slot.id,
                    location = slot.location,
                    day = slot.dayOfWeek.takeIf { it in visibleDays } ?: initialDay,
                    startSection = slot.startSection.coerceIn(1, sectionsPerDay),
                    endSection = slot.endSection.coerceIn(1, sectionsPerDay),
                    weekMode = mode,
                    customWeeks = WeekRules.compactWeeks(slot.activeWeeks)
                )
            }?.ifEmpty { null } ?: listOf(
                SlotDraft(
                    day = initialDay,
                    startSection = initialSection,
                    endSection = initialSection,
                    weekMode = WeekMode.ALL,
                    customWeeks = ""
                )
            )
        )
    }

    val valid = name.isNotBlank() && slots.isNotEmpty() && slots.all { slot ->
        slot.day in visibleDays &&
            slot.startSection in 1..sectionsPerDay &&
            slot.endSection in slot.startSection..sectionsPerDay &&
            slot.normalizedWeeks(totalWeeks).isNotBlank()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.94f)
                .clearFocusOnTap(focusManager),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text(
                    when {
                        isCopy -> "复制课程"
                        course == null -> "添加课程"
                        else -> "编辑课程"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text("课程名称 *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        teacher,
                        { teacher = it },
                        label = { Text("教师") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    slots.forEachIndexed { index, slot ->
                        Card(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("时段 ${index + 1}", fontWeight = FontWeight.SemiBold)
                                    Row {
                                        IconButton(onClick = { slots = insertSlotCopy(slots, index) }) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "在时段 ${index + 1} 后插入时段"
                                            )
                                        }
                                        if (slots.size > 1) {
                                            IconButton(onClick = { slots = slots.toMutableList().also { it.removeAt(index) } }) {
                                                Icon(Icons.Default.Delete, contentDescription = "删除时段 ${index + 1}")
                                            }
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    slot.location,
                                    { value -> slots = slots.toMutableList().also { it[index] = slot.copy(location = value) } },
                                    label = { Text("教室") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedButton(
                                    onClick = { pickerIndex = index },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text("${editorDayNames[slot.day - 1]} · 第 ${slot.startSection}–${slot.endSection} 节")
                                }
                                Text(
                                    "上课周次",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    listOf(
                                        WeekMode.ALL to "每周",
                                        WeekMode.ODD to "单周",
                                        WeekMode.CUSTOM to "自定义"
                                    ).forEach { (mode, label) ->
                                        FilterChip(
                                            selected = slot.weekMode == mode,
                                            onClick = { slots = slots.toMutableList().also { it[index] = slot.copy(weekMode = mode) } },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                                if (slot.weekMode == WeekMode.CUSTOM) {
                                    OutlinedTextField(
                                        slot.customWeeks,
                                        { value -> slots = slots.toMutableList().also { it[index] = slot.copy(customWeeks = value) } },
                                        label = { Text("周次，例如 3-10,12") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("课程颜色", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        maxItemsInEachRow = 5,
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        editorColors.forEach { option ->
                            ColorCircle(
                                color = option,
                                selected = color == option,
                                onClick = { color = option }
                            )
                        }
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                                .then(
                                    if (color !in editorColors) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { showCustomColor = true }
                        )
                    }
                    OutlinedTextField(
                        note,
                        { note = it },
                        label = { Text("备注") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) { Text("删除课程", color = MaterialTheme.colorScheme.error) }
                    } else Spacer(Modifier.width(1.dp))
                    Row {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Button(
                            onClick = {
                                onSave(
                                    CourseWithSlots(
                                        course = CourseEntity(
                                            id = courseId,
                                            name = name.trim(),
                                            teacher = teacher.trim(),
                                            colorArgb = color,
                                            note = note.trim()
                                        ),
                                        slots = slots.map { slot ->
                                            CourseSlotEntity(
                                                id = slot.id,
                                                courseId = courseId,
                                                location = slot.location.trim(),
                                                dayOfWeek = slot.day,
                                                startSection = slot.startSection,
                                                endSection = slot.endSection,
                                                activeWeeks = slot.normalizedWeeks(totalWeeks)
                                            )
                                        }
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

    pickerIndex?.let { index ->
        SlotPickerDialog(
            current = slots[index],
            visibleDays = visibleDays,
            sectionsPerDay = sectionsPerDay,
            onDismiss = { pickerIndex = null },
            onConfirm = { value ->
                slots = slots.toMutableList().also { it[index] = value }
                pickerIndex = null
            }
        )
    }
    if (showCustomColor) {
        CustomColorDialog(
            initialColor = color,
            onDismiss = { showCustomColor = false },
            onConfirm = {
                color = it
                showCustomColor = false
            }
        )
    }
}

@Composable
private fun ColorCircle(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(color.toInt()))
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SlotPickerDialog(
    current: SlotDraft,
    visibleDays: Set<Int>,
    sectionsPerDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (SlotDraft) -> Unit
) {
    val days = visibleDays.sorted().ifEmpty { listOf(1) }
    var dayIndex by remember { mutableIntStateOf(days.indexOf(current.day).coerceAtLeast(0)) }
    var start by remember { mutableIntStateOf(current.startSection) }
    var end by remember { mutableIntStateOf(current.endSection) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(20.dp)) {
                Text("选择星期和节次", style = MaterialTheme.typography.titleLarge)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WheelPicker(
                        label = "星期",
                        value = dayIndex,
                        min = 0,
                        max = days.lastIndex,
                        displayedValues = days.map { editorDayNames[it - 1] },
                        onValueChange = { dayIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        label = "开始节次",
                        value = start,
                        min = 1,
                        max = sectionsPerDay,
                        onValueChange = { start = it },
                        modifier = Modifier.weight(1f)
                    )
                    WheelPicker(
                        label = "结束节次",
                        value = end,
                        min = 1,
                        max = sectionsPerDay,
                        onValueChange = { end = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(onClick = {
                        val safeEnd = end.coerceAtLeast(start)
                        onConfirm(current.copy(day = days[dayIndex], startSection = start, endSection = safeEnd))
                    }) { Text("确定") }
                }
            }
        }
    }
}

@Composable
private fun WheelPicker(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    modifier: Modifier = Modifier,
    displayedValues: List<String>? = null,
    onValueChange: (Int) -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        AndroidView(
            factory = { context ->
                NumberPicker(context).apply {
                    minValue = min
                    maxValue = max
                    this.displayedValues = displayedValues?.toTypedArray()
                    this.value = value.coerceIn(min, max)
                    wrapSelectorWheel = max - min >= 2
                    setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
                }
            },
            update = { picker -> picker.value = value.coerceIn(min, max) },
            modifier = Modifier.fillMaxWidth().height(160.dp)
        )
    }
}

@Composable
private fun CustomColorDialog(
    initialColor: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initial = initialColor.toInt()
    var red by remember { mutableIntStateOf(AndroidColor.red(initial)) }
    var green by remember { mutableIntStateOf(AndroidColor.green(initial)) }
    var blue by remember { mutableIntStateOf(AndroidColor.blue(initial)) }
    val initialHsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var mode by remember { mutableStateOf(ColorMode.RGB) }
    val packed = AndroidColor.rgb(red, green, blue)
    val canonicalHex = "#%06X".format(packed and 0xFFFFFF)
    var hexInput by remember { mutableStateOf(canonicalHex) }
    var hexFocused by remember { mutableStateOf(false) }
    val hexValid = remember(hexInput) { Regex("^#[0-9A-Fa-f]{6}$").matches(hexInput) }

    LaunchedEffect(canonicalHex, hexFocused) {
        if (!hexFocused) hexInput = canonicalHex
    }

    fun updateRgbFromHsb(h: Float = hue, s: Float = saturation, b: Float = brightness) {
        val value = AndroidColor.HSVToColor(floatArrayOf(h, s, b))
        red = AndroidColor.red(value)
        green = AndroidColor.green(value)
        blue = AndroidColor.blue(value)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp) {
            Column(Modifier.padding(22.dp)) {
                Text("自定义课程颜色", style = MaterialTheme.typography.titleLarge)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color(packed)))
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { value ->
                            val digits = value
                                .removePrefix("#")
                                .filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
                                .uppercase()
                                .take(6)
                            hexInput = "#$digits"
                            if (digits.length == 6) {
                                val colorValue = digits.toInt(16)
                                red = (colorValue shr 16) and 0xFF
                                green = (colorValue shr 8) and 0xFF
                                blue = colorValue and 0xFF
                                val hsv = FloatArray(3).also {
                                    AndroidColor.colorToHSV(AndroidColor.rgb(red, green, blue), it)
                                }
                                hue = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                            }
                        },
                        label = { Text("HEX 色值") },
                        supportingText = {
                            if (!hexValid) Text("请输入 #RRGGBB")
                        },
                        singleLine = true,
                        isError = !hexValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (hexFocused && !focusState.isFocused && !hexValid) {
                                    hexInput = canonicalHex
                                }
                                hexFocused = focusState.isFocused
                            }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorMode.entries.forEach { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = {
                                if (item == ColorMode.HSB) {
                                    val hsv = FloatArray(3).also { AndroidColor.colorToHSV(AndroidColor.rgb(red, green, blue), it) }
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                }
                                mode = item
                            },
                            label = { Text(item.name) }
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(350.dp)) {
                    Column(Modifier.fillMaxSize()) {
                        if (mode == ColorMode.RGB) {
                            RgbGamutPicker(red, green, blue) { value ->
                                red = AndroidColor.red(value)
                                green = AndroidColor.green(value)
                                blue = AndroidColor.blue(value)
                            }
                            ColorSlider("R", red.toFloat(), 0f..255f, red.toString()) { red = it.roundToInt() }
                            ColorSlider("G", green.toFloat(), 0f..255f, green.toString()) { green = it.roundToInt() }
                            ColorSlider("B", blue.toFloat(), 0f..255f, blue.toString()) { blue = it.roundToInt() }
                        } else {
                            HsbColorRing(hue) {
                                hue = it
                                updateRgbFromHsb(h = it)
                            }
                            ColorSlider("H", hue, 0f..360f, "${hue.roundToInt()}°") {
                                hue = it
                                updateRgbFromHsb(h = it)
                            }
                            ColorSlider("S", saturation, 0f..1f, "${(saturation * 100).roundToInt()}%") {
                                saturation = it
                                updateRgbFromHsb(s = it)
                            }
                            ColorSlider("B", brightness, 0f..1f, "${(brightness * 100).roundToInt()}%") {
                                brightness = it
                                updateRgbFromHsb(b = it)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Button(
                        onClick = { onConfirm(AndroidColor.rgb(red, green, blue).toLong()) },
                        enabled = hexValid
                    ) { Text("使用") }
                }
            }
        }
    }
}

@Composable
private fun RgbGamutPicker(
    red: Int,
    green: Int,
    blue: Int,
    onColorChange: (Int) -> Unit
) {
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val hsv = remember(red, green, blue) {
        FloatArray(3).also { AndroidColor.colorToHSV(AndroidColor.rgb(red, green, blue), it) }
    }
    fun select(position: Offset) {
        if (fieldSize.width == 0 || fieldSize.height == 0) return
        val x = position.x.coerceIn(0f, fieldSize.width.toFloat()) / fieldSize.width
        val y = position.y.coerceIn(0f, fieldSize.height.toFloat()) / fieldSize.height
        val saturation = if (y <= 0.5f) y * 2f else 1f
        val brightness = if (y <= 0.5f) 1f else (2f * (1f - y)).coerceIn(0f, 1f)
        onColorChange(AndroidColor.HSVToColor(floatArrayOf(x * 360f, saturation, brightness)))
    }
    val cursorX = hsv[0] / 360f
    val cursorY = if (hsv[2] < 0.999f) 0.5f + (1f - hsv[2]) / 2f else hsv[1] / 2f

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .onSizeChanged { fieldSize = it }
            .pointerInput(fieldSize) { detectTapGestures { select(it) } }
            .pointerInput(fieldSize) {
                detectDragGestures { change, _ ->
                    change.consume()
                    select(change.position)
                }
            }
    ) {
        drawRect(
            Brush.horizontalGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
            )
        )
        drawRect(Brush.verticalGradient(listOf(Color.White, Color.Transparent, Color.Black)))
        val center = Offset(cursorX * size.width, cursorY * size.height)
        drawCircle(Color.Black, radius = 8.dp.toPx(), center = center)
        drawCircle(Color.White, radius = 5.dp.toPx(), center = center)
    }
}

@Composable
private fun HsbColorRing(hue: Float, onHueChange: (Float) -> Unit) {
    var ringSize by remember { mutableStateOf(IntSize.Zero) }
    fun select(position: Offset) {
        if (ringSize.width == 0 || ringSize.height == 0) return
        val centerX = ringSize.width / 2f
        val centerY = ringSize.height / 2f
        val degrees = Math.toDegrees(atan2(position.y - centerY, position.x - centerX).toDouble()).toFloat()
        onHueChange((degrees + 360f) % 360f)
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(8.dp)
            .onSizeChanged { ringSize = it }
            .pointerInput(ringSize) { detectTapGestures { select(it) } }
            .pointerInput(ringSize) {
                detectDragGestures { change, _ ->
                    change.consume()
                    select(change.position)
                }
            }
    ) {
        val strokeWidth = 30.dp.toPx()
        val radius = min(size.width, size.height) / 2f - strokeWidth
        drawCircle(
            brush = Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
            ),
            radius = radius,
            style = Stroke(width = strokeWidth)
        )
        val angle = Math.toRadians(hue.toDouble())
        val marker = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius
        )
        drawCircle(Color.Black, radius = 9.dp.toPx(), center = marker)
        drawCircle(Color.White, radius = 6.dp.toPx(), center = marker)
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(24.dp), fontWeight = FontWeight.SemiBold)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(valueText, modifier = Modifier.width(48.dp))
    }
}
