package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutEntry
import com.example.data.model.WorkoutIntensity
import com.example.data.model.estimateCaloriesBurned
import com.example.ui.components.WorkoutCard
import com.example.ui.components.workoutCategoryColor
import com.example.ui.components.workoutCategoryIcon
import com.example.ui.theme.LocalFlexColors
import com.example.ui.viewmodel.FitnessDashboardData
import com.example.ui.viewmodel.LiveTimerState
import com.example.util.DateUtils

private data class WorkoutPreset(
  val title: String,
  val category: WorkoutCategory,
  val intensity: WorkoutIntensity,
  val defaultMinutes: Long,
  val defaultSets: Long = 0L,
  val defaultDistanceKm: Double = 0.0,
)

private val PRESETS = listOf(
  WorkoutPreset("Upper Body Power", WorkoutCategory.STRENGTH, WorkoutIntensity.VIGOROUS, 40L, defaultSets = 16L),
  WorkoutPreset("5K Tempo Run", WorkoutCategory.CARDIO, WorkoutIntensity.VIGOROUS, 30L, defaultDistanceKm = 5.0),
  WorkoutPreset("Tabata HIIT Blast", WorkoutCategory.HIIT, WorkoutIntensity.VIGOROUS, 20L),
  WorkoutPreset("Morning Mobility", WorkoutCategory.YOGA, WorkoutIntensity.LIGHT, 15L),
)

