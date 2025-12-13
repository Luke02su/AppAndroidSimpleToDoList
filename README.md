## 📝 Simple ToDo List App (Updated)

[](https://kotlinlang.org/)
[](https://developer.android.com/studio)
[](https://www.google.com/search?q=LICENSE)

-----

A minimalist **ToDo List** application built with **Kotlin** and **Jetpack Compose**, designed for simplicity, clarity, and persistence.
It allows users to **add, edit, mark, and remove** tasks — and all data is **saved locally** using `SharedPreferences`, so nothing is lost when the app closes.

## 🚀 Features

  - ✅ Add new tasks with **title** and **description**, and **Toast** for signage when fields be empty
  - ✏️ Edit task details in a dedicated screen
  - ☑️ Mark tasks as completed **(State is now saved and restored)**
  - 🗑️ Delete tasks easily
  - 💾 Automatic **data persistence** (keeps tasks after closing the app)
  - 🧭 Simple navigation between list and detail screens using **Navigation Compose**
  - ⏰ **Scheduled Reminders:** Users can set an exact time for a task, triggering a local notification (AlarmManager/BroadcastReceiver)
  - 🌙 **Dark/Light Theme Toggle:** Ability to switch between dark and light themes, with preference persistence.

-----

## 📱 Print of Screen 9Old Version)

<p align="center">
   <img width="300" height="600" alt="Tela 1" src="https://github.com/user-attachments/assets/9e10e2b2-3732-4b53-8294-7be8cba353bd" />
   <img width="300" height="600" alt="Tela 2" src="https://github.com/user-attachments/assets/5c8595df-6f2a-4252-a798-83876c60e453" />
   <img width="300" height="600" alt="Tela 3" src="https://github.com/user-attachments/assets/78dddeea-fe81-4854-a1d5-b65a5b74c8ed" />
</p>

-----

## 🧱 Tech Stack

  - **Language:** Kotlin
  - **UI Framework:** Jetpack Compose
  - **State Management:** `remember` + `mutableStateOf` / `ViewModel`
  - **Local Storage:** SharedPreferences (JSON serialization)
  - **Navigation:** Navigation Compose
  - **Scheduling:** `AlarmManager` and `BroadcastReceiver` (for reminders)
  - **Theming:** Material 3 Dynamic Theming (Dark/Light Mode)

-----

## 📱 How It Works

1.  Type a **title** and an optional **description** for your task.
2.  **(New)** Tap the "Reminder Time" field to open the **Time Picker** and set an alarm time.
3.  Tap **+ Add** to include it in the decreasing list.
4.  Tap the edit icon ✏️​ to modify it (including the scheduled time).
5.  Tap the trash icon 🗑️ to remove it.
6.  Check the box to mark a task as completed. **(State is saved immediately)**
7.  **(New)** Tap the Sun/Moon icon in the header to switch between Dark and Light Themes.

All your tasks and theme preference are automatically **saved** and **restored** when you reopen the app.

-----

## 💡 Future Improvements

  - Add draggable for move tasks in vertical orientation
  - Add a progress indicator (e.g., “3 of 10 tasks done”)
  - Add search or filter for completed tasks
  - Switch to **Room Database** for more advanced persistence
  - Add cloud sync (e.g., Firebase integration)

-----

## 🧑‍💻 Author

**Lucas Samuel Dias**
Developed for learning and demonstration purposes with a focus on simplicity and usability.

-----

## 🪪 License

This project is open source and available under the **MIT License**.
