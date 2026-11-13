package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutEntry
import com.example.data.model.WorkoutIntensity
import com.example.data.model.estimateCaloriesBurned
import com.example.ui.theme.LocalFlexColors
import com.example.ui.viewmodel.LiveTimerState
import com.example.util.DateUtils

fun workoutCategoryIcon(category: WorkoutCategory): ImageVector = when (category) {
  WorkoutCategory.STRENGTH -> Icons.Rounded.FitnessCenter
  WorkoutCategory.CARDIO -> Icons.AutoMirrored.Rounded.DirectionsRun
  WorkoutCategory.HIIT -> Icons.Rounded.Bolt
  WorkoutCategory.YOGA -> Icons.Rounded.SelfImprovement
  WorkoutCategory.SPORTS -> Icons.Rounded.SportsBasketball
  WorkoutCategory.OTHER -> Icons.Rounded.Timer
}

@Composable
fun workoutCategoryColor(category: WorkoutCategory): Color {
  val flex = LocalFlexColors.current
  return when (category) {
    WorkoutCategory.STRENGTH -> flex.volt
    WorkoutCategory.CARDIO -> flex.hydration
    WorkoutCategory.HIIT -> flex.energy
    WorkoutCategory.YOGA -> Color(0xFF34D399)
    WorkoutCategory.SPORTS -> Color(0xFFFBBF24)
    WorkoutCategory.OTHER -> MaterialTheme.colorScheme.primary
  }
}

@Composable
fun ActivityRingsHeroCard(
  caloriesBurned: Long,
  calorieGoal: Long,
  activeMinutes: Long,
  activeMinutesGoal: Long,
  waterMl: Long,
  waterGoalMl: Long,
  modifier: Modifier = Modifier,
) {
  val flexColors = LocalFlexColors.current
  val calProgress by animateFloatAsState(
    targetValue = (caloriesBurned.toFloat() / calorieGoal.coerceAtLeast(1L)).coerceIn(0f, 1f),
    animationSpec = spring(),
    label = "calProgress",
  )
  val minProgress by animateFloatAsState(
    targetValue = (activeMinutes.toFloat() / activeMinutesGoal.coerceAtLeast(1L)).coerceIn(0f, 1f),
    animationSpec = spring(),
    label = "minProgress",
  )
  val waterProgress by animateFloatAsState(
    targetValue = (waterMl.toFloat() / waterGoalMl.coerceAtLeast(1L)).coerceIn(0f, 1f),
    animationSpec = spring(),
    label = "waterProgress",
  )

  Surface(
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp)),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_daily_banner),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .matchParentSize()
          .alpha(0.16f),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        Box(
          modifier = Modifier.size(134.dp),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.size(134.dp)) {
            val strokeWidth = 11.dp.toPx()
            val gap = 4.dp.toPx()
            val ringColors = listOf(flexColors.volt, flexColors.energy, flexColors.hydration)
            val progresses = listOf(calProgress, minProgress, waterProgress)

            progresses.forEachIndexed { index, progress ->
              val inset = strokeWidth / 2f + index * (strokeWidth + gap)
              val diameter = size.minDimension - inset * 2f
              if (diameter > 0f) {
                val topLeft = Offset(inset, inset)
                val arcSize = Size(diameter, diameter)
                drawArc(
                  color = flexColors.trackBg,
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  topLeft = topLeft,
                  size = arcSize,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                if (progress > 0f) {
                  drawArc(
                    color = ringColors[index],
                    startAngle = -90f,
                    sweepAngle = (progress * 360f).coerceAtLeast(4f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                  )
                }
              }
            }
          }

          val overallPct = (((calProgress + minProgress + waterProgress) / 3f) * 100).toInt()
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$overallPct%",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "DAILY GOAL",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          RingMetricLine(
            color = flexColors.volt,
            icon = Icons.Rounded.LocalFireDepartment,
            label = "Move",
            valueText = "$caloriesBurned",
            goalText = "/ $calorieGoal kcal",
          )
          RingMetricLine(
            color = flexColors.energy,
            icon = Icons.Rounded.Timer,
            label = "Exercise",
            valueText = "$activeMinutes",
            goalText = "/ $activeMinutesGoal min",
          )
          RingMetricLine(
            color = flexColors.hydration,
            icon = Icons.Rounded.WaterDrop,
            label = "Hydration",
            valueText = "${String.format(java.util.Locale.US, "%.1f", waterMl / 1000.0)}L",
            goalText = "/ ${String.format(java.util.Locale.US, "%.1f", waterGoalMl / 1000.0)}L",
          )
        }
      }
    }
  }
}

