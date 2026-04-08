# Power*Architect

Power\*Architect is an open-source data modeling and database design tool built with Java Swing. It supports forward and reverse engineering of databases, visual ERD editing, and generating DDL scripts for multiple database platforms.

---

## Table of Contents

- [Requirements](#requirements)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
- [Build Configuration](#build-configuration)
- [Building the Project](#building-the-project)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Debugging](#debugging)
- [Generating Release Binaries](#generating-release-binaries)
- [Code Quality](#code-quality)

---

## Requirements

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 11 | OpenJDK 11 recommended |
| Apache Ant | 1.7+ | Build tool (the build verifies this at startup) |
| Git | Any | Required to auto-clone `sqlpower-library` |

> **Note:** The build path must not contain spaces. The XSLT-based help file generation will fail if the path includes spaces.

---

## Project Structure

```
power-architect/
├── src/main/java/        # Application source code
├── src/main/resources/   # Images, sounds, configs, and other resources
├── regress/              # Unit and regression tests
├── example_code/         # API usage examples
├── lib/                  # Runtime dependency JARs (bundled with distributions)
├── buildlib/             # Build/test-time JARs (not distributed)
├── jdbc_drivers/         # JDBC drivers for testing
├── ext-tools-home/       # External tools: izpack, launch4j, findbugs, pmd, clover
├── doc/                  # Documentation source and XSLT tools
├── installer/            # Windows installer files
├── build.xml             # Main Ant build file
└── build.properties      # Local build overrides (create from build.properties.example)
```

The project depends on **sqlpower-library**, a sibling project that is automatically cloned from GitHub during the first build (see [Local Setup](#local-setup)).

---

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/casivaagustin/power-architect.git
cd power-architect
```

### 2. sqlpower-library dependency

The build system will automatically clone `sqlpower-library` from GitHub into `../sqlpower-library` (a sibling directory) the first time you run any Ant target. No manual action is required.

If you want to clone it manually or use a different location:

```bash
# Manual clone (into the default sibling location)
git clone https://github.com/casivaagustin/sqlpower-library ../sqlpower-library

# Or specify a custom path at build time
ant -Dsqlpower.library.home=/path/to/sqlpower-library <target>
```

### 3. Configure the build (optional)

Copy the example properties file and edit as needed:

```bash
cp build.properties.example build.properties
```

Key properties you can override in `build.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `sqlpower.library.home` | `../sqlpower-library` | Path to the sqlpower-library project |
| `only.copy.sqlpower.library` | `false` | Set to `true` to skip rebuilding the library and only copy its JARs |
| `findbugs.report.format` | — | Output format for FindBugs report (e.g. `html`) |
| `pmd.report.format` | — | Output format for PMD report (e.g. `html`) |
| `clover.report.format` | — | Output format for Clover coverage report (e.g. `html`) |
| `sqlpower.certificate` | — | Absolute path to a self-signed certificate for signing distributions |

### 4. JDBC drivers (for database testing)

Place any JDBC driver JARs you want to test against in the `jdbc_drivers/` directory. These are included in the test classpath automatically.

---

## Build Configuration

The build system resolves the application version automatically by compiling and running `ArchitectVersionTask`. The resulting version string follows the pattern `major.minor.tiny[-suffix]` and is used to name distribution artifacts (e.g. `dist/architect-1.0.0/`).

All compiled output lands in:

| Directory | Contents |
|-----------|----------|
| `build/` | Compiled application classes and resources |
| `build_tests/` | Compiled test classes |
| `build_examples/` | Compiled example code |
| `generated/` | Java files produced by annotation processing |
| `dist/` | Distribution packages and release artifacts |
| `staging/` | Intermediate staging area for OS-specific packaging |

---

## Building the Project

All commands are run from the root of the repository.

### Compile only

```bash
ant compile
```

This will:
1. Auto-clone `sqlpower-library` from GitHub if not already present
2. Build `sqlpower-library` (unless `only.copy.sqlpower.library=true`)
3. Copy library JARs into `buildlib/`
4. Run annotation processing
5. Compile all application sources into `build/`

### Build a runnable JAR

```bash
ant jar
```

Produces `staging/architect.jar` — a self-contained JAR with a `Main-Class` manifest entry pointing to `ca.sqlpower.architect.swingui.ArchitectFrame`.

### Clean build artifacts

```bash
# Remove compiled classes and staging output
ant clean

# Remove everything including dist/ directories
ant dist-clean
```

---

## Running the Application

### Via Ant

```bash
ant run
```

Launches the Swing UI (`ArchitectFrame`) with `log4j` logging configured.

### Via Java directly (after building)

```bash
java -jar staging/architect.jar
```

For large projects with many tables, increase the heap size:

```bash
java -Xmx600m -jar staging/architect.jar
```

---

## Running Tests

### Run the full JUnit test suite

```bash
ant junit
```

Test results are written to `dist/architect-<version>/reports/`.

### Run tests with code coverage (Clover)

```bash
ant clover.test.and.report
```

Generates a Clover HTML report in the dist reports directory.

### Run all checks (tests + static analysis)

```bash
ant alltests
```

Runs `junit`, `pmd`, and `findbugs` in sequence.

---

## Debugging

### From the command line

Build the project and launch with remote debugging enabled:

```bash
ant compile

java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -jar staging/architect.jar
```

Then attach your IDE's remote debugger to `localhost:5005`.

### From Eclipse

The project includes `.classpath` and `.project` files for Eclipse. Import it as an existing project:

1. **File → Import → Existing Projects into Workspace**
2. Select the `power-architect` root directory
3. Make sure `sqlpower-library` is also imported as a sibling project (or already built so its JARs are available in `buildlib/`)
4. Run `ArchitectFrame.java` directly as a Java application

### From IntelliJ IDEA

1. **File → Open** and select the `power-architect` directory
2. Mark `src/main/java` as Sources Root and `regress/` as Test Sources Root
3. Add all JARs from `lib/`, `buildlib/`, and `../sqlpower-library/lib/main/` to the module dependencies
4. Create a Run Configuration with main class `ca.sqlpower.architect.swingui.ArchitectFrame`

---

## Generating Release Binaries

### All platforms (default target)

```bash
ant dist
```

Builds distributions for all supported platforms: Windows (installer + EXE), macOS (DMG), and generic Unix (TAR.GZ). Output lands in `dist/architect-<version>/`.

### Platform-specific targets

| Target | Output |
|--------|--------|
| `ant windows_exe_installer` | Windows EXE installer (via launch4j + izpack) |
| `ant windows_jdbc_exe_installer` | Windows EXE installer with bundled JDBC drivers |
| `ant osx_dmg` | macOS DMG disk image |
| `ant osx_jdbc_dmg` | macOS DMG with bundled JDBC drivers |
| `ant generic_install` | Generic Unix TAR.GZ |
| `ant generic_jdbc_install` | Generic Unix TAR.GZ with bundled JDBC drivers |
| `ant source_tgz` | Source code TAR.GZ |

### Custom JRE bundling

The build can embed a trimmed custom JRE (via `jlink`) into platform distributions:

```bash
ant mac_jre    # macOS
ant win_jre    # Windows
ant unix_jre   # Linux/Unix
```

Custom JREs are placed in `custom_jre/mac/`, `custom_jre/win/`, and `custom_jre/unix/` respectively and picked up automatically by the distribution targets.

### WebStart distribution

```bash
ant webstart_dist
```

---

## Code Quality

### Static analysis

```bash
# PMD (code style and bug patterns)
ant pmd

# PMD Copy/Paste Detector
ant pmd-cpd

# FindBugs
ant findbugs
```

Reports are written to `dist/architect-<version>/reports/`.

### API documentation

```bash
# Standard Javadoc
ant javadoc

# Javadoc with UML class diagrams (requires UmlGraph)
ant javadoc.with.umlgraph
```

### User guide

```bash
ant userguide   # Generates HTML + PDF user guide
ant html        # HTML only
ant pdf         # PDF only
```
