Iteration 3 - Deployment and OCL

## What's New

| Feature | Description |
|---|---|
| **OCL Constraints** | Formal business rules in Object Constraint Language |
| **iCal Export** | Export tasks to `.ics` for Google Calendar / Outlook / Apple Calendar |
| **Overload Detection** | Menu item listing collaborators exceeding their task limit |
| **Persistence** | JSON file-based storage; data survives between runs |


## Project Structure
```
iteration-3/
├── OCL_Constraints.md
├── README.md
├── src/
│   ├── Main.java
│   ├── gateway/
│   │   ├── ICalGateway.java       ← Gateway interface
│   │   └── ICalGatewayImpl.java   ← RFC 5545 iCal writer
│   ├── models/                    ← All domain models (updated from iter 2)
│   ├── persistence/
│   │   ├── TaskRepository.java    ← Repository interface
│   │   └── JsonTaskRepository.java
│   └── services/
│       └── TaskService.java       ← Updated with all iter 3 features
```

## How to Compile & Run

### Requirements
- Java 11 or higher
- No external libraries required

### Compile 
```bash
cd src
javac -d ../out \
  models/*.java \
  services/*.java \
  gateway/*.java \
  persistence/*.java \
  Main.java
```

### Run
```bash
cd out
java Main
```

Data is saved automatically to `data/tasks.json` and `data/projects.json` on exit.


## iCal Export Rules

- Only tasks **with a due date** are exported — tasks without one are skipped.
- Each task becomes one `VEVENT` with: title, description, due date, status, priority, project name.
- Subtasks appear as a summary inside the parent task's description — NOT as separate events.

### Three export modes (menu options 4 / 5 / 6)

| Option | Description |
|---|---|
| 4 | Export a single chosen task |
| 5 | Export all tasks in a project |
| 6 | Export a filtered list (by date range / status) |


## Overloaded Collaborators (Menu Option 7)

Lists all collaborators whose open task count exceeds their category limit:

- Senior: limit = 2
- Intermediate: limit = 5
- Junior: limit = 10


## Persistence

| File | Contents |
|---|---|
| `data/tasks.json` | All tasks with embedded subtasks |
| `data/projects.json` | All projects with embedded collaborators |

Data loads automatically on startup and saves automatically on exit.
