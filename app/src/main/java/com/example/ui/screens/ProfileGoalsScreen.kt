package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.LocalFlexColors
import com.example.ui.viewmodel.FitnessDashboardData

@Composable
fun ProfileGoalsScreen(
  data: FitnessDashboardData,
  userEmail: String?,
  photoUrl: String?,
  isBusy: Boolean,
  onSaveGoals: (
    displayName: String,
    calorieGoal: Long,
    activeMinutesGoal: Long,
    waterGoalMl: Long,
    stepsGoal: Long,
    weeklyWorkoutsGoal: Long,
    preferredUnit: String,
  ) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val flexColors = LocalFlexColors.current
  val profile = data.profile

  var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
  var calorieGoalText by remember(profile.calorieGoal) { mutableStateOf(profile.calorieGoal.toString()) }
  var minutesGoalText by remember(profile.activeMinutesGoal) { mutableStateOf(profile.activeMinutesGoal.toString()) }
  var waterGoalText by remember(profile.waterGoalMl) { mutableStateOf(profile.waterGoalMl.toString()) }
  var stepsGoalText by remember(profile.stepsGoal) { mutableStateOf(profile.stepsGoal.toString()) }
  var weeklyWorkoutsText by remember(profile.weeklyWorkoutsGoal) { mutableStateOf(profile.weeklyWorkoutsGoal.toString()) }
  var preferredUnit by remember(profile.preferredUnit) { mutableStateOf(profile.preferredUnit) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column {
        Text(
          text = "PROFILE & TARGETS",
          style = MaterialTheme.typography.labelSmall,
          color = flexColors.volt,
        )
        Text(
          text = "Your Fitness Goals",
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
    }

    // Account Card
    item {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
              model = photoUrl,
              contentDescription = "Profile photo",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, flexColors.volt, CircleShape),
            )
          } else {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(flexColors.volt.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = "Profile avatar",
                tint = flexColors.volt,
                modifier = Modifier.size(28.dp),
              )
            }
          }

          Spacer(Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = profile.displayName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            if (!userEmail.isNullOrBlank()) {
              Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(44.dp).testTag("sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Logout,
              contentDescription = stringResource(R.string.sign_out),
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.sign_out))
          }
        }
      }
    }

    // Goals Editor Card
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
          Text(
            text = "Daily Targets",
            style = MaterialTheme.typography.titleLarge,
          )

          OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= 80) displayName = it },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("goal_display_name_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
              value = calorieGoalText,
              onValueChange = { calorieGoalText = it.filter { ch -> ch.isDigit() }.take(5) },
              label = { Text("Move goal (kcal)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("goal_calories_input"),
            )
            OutlinedTextField(
              value = minutesGoalText,
              onValueChange = { minutesGoalText = it.filter { ch -> ch.isDigit() }.take(3) },
              label = { Text("Exercise (min)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("goal_minutes_input"),
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
              value = waterGoalText,
              onValueChange = { waterGoalText = it.filter { ch -> ch.isDigit() }.take(5) },
              label = { Text("Water (ml)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("goal_water_input"),
            )
            OutlinedTextField(
              value = stepsGoalText,
              onValueChange = { stepsGoalText = it.filter { ch -> ch.isDigit() }.take(6) },
              label = { Text("Daily steps") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("goal_steps_input"),
            )
          }

          OutlinedTextField(
            value = weeklyWorkoutsText,
            onValueChange = { weeklyWorkoutsText = it.filter { ch -> ch.isDigit() }.take(2) },
            label = { Text("Weekly workout sessions goal") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("goal_weekly_workouts_input"),
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Body Weight Unit", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              listOf("kg", "lbs").forEach { unit ->
                FilterChip(
                  selected = preferredUnit == unit,
                  onClick = { preferredUnit = unit },
                  label = { Text(unit.uppercase()) },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          Button(
            onClick = {
              onSaveGoals(
                displayName.ifBlank { "Athlete" },
                (calorieGoalText.toLongOrNull() ?: profile.calorieGoal).coerceIn(100L, 10000L),
                (minutesGoalText.toLongOrNull() ?: profile.activeMinutesGoal).coerceIn(5L, 600L),
                (waterGoalText.toLongOrNull() ?: profile.waterGoalMl).coerceIn(250L, 10000L),
                (stepsGoalText.toLongOrNull() ?: profile.stepsGoal).coerceIn(500L, 100000L),
                (weeklyWorkoutsText.toLongOrNull() ?: profile.weeklyWorkoutsGoal).coerceIn(1L, 14L),
                preferredUnit,
              )
            },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("save_goals_button"),
          ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isBusy) "Saving..." else "Save Targets")
          }
        }
      }
    }
  }
}
