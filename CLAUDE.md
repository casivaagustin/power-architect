# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Power*Architect is a Java Swing desktop application for data modeling, ERD editing, and DDL generation. This is **a fork** of [SQLPower/power-architect](https://github.com/SQLPower/power-architect) with custom workflow improvements documented in `FORK.md` (keyboard shortcuts, default table columns, auto FK naming, PostgreSQL type extensions, etc.). Read `FORK.md` before changing UX behavior — many seemingly arbitrary defaults are intentional fork-specific decisions.

## Toolchain

- JDK **11** (pinned via `.sdkmanrc` to `11.0.23-tem`) and Apache Ant **1.7+**. Run `sdk env` in the repo root to activate the pinned JDK.
- The build path **must not contain spaces** — the XSLT help generation writes `%20`-encoded paths and never decodes them back.
- A JDK 21 migration assessment exists in `upgrade-to-jdk-21.md`; the repo still targets JDK 11.

## Common commands

```bash
ant compile        # auto-clones sqlpower-library if needed, runs annotation processing, compiles to build/
ant stage          # produces staging/architect.jar with runtime libs in staging/lib/ — runnable
ant run            # launches ArchitectFrame Swing UI with log4j configured
ant jar            # produces dist/architect.jar (manifest references ../lib relative — not directly runnable from dist/)
ant junit          # runs the four registered test suites; HTML report under dist/architect-<version>/reports/junit/
ant alltests       # junit + pmd + findbugs
ant clean          # remove build/ and staging/
ant dist-clean     # also remove dist/
```

Running a single test: the `junit` target invokes four hard-coded suite classes (see `build.xml:451-454`) — `ArchitectBusinessTestSuite`, `ArchitectSwingTestSuite`, `ArchitectAutoTests`, `ProfileTests`. To run one test class, compile with `ant compile-tests` then invoke JUnit directly, e.g.:

```bash
java -cp "build_tests:build:buildlib/*:lib/*" \
     -Dca.sqlpower.headless=true \
     junit.textui.TestRunner ca.sqlpower.architect.ddl.TestPostgresDDLGenerator
```

Tests use **JUnit 3** (`junit.framework.TestCase`) — new tests should follow the same style for suite compatibility. Headless tests require `-Dca.sqlpower.headless=true`.

## The sqlpower-library dependency

Power*Architect depends on a sibling project `sqlpower-library` (provides the `SPObject` framework, JDBC pooling, `SPSwingWorker`, etc.). The Ant `init` target auto-clones it to `../sqlpower-library` from GitHub on first build and compiles it into `buildlib/`. Override with `-Dsqlpower.library.home=/path/to/lib`. Set `only.copy.sqlpower.library=true` in `build.properties` to skip rebuilding the library JARs every compile (much faster iteration).

When `SQLObject`, `SQLTable`, `SQLColumn`, `SQLRelationship`, `SPObject`, or persistence annotations behave unexpectedly, the source lives in `../sqlpower-library/`, not in this repo.

## Annotation-processor build step

`ant compile` does a **three-pass javac**:
1. Compile sources to `build/`
2. Run `ca.sqlpower.object.annotation.SPAnnotationProcessor` (from sqlpower-library) over the same sources with `-proc:only`, emitting `*Persister.java` and `*PersisterHelper.java` files into `generated/`
3. Compile the generated sources into `build/`

Classes annotated with `@Persistable`, `@Constructor`, `@ConstructorParameter`, `@Accessor`, `@Mutator`, `@Transient` participate in this. The persistence framework (used for the .architect file format and enterprise server sync) drives the constructor/property contract — see `ArchitectSwingProject.java` for a canonical example. If you add a persistable property, both an `@Accessor`-annotated getter and `@Mutator`-annotated setter are required, otherwise round-tripping through save/load silently drops the value.

If compilation succeeds but generated classes go stale, `ant clean compile` regenerates them.

## Architecture

Two-layer split:

**Core layer — `ca.sqlpower.architect.*`** (`src/main/java/ca/sqlpower/architect/`)
- `ArchitectSessionContext` / `ArchitectSession` / `ArchitectProject` — headless data-model hierarchy. A *Context* owns multiple *Sessions*; each Session owns one *Project* containing the `SQLObjectRoot` (databases → schemas → tables → columns).
- `ddl/` — DDL generator hierarchy: `DDLGenerator` interface, `GenericDDLGenerator` base, per-database subclasses (`PostgresDDLGenerator`, `OracleDDLGenerator`, `MySqlDDLGenerator`, `SQLServer2005DDLGenerator`, `LiquibaseDDLGenerator`, etc.). Each generator owns a `TypeMap` that translates JDBC types to its native SQL syntax. The fork's PostgreSQL JSON/PostGIS types are registered in `PostgresDDLGenerator`.
- `diff/` — model comparison (CompareDM feature).
- `profile/` — column profiling against live databases.
- `layout/` — auto-layout algorithms for diagrams.
- `enterprise/` — client code for the SQL Power Enterprise server (multi-user collaboration).

**Swing UI layer — `ca.sqlpower.architect.swingui.*`** (same path under `swingui/`)
- `ArchitectFrame` — main `JFrame` and `public static void main(...)` entry point.
- `ArchitectSwingSession` / `ArchitectSwingSessionImpl` — Swing extension of `ArchitectSession`, owns the `PlayPen` and frame.
- `ArchitectSwingProject` — Swing extension of `ArchitectProject`, additionally persists `PlayPenContentPane` (visual layout).
- `PlayPen` — the canvas `JPanel`. Hosts `TablePane` and `Relationship` components, handles selection/drag/zoom. Drag-and-drop, keyboard handling, and undo all wire through this class.
- `TablePane` / `BasicTablePaneUI` — table rendering (header, columns, PK separator). The fork's custom layout constants (`TITLE_PADDING`, `ROW_GAP`, etc.) and per-zoom font hints live in `BasicTablePaneUI`.
- `Relationship` / `BasicRelationshipUI` — FK relationship lines.
- `ColumnEditPanel`, table dialogs, DDL export panels, compare dialog — main editing UI.
- `action/` — `Action` subclasses for menu/toolbar/keyboard commands (e.g. `CreateRelationshipAction`).
- `dbtree/` — the left-panel source database tree.
- `critic/` — model-checking warnings shown to the user.

A typical interaction flow: user input → an `Action` in `swingui/action/` → mutates `SQLObject`s on the session's `Project` → core fires `SPObject` change events → `PlayPen` / `TablePane` listeners repaint. Never mutate `SQLObject` state from the EDT directly without going through the established Action/listener path — the annotation-generated persisters expect events to fire.

## Project file format

`.architect` files are XML serialized via the SPObject persistence framework. `ProjectLoader` reads them; saving is driven by the generated `*Persister` classes. Adding a new persistable field requires the `@Accessor`/`@Mutator` pair (see annotation section above) — there is no manual XML schema to edit.

## Tests layout

Tests live in `regress/` (mirrors `src/main/java/` package structure). The four entry suites in `regress/ca/sqlpower/` aggregate the rest. Compiled to `build_tests/`. `TestingArchitectSession` / `StubArchitectSession` provide headless session fixtures; `ArchitectValueMaker` generates synthetic SQLObject trees for property-round-trip tests.

## Distribution

`ant dist` builds Windows EXE installer (launch4j + izpack), macOS DMG, and Unix TAR.GZ. Platform-specific targets and JRE-bundling targets (`ant mac_jre`, `ant win_jre`, `ant unix_jre`) are documented in `README.md`. Native packaging tooling lives in `installer/`, `osx_packaging_utils/`, and `ext-tools-home/`.
