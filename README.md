# SOEN342 Project

## Team
- Karim Mikhaeil - 40233685
- Marc EL Haddad - 40231208
- Carlos Mansour - 40297031

## Overview
This repository contains the final implementation of the Personal Task Management System developed for SOEN342.

The final version combines the features developed across the project iterations into one runnable Java application. The system supports task creation and management, recurring tasks, collaborators with workload limits, projects, task history, search, CSV import/export, iCal export, and JSON persistence.

## Final Features
- Create regular tasks
- Create recurring tasks
- Update task details
- Change task status and reopen tasks
- Manage tags
- Manage subtasks
- Create projects
- Assign and remove tasks from projects
- Create collaborators
- Assign collaborators to tasks through subtasks
- Search tasks by criteria
- View task activity history
- Import tasks from CSV
- Export tasks to CSV
- Export a single task to iCal
- Export project tasks to iCal
- Export filtered tasks to iCal
- View overloaded collaborators
- Save and load application data

## Source Layout
The project uses a single canonical source tree at the repository root:

```text
src/
|-- Main.java
|-- gateway/
|-- models/
|-- persistence/
`-- services/
```

The iteration folders are kept in the repository under `archives/` for archived artifacts and documentation:

```text
archives/
|-- iteration-1/
|-- iteration-2/
`-- iteration-3/
```

## Main Components
- `Main` provides the interactive command-line interface.
- `models/` contains the domain classes and enums.
- `services/` contains the application logic for tasks, search, CSV import/export, and related operations.
- `persistence/` contains JSON-based save/load support.
- `gateway/` contains the iCal export gateway.

## How To Compile
From the repository root:

```powershell
javac -d out (Get-ChildItem -Recurse -File -Filter *.java -Path src | ForEach-Object { $_.FullName })
```

## How To Run
From the repository root:

```powershell
java -cp out Main
```

## Main Menu
The application launches an interactive menu with the following top-level options:

1. List all tasks
2. Create task
3. Create project
4. Create collaborator
5. Manage tasks
6. Manage projects
7. Manage collaborators
8. Search tasks
9. View task activity history
10. Import tasks from CSV
11. Export tasks
12. Save system state
13. Exit

## Generated Files
When the application runs, it may generate:
- `out/` for compiled `.class` files
- `data/` for persisted JSON files
- `.csv` files for CSV import/export
- `.ics` files for iCal export

These generated files are not part of the source code and should not be treated as implementation artifacts.

## Notes
- The final project code is the root `src/` folder.
- Archived iteration artifacts remain in the repo for reference and submission history.
- The system starts using saved data from `data/` when available.

## Demo Video
The demo video link is provided in [Demo Link.md](./Demo%20Link.md).
