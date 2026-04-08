# Upgrade to JDK 21

Migration assessment for moving Power*Architect from JDK 11 to Java 21.

---

## High Risk (likely to break compilation/runtime)

| Issue | Details |
|-------|---------|
| **Ancient dependencies** | 13+ JARs from 2002–2012 (Kettle, Mondrian, jFreeChart, Velocity, etc.) — many use internal JDK APIs removed in Java 9+ |
| **Missing `serialVersionUID`** | 10 `Serializable` classes lack version IDs — deserialization warnings/failures |
| **Reflection (macOS adapter)** | `src/main/java/ca/sqlpower/architect/swingui/ArchitectSwingSessionImpl.java:546,552` uses `getDeclaredMethod` — stricter module encapsulation in Java 21 may break this |

## Medium Risk

| Issue | Details |
|-------|---------|
| **No explicit compiler version** | `build.xml` has no `source`/`target` flags — defaults to whatever JDK is running |
| **JUnit 3 test style** | `regress/` uses `junit.framework.TestCase` — old but still works |
| **Legacy `Vector` usage** | 5 files — deprecated in favor of `ArrayList`, but still compiles fine |

## No Issues Found

- No `SecurityManager` calls
- No `Thread.stop()`/`suspend()`/`resume()`
- No `sun.*` / `com.sun.*` imports
- No JAXB usage
- No `Applet` usage

---

## Required Changes

1. **Update `build.xml`** — Add explicit `-source 21 -target 21` (or compatible version) to all `<javac>` tasks
2. **Upgrade/replace `lib/` dependencies** — All ancient JARs need modern equivalents (largest effort)
3. **Add `serialVersionUID`** — To all 10 `Serializable` classes
4. **Fix reflection calls** — Add proper module-open declarations or use Java 21-compatible reflection in `ArchitectSwingSessionImpl.java`
5. **Update test framework** — Migrate from JUnit 3 to JUnit 4/5 (optional but recommended)

## Affected Libraries (lib/)

| JAR | Approx. Release | Risk |
|-----|----------------|------|
| `commons-io-1.4.jar` | 2008 | Medium |
| `commons-vfs-1.0.jar` | 2007 | Medium |
| `edtftpj-1.5.4.jar` | ~2007 | Medium |
| `jcommon-1.0.0.jar` | 2003 | High |
| `jfreechart-1.0.1.jar` | 2006 | High |
| `jakarta-regexp-1.2.jar` | 2002 | High |
| `iText-2.0.8.jar` | 2009 | High |
| `kettle-core-3.2.2.jar` | 2009 | High |
| `kettle-db-3.2.2.jar` | 2009 | High |
| `kettle-engine-3.2.2.jar` | 2009 | High |
| `mondrian-3.14.0.0-12.jar` | 2012 | High |
| `velocity-1.6.2.jar` | 2010 | High |
| `velocity-tools-generic-1.4.jar` | 2009 | High |

## Recommended First Step

Before making any changes, run the build with JDK 21 using `--add-opens` to see what actually breaks vs. what compiles fine:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ant compile
```

Then use `jdeps` to scan for internal API usage in the existing JARs:

```bash
jdeps --jdk-internals -cp lib/*.jar staging/architect.jar
```
