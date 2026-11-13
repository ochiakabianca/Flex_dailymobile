package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.WorkoutEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FitnessRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserProfile_and_updateUserProfile_succeedsForOwner() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = FitnessRepository(firestore, auth)

    val ensureResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserProfile("Alice Flex") }
    assertTrue(ensureResult.isSuccess)

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeUserProfile().first { it != null }
    }
    assertNotNull(profile)
    assertEquals("Alice Flex", profile!!.displayName)
    assertEquals(500L, profile.calorieGoal)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateUserProfile(
        displayName = "Alice Athlete",
        calorieGoal = 650L,
        activeMinutesGoal = 60L,
        waterGoalMl = 3000L,
        stepsGoal = 10000L,
        weeklyWorkoutsGoal = 6L,
        preferredUnit = "lbs",
      )
    }
    assertTrue(updateResult.isSuccess)
  }

  @Test
  fun upsertDailyLog_createAndIncrementalUpdate_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = FitnessRepository(firestore, auth)
    val dateKey = "2026-11-13"

    val firstWrite = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertDailyLog(
        dateKey = dateKey,
        waterMl = 500L,
        steps = 3200L,
        habitsCompleted = listOf("morning_mobility"),
        mood = "energized",
      )
    }
    assertTrue(firstWrite.isSuccess)

    val secondWrite = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertDailyLog(
        dateKey = dateKey,
        waterMl = 1000L,
        steps = 5200L,
        habitsCompleted = listOf("morning_mobility", "protein_goal"),
        mood = "good",
      )
    }
    assertTrue(secondWrite.isSuccess)

    val logs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDailyLogs().first { list -> list.any { it.dateKey == dateKey && it.waterMl == 1000L } }
    }
    assertTrue(logs.any { it.dateKey == dateKey && it.waterMl == 1000L })
  }

  @Test
  fun workouts_createObserveUpdateDelete_andCrossUserIsolation() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = FitnessRepository(firestore, auth)

    val workoutId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.addWorkout(
        WorkoutEntry(
          dateKey = "2026-11-13",
          title = "Morning Kettlebell Circuit",
          category = "strength",
          durationMinutes = 35L,
          caloriesBurned = 280L,
          intensity = "vigorous",
          distanceKm = 0.0,
          setsCompleted = 12L,
          notes = "Solid tempo",
        )
      ).getOrThrow()
    }
    assertTrue(workoutId.isNotEmpty())

    val observed = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeWorkouts().first { list -> list.any { it.id == workoutId } }
    }
    val saved = observed.first { it.id == workoutId }
    assertEquals("Morning Kettlebell Circuit", saved.title)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.updateWorkout(saved.copy(caloriesBurned = 310L, notes = "Added finisher"))
    }
    assertTrue(updateResult.isSuccess)

    // Cross-user isolation check: Bob cannot read Alice's workout directly
    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("workouts").document(workoutId)
          .get().await()
      }
      fail("Expected PERMISSION_DENIED for cross-user read")
    } catch (e: Exception) {
      val firestoreEx = e as? FirebaseFirestoreException ?: e.unwrapFirestoreException()
      assertNotNull(firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }

    // Re-sign in Alice and clean up
    signInTestUser(ALICE_EMAIL)
    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.deleteWorkout(workoutId) }
    assertTrue(deleteResult.isSuccess)
  }

  @Test
  fun observeWorkouts_unauthenticatedUser_failsFastWithIllegalState() = runBlocking {
    auth.signOut()
    val repo = FitnessRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeWorkouts().first() }
      fail("Expected IllegalStateException when unauthenticated")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("signed in"))
    }
  }

  @Test
  fun directUnauthenticatedFirestoreRead_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document("some_uid")
          .collection("workouts").get().await()
      }
      fail("Expected PERMISSION_DENIED when unauthenticated")
    } catch (e: Exception) {
      val firestoreEx = e as? FirebaseFirestoreException ?: e.unwrapFirestoreException()
      assertNotNull(firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_flex@test.com"
    const val BOB_EMAIL = "bob_flex@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
