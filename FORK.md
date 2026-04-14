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

- If the source table name is **plural**, it is automatically converted to **singular** before building the column name.
- If a column with that name and matching type already exists in Table B, it is **reused** rather than duplicated.
- Examples:
  - `orders` → `customer` creates `order_id` (plural → singular)
  - `categories` → `product` creates `category_id` (ies → y)
  - `matches` → `player` creates `match_id` (ches → ch)
  - `order` → `customer` creates `order_id` (already singular, unchanged)

### Mapping to an Existing Column (Ctrl+click)

After selecting the PK table (step 1 of `I`/`N`), you can **Ctrl+click a specific column** in Table B to connect the relationship to that exact existing field instead of auto-generating a new one.

- **Normal click** on Table B → auto-generates (or reuses) the FK column by name.
- **Ctrl+click on a column** in Table B → maps the relationship directly to that column, no new column is created.

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

## Table Browser Tree

### Focus PlayPen on Double-click

Double-clicking a table in the **table browser tree** (left panel) immediately scrolls and focuses the PlayPen canvas to that table, selecting it so it is visible.

- If the table is not on the canvas (e.g. it belongs to a source database that was never placed), the double-click has no effect.

---

## Canvas Navigation

### Zoom

Hold **Ctrl** and scroll the **mouse wheel** to zoom in and out on the canvas.

---

## Supported Column Types (PostgreSQL)

The PostgreSQL DDL generator includes the following types as built-in defaults, available without a live database connection.

### JSON Types

| Type | Description |
|------|-------------|
| `JSON` | Stores JSON text with validation |
| `JSONB` | Binary JSON — indexed, faster queries |
| `JSONPATH` | JSON path expression (PostgreSQL 12+) |

### PostGIS Geometry Types

| Type | Description |
|------|-------------|
| `GEOMETRY` | Generic geometry (base PostGIS type) |
| `GEOGRAPHY` | Geodetic (spheroid-based) geometry |
| `POINT` | Single 2D point |
| `LINESTRING` | Sequence of points forming a line |
| `POLYGON` | Closed ring(s) |
| `MULTIPOINT` | Collection of points |
| `MULTILINESTRING` | Collection of line strings |
| `MULTIPOLYGON` | Collection of polygons |
| `GEOMETRYCOLLECTION` | Heterogeneous collection of geometries |
| `BOX2D` | 2D bounding box |
| `BOX3D` | 3D bounding box |
| `RASTER` | Raster data (PostGIS Raster extension) |

All JSON and PostGIS types have no precision or scale — they are emitted as bare type names in generated DDL (e.g., `col JSONB`, `geom GEOMETRY`).

---

## Table Visual Appearance

### Layout & Spacing

| Constant | Value | Effect |
|----------|-------|--------|
| `TITLE_PADDING` | 10 px | Extra vertical padding (top + bottom) inside the header area |
| `ROW_GAP` | 4 px | Extra vertical space added below each column row |
| `BOTTOM_PADDING` | 8 px | Empty space below the last column row |

### Header

- The table name is rendered in **bold**.
- The header has a **black border** that is part of the unified outer table border — no separate box is drawn for the column area.
- A **separator line** is drawn at the exact bottom of the header, flush with the outer border.

### PK Separator Line

The dashed line that divides primary-key columns from non-PK columns is inset by `BOX_LINE_THICKNESS` on both sides so it sits cleanly inside the outer border.

### Font Rendering

Rendering hints are applied per paint call and adapt to the current zoom level:

| Zoom | Text anti-aliasing | Fractional metrics |
|------|--------------------|--------------------|
| ≥ 75 % | LCD sub-pixel (`LCD_HRGB`) — crisp on standard LCD screens | On |
| < 75 % | Grayscale AA — avoids sub-pixel fringing on small glyphs | Off |

Shape anti-aliasing (`ANTIALIAS_ON`) and quality rendering (`RENDER_QUALITY`) are always enabled.

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
