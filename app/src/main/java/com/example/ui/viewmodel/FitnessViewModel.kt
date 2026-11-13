package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DailyLog
import com.example.data.model.UserProfile
import com.example.data.model.WeightEntry
import com.example.data.model.WorkoutCategory
import com.example.data.model.WorkoutEntry
import com.example.data.model.WorkoutIntensity
import com.example.data.model.estimateCaloriesBurned
import com.example.data.repository.FitnessRepository
import com.example.util.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class DailyBarPoint(
  val dateKey: String,
  val shortDay: String,
  val calories: Long,
  val activeMinutes: Long,
  val waterMl: Long,
  val workoutCount: Int,
)

data class LiveTimerState(
  val isActive: Boolean = false,
  val isPaused: Boolean = false,
  val elapsedSeconds: Long = 0L,
  val title: String = "Quick Session",
  val category: WorkoutCategory = WorkoutCategory.STRENGTH,
  val intensity: WorkoutIntensity = WorkoutIntensity.MODERATE,
)

data class FitnessDashboardData(
  val profile: UserProfile,
  val selectedDateKey: String,
  val weekDateKeys: List<String>,
  val dailyLogsByDate: Map<String, DailyLog>,
  val workouts: List<WorkoutEntry>,
  val workoutsForSelectedDate: List<WorkoutEntry>,
  val selectedDayLog: DailyLog,
  val selectedDayCalories: Long,
  val selectedDayMinutes: Long,
  val weeklyBars: List<DailyBarPoint>,
  val currentStreakDays: Int,
  val weightLogs: List<WeightEntry>,
  val latestWeightKg: Double,
)

