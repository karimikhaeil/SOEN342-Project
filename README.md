# SOEN342 Project

## Team
- Karim Mikhaeil - 40233685
- Marc EL Haddad - 40231208
- Carlos Mansour - 40297031

## Source Layout
The project now uses a single canonical source tree:

```text
src/
|-- Main.java
|-- gateway/
|-- models/
|-- persistence/
`-- services/
```

The `iteration-1`, `iteration-2`, and `iteration-3` folders remain in the repo for submitted artifacts and documentation, but the runnable Java code now lives only in the root `src/` folder.

## Compile
From the repository root:

```powershell
javac -d out (Get-ChildItem -Recurse -File -Filter *.java -Path src | ForEach-Object { $_.FullName })
```

## Run
From the repository root:

```powershell
java -cp out Main
```

## Runtime Output
When the application runs, it may generate:

- `out/` for compiled classes
- `data/` for persisted JSON data
- `.ics` files for calendar export
- `.csv` files for CSV export