@Composable
fun WorkoutsScreen(
  data: FitnessDashboardData,
  liveTimer: LiveTimerState,
  onStartLiveTimer: (String, WorkoutCategory, WorkoutIntensity) -> Unit,
  onTogglePauseTimer: () -> Unit,
  onFinishTimer: () -> Unit,
  onDiscardTimer: () -> Unit,
  onQuickLogPreset: (
    title: String,
    category: WorkoutCategory,
    durationMinutes: Long,
    caloriesBurned: Long,
    intensity: WorkoutIntensity,
    distanceKm: Double,
    setsCompleted: Long,
  ) -> Unit,
  onOpenNewWorkout: () -> Unit,
  onEditWorkout: (WorkoutEntry) -> Unit,
  onDeleteWorkout: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val flexColors = LocalFlexColors.current
  var selectedFilter by remember { mutableStateOf<WorkoutCategory?>(null) }
  var showStartTimerDialog by remember { mutableStateOf(false) }

  val filteredWorkouts = remember(data.workouts, selectedFilter) {
    if (selectedFilter == null) data.workouts
    else data.workouts.filter { it.category == selectedFilter?.key }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column {
        Text(
          text = "TRAINING STUDIO",
          style = MaterialTheme.typography.labelSmall,
          color = flexColors.volt,
        )
        Text(
          text = "Workouts & Live Timer",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    // Live Timer Hero Card
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = 1.dp,
            color = if (liveTimer.isActive) flexColors.volt.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(24.dp),
          ),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (liveTimer.isActive) {
            val mins = liveTimer.elapsedSeconds / 60L
            val secs = liveTimer.elapsedSeconds % 60L
            val formattedClock = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
            val estBurn = estimateCaloriesBurned(
              category = liveTimer.category,
              intensity = liveTimer.intensity,
              durationMinutes = ((liveTimer.elapsedSeconds + 59L) / 60L).coerceAtLeast(1L),
              weightKg = data.latestWeightKg,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = if (liveTimer.isPaused) "SESSION PAUSED" else "RECORDING SESSION",
                  style = MaterialTheme.typography.labelSmall,
                  color = flexColors.volt,
                )
                Text(
                  text = liveTimer.title,
                  style = MaterialTheme.typography.titleLarge,
                )
              }
              Text(
                text = "~$estBurn kcal",
                style = MaterialTheme.typography.titleSmall,
                color = flexColors.energy,
              )
            }

            Text(
              text = formattedClock,
              style = MaterialTheme.typography.displayLarge,
              fontWeight = FontWeight.Bold,
              color = flexColors.volt,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = onTogglePauseTimer,
                modifier = Modifier.weight(1f).height(48.dp).testTag("studio_pause_resume"),
              ) {
                Icon(
                  imageVector = if (liveTimer.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                  contentDescription = null,
                )
                Spacer(Modifier.width(6.dp))
                Text(if (liveTimer.isPaused) "Resume" else "Pause")
              }
              Button(
                onClick = onFinishTimer,
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.weight(1.2f).height(48.dp).testTag("studio_finish_save"),
              ) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Finish & Save")
              }
              TextButton(
                onClick = onDiscardTimer,
                modifier = Modifier.height(48.dp),
              ) {
                Text("Discard")
              }
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Live Session Timer",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = "Time your workout in real time and auto-calculate calories burned.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(Modifier.width(12.dp))
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(flexColors.volt.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Timer,
                  contentDescription = null,
                  tint = flexColors.volt,
                  modifier = Modifier.size(26.dp),
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { showStartTimerDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.weight(1f).height(48.dp).testTag("start_live_timer_button"),
              ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.start_timer))
              }
              OutlinedButton(
                onClick = onOpenNewWorkout,
                modifier = Modifier.height(48.dp).testTag("manual_log_button"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Manual")
              }
            }
          }
        }
      }
    }

    // Quick Presets Carousel
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Quick Templates",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          PRESETS.forEach { preset ->
            val catColor = workoutCategoryColor(preset.category)
            val estCal = estimateCaloriesBurned(
              preset.category,
              preset.intensity,
              preset.defaultMinutes,
              data.latestWeightKg,
            )
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 1.dp,
              modifier = Modifier
                .width(196.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                  onQuickLogPreset(
                    preset.title,
                    preset.category,
                    preset.defaultMinutes,
                    estCal,
                    preset.intensity,
                    preset.defaultDistanceKm,
                    preset.defaultSets,
                  )
                }
                .testTag("preset_${preset.category.key}"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catColor.copy(alpha = 0.16f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = workoutCategoryIcon(preset.category),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(20.dp),
                  )
                }
                Text(
                  text = preset.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${preset.defaultMinutes} min  •  ~$estCal kcal",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "Tap to log for today",
                  style = MaterialTheme.typography.labelSmall,
                  color = catColor,
                )
              }
            }
          }
        }
      }
    }

    // Filter Chips + History Title
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Session History (${filteredWorkouts.size})",
          style = MaterialTheme.typography.titleLarge,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedFilter == null,
            onClick = { selectedFilter = null },
            label = { Text("All") },
          )
          WorkoutCategory.entries.forEach { cat ->
            FilterChip(
              selected = selectedFilter == cat,
              onClick = { selectedFilter = if (selectedFilter == cat) null else cat },
              label = { Text(cat.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
            )
          }
        }
      }
    }

    if (filteredWorkouts.isEmpty()) {
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
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_empty_workouts),
              contentDescription = "No workouts recorded",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(140.dp)
                .clip(RoundedCornerShape(18.dp)),
            )
            Text(
              text = stringResource(R.string.empty_workouts_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = stringResource(R.string.empty_workouts_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    } else {
      items(filteredWorkouts, key = { it.id }) { workout ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = DateUtils.friendlyDateLabel(workout.dateKey),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
          )
          WorkoutCard(
            workout = workout,
            onClick = { onEditWorkout(workout) },
            onDelete = { onDeleteWorkout(workout.id) },
          )
        }
      }
    }
  }

  if (showStartTimerDialog) {
    StartTimerConfigDialog(
      onDismiss = { showStartTimerDialog = false },
      onStart = { title, category, intensity ->
        onStartLiveTimer(title, category, intensity)
        showStartTimerDialog = false
      },
    )
  }
}

@Composable
private fun StartTimerConfigDialog(
  onDismiss: () -> Unit,
  onStart: (String, WorkoutCategory, WorkoutIntensity) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(WorkoutCategory.STRENGTH) }
  var intensity by remember { mutableStateOf(WorkoutIntensity.MODERATE) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Start Live Session") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { if (it.length <= 80) title = it },
          label = { Text("Session name") },
          placeholder = { Text("e.g. Full Body Circuit") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("timer_title_input"),
        )
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          WorkoutCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat.label) },
            )
          }
        }
        Text("Intensity", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          WorkoutIntensity.entries.forEach { level ->
            FilterChip(
              selected = intensity == level,
              onClick = { intensity = level },
              label = { Text(level.label) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onStart(title.ifBlank { "${category.label} Session" }, category, intensity)
        },
        modifier = Modifier.testTag("confirm_start_timer_button"),
      ) {
        Text("Start")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
