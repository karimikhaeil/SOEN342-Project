# Iteration 2 - Task Management System

## Overview
This iteration extends the task management system with a proof of concept for:

- recurring tasks
- collaborator assignment with workload limits
- task search with filtering criteria
- CSV import
- CSV export

## Project Structure
```text
iteration-2/
|-- README.md
|-- Updated Domain Model.pdf
|-- Updated Use Case Diagram.pdf
|-- SSDs/
|-- src/
|   |-- Main.java
|   |-- models/
|   `-- services/
```

## How to Compile
From the `iteration-2` folder, compile all Java source files into the `out` directory:

```powershell
javac -d out (Get-ChildItem -Recurse -File -Filter *.java -Path src | ForEach-Object { $_.FullName })
```

## How to Run
After compiling, run the program with:

```powershell
java -cp out Main
```

## Program Behavior
When the program runs, it will:

1. create sample manual tasks
2. create a recurring task and generate occurrences
3. create a demo `tasks.csv` file
4. import tasks from that CSV file
5. search and display open tasks
6. search tasks by keyword
7. export the resulting task list to `output.csv`

## Notes
- Source code is stored in `src/`
- Compiled `.class` files are generated in `out/`
- `tasks.csv` and `output.csv` are generated during the demo run
