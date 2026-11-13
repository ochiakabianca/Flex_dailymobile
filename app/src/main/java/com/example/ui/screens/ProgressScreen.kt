package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.WeightEntry
import com.example.ui.theme.LocalFlexColors
import com.example.ui.viewmodel.DailyBarPoint
import com.example.ui.viewmodel.FitnessDashboardData
import com.example.util.DateUtils
import java.util.Locale

private enum class ChartMetric(val label: String) {
  CALORIES("Calories"),
  MINUTES("Minutes"),
}

@Composable
fun ProgressScreen(
  data: FitnessDashboardData,
  onAddWeightLog: (Double, String) -> Unit,
  onDeleteWeightLog: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val flexColors = LocalFlexColors.current
  var selectedMetric by remember { mutableStateOf(ChartMetric.CALORIES) }
  var selectedBarIndex by remember(data.weeklyBars) {
    mutableIntStateOf((data.weeklyBars.size - 1).coerceAtLeast(0))
  }
  var showWeightDialog by remember { mutableStateOf(false) }

  val weeklyWorkouts = remember(data.weeklyBars) { data.weeklyBars.sumOf { it.workoutCount } }
  val weeklyCalories = remember(data.weeklyBars) { data.weeklyBars.sumOf { it.calories } }
  val weeklyMinutes = remember(data.weeklyBars) { data.weeklyBars.sumOf { it.activeMinutes } }
  val avgWaterLiters = remember(data.weeklyBars) {
    val totalMl = data.weeklyBars.sumOf { it.waterMl }
    (totalMl / 7.0) / 1000.0
  }

  val unit = data.profile.preferredUnit
  fun formatWeight(kg: Double): String {
    val value = if (unit == "lbs") kg / 0.45359237 else kg
    return String.format(Locale.US, "%.1f %s", value, unit)
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column {
        Text(
          text = "PERFORMANCE ANALYTICS",
          style = MaterialTheme.typography.labelSmall,
          color = flexColors.volt,
        )
        Text(
          text = "7-Day Progress",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    // Weekly Bar Chart Card
    item {
      val bars = data.weeklyBars
      val goalValue = if (selectedMetric == ChartMetric.CALORIES) {
        data.profile.calorieGoal.toFloat()
      } else {
        data.profile.activeMinutesGoal.toFloat()
      }
      val maxVal = remember(bars, selectedMetric, goalValue) {
        val peak = bars.maxOfOrNull {
          if (selectedMetric == ChartMetric.CALORIES) it.calories.toFloat() else it.activeMinutes.toFloat()
        } ?: 0f
        maxOf(peak, goalValue, 10f) * 1.15f
      }
      val focusedBar: DailyBarPoint? = bars.getOrNull(selectedBarIndex)

      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = focusedBar?.let { DateUtils.friendlyDateLabel(it.dateKey) } ?: "This Week",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              val valStr = if (selectedMetric == ChartMetric.CALORIES) {
                "${focusedBar?.calories ?: 0} kcal"
              } else {
                "${focusedBar?.activeMinutes ?: 0} min"
              }
              Text(
                text = valStr,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ChartMetric.entries.forEach { metric ->
                FilterChip(
                  selected = selectedMetric == metric,
                  onClick = { selectedMetric = metric },
                  label = { Text(metric.label) },
                )
              }
            }
          }

          val barColor = if (selectedMetric == ChartMetric.CALORIES) flexColors.volt else flexColors.energy
          val goalLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
          val trackColor = flexColors.trackBg

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val count = bars.size.coerceAtLeast(1)
              val slotWidth = size.width / count
              val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
              val chartHeight = size.height - 12.dp.toPx()

              // Goal dashed reference line
              val goalY = chartHeight - (goalValue / maxVal).coerceIn(0f, 1f) * chartHeight
              drawLine(
                color = goalLineColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
              )

              bars.forEachIndexed { idx, point ->
                val raw = if (selectedMetric == ChartMetric.CALORIES) {
                  point.calories.toFloat()
                } else {
                  point.activeMinutes.toFloat()
                }
                val ratio = (raw / maxVal).coerceIn(0f, 1f)
                val barHeight = (ratio * chartHeight).coerceAtLeast(6.dp.toPx())
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f

                // Track background pill
                drawRoundRect(
                  color = trackColor.copy(alpha = 0.35f),
                  topLeft = Offset(left, 0f),
                  size = Size(barWidth, chartHeight),
                  cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                )

                // Value bar
                drawRoundRect(
                  color = if (idx == selectedBarIndex) barColor else barColor.copy(alpha = 0.45f),
                  topLeft = Offset(left, chartHeight - barHeight),
                  size = Size(barWidth, barHeight),
                  cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                )
              }
            }

            // Tap targets over each bar slot
            Row(modifier = Modifier.fillMaxSize()) {
              bars.forEachIndexed { idx, _ ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { selectedBarIndex = idx },
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            bars.forEachIndexed { idx, point ->
              Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = point.shortDay,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (idx == selectedBarIndex) FontWeight.Bold else FontWeight.Medium,
                  color = if (idx == selectedBarIndex) {
                    MaterialTheme.colorScheme.onSurface
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }
            }
          }
        }
      }
    }

    // 2x2 Summary Stat Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SummaryStatTile(
            icon = Icons.Rounded.FitnessCenter,
            accent = flexColors.volt,
            label = "Workouts",
            value = "$weeklyWorkouts / ${data.profile.weeklyWorkoutsGoal}",
            subtitle = "7-day sessions",
            modifier = Modifier.weight(1f),
          )
          SummaryStatTile(
            icon = Icons.Rounded.LocalFireDepartment,
            accent = flexColors.energy,
            label = "Calories",
            value = "$weeklyCalories kcal",
            subtitle = "7-day active burn",
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SummaryStatTile(
            icon = Icons.Rounded.Timer,
            accent = MaterialTheme.colorScheme.primary,
            label = "Active Time",
            value = "$weeklyMinutes min",
            subtitle = "7-day total",
            modifier = Modifier.weight(1f),
          )
          SummaryStatTile(
            icon = Icons.Rounded.WaterDrop,
            accent = flexColors.hydration,
            label = "Avg Hydration",
            value = String.format(Locale.US, "%.1f L/day", avgWaterLiters),
            subtitle = "7-day average",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Body Weight Trend Card
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(flexColors.hydration.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.MonitorWeight,
                  contentDescription = "Body Weight",
                  tint = flexColors.hydration,
                )
              }
              Column {
                Text("Body Weight", style = MaterialTheme.typography.titleMedium)
                Text(
                  text = if (data.weightLogs.isEmpty()) "No weigh-ins yet" else "Latest: ${formatWeight(data.latestWeightKg)}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Button(
              onClick = { showWeightDialog = true },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.height(44.dp).testTag("log_weight_button"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(4.dp))
              Text("Log")
            }
          }

          if (data.weightLogs.size >= 2) {
            WeightSparkline(
              entries = data.weightLogs.take(12).reversed(),
              lineColor = flexColors.hydration,
              modifier = Modifier
                .fillMaxWidth()
                .height(92.dp),
            )
          }
        }
      }
    }

    if (data.weightLogs.isNotEmpty()) {
      item {
        Text(
          text = "Recent Weigh-Ins",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
      items(data.weightLogs.take(10), key = { it.id }) { entry ->
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = formatWeight(entry.weightKg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              val sub = buildString {
                append(DateUtils.friendlyDateLabel(entry.dateKey))
                if (entry.note.isNotBlank()) {
                  append("  •  ")
                  append(entry.note)
                }
              }
              Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = { onDeleteWeightLog(entry.id) },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete weight entry",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showWeightDialog) {
    LogWeightDialog(
      preferredUnit = unit,
      initialWeightKg = data.latestWeightKg,
      onDismiss = { showWeightDialog = false },
      onConfirm = { displayValue, note ->
        onAddWeightLog(displayValue, note)
        showWeightDialog = false
      },
    )
  }
}

@Composable
private fun SummaryStatTile(
  icon: ImageVector,
  accent: Color,
  label: String,
  value: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun WeightSparkline(
  entries: List<WeightEntry>,
  lineColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
    if (entries.size < 2) return@Canvas
    val minW = entries.minOf { it.weightKg }
    val maxW = entries.maxOf { it.weightKg }
    val range = (maxW - minW).coerceAtLeast(0.5)
    val stepX = size.width / (entries.size - 1).coerceAtLeast(1)

    val path = Path()
    val points = entries.mapIndexed { index, item ->
      val x = index * stepX
      val normalized = ((item.weightKg - minW) / range).toFloat()
      val y = size.height - normalized * (size.height - 16.dp.toPx()) - 8.dp.toPx()
      Offset(x, y)
    }

    points.forEachIndexed { index, pt ->
      if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }

    drawPath(
      path = path,
      color = lineColor,
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    points.forEach { pt ->
      drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = pt)
    }
  }
}

@Composable
private fun LogWeightDialog(
  preferredUnit: String,
  initialWeightKg: Double,
  onDismiss: () -> Unit,
  onConfirm: (Double, String) -> Unit,
) {
  val initialDisplay = if (preferredUnit == "lbs") initialWeightKg / 0.45359237 else initialWeightKg
  var weightText by remember {
    mutableStateOf(String.format(Locale.US, "%.1f", initialDisplay))
  }
  var noteText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Log Body Weight ($preferredUnit)") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = weightText,
          onValueChange = { raw ->
            if (raw.count { it == '.' } <= 1 && raw.all { it.isDigit() || it == '.' }) {
              weightText = raw.take(6)
            }
          },
          label = { Text("Weight ($preferredUnit)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("weight_input_field"),
        )
        OutlinedTextField(
          value = noteText,
          onValueChange = { if (it.length <= 200) noteText = it },
          label = { Text("Note (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = weightText.toDoubleOrNull()
          if (parsed != null && parsed > 0.0) {
            onConfirm(parsed, noteText)
          }
        },
        modifier = Modifier.testTag("save_weight_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
