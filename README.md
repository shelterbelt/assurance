# Assurance

*A cross-platform application to analyze and synchronize the contents of file system directories.*

> **Year Implemented: 2015 (V2: 2026)** 
The motivation for Assurance was to gain greater familiarity with many of the popular technologies in the Java ecosystem at the time.
> 
> The first version of Assurance shipped in 2015 as a pure Java application with an AWT/Swing UI. The second version of the application re-imagines Assurance with modern desktop UI technolgies. Assurance 2.0 keeps the Java/Spring/Hibernate engine while adding an Electron + React desktop shell that talks to the engine over WebSocket JSON-RPC. The Swing UI remains bundled with the application and can be used when the engine is started directly without `--headless`.

## What Assurance is

At its core, Assurance compares directories and can synchronize them according to scan definitions you configure. The **2.0** codebase is split:

| Area | Location | Role |
|------|----------|------|
| Engine | [`engine/`](engine/) | Java application: Spring, Hibernate, H2, file comparison and merge logic. In production it runs **headless** and exposes JSON-RPC over WebSockets. |
| Desktop UI | [`ui/`](ui/) | Electron (Electron Forge + Webpack), React, TypeScript. Spawns the engine as a child process and drives it via IPC. |

## Documentation

| Document | Audience |
|----------|----------|
| [**Developer getting started**](docs/dev-getting-started.md) | Prerequisites, one-time engine build, running the UI, H2 database layout, packaging (`package-engine` + `yarn make`), UI tests, troubleshooting, environment variables. |
| [**IPC contract**](docs/ipc-contract.md) | JSON-RPC methods and notifications between the UI and the engine—useful when changing either side. |
| [**AGENTS.md**](AGENTS.md) | Guidance for AI coding agents (Cursor, Claude Code, etc.) working in this repository. |

Maven resolves engine dependencies (Spring 6, Hibernate 6, JUnit, H2, Jackson, Apache Commons, and others) automatically; you do not need to install those libraries separately.

## Prerequisites (2.0)

For day-to-day development of the Electron app against a locally built engine:

- **JDK 17+** (Temurin or another distribution with `jlink` for production packaging)
- **Maven 3.8+**
- **Node.js 18+** and **Yarn Classic (1.x)**

Some **legacy Maven profiles** in [`engine/pom.xml`](engine/pom.xml) still use **Apache Ant** and the Oracle **AppBundler** JAR to produce a macOS `.app` from `mvn package`; that path is separate from the **recommended** 2.0 flow in [Developer getting started](docs/dev-getting-started.md) (`package-engine` + Electron Forge).

## Quick start (from source)

Full detail, path variants, and smoke tests: [**Developer getting started**](docs/dev-getting-started.md).

**1. Build the engine once** (repeat after Java changes):

```bash
cd engine
mvn package -Pdevelopment -DskipTests
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency_libs
```

**2. Run the UI**:

```bash
cd ui
yarn install   # first time only
yarn start
```

**3. Run tests**:

```bash
# Java engine
cd engine
mvn test

# UI (Jest); see dev-getting-started for Playwright e2e
cd ui
yarn test
```

## License

Assurance is released under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

See [`LICENSE.txt`](LICENSE.txt) for the full license text.

## Acknowledgments

Assurance includes a modified version of the `com.ibatis.common.jdbc.ScriptRunner` class from the Apache iBATIS project (the lineage that became [MyBatis](https://mybatis.org/)).

## IDE

Use any current Java IDE (IntelliJ IDEA, VS Code with Java extensions, Cursor, etc.) plus a TypeScript-aware editor for `ui/`. Legacy Spring Tool Suite project metadata may still exist under `engine/` for historical compatibility.

## Disclaimers

Assurance has been validated most thoroughly on macOS. Windows and Linux builds are supported in 2.0 via Electron Forge, but real-world compatibility has only been minimally varified.

**AI Acknowledgement:** The original version 1.x implementation was completely coded by hand. AI tooling and agents where used extensively to implement version 2.x of this project.

*Copyright © 2015–2026 [Mark Johnson](https://www.markallenjohnson.com)*