@Composable
private fun RingMetricLine(
  color: Color,
  icon: ImageVector,
  label: String,
  valueText: String,
  goalText: String,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(color.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = color,
        modifier = Modifier.size(16.dp),
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = valueText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = goalText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun WeekDateStrip(
  weekKeys: List<String>,
  selectedDateKey: String,
  activeDateKeys: Set<String>,
  onSelectDate: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    weekKeys.forEach { dateKey ->
      val isSelected = dateKey == selectedDateKey
      val hasActivity = activeDateKeys.contains(dateKey)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 0.dp else 1.dp,
        modifier = Modifier
          .weight(1f)
          .minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(16.dp))
          .clickable { onSelectDate(dateKey) }
          .testTag("date_pill_$dateKey"),
      ) {
        Column(
          modifier = Modifier.padding(vertical = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = DateUtils.dayOfWeekShort(dateKey),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = DateUtils.dayOfMonthNumber(dateKey),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
          )
          Box(
            modifier = Modifier
              .size(5.dp)
              .clip(CircleShape)
              .background(
                when {
                  isSelected && hasActivity -> MaterialTheme.colorScheme.onPrimary
                  hasActivity -> LocalFlexColors.current.volt
                  else -> Color.Transparent
                }
              ),
          )
        }
      }
    }
  }
}

