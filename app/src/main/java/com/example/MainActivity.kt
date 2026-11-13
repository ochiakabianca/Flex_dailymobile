package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.WorkoutEntry
import com.example.data.repository.FitnessRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.WorkoutEditorSheet
import com.example.ui.screens.ProfileGoalsScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WorkoutsScreen
import com.example.ui.theme.FlexDailyTheme
import com.example.ui.viewmodel.FitnessViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.DateUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private enum class FlexTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  TODAY("today", R.string.nav_today, Icons.Rounded.Home, Icons.Outlined.Home),
  WORKOUTS("workouts", R.string.nav_workouts, Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter),
  PROGRESS("progress", R.string.nav_progress, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
  PROFILE("profile", R.string.nav_profile, Icons.Rounded.Person, Icons.Outlined.Person),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      FlexDailyTheme {
        AppNavigation()
      }
    }
  }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      FlexDailyMainScaffold(
        currentUserId = user.uid,
        displayName = user.displayName ?: "Athlete",
        userEmail = user.email,
        photoUrl = user.photoUrl?.toString(),
      )
    }
  }
}

@Composable
private fun FlexDailyMainScaffold(
  currentUserId: String,
  displayName: String,
  userEmail: String?,
  photoUrl: String?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: FitnessViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        FitnessViewModel(
          repository = FitnessRepository(db),
          currentUserId = currentUserId,
          initialDisplayName = displayName,
        )
      }
    },
  )

  val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
  val liveTimer by viewModel.liveTimer.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    viewModel.clearFeedback()
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: FlexTab.TODAY.route

  var showWorkoutSheet by remember { mutableStateOf(false) }
  var editingWorkout by remember { mutableStateOf<WorkoutEntry?>(null) }

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useNavRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            FlexTab.entries.forEach { tab ->
              val selected = currentRoute == tab.route
              val label = stringResource(tab.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("nav_${tab.route}"),
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (currentRoute == FlexTab.TODAY.route || currentRoute == FlexTab.WORKOUTS.route) {
          ExtendedFloatingActionButton(
            onClick = {
              editingWorkout = null
              showWorkoutSheet = true
            },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.log_workout)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("fab_log_workout"),
          )
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useNavRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            Spacer(Modifier.height(12.dp))
            FlexTab.entries.forEach { tab ->
              val selected = currentRoute == tab.route
              val label = stringResource(tab.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(tab.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag("rail_${tab.route}"),
              )
            }
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          when (val state = dashboardState) {
            UiState.Loading -> {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                  Text(
                    text = "Syncing your Flex Daily studio...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            is UiState.Error -> {
              Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                  )
                  Button(
                    onClick = { signOut(credentialManager, scope) },
                  ) {
                    Text("Sign Out & Retry")
                  }
                }
              }
            }

            is UiState.Success -> {
              val data = state.data
              val contentModifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)

              NavHost(
                navController = navController,
                startDestination = FlexTab.TODAY.route,
                modifier = contentModifier,
              ) {
                composable(FlexTab.TODAY.route) {
                  TodayScreen(
                    data = data,
                    liveTimer = liveTimer,
                    onSelectDate = viewModel::selectDate,
                    onAdjustWater = viewModel::adjustWater,
                    onUpdateSteps = viewModel::updateSteps,
                    onToggleHabit = viewModel::toggleHabit,
                    onSelectMood = viewModel::setDailyMood,
                    onOpenNewWorkout = {
                      editingWorkout = null
                      showWorkoutSheet = true
                    },
                    onEditWorkout = { workout ->
                      editingWorkout = workout
                      showWorkoutSheet = true
                    },
                    onDeleteWorkout = viewModel::deleteWorkout,
                    onTogglePauseTimer = viewModel::togglePauseTimer,
                    onFinishTimer = { viewModel.finishAndSaveTimer(data.latestWeightKg) },
                    onDiscardTimer = viewModel::discardTimer,
                  )
                }

                composable(FlexTab.WORKOUTS.route) {
                  WorkoutsScreen(
                    data = data,
                    liveTimer = liveTimer,
                    onStartLiveTimer = viewModel::startLiveTimer,
                    onTogglePauseTimer = viewModel::togglePauseTimer,
                    onFinishTimer = { viewModel.finishAndSaveTimer(data.latestWeightKg) },
                    onDiscardTimer = viewModel::discardTimer,
                    onQuickLogPreset = { title, cat, mins, kcal, intensity, dist, sets ->
                      viewModel.saveWorkout(
                        editingId = null,
                        dateKey = DateUtils.todayKey(),
                        title = title,
                        category = cat,
                        durationMinutes = mins,
                        caloriesBurned = kcal,
                        intensity = intensity,
                        distanceKm = dist,
                        setsCompleted = sets,
                        notes = "Quick template",
                      )
                    },
                    onOpenNewWorkout = {
                      editingWorkout = null
                      showWorkoutSheet = true
                    },
                    onEditWorkout = { workout ->
                      editingWorkout = workout
                      showWorkoutSheet = true
                    },
                    onDeleteWorkout = viewModel::deleteWorkout,
                  )
                }

                composable(FlexTab.PROGRESS.route) {
                  ProgressScreen(
                    data = data,
                    onAddWeightLog = { value, note ->
                      viewModel.addWeightLog(value, data.profile.preferredUnit, note)
                    },
                    onDeleteWeightLog = viewModel::deleteWeightLog,
                  )
                }

                composable(FlexTab.PROFILE.route) {
                  ProfileGoalsScreen(
                    data = data,
                    userEmail = userEmail,
                    photoUrl = photoUrl,
                    isBusy = isBusy,
                    onSaveGoals = viewModel::saveUserGoals,
                    onSignOut = { signOut(credentialManager, scope) },
                  )
                }
              }

              if (showWorkoutSheet) {
                WorkoutEditorSheet(
                  initialWorkout = editingWorkout,
                  selectedDateKey = data.selectedDateKey,
                  latestWeightKg = data.latestWeightKg,
                  isSaving = isBusy,
                  onDismiss = {
                    showWorkoutSheet = false
                    editingWorkout = null
                  },
                  onSave = { id, dateKey, title, cat, dur, kcal, intensity, dist, sets, notes ->
                    viewModel.saveWorkout(
                      editingId = id,
                      dateKey = dateKey,
                      title = title,
                      category = cat,
                      durationMinutes = dur,
                      caloriesBurned = kcal,
                      intensity = intensity,
                      distanceKm = dist,
                      setsCompleted = sets,
                      notes = notes,
                      onDone = {
                        showWorkoutSheet = false
                        editingWorkout = null
                      },
                    )
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}
