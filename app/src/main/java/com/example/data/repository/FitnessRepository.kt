package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.DailyLog
import com.example.data.model.UserProfile
import com.example.data.model.WeightEntry
import com.example.data.model.WorkoutEntry
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FitnessRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)
  private fun dailyLogsCol(uid: String) = userDoc(uid).collection("dailyLogs")
  private fun workoutsCol(uid: String) = userDoc(uid).collection("workouts")
  private fun weightLogsCol(uid: String) = userDoc(uid).collection("weightLogs")

  private fun logFlowErrorIfSignedIn(error: Throwable, op: OperationType, path: String, expectedUid: String) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
  }

  fun observeUserProfile(): Flow<UserProfile?> = flow {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, OperationType.GET, docRef.path, uid)
          throw error
        }
    )
  }

  suspend fun ensureUserProfile(displayName: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    val safeName = displayName.trim().take(80).ifEmpty { "Athlete" }
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "calorieGoal" to 500L,
          "activeMinutesGoal" to 45L,
          "waterGoalMl" to 2500L,
          "stepsGoal" to 8000L,
          "weeklyWorkoutsGoal" to 5L,
          "preferredUnit" to "kg",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserProfile(
    displayName: String,
    calorieGoal: Long,
    activeMinutesGoal: Long,
    waterGoalMl: Long,
    stepsGoal: Long,
    weeklyWorkoutsGoal: Long,
    preferredUnit: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = userDoc(uid)
    val safeName = displayName.trim().take(80).ifEmpty { "Athlete" }
    val safeUnit = if (preferredUnit == "lbs") "lbs" else "kg"
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "calorieGoal" to calorieGoal.coerceIn(100L, 10000L),
          "activeMinutesGoal" to activeMinutesGoal.coerceIn(5L, 600L),
          "waterGoalMl" to waterGoalMl.coerceIn(250L, 10000L),
          "stepsGoal" to stepsGoal.coerceIn(500L, 100000L),
          "weeklyWorkoutsGoal" to weeklyWorkoutsGoal.coerceIn(1L, 14L),
          "preferredUnit" to safeUnit,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(payload).await()
      } else {
        val updates = mapOf(
          "displayName" to safeName,
          "calorieGoal" to calorieGoal.coerceIn(100L, 10000L),
          "activeMinutesGoal" to activeMinutesGoal.coerceIn(5L, 600L),
          "waterGoalMl" to waterGoalMl.coerceIn(250L, 10000L),
          "stepsGoal" to stepsGoal.coerceIn(500L, 100000L),
          "weeklyWorkoutsGoal" to weeklyWorkoutsGoal.coerceIn(1L, 14L),
          "preferredUnit" to safeUnit,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeDailyLogs(): Flow<List<DailyLog>> = flow {
    val uid = requireUserId()
    val colRef = dailyLogsCol(uid)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, OperationType.LIST, colRef.path, uid)
          throw error
        }
    )
  }

  suspend fun upsertDailyLog(
    dateKey: String,
    waterMl: Long,
    steps: Long,
    habitsCompleted: List<String>,
    mood: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = dailyLogsCol(uid).document(dateKey)
    val safeHabits = habitsCompleted.distinct().take(10)
    val safeWater = waterMl.coerceIn(0L, 20000L)
    val safeSteps = steps.coerceIn(0L, 200000L)
    return try {
      db.runTransaction { transaction ->
        val existing = transaction.get(docRef)
        if (!existing.exists()) {
          val createPayload = mapOf(
            "userId" to uid,
            "dateKey" to dateKey,
            "waterMl" to safeWater,
            "steps" to safeSteps,
            "habitsCompleted" to safeHabits,
            "mood" to mood,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          transaction.set(docRef, createPayload)
        } else {
          val updatePayload = mapOf(
            "waterMl" to safeWater,
            "steps" to safeSteps,
            "habitsCompleted" to safeHabits,
            "mood" to mood,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
          transaction.update(docRef, updatePayload)
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeWorkouts(): Flow<List<WorkoutEntry>> = flow {
    val uid = requireUserId()
    val colRef = workoutsCol(uid)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(WorkoutEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, OperationType.LIST, colRef.path, uid)
          throw error
        }
    )
  }

  suspend fun getWorkoutById(workoutId: String): Result<WorkoutEntry?> {
    val uid = requireUserId()
    val docRef = workoutsCol(uid).document(workoutId)
    return try {
      val snapshot = docRef.get().await()
      Result.success(
        if (snapshot.exists()) {
          snapshot.toObject(WorkoutEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addWorkout(entry: WorkoutEntry): Result<String> {
    val uid = requireUserId()
    val docRef = workoutsCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "dateKey" to entry.dateKey,
      "title" to entry.title.trim().take(100),
      "category" to entry.category,
      "durationMinutes" to entry.durationMinutes.coerceIn(1L, 600L),
      "caloriesBurned" to entry.caloriesBurned.coerceIn(0L, 5000L),
      "intensity" to entry.intensity,
      "distanceKm" to entry.distanceKm.coerceIn(0.0, 500.0),
      "setsCompleted" to entry.setsCompleted.coerceIn(0L, 200L),
      "notes" to entry.notes.trim().take(500),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateWorkout(entry: WorkoutEntry): Result<Unit> {
    val uid = requireUserId()
    require(entry.id.isNotBlank()) { "Workout ID must not be blank for update" }
    val docRef = workoutsCol(uid).document(entry.id)
    val updates = mapOf(
      "dateKey" to entry.dateKey,
      "title" to entry.title.trim().take(100),
      "category" to entry.category,
      "durationMinutes" to entry.durationMinutes.coerceIn(1L, 600L),
      "caloriesBurned" to entry.caloriesBurned.coerceIn(0L, 5000L),
      "intensity" to entry.intensity,
      "distanceKm" to entry.distanceKm.coerceIn(0.0, 500.0),
      "setsCompleted" to entry.setsCompleted.coerceIn(0L, 200L),
      "notes" to entry.notes.trim().take(500),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteWorkout(workoutId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = workoutsCol(uid).document(workoutId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeWeightLogs(): Flow<List<WeightEntry>> = flow {
    val uid = requireUserId()
    val colRef = weightLogsCol(uid)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(WeightEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, OperationType.LIST, colRef.path, uid)
          throw error
        }
    )
  }

  suspend fun addWeightLog(dateKey: String, weightKg: Double, note: String): Result<String> {
    val uid = requireUserId()
    val docRef = weightLogsCol(uid).document()
    val payload = mapOf(
      "userId" to uid,
      "dateKey" to dateKey,
      "weightKg" to weightKg.coerceIn(20.0, 400.0),
      "note" to note.trim().take(200),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteWeightLog(entryId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = weightLogsCol(uid).document(entryId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }
}
