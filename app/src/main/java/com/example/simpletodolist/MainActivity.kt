package com.example.simpletodolist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.navigation.compose.*
import com.example.simpletodolist.ui.theme.SimpleToDoListTheme
import org.json.JSONArray
import org.json.JSONObject

data class Task(var title: String, var description: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleToDoListTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ToDoApp()
                }
            }
        }
    }
}

// Navigation beetwen of screens of tasks e details
@Composable
fun ToDoApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(loadTasks(context)) }

    NavHost(navController, startDestination = "list") {
        composable("list") {
            ToDoListScreen(
                tasks = tasks,
                onTasksChange = {
                    tasks = it
                    saveTasks(context, it)
                },
                onTaskClick = { index -> navController.navigate("details/$index") }
            )
        }
        composable("details/{index}") { backStack ->
            val index = backStack.arguments?.getString("index")?.toIntOrNull() ?: 0
            TaskDetailScreen(
                task = tasks[index],
                onSave = { updated ->
                    val newList = tasks.toMutableList()
                    newList[index] = updated
                    tasks = newList
                    saveTasks(context, newList)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// Main screen with list of tasks
@Composable
fun ToDoListScreen(
    tasks: List<Task>,
    onTasksChange: (List<Task>) -> Unit,
    onTaskClick: (Int) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 40.dp)) {
        Text("Simple To Do List", fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title of task") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description of task") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        Button(
            onClick = {
                if (title.isNotBlank()) {
                    onTasksChange(tasks + Task(title.trim(), description.trim()))
                    title = ""
                    description = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
        ) { Text("+ Add") }

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(tasks.indices.toList()) { index ->
                val task = tasks[index]
                ToDoItem(
                    task = task,
                    onRemove = {
                        onTasksChange(tasks.toMutableList().apply { removeAt(index) })
                    },
                    onEdit = { onTaskClick(index) }
                )
            }
        }
    }
}

// Card show tasks
@Composable
fun ToDoItem(task: Task, onRemove: () -> Unit, onEdit: () -> Unit) {
    var done by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Checkbox(checked = done, onCheckedChange = { done = it })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = task.title,
                    style = if (done) TextStyle(textDecoration = TextDecoration.LineThrough) else LocalTextStyle.current
                )
                if (task.description.isNotBlank())
                    Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remover") }
        }
    }
}

// Details of tasks
@Composable
fun TaskDetailScreen(task: Task, onSave: (Task) -> Unit, onBack: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(task.title) }
    var description by rememberSaveable { mutableStateOf(task.description) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Edit Task", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title of task") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description of task") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { onSave(Task(title, description)) }) { Text("Save") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Return") }
        }
    }
}

// Save tasks to persistance
fun saveTasks(context: Context, tasks: List<Task>) {
    val jsonArray = JSONArray()
    tasks.forEach {
        val obj = JSONObject()
        obj.put("title", it.title)
        obj.put("description", it.description)
        jsonArray.put(obj)
    }
    context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
        .edit()
        .putString("data", jsonArray.toString())
        .apply()
}

// Load tasks to persistence
fun loadTasks(context: Context): List<Task> {
    val prefs = context.getSharedPreferences("tasks", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("data", "[]") ?: "[]"
    val array = JSONArray(jsonString)
    return List(array.length()) { i ->
        val obj = array.getJSONObject(i)
        Task(obj.getString("title"), obj.optString("description", ""))
    }
}