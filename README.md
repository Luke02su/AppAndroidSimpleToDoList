# 📝 Simple ToDo List App

A minimalist **ToDo List** application built with **Kotlin** and **Jetpack Compose**, designed for simplicity, clarity, and persistence.  
It allows users to **add, edit, mark, and remove** tasks — and all data is **saved locally** using `SharedPreferences`, so nothing is lost when the app closes.

## 🚀 Features

- ✅ Add new tasks with **title** and **description**
- ✏️ Edit task details in a dedicated screen
- ☑️ Mark tasks as completed
- 🗑️ Delete tasks easily
- 💾 Automatic **data persistence** (keeps tasks after closing the app)
- 🧭 Simple navigation between list and detail screens using **Navigation Compose**

## 🧱 Tech Stack

- **Language:** Kotlin  
- **UI Framework:** Jetpack Compose  
- **State Management:** `remember` + `mutableStateOf`  
- **Local Storage:** SharedPreferences (JSON serialization)  
- **Navigation:** Navigation Compose  

## 📱 How It Works

1. Type a **title** and an optional **description** for your task.  
2. Tap **+ Add** to include it in the list.  
3. Tap a task to **edit or update** its details.  
4. Check the box to mark a task as completed.  
5. Tap the trash icon 🗑️ to remove it.  

All your tasks are automatically **saved** and **restored** when you reopen the app.

## 💡 Future Improvements

- Add a progress indicator (e.g., “3 of 10 tasks done”)  
- Add search or filter for completed tasks  
- Switch to **Room Database** for more advanced persistence  
- Add cloud sync (e.g., Firebase integration)

## 🧑‍💻 Author

**Lucas Samuel Dias**  
Developed for learning and demonstration purposes with a focus on simplicity and usability.

## 🪪 License

This project is open source and available under the **MIT License**.


## 🧩 Project Structure

