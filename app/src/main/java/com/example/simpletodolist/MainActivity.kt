package com.example.simpletodolist

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.simpletodolist.ui.theme.SimpleToDoListTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

import androidx.compose.material3.ExperimentalMaterial3Api

// --- 1. DATA MODEL UPDATED ---
data class Task(
    var title: String,
    var description: String,
    val id: Int = System.currentTimeMillis().toInt(),
    var scheduledTimeMillis: Long = 0L,
    var isDone: Boolean = false
)

// --- 2. THEME VIEW MODEL ---
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var isDarkTheme by mutableStateOf(prefs.getBoolean("is_dark_theme", false))
        private set

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
    }
}

// --- 3. ALARM RECEIVER ---
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", 0)
        val title = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        var description = intent.getStringExtra("TASK_DESCRIPTION") ?: "It's time to complete this activity."

        val CHANNEL_ID = "task_alarm_channel"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Channel for scheduled task notifications." }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(taskId, notification)
    }
}

// --- 4. ALARM FUNCTIONS ---
fun scheduleAlarm(context: Context, task: Task) {
    if (task.scheduledTimeMillis == 0L || task.scheduledTimeMillis < System.currentTimeMillis()) return

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("TASK_ID", task.id)
        putExtra("TASK_TITLE", task.title)
        putExtra("TASK_DESCRIPTION", task.description)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        task.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        task.scheduledTimeMillis,
        pendingIntent
    )
    Toast.makeText(context, "Reminder scheduled for: ${task.title}", Toast.LENGTH_SHORT).show()
}

fun cancelAlarm(context: Context, taskId: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        taskId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}


// --- 5. MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme

            SimpleToDoListTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ToDoApp(themeViewModel)
                }
            }
        }
    }
}

