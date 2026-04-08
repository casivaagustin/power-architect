# Power*Architect — Custom Fork

This document describes the personalizations and workflow improvements added to this fork of [SQL Power Architect](https://github.com/SQLPower/power-architect).

---

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `T` | Create a new table at the current mouse cursor position |
| `I` | Start drawing an **identifying** relationship |
| `N` | Start drawing a **non-identifying** relationship |
| `E` | Open the **column editor** for the focused column |
| `↑` / `↓` | Move focus between columns inside a selected table |
| `Enter` | Edit the selected item (table or column) |
| `Escape` | Cancel current operation |

---

## Creating Tables

Pressing `T` immediately opens the **Table Properties** dialog at the current mouse position — no extra click required to place the table.

Every new table is automatically created with the following default columns:

| Column | Type | Constraints | Default |
|--------|------|-------------|---------|
| `id` | BIGINT | Primary Key, Not Null | — |
| `created_at` | TIMESTAMP | Not Null | `now()` |
| `updated_at` | TIMESTAMP | Nullable | `null` |

---

## Table Properties Dialog

When creating or editing a table, the **Logical Name** field drives the other fields automatically:

- **Physical Name** is kept in sync with the logical name (lowercase).
- **Primary Key Name** is automatically set to `<table_name>_pk`.

All table names are saved in **lowercase**.

---

## Creating Relationships

- Press `I` to draw an **identifying relationship** (child cannot exist without parent).
- Press `N` to draw a **non-identifying relationship** (child exists independently).

### Foreign Key Naming

When a relationship is created from **Table A** to **Table B**, the foreign key column in Table B is automatically named `<table_a_name>_id`.

- If a column with that name and matching type already exists in Table B, it is **reused** rather than duplicated.
- Example: a relationship from `order` → `customer` creates/reuses a column called `order_id` in `customer`.

---

## Editing Columns

### Auto-configuration for `id` columns

When you name a column `id`, the editor automatically sets:
- **Type:** BIGINT
- **Primary Key:** checked
- **Allows Nulls:** No

### Column Type Autocomplete

The type selector is an **autocomplete text field**. Start typing the type name (e.g. `big`, `var`, `time`) and a dropdown will appear with matching types. Use:
- `↑` / `↓` to navigate the list
- `Enter` to select
- `Escape` to close the dropdown

### Column Form Layout

The column editor form is organized in this order:
1. Logical Name
2. Physical Name
3. Type *(autocomplete)*
4. Precision / Scale
5. Allows Nulls
6. Auto Increment
7. In Primary Key
8. Default Value
9. Sequence Name
10. Remarks
11. Source for ETL Mapping

All column names are saved in **lowercase**.

### Shortcut: Edit Focused Column

While navigating a table with `↑`/`↓`, press `E` to open the column editor for the currently focused column.

---

## Canvas Navigation

### Zoom

Hold **Ctrl** and scroll the **mouse wheel** to zoom in and out on the canvas.

---

## Building

Requirements:
- OpenJDK 11+
- Apache Ant
- The `sqlpower-library` project cloned as a sibling directory

```bash
# Clone dependencies
git clone https://github.com/SQLPower/sqlpower-library.git ../sqlpower-library

# Build sqlpower-library first
cd ../sqlpower-library && ant jar && cd -

# Build the application JAR
ant jar

# Run
ant run
```

The output JAR is at `dist/architect.jar`.