class FitnessViewModel(
  private val repository: FitnessRepository,
  private val currentUserId: String,
  private val initialDisplayName: String,
) : ViewModel() {

  private val _selectedDateKey = MutableStateFlow(DateUtils.todayKey())
  val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

  private val _liveTimer = MutableStateFlow(LiveTimerState())
  val liveTimer: StateFlow<LiveTimerState> = _liveTimer.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private var timerJob: Job? = null

  private val profileFlow = repository.observeUserProfile()
    .map<UserProfile?, Result<UserProfile?>> { Result.success(it) }
    .catch { e -> emit(Result.failure(unwrapStreamError(e, "profile"))) }

  private val dailyLogsFlow = repository.observeDailyLogs()
    .map<List<DailyLog>, Result<List<DailyLog>>> { Result.success(it) }
    .catch { e -> emit(Result.failure(unwrapStreamError(e, "dailyLogs"))) }

  private val workoutsFlow = repository.observeWorkouts()
    .map<List<WorkoutEntry>, Result<List<WorkoutEntry>>> { Result.success(it) }
    .catch { e -> emit(Result.failure(unwrapStreamError(e, "workouts"))) }

  private val weightLogsFlow = repository.observeWeightLogs()
    .map<List<WeightEntry>, Result<List<WeightEntry>>> { Result.success(it) }
    .catch { e -> emit(Result.failure(unwrapStreamError(e, "weightLogs"))) }

  val dashboardState: StateFlow<UiState<FitnessDashboardData>> = combine(
    profileFlow,
    dailyLogsFlow,
    workoutsFlow,
    weightLogsFlow,
    _selectedDateKey,
  ) { profileRes, logsRes, workoutsRes, weightsRes, selectedDate ->
    val firstError = listOf(profileRes, logsRes, workoutsRes, weightsRes)
      .firstOrNull { it.isFailure }
      ?.exceptionOrNull()
    if (firstError != null) {
      return@combine UiState.Error(firstError.message ?: "Failed to sync fitness data")
    }

    val profile = profileRes.getOrNull() ?: UserProfile(
      userId = currentUserId,
      displayName = initialDisplayName.ifBlank { "Athlete" },
    )
    val dailyLogs = logsRes.getOrNull().orEmpty()
    val workouts = workoutsRes.getOrNull().orEmpty()
      .sortedWith(
        compareByDescending<WorkoutEntry> { it.dateKey }
          .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
      )
    val weightLogs = weightsRes.getOrNull().orEmpty()
      .sortedWith(
        compareByDescending<WeightEntry> { it.dateKey }
          .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
      )

    val logsByDate = dailyLogs.associateBy { it.dateKey }
    val selectedDayLog = logsByDate[selectedDate] ?: DailyLog(
      userId = currentUserId,
      dateKey = selectedDate,
    )
    val selectedDayWorkouts = workouts.filter { it.dateKey == selectedDate }
    val selectedDayCalories = selectedDayWorkouts.sumOf { it.caloriesBurned }
    val selectedDayMinutes = selectedDayWorkouts.sumOf { it.durationMinutes }

    val weekKeys = DateUtils.recentDateKeys(7)
    val weeklyBars = weekKeys.map { key ->
      val dayWorkouts = workouts.filter { it.dateKey == key }
      val dayLog = logsByDate[key]
      DailyBarPoint(
        dateKey = key,
        shortDay = DateUtils.dayOfWeekShort(key),
        calories = dayWorkouts.sumOf { it.caloriesBurned },
        activeMinutes = dayWorkouts.sumOf { it.durationMinutes },
        waterMl = dayLog?.waterMl ?: 0L,
        workoutCount = dayWorkouts.size,
      )
    }

    val activeDates = buildSet {
      workouts.forEach { if (it.dateKey.isNotBlank()) add(it.dateKey) }
      dailyLogs.forEach { log ->
        if (log.waterMl > 0L || log.steps >= 3000L || log.habitsCompleted.isNotEmpty()) {
          add(log.dateKey)
        }
      }
    }
    val currentStreak = DateUtils.computeStreak(activeDates)
    val latestWeight = weightLogs.firstOrNull()?.weightKg ?: 70.0

    UiState.Success(
      FitnessDashboardData(
        profile = profile,
        selectedDateKey = selectedDate,
        weekDateKeys = weekKeys,
        dailyLogsByDate = logsByDate,
        workouts = workouts,
        workoutsForSelectedDate = selectedDayWorkouts,
        selectedDayLog = selectedDayLog,
        selectedDayCalories = selectedDayCalories,
        selectedDayMinutes = selectedDayMinutes,
        weeklyBars = weeklyBars,
        currentStreakDays = currentStreak,
        weightLogs = weightLogs,
        latestWeightKg = latestWeight,
      )
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
    initialValue = UiState.Loading,
  )

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(initialDisplayName)
    }
  }

  private fun unwrapStreamError(error: Throwable, streamName: String): Throwable {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val root = firestoreError ?: error
    Log.w(TAG, "Error observing $streamName", root)
    return root
  }

  fun selectDate(dateKey: String) {
    _selectedDateKey.value = dateKey
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun adjustWater(currentLog: DailyLog, deltaMl: Long) {
    val targetWater = (currentLog.waterMl + deltaMl).coerceIn(0L, 20000L)
    viewModelScope.launch {
      val result = repository.upsertDailyLog(
        dateKey = currentLog.dateKey.ifBlank { _selectedDateKey.value },
        waterMl = targetWater,
        steps = currentLog.steps,
        habitsCompleted = currentLog.habitsCompleted,
        mood = currentLog.mood,
      )
      if (result.isSuccess) {
        if (deltaMl > 0) {
          _feedbackMessage.value = "Hydration +${deltaMl} ml logged"
        }
      } else {
        _feedbackMessage.value = "Could not update hydration"
      }
    }
  }

  fun updateSteps(currentLog: DailyLog, newSteps: Long) {
    val safeSteps = newSteps.coerceIn(0L, 200000L)
    viewModelScope.launch {
      val result = repository.upsertDailyLog(
        dateKey = currentLog.dateKey.ifBlank { _selectedDateKey.value },
        waterMl = currentLog.waterMl,
        steps = safeSteps,
        habitsCompleted = currentLog.habitsCompleted,
        mood = currentLog.mood,
      )
      if (result.isSuccess) {
        _feedbackMessage.value = "Steps updated to $safeSteps"
      } else {
        _feedbackMessage.value = "Could not update steps"
      }
    }
  }

  fun toggleHabit(currentLog: DailyLog, habitId: String) {
    val updated = if (currentLog.habitsCompleted.contains(habitId)) {
      currentLog.habitsCompleted - habitId
    } else {
      (currentLog.habitsCompleted + habitId).take(10)
    }
    viewModelScope.launch {
      val result = repository.upsertDailyLog(
        dateKey = currentLog.dateKey.ifBlank { _selectedDateKey.value },
        waterMl = currentLog.waterMl,
        steps = currentLog.steps,
        habitsCompleted = updated,
        mood = currentLog.mood,
      )
      if (result.isFailure) {
        _feedbackMessage.value = "Could not save habit check-in"
      }
    }
  }

  fun setDailyMood(currentLog: DailyLog, moodKey: String) {
    val nextMood = if (currentLog.mood == moodKey) "" else moodKey
    viewModelScope.launch {
      val result = repository.upsertDailyLog(
        dateKey = currentLog.dateKey.ifBlank { _selectedDateKey.value },
        waterMl = currentLog.waterMl,
        steps = currentLog.steps,
        habitsCompleted = currentLog.habitsCompleted,
        mood = nextMood,
      )
      if (result.isFailure) {
        _feedbackMessage.value = "Could not save readiness check-in"
      }
    }
  }

  fun saveWorkout(
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
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val entry = WorkoutEntry(
        id = editingId.orEmpty(),
        userId = currentUserId,
        dateKey = dateKey,
        title = title.trim().ifEmpty { "${category.label} Session" },
        category = category.key,
        durationMinutes = durationMinutes.coerceIn(1L, 600L),
        caloriesBurned = caloriesBurned.coerceIn(0L, 5000L),
        intensity = intensity.key,
        distanceKm = distanceKm.coerceIn(0.0, 500.0),
        setsCompleted = setsCompleted.coerceIn(0L, 200L),
        notes = notes.trim().take(500),
      )
      val result = if (editingId.isNullOrBlank()) {
        repository.addWorkout(entry).map { Unit }
      } else {
        repository.updateWorkout(entry)
      }
      _isBusy.value = false
      if (result.isSuccess) {
        _feedbackMessage.value = if (editingId.isNullOrBlank()) "Workout logged!" else "Workout updated"
        onDone()
      } else {
        _feedbackMessage.value = "Failed to save workout"
      }
    }
  }

  fun deleteWorkout(workoutId: String) {
    viewModelScope.launch {
      val result = repository.deleteWorkout(workoutId)
      if (result.isSuccess) {
        _feedbackMessage.value = "Workout removed"
      } else {
        _feedbackMessage.value = "Could not delete workout"
      }
    }
  }

  fun addWeightLog(weightDisplayValue: Double, preferredUnit: String, note: String, onDone: () -> Unit = {}) {
    val weightKg = if (preferredUnit == "lbs") weightDisplayValue * 0.45359237 else weightDisplayValue
    if (weightKg < 20.0 || weightKg > 400.0) {
      _feedbackMessage.value = "Please enter a valid weight"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.addWeightLog(
        dateKey = _selectedDateKey.value,
        weightKg = (kotlin.math.round(weightKg * 10.0) / 10.0),
        note = note,
      )
      _isBusy.value = false
      if (result.isSuccess) {
        _feedbackMessage.value = "Weight entry saved"
        onDone()
      } else {
        _feedbackMessage.value = "Failed to save weight entry"
      }
    }
  }

  fun deleteWeightLog(entryId: String) {
    viewModelScope.launch {
      val result = repository.deleteWeightLog(entryId)
      if (result.isFailure) {
        _feedbackMessage.value = "Could not delete weight entry"
      }
    }
  }

  fun saveUserGoals(
    displayName: String,
    calorieGoal: Long,
    activeMinutesGoal: Long,
    waterGoalMl: Long,
    stepsGoal: Long,
    weeklyWorkoutsGoal: Long,
    preferredUnit: String,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val result = repository.updateUserProfile(
        displayName = displayName,
        calorieGoal = calorieGoal,
        activeMinutesGoal = activeMinutesGoal,
        waterGoalMl = waterGoalMl,
        stepsGoal = stepsGoal,
        weeklyWorkoutsGoal = weeklyWorkoutsGoal,
        preferredUnit = preferredUnit,
      )
      _isBusy.value = false
      if (result.isSuccess) {
        _feedbackMessage.value = "Goals & profile saved"
      } else {
        _feedbackMessage.value = "Failed to save goals"
      }
    }
  }

  // --- Live Workout Timer ---

  fun startLiveTimer(title: String, category: WorkoutCategory, intensity: WorkoutIntensity) {
    timerJob?.cancel()
    _liveTimer.value = LiveTimerState(
      isActive = true,
      isPaused = false,
      elapsedSeconds = 0L,
      title = title.ifBlank { "${category.label} Session" },
      category = category,
      intensity = intensity,
    )
    launchTimerLoop()
  }

  fun togglePauseTimer() {
    val current = _liveTimer.value
    if (!current.isActive) return
    if (current.isPaused) {
      _liveTimer.update { it.copy(isPaused = false) }
      launchTimerLoop()
    } else {
      timerJob?.cancel()
      _liveTimer.update { it.copy(isPaused = true) }
    }
  }

  fun discardTimer() {
    timerJob?.cancel()
    _liveTimer.value = LiveTimerState()
  }

  fun finishAndSaveTimer(weightKg: Double) {
    val current = _liveTimer.value
    if (!current.isActive) return
    timerJob?.cancel()
    val durationMins = ((current.elapsedSeconds + 59L) / 60L).coerceIn(1L, 600L)
    val estimatedKcal = estimateCaloriesBurned(
      category = current.category,
      intensity = current.intensity,
      durationMinutes = durationMins,
      weightKg = weightKg,
    )
    _liveTimer.value = LiveTimerState()
    saveWorkout(
      editingId = null,
      dateKey = DateUtils.todayKey(),
      title = current.title,
      category = current.category,
      durationMinutes = durationMins,
      caloriesBurned = estimatedKcal,
      intensity = current.intensity,
      distanceKm = 0.0,
      setsCompleted = 0L,
      notes = "Recorded with live workout timer",
    )
  }

  private fun launchTimerLoop() {
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (true) {
        delay(1000L)
        _liveTimer.update { state ->
          if (state.isActive && !state.isPaused) {
            state.copy(elapsedSeconds = state.elapsedSeconds + 1L)
          } else {
            state
          }
        }
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
  }

  private companion object {
    const val TAG = "FitnessVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