// --- 6. NAVIGATION ---
@Composable
fun ToDoApp(themeViewModel: ThemeViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(loadTasks(context)) }

    NavHost(navController, startDestination = "list") {
        composable("list") {
            ToDoListScreen(
                tasks = tasks,
                onTasksChange = { newList ->
                    tasks.forEach { oldTask ->
                        if (!newList.any { it.id == oldTask.id }) {
                            cancelAlarm(context, oldTask.id)
                        }
                    }
                    tasks = newList
                    saveTasks(context, newList)
                },
                onTaskClick = { index -> navController.navigate("details/$index") },
                themeViewModel = themeViewModel
            )
        }
        composable("details/{index}") { backStack ->
            val index = backStack.arguments?.getString("index")?.toIntOrNull()
            if (index == null || index !in tasks.indices) {
                navController.popBackStack()
                return@composable
            }

            TaskDetailScreen(
                task = tasks[index],
                onSave = { updated ->
                    cancelAlarm(context, tasks[index].id)

                    val newList = tasks.toMutableList()
                    newList[index] = updated
                    tasks = newList
                    saveTasks(context, newList)

                    scheduleAlarm(context, updated)

                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// --- 7. TO DO LIST SCREEN (Click Fix and TimePicker Logic) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoListScreen(
    tasks: List<Task>,
    onTasksChange: (List<Task>) -> Unit,
    onTaskClick: (Int) -> Unit,
    themeViewModel: ThemeViewModel
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    val calendar = Calendar.getInstance()
    var selectedHour by rememberSaveable { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by rememberSaveable { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 40.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Simple To Do List", fontSize = 26.sp, textAlign = TextAlign.Start)
            IconButton(onClick = { themeViewModel.toggleTheme() }) {
                Icon(
                    imageVector = if (themeViewModel.isDarkTheme) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                    contentDescription = "Toggle Theme"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {

            // --- FIX: Box for reliable click area ---
            Box(modifier = Modifier.weight(1f).clickable { showTimePicker = true }) {
                OutlinedTextField(
                    value = if (selectedHour != 0 || selectedMinute != 0) formattedTime else "No Alarm Set",
                    onValueChange = { /* Not editable */ },
                    label = { Text("Reminder Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    // Disable the TextField's interaction, letting the Box handle the click
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        // Ensure colors look normal despite being 'disabled'
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = "Select Time") }
                )
            }
            // --- END FIX ---

            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (title.isNotBlank()) {
                    val taskTime = if (selectedHour != 0 || selectedMinute != 0) {
                        calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                        calendar.set(Calendar.MINUTE, selectedMinute)
                        calendar.set(Calendar.SECOND, 0)

                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        calendar.timeInMillis
                    } else 0L

                    val newTask = Task(title.trim(), description.trim(), scheduledTimeMillis = taskTime)
                    onTasksChange(tasks + newTask)

                    scheduleAlarm(context, newTask)

                    title = ""
                    description = ""

                    calendar.timeInMillis = System.currentTimeMillis()
                    selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
                    selectedMinute = calendar.get(Calendar.MINUTE)

                } else {
                    Toast.makeText(context, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.align(Alignment.CenterVertically)
            ) { Text("+ Add") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            itemsIndexed(tasks.indices.toList().reversed()) { index, originalIndex ->
                val task = tasks[originalIndex]
                ToDoItem(
                    task = task,
                    onToggleDone = { isDone ->
                        val updatedTask = task.copy(isDone = isDone)
                        val newList = tasks.toMutableList()
                        newList[originalIndex] = updatedTask
                        onTasksChange(newList)
                    },
                    onRemove = {
                        cancelAlarm(context, task.id)
                        onTasksChange(tasks.toMutableList().apply { removeAt(originalIndex) })
                    },
                    onEdit = { onTaskClick(originalIndex) }
                )
            }
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    // Update the state variables with the time chosen in the TimePicker
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

// --- 8. TASK DETAIL SCREEN (Click Fix and TimePicker Logic) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(task: Task, onSave: (Task) -> Unit, onBack: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var description by rememberSaveable { mutableStateOf(task.description) }

    val calendar = Calendar.getInstance().apply { timeInMillis = task.scheduledTimeMillis.takeIf { it > 0L } ?: System.currentTimeMillis() }
    var selectedHour by rememberSaveable { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by rememberSaveable { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
    val hasAlarm = task.scheduledTimeMillis > 0L

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Edit Task", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        // --- FIX: Box for reliable click area ---
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showTimePicker = true }) {
            OutlinedTextField(
                value = if (hasAlarm) formattedTime else "No Alarm Set",
                onValueChange = { /* Not editable */ },
                label = { Text("Reminder Time") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                // Disable the TextField's interaction, letting the Box handle the click
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                ),
                trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = "Select Time") }
            )
        }
        // --- END FIX ---

        if (showTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("Select Time") },
                text = {
                    TimePicker(state = timePickerState)
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Update the state variables with the time chosen in the TimePicker
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = {
                val updatedCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                    set(Calendar.SECOND, 0)
                }

                if (updatedCalendar.timeInMillis <= System.currentTimeMillis() && hasAlarm) {
                    updatedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val newTimeMillis = updatedCalendar.timeInMillis

                onSave(Task(title, description, task.id, newTimeMillis, task.isDone))
            }) { Text("Save") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}

// --- 9. PERSISTENCE FUNCTIONS ---
fun saveTasks(context: Context, tasks: List<Task>) {
    val jsonArray = JSONArray()
    tasks.forEach {
        val obj = JSONObject()
        obj.put("title", it.title)
        obj.put("description", it.description)
        obj.put("id", it.id)
        obj.put("scheduledTimeMillis", it.scheduledTimeMillis)
        obj.put("isDone", it.isDone)
        jsonArray.put(obj)
    }
    context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        .edit()
        .putString("data", jsonArray.toString())
        .apply()
}

fun loadTasks(context: Context): List<Task> {
    val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("data", "[]") ?: "[]"
    val array = JSONArray(jsonString)
    return List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        Task(
            title = obj.getString("title"),
            description = obj.optString("description", ""),
            id = obj.optInt("id", obj.getString("title").hashCode()),
            scheduledTimeMillis = obj.optLong("scheduledTimeMillis", 0L),
            isDone = obj.optBoolean("isDone", false)
        )
    }
}

// --- 10. TO DO ITEM COMPONENT ---
@Composable
fun ToDoItem(
    task: Task,
    onToggleDone: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    var done by rememberSaveable { mutableStateOf(task.isDone) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Checkbox(
                checked = done,
                onCheckedChange = { isChecked ->
                    done = isChecked
                    onToggleDone(isChecked) // Saves the state
                }
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = task.title,
                    style = if (done) TextStyle(textDecoration = TextDecoration.LineThrough) else LocalTextStyle.current
                )
                if (task.description.isNotBlank())
                    Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (task.scheduledTimeMillis > 0L) {
                    val calendar = Calendar.getInstance().apply { timeInMillis = task.scheduledTimeMillis }
                    val timeString = String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
                    Text("Reminder: $timeString", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove") }
        }
    }
}