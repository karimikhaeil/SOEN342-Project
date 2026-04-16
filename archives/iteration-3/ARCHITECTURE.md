# Iteration 3 Architecture

## Overview
The final implementation has been consolidated into the repository-level `src/` folder.

The runnable system is organized into these packages:

- `src/`
- `src/models/`
- `src/services/`
- `src/gateway/`
- `src/persistence/`

## Layering

### Presentation
- `Main.java`
- Provides the command-line menu for listing tasks, creating tasks, searching, exporting, and saving.

### Domain Model
- `models/`
- Contains the core entities such as `Task`, `Subtask`, `Collaborator`, `Project`, recurrence types, statuses, and activity tracking.

### Services
- `services/TaskService.java`
- Coordinates task creation, search, OCL rule checks, overload detection, and export/persistence integration.

### Gateway
- `gateway/ICalGateway.java`
- `gateway/ICalGatewayImpl.java`
- Encapsulates iCalendar export so the service layer does not depend on format details directly.

### Persistence
- `persistence/TaskRepository.java`
- `persistence/JsonTaskRepository.java`
- Handles saving and loading the application state using JSON files under `data/`.

## Key Iteration 3 Features

- OCL-style business rule enforcement
- iCal export for single task, project tasks, and filtered task lists
- overload detection for collaborators
- JSON persistence

## Compile And Run
From the repository root:

```powershell
javac -d out (Get-ChildItem -Recurse -File -Filter *.java -Path src | ForEach-Object { $_.FullName })
java -cp out Main
```
