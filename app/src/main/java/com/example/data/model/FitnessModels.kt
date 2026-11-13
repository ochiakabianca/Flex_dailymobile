package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  val userId: String = "",
  val displayName: String = "Athlete",
  val calorieGoal: Long = 500L,
  val activeMinutesGoal: Long = 45L,
  val waterGoalMl: Long = 2500L,
  val stepsGoal: Long = 8000L,
  val weeklyWorkoutsGoal: Long = 5L,
  val preferredUnit: String = "kg",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DailyLog(
  val userId: String = "",
  val dateKey: String = "",
  val waterMl: Long = 0L,
  val steps: Long = 0L,
  val habitsCompleted: List<String> = emptyList(),
  val mood: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WorkoutEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val title: String = "",
  val category: String = "strength",
  val durationMinutes: Long = 30L,
  val caloriesBurned: Long = 200L,
  val intensity: String = "moderate",
  val distanceKm: Double = 0.0,
  val setsCompleted: Long = 0L,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WeightEntry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val weightKg: Double = 70.0,
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class WorkoutCategory(
  val key: String,
  val label: String,
  val baseMetModerate: Double,
) {
  STRENGTH("strength", "Strength", 5.5),
  CARDIO("cardio", "Cardio", 8.0),
  HIIT("hiit", "HIIT", 9.5),
  YOGA("yoga", "Mobility", 3.2),
  SPORTS("sports", "Sports", 7.2),
  OTHER("other", "Other", 4.8);

  companion object {
    fun fromKey(key: String): WorkoutCategory =
      entries.firstOrNull { it.key == key } ?: OTHER
  }
}

enum class WorkoutIntensity(
  val key: String,
  val label: String,
  val metMultiplier: Double,
) {
  LIGHT("light", "Light", 0.72),
  MODERATE("moderate", "Moderate", 1.0),
  VIGOROUS("vigorous", "Vigorous", 1.32);

  companion object {
    fun fromKey(key: String): WorkoutIntensity =
      entries.firstOrNull { it.key == key } ?: MODERATE
  }
}

enum class DailyMood(val key: String, val label: String) {
  ENERGIZED("energized", "Energized"),
  GOOD("good", "Strong"),
  STEADY("steady", "Steady"),
  TIRED("tired", "Low Fuel"),
  SORE("sore", "Recovering");

  companion object {
    fun fromKey(key: String): DailyMood? = entries.firstOrNull { it.key == key }
  }
}

data class DailyHabitDefinition(
  val id: String,
  val title: String,
  val subtitle: String,
)

object DefaultHabits {
  val items: List<DailyHabitDefinition> = listOf(
    DailyHabitDefinition("morning_mobility", "10m Mobility Flow", "Joint prep & dynamic stretch"),
    DailyHabitDefinition("protein_goal", "Hit Protein Target", "Fuel muscle recovery"),
    DailyHabitDefinition("sleep_recovery", "7h+ Quality Sleep", "Overnight readiness check"),
    DailyHabitDefinition("screen_break", "Outdoor Daylight Walk", "15+ min fresh air movement"),
  )
}

fun estimateCaloriesBurned(
  category: WorkoutCategory,
  intensity: WorkoutIntensity,
  durationMinutes: Long,
  weightKg: Double = 70.0,
): Long {
  val met = category.baseMetModerate * intensity.metMultiplier
  val kcal = (met * 3.5 * weightKg / 200.0) * durationMinutes.coerceAtLeast(1L)
  return kcal.toLong().coerceIn(5L, 5000L)
}
