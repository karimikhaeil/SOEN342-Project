# Iteration 2 — Use Cases (Iteration 2)

## Critical Use Cases

---

### UC6 — Search Tasks
**Primary Actor:** User  
**Goal:** Find tasks matching given criteria  
**Level:** User goal  

**Preconditions:**
- The system is running and contains tasks.

**Postconditions on Success:**
- A list of matching tasks is returned, sorted by due date ascending.
- If no criteria provided, all open tasks are returned.

**Main Success Scenario:**
1. User provides search criteria (period, title keyword, day of week, status).
2. System validates the criteria.
3. System filters tasks based on criteria.
4. System returns results sorted by due date ascending.

**Extensions:**  
4a. No tasks match → system returns empty list with message "No tasks found."

---

### UC12 — Export Tasks to CSV
**Primary Actor:** User  
**Goal:** Export all tasks to a CSV file  
**Level:** User goal  

**Preconditions:**
- The system contains at least one task.

**Postconditions on Success:**
- A CSV file is created with columns: TaskName, Description, Subtask, Status, Priority, DueDate, ProjectName, ProjectDescription, Collaborator, CollaboratorCategory.

**Main Success Scenario:**
1. User requests export with a target file path.
2. System fetches all tasks.
3. System formats each task into a CSV row.
4. System writes the file.
5. System confirms export success.

---

### UC13 — Import Tasks from CSV
**Primary Actor:** User  
**Goal:** Import tasks into the system from a CSV file  
**Level:** User goal  

**Preconditions:**
- A valid CSV file exists at the given path.
- CSV columns follow the defined format.

**Postconditions on Success:**
- All valid rows are imported as tasks.
- Missing projects are automatically created (project names are unique).
- Missing collaborators are automatically created.

**Main Success Scenario:**
1. User provides the path to a CSV file.
2. System reads and parses each row.
3. For each row, system checks if the Project exists; creates it if not.
4. System checks if Collaborator exists; creates it if not.
5. System creates each Task and links it accordingly.
6. System reports the number of successfully imported tasks.

**Extensions:**  
2a. File not found → displayError("File not found")  
3a. Invalid row format → skip row, log warning, continue  

---

## Non-Critical Use Cases

### UC2 — Create Recurring Task
**Actor:** User  
**Goal:** Create a task with a recurrence pattern  

**Main Success Scenario:**
1. User creates a task and specifies a recurrence pattern (daily / weekly / monthly / custom).
2. User provides start date, end date, and interval.
3. System creates the task and generates occurrences.
4. Each occurrence has the same title but a unique due date.

**Constraint:** Completing one occurrence does NOT complete future ones.

---

### UC8 — Add Collaborator to Task
**Actor:** User  
**Goal:** Link a collaborator to a project task  

**Main Success Scenario:**
1. User selects a task under a project.
2. User selects a collaborator defined under that project.
3. System checks if collaborator is within their open task limit.
4. System creates a subtask and links it to the collaborator.

**Extensions:**  
3a. Collaborator is at their limit → displayError("Collaborator has reached open task limit")

---

### UC5 — Reopen Task
**Actor:** User  
**Goal:** Reopen a completed or cancelled task  

**Main Success Scenario:**
1. User selects a completed or cancelled task.
2. System sets status back to open.
3. System records an ActivityEntry with actionType = updated.

**Constraint:** Reopening a task may cause a collaborator to become overloaded.

---

### UC9 — Change Collaborator Category / Limit
**Actor:** User  
**Goal:** Change the category or limit of a collaborator  

**Main Success Scenario:**
1. User selects a collaborator.
2. User sets a new category (Junior / Intermediate / Senior).
3. System updates the limit accordingly.

**Constraint:** Change takes effect immediately even if the collaborator currently has assigned open tasks.