@Composable
fun WorkoutCard(
  workout: WorkoutEntry,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val category = WorkoutCategory.fromKey(workout.category)
  val intensity = WorkoutIntensity.fromKey(workout.intensity)
  val catColor = workoutCategoryColor(category)

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .testTag("workout_card_${workout.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(catColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = workoutCategoryIcon(category),
          contentDescription = category.label,
          tint = catColor,
          modifier = Modifier.size(24.dp),
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = workout.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        val metrics = buildList {
          add("${workout.durationMinutes} min")
          add("${workout.caloriesBurned} kcal")
          if (workout.setsCompleted > 0L) add("${workout.setsCompleted} sets")
          if (workout.distanceKm > 0.0) add("${String.format(java.util.Locale.US, "%.1f", workout.distanceKm)} km")
        }.joinToString("  •  ")
        Text(
          text = metrics,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          SmallBadge(text = category.label, color = catColor)
          SmallBadge(text = intensity.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (workout.notes.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = workout.notes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("delete_workout_${workout.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = "Delete workout",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun SmallBadge(
  text: String,
  color: Color,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.14f),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun ActiveTimerFloatingCard(
  timerState: LiveTimerState,
  onTogglePause: () -> Unit,
  onFinish: () -> Unit,
  onDiscard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val flex = LocalFlexColors.current
  val mins = timerState.elapsedSeconds / 60L
  val secs = timerState.elapsedSeconds % 60L
  val clock = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    tonalElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, flex.volt.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = if (timerState.isPaused) "PAUSED • ${timerState.category.label.uppercase()}" else "LIVE • ${timerState.category.label.uppercase()}",
          style = MaterialTheme.typography.labelSmall,
          color = flex.volt,
        )
        Text(
          text = "$clock  —  ${timerState.title}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onTogglePause,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("timer_pause_resume"),
        ) {
          Icon(
            imageVector = if (timerState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            contentDescription = if (timerState.isPaused) "Resume timer" else "Pause timer",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
        IconButton(
          onClick = onFinish,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("timer_finish_save"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Stop,
            contentDescription = "Finish and save workout",
            tint = flex.volt,
          )
        }
        TextButton(
          onClick = onDiscard,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("timer_discard"),
        ) {
          Text("Cancel", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorSheet(
  initialWorkout: WorkoutEntry?,
  selectedDateKey: String,
  latestWeightKg: Double,
  isSaving: Boolean,
  onDismiss: () -> Unit,
  onSave: (
    editingId: String?,
    dateKey: String,
    title: String,
    category: WorkoutCategory,
    durationMinutes: Long,
    caloriesBurned: Long,
    intensity: WorkoutIntensity,
    distanceKm: Double,
    setsCompleted: Long,
    notes: String,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember(initialWorkout) { mutableStateOf(initialWorkout?.title.orEmpty()) }
  var category by remember(initialWorkout) {
    mutableStateOf(WorkoutCategory.fromKey(initialWorkout?.category ?: "strength"))
  }
  var intensity by remember(initialWorkout) {
    mutableStateOf(WorkoutIntensity.fromKey(initialWorkout?.intensity ?: "moderate"))
  }
  var durationText by remember(initialWorkout) {
    mutableStateOf((initialWorkout?.durationMinutes ?: 35L).toString())
  }
  var caloriesText by remember(initialWorkout) {
    val initCal = initialWorkout?.caloriesBurned ?: estimateCaloriesBurned(
      category = category,
      intensity = intensity,
      durationMinutes = 35L,
      weightKg = latestWeightKg,
    )
    mutableStateOf(initCal.toString())
  }
  var caloriesEditedManually by remember(initialWorkout) { mutableStateOf(initialWorkout != null) }
  var setsText by remember(initialWorkout) {
    mutableStateOf((initialWorkout?.setsCompleted ?: 0L).let { if (it > 0L) it.toString() else "" })
  }
  var distanceText by remember(initialWorkout) {
    mutableStateOf((initialWorkout?.distanceKm ?: 0.0).let { if (it > 0.0) it.toString() else "" })
  }
  var notes by remember(initialWorkout) { mutableStateOf(initialWorkout?.notes.orEmpty()) }

  fun refreshEstimateIfAuto(newCat: WorkoutCategory, newInt: WorkoutIntensity, newDurStr: String) {
    if (!caloriesEditedManually) {
      val dur = newDurStr.toLongOrNull()?.coerceIn(1L, 600L) ?: 30L
      caloriesText = estimateCaloriesBurned(newCat, newInt, dur, latestWeightKg).toString()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = if (initialWorkout == null) "Log Workout" else "Edit Workout",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Date: ${DateUtils.friendlyDateLabel(initialWorkout?.dateKey ?: selectedDateKey)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = title,
        onValueChange = { if (it.length <= 100) title = it },
        label = { Text("Workout title") },
        placeholder = { Text("e.g. Morning Kettlebell Flow") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("workout_title_input"),
      )

      Text("Category", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        WorkoutCategory.entries.forEach { cat ->
          val selected = category == cat
          FilterChip(
            selected = selected,
            onClick = {
              category = cat
              refreshEstimateIfAuto(cat, intensity, durationText)
            },
            label = { Text(cat.label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
          )
        }
      }

      Text("Intensity", style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WorkoutIntensity.entries.forEach { level ->
          val selected = intensity == level
          FilterChip(
            selected = selected,
            onClick = {
              intensity = level
              refreshEstimateIfAuto(category, level, durationText)
            },
            label = { Text(level.label) },
            modifier = Modifier.weight(1f),
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = durationText,
          onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() }.take(3)
            durationText = filtered
            refreshEstimateIfAuto(category, intensity, filtered)
          },
          label = { Text("Minutes") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("workout_duration_input"),
        )
        OutlinedTextField(
          value = caloriesText,
          onValueChange = { raw ->
            caloriesEditedManually = true
            caloriesText = raw.filter { it.isDigit() }.take(4)
          },
          label = { Text("Calories (kcal)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("workout_calories_input"),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = setsText,
          onValueChange = { setsText = it.filter { ch -> ch.isDigit() }.take(3) },
          label = { Text("Sets (optional)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = distanceText,
          onValueChange = { raw ->
            if (raw.count { it == '.' } <= 1 && raw.all { it.isDigit() || it == '.' }) {
              distanceText = raw.take(6)
            }
          },
          label = { Text("Distance km") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { if (it.length <= 500) notes = it },
        label = { Text("Session notes (optional)") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).height(50.dp),
        ) {
          Text("Cancel")
        }
        Button(
          onClick = {
            val dur = (durationText.toLongOrNull() ?: 30L).coerceIn(1L, 600L)
            val kcal = (caloriesText.toLongOrNull()
              ?: estimateCaloriesBurned(category, intensity, dur, latestWeightKg)).coerceIn(0L, 5000L)
            val sets = (setsText.toLongOrNull() ?: 0L).coerceIn(0L, 200L)
            val dist = (distanceText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 500.0)
            onSave(
              initialWorkout?.id,
              initialWorkout?.dateKey ?: selectedDateKey,
              title.ifBlank { "${category.label} Workout" },
              category,
              dur,
              kcal,
              intensity,
              dist,
              sets,
              notes,
            )
          },
          enabled = !isSaving,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier.weight(1.4f).height(50.dp).testTag("save_workout_button"),
        ) {
          Text(if (isSaving) "Saving..." else "Save Session")
        }
      }
    }
  }
}

@Composable
fun StepsEditorDialog(
  currentSteps: Long,
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit,
) {
  var stepsInput by remember { mutableStateOf(currentSteps.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Update Daily Steps") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = stepsInput,
          onValueChange = { stepsInput = it.filter { ch -> ch.isDigit() }.take(6) },
          label = { Text("Steps") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("steps_input_field"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(1000L, 2500L, 5000L).forEach { add ->
            FilledTonalButton(
              onClick = {
                val base = stepsInput.toLongOrNull() ?: 0L
                stepsInput = (base + add).coerceAtMost(200000L).toString()
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("+${add / 1000}k")
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm((stepsInput.toLongOrNull() ?: currentSteps).coerceIn(0L, 200000L))
        },
        modifier = Modifier.testTag("save_steps_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
