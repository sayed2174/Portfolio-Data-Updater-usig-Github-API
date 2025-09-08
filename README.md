# GitHub JSON CRUD Android App

A modern Android application for performing CRUD (Create, Read, Update, Delete) operations on JSON files directly within a GitHub repository. The app features a clean, user-friendly interface built with Material Design 3 and leverages the Chaquopy plugin to run a Python backend for GitHub API interactions.

![GitHub CRUD App Demo](https://i.imgur.com/your-app-demo.gif)
*(Suggestion: Record a GIF of your app in action and replace the link above)*

---

## ✨ Features

* **Connect to any GitHub Repository**: Securely access public or private repositories using a Personal Access Token (PAT).
* **JSON File Management**: Automatically lists and allows searching of all `.json` files in the repository.
* **Full CRUD Functionality**:
    * **Create**: Add new JSON files with initial content.
    * **Read**: View and edit JSON data in a user-friendly key-value format.
    * **Update**: Modify keys, values, and structure, then commit changes with a custom message.
    * **Delete**: Easily remove files or individual key-value pairs.
* **Advanced JSON Editor**:
    * Supports nested JSON objects and arrays with a drill-down editing interface.
    * Convenient row-level actions including **Copy**, **Duplicate Above**, and **Duplicate Below**.
* **Modern UI/UX**:
    * A clean, responsive "box-like" UI built with Material Design 3.
    * Fluid animations for list operations (add, remove, update).
    * Integrated light and dark theme support.
* **Local Preview Mode**: Test the app's editing functionality offline using sample JSON files stored in the app's assets.

---

## 🛠️ Technology Stack

* **Language**: **Java**
* **Platform**: **Android**
* **Core Libraries**: AndroidX (AppCompat, RecyclerView, CardView, ConstraintLayout)
* **UI Components**: Google Material Components (MaterialCardView, TextInputLayout, FloatingActionButton)
* **Python Integration**: **Chaquopy** (for running Python scripts within the Android app)
* **Backend**: **GitHub API** (managed via a Python script)

---

## 🚀 Setup and Installation

Follow these steps to get the project up and running on your local machine.

### Prerequisites

* **Android Studio** (latest stable version recommended)
* **JDK 11** or higher

### Steps

1.  **Clone the repository**:
    ```sh
    git clone [https://github.com/your-username/your-repo-name.git](https://github.com/your-username/your-repo-name.git)
    ```

2.  **Open in Android Studio**:
    * Launch Android Studio.
    * Select `File` -> `Open` and navigate to the cloned project directory.

3.  **Add the Python Backend Script**:
    * The app uses a Python script to handle all communication with the GitHub API.
    * Create a Python script named `github_api.py`.
    * Place this script inside the `app/src/main/python/` directory. You may need to create the `python` folder.

    Your `github_api.py` script must contain the following functions that the Java code calls:
    * `list_json_files(owner, repo, pat, path)`
    * `get_file_content(owner, repo, path, pat, branch)`
    * `create_file(owner, repo, path, pat, content, message)`
    * `update_file(owner, repo, path, pat, content, sha, message, branch)`

4.  **Build and Run**:
    * Let Android Studio sync the Gradle files. Chaquopy will be configured automatically.
    * Run the app on an emulator or a physical device.

---
