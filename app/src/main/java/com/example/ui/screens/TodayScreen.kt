package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyLog
import com.example.data.model.DailyMood
import com.example.data.model.DefaultHabits
import com.example.data.model.WorkoutEntry
import com.example.ui.components.ActivityRingsHeroCard
import com.example.ui.components.ActiveTimerFloatingCard
import com.example.ui.components.StepsEditorDialog
import com.example.ui.components.WeekDateStrip
import com.example.ui.components.WorkoutCard
import com.example.ui.theme.LocalFlexColors
import com.example.ui.viewmodel.FitnessDashboardData
import com.example.ui.viewmodel.LiveTimerState
import com.example.util.DateUtils

@Composable
fun TodayScreen(
  data: FitnessDashboardData,
  liveTimer: LiveTimerState,
  onSelectDate: (String) -> Unit,
  onAdjustWater: (DailyLog, Long) -> Unit,
  onUpdateSteps: (DailyLog, Long) -> Unit,
  onToggleHabit: (DailyLog, String) -> Unit,
  onSelectMood: (DailyLog, String) -> Unit,
  onOpenNewWorkout: () -> Unit,
  onEditWorkout: (WorkoutEntry) -> Unit,
  onDeleteWorkout: (String) -> Unit,
  onTogglePauseTimer: () -> Unit,
  onFinishTimer: () -> Unit,
  onDiscardTimer: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val flexColors = LocalFlexColors.current
  var showStepsDialog by remember { mutableStateOf(false) }

  val activeDates = remember(data.workouts, data.dailyLogsByDate) {
    buildSet {
      data.workouts.forEach { if (it.dateKey.isNotBlank()) add(it.dateKey) }
      data.dailyLogsByDate.values.forEach { log ->
        if (log.waterMl > 0L || log.steps > 0L || log.habitsCompleted.isNotEmpty()) {
          add(log.dateKey)
        }
      }
    }
  }

  val waterProgress by animateFloatAsState(
    targetValue = (data.selectedDayLog.waterMl.toFloat() / data.profile.waterGoalMl.coerceAtLeast(1L)).coerceIn(0f, 1f),
    animationSpec = spring(),
    label = "waterBar",
  )
  val stepsProgress by animateFloatAsState(
    targetValue = (data.selectedDayLog.steps.toFloat() / data.profile.stepsGoal.coerceAtLeast(1L)).coerceIn(0f, 1f),
    animationSpec = spring(),
    label = "stepsBar",
  )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = DateUtils.fullHeaderDate(data.selectedDateKey).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Hey, ${data.profile.displayName.substringBefore(" ")}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        Surface(
          shape = RoundedCornerShape(18.dp),
          color = flexColors.energy.copy(alpha = 0.16f),
          modifier = Modifier.testTag("streak_badge"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocalFireDepartment,
              contentDescription = "Current streak",
              tint = flexColors.energy,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "${data.currentStreakDays}d streak",
              style = MaterialTheme.typography.labelLarge,
              color = flexColors.energy,
            )
          }
        }
      }
    }

    item {
      WeekDateStrip(
        weekKeys = data.weekDateKeys,
        selectedDateKey = data.selectedDateKey,
        activeDateKeys = activeDates,
        onSelectDate = onSelectDate,
      )
    }

    if (liveTimer.isActive) {
      item {
        AnimatedVisibility(visible = liveTimer.isActive) {
          ActiveTimerFloatingCard(
            timerState = liveTimer,
            onTogglePause = onTogglePauseTimer,
            onFinish = onFinishTimer,
            onDiscard = onDiscardTimer,
          )
        }
      }
    }

    item {
      ActivityRingsHeroCard(
        caloriesBurned = data.selectedDayCalories,
        calorieGoal = data.profile.calorieGoal,
        activeMinutes = data.selectedDayMinutes,
        activeMinutesGoal = data.profile.activeMinutesGoal,
        waterMl = data.selectedDayLog.waterMl,
        waterGoalMl = data.profile.waterGoalMl,
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Hydration Quick Card
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.WaterDrop,
                  contentDescription = "Hydration",
                  tint = flexColors.hydration,
                  modifier = Modifier.size(18.dp),
                )
                Text("Water", style = MaterialTheme.typography.titleSmall)
              }
              IconButton(
                onClick = { onAdjustWater(data.selectedDayLog, -250L) },
                enabled = data.selectedDayLog.waterMl > 0L,
                modifier = Modifier.size(28.dp).testTag("water_minus_250"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Remove,
                  contentDescription = "Remove 250 ml",
                  modifier = Modifier.size(16.dp),
                )
              }
            }

            Column {
              Text(
                text = "${data.selectedDayLog.waterMl} ml",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Goal ${data.profile.waterGoalMl} ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            LinearProgressIndicator(
              progress = { waterProgress },
              color = flexColors.hydration,
              trackColor = flexColors.trackBg,
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Button(
                onClick = { onAdjustWater(data.selectedDayLog, 250L) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = flexColors.hydration.copy(alpha = 0.18f),
                  contentColor = flexColors.hydration,
                ),
                modifier = Modifier.weight(1f).height(40.dp).testTag("water_add_250"),
              ) {
                Text("+250", style = MaterialTheme.typography.labelMedium)
              }
              Button(
                onClick = { onAdjustWater(data.selectedDayLog, 500L) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = flexColors.hydration.copy(alpha = 0.18f),
                  contentColor = flexColors.hydration,
                ),
                modifier = Modifier.weight(1f).height(40.dp).testTag("water_add_500"),
              ) {
                Text("+500", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }

        // Steps Quick Card
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable { showStepsDialog = true }
            .testTag("steps_card"),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                contentDescription = "Steps",
                tint = flexColors.volt,
                modifier = Modifier.size(18.dp),
              )
              Text("Steps", style = MaterialTheme.typography.titleSmall)
            }

            Column {
              Text(
                text = "${data.selectedDayLog.steps}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Goal ${data.profile.stepsGoal}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            LinearProgressIndicator(
              progress = { stepsProgress },
              color = flexColors.volt,
              trackColor = flexColors.trackBg,
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Button(
                onClick = { onUpdateSteps(data.selectedDayLog, data.selectedDayLog.steps + 1000L) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = flexColors.volt.copy(alpha = 0.18f),
                  contentColor = flexColors.volt,
                ),
                modifier = Modifier.weight(1f).height(40.dp).testTag("steps_add_1k"),
              ) {
                Text("+1k", style = MaterialTheme.typography.labelMedium)
              }
              Button(
                onClick = { showStepsDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                  contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.weight(1f).height(40.dp).testTag("steps_edit_button"),
              ) {
                Text("Edit", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }
    }

    // Readiness Mood Check-In
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Daily Readiness",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DailyMood.entries.forEach { mood ->
              val selected = data.selectedDayLog.mood == mood.key
              FilterChip(
                selected = selected,
                onClick = { onSelectMood(data.selectedDayLog, mood.key) },
                label = { Text(mood.label) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              )
            }
          }
        }
      }
    }

    // Daily Movement Habits
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Flex Habits",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "${data.selectedDayLog.habitsCompleted.size} / ${DefaultHabits.items.size}",
              style = MaterialTheme.typography.labelLarge,
              color = flexColors.volt,
            )
          }

          DefaultHabits.items.forEach { habit ->
            val checked = data.selectedDayLog.habitsCompleted.contains(habit.id)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                  if (checked) flexColors.volt.copy(alpha = 0.10f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
                .clickable { onToggleHabit(data.selectedDayLog, habit.id) }
                .minimumInteractiveComponentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("habit_${habit.id}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (checked) "Completed" else "Incomplete",
                tint = if (checked) flexColors.volt else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = habit.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = habit.subtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }

    // Selected Day Workouts Section Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${DateUtils.friendlyDateLabel(data.selectedDateKey)}'s Sessions",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(
          onClick = onOpenNewWorkout,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("today_add_workout_link"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Log")
        }
      }
    }

    if (data.workoutsForSelectedDate.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 1.dp,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp)),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_empty_workouts),
              contentDescription = "No workouts logged",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(16.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Rest or ready to move?",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Log a workout session to fill your Move & Exercise rings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    } else {
      items(data.workoutsForSelectedDate, key = { it.id }) { workout ->
        WorkoutCard(
          workout = workout,
          onClick = { onEditWorkout(workout) },
          onDelete = { onDeleteWorkout(workout.id) },
        )
      }
    }
  }

  if (showStepsDialog) {
    StepsEditorDialog(
      currentSteps = data.selectedDayLog.steps,
      onDismiss = { showStepsDialog = false },
      onConfirm = { updated ->
        onUpdateSteps(data.selectedDayLog, updated)
        showStepsDialog = false
      },
    )
  }
}
