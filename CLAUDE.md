# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Local Dev Notes

This file is local-only.  Never commit it or the `.claude/` folder to source control.

## Shell

Shell is PowerShell (Windows), not bash.  Use PowerShell syntax for any commands run outside the sandboxed Bash tool.

## JDK

`C:/Users/ivan/.jdks/temurin-21.0.5` (Java 21, matches `java.version` in `pom.xml`).

## Comments and Javadocs

No em dashes.  Use 2 spaces after periods.  Be concise and brief.  Only write Javadocs/comments when necessary.

## Java style

Prefer `getFirst()` over `get(0)` on `List`.

---

# Repository Layout

Two independent Maven projects plus a submodule:

- `jpo-conflictmonitor/` - the main real-time Kafka Streams application (Spring Boot).  This is where nearly all work happens.
- `jpo-conflictmonitor-batch-processing/` - a separate, independently deployed Spring Boot app for pull-based analyses (ATSPM controller event log vs. SPaT comparison).  Talks to MongoDB and an ATSPM HTTP API; does not participate in the Kafka topology.  See its own README.
- `jpo-utils/` - git submodule providing base infra (Kafka, Kafka Connect, MongoDB) via docker compose.
- `test-message-sender/` - separate Maven project for injecting test messages.

There is no aggregator POM.  Build each project from its own directory.

# Build and Test

A GitHub personal access token with `read:packages` is required to resolve the `jpo-ode` and `jpo-geojsonconverter` artifacts from GitHub Packages.  Copy `settings.xml` to `~/.m2/settings.xml` and fill in the token, or pass `-s ../settings.xml` with the `MAVEN_GITHUB_TOKEN_NAME` / `MAVEN_GITHUB_TOKEN` / `MAVEN_GITHUB_ORG` env vars set (this is what CI does).

```powershell
# All commands run from the jpo-conflictmonitor/ subdirectory
mvn clean package            # build + test, produces the shaded jar in target/
mvn --batch-mode test        # unit tests only (what CI runs)
mvn test -Dtest=SpatValidationTopologyTest              # single test class
mvn test -Dtest=SpatValidationTopologyTest#testMethod   # single test method
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar   # coverage + Sonar
```

The batch project builds the same way from `jpo-conflictmonitor-batch-processing/` (note its `settings.xml` is in its own directory, not the parent).

Running locally outside Docker: `java -jar target/jpo-conflictmonitor-<version>.jar` (see `run.sh`, whose hardcoded version is stale).  The app serves HTTP on port 8082 and exposes JMX on 10090.

Docker: `docker-compose up --build -d` from the repo root.  Requires `.env` (copy from `sample.env`) both here and in `jpo-utils/`.  Compose services are gated by profiles (`cm_base`, `cm_build`, `cm_release`, `cm_full`, `cm_batch`, `all`) documented in `sample.env`.

# Architecture

## The pluggable-algorithm pattern

This is the single most important structural idea in the codebase, and it is why there are so many small files.  Read `docs/ModularArchitecture.md` first.

Every analysis is a named **algorithm** selected at runtime by a Spring
`ServiceLocatorFactoryBean`.  For a feature `foo`, the package
`monitor/algorithms/foo/` typically contains:

| File | Role |
|---|---|
| `FooAlgorithm.java` | interface, extends `Algorithm<FooParameters>` (startable) or `ConfigurableAlgorithm<FooParameters>` (sub-algorithm, cannot start/stop on its own) |
| `FooStreamsAlgorithm.java` | streams-specific sub-interface declaring the `buildTopology(...)` signature |
| `FooAlgorithmFactory.java` | `getAlgorithm(String name)` locator interface, implemented by Spring |
| `FooAlgorithms.java` | `@Configuration` that registers the `ServiceLocatorFactoryBean` for the factory |
| `FooParameters.java` | `@ConfigurationProperties(prefix = "foo")` `@Data` bean, one field per tunable |
| `FooConstants.java` | the `String` bean names, e.g. `DEFAULT_FOO_ALGORITHM = "defaultFooAlgorithm"` |

The implementation lives in `monitor/topologies/FooTopology.java` as a
`@Component("defaultFooAlgorithm")` implementing `FooStreamsAlgorithm` and
`BaseStreamsTopology<FooParameters>`.  `application.yaml` picks the
implementation with `foo.algorithm: defaultFooAlgorithm`, so adding an
alternative implementation means adding a new `@Component` with a new bean name
and flipping that one property.  (Live example: `spat.validation.algorithm` is
currently set to `cti4501V2SpatValidationAlgorithm` rather than the default.)

Sub-algorithms plug into larger algorithms via getter/setter pairs on the parent
interface and a `buildTopology(StreamsBuilder, KStream...)` call at the right
point in the parent's topology, rather than by writing to an intermediate Kafka
topic.

## Wiring

`MonitorServiceController` is one ~900-line class whose constructor wires everything: for each topology it resolves the factory, looks up the configured algorithm name, injects parameters, registers config listeners, sets streams properties, installs a `StateChangeHandler` and `StreamsExceptionHandler`, adds a shutdown hook, and starts it.  Adding a new top-level topology means adding a block here.  It is `@Profile("!test && !testConfig")` so tests do not start real streams.

`ConflictMonitorProperties` is the `@ConfigurationProperties` bean holding every topic name, every algorithm-name property, and the injected factories; `createStreamProperties(name)` builds per-topology `StreamsConfig` (thread counts per topology come from the `streams.config.topologies` list in `application.yaml`).

## Runtime configuration

Parameters are not static.  Fields on `*Parameters` classes annotated with
`@ConfigData(key=..., description=..., units=..., updateType=...)` are exposed as
live-updatable config.  `ConfigTopology` maintains Kafka-backed default and
per-intersection config tables (`config.defaultTableName`, `customTableName`,
`mergedTableName`, `intersectionTableName`) and pushes updates to registered
listeners.  `ConfigController` exposes these over REST at `/config/**`
(`GET|POST /default/{key}`, `GET|POST|DELETE /intersection/{region}/{intersectionId}/{key}`).

`updateType` on `@ConfigData` matters: `READ_ONLY` (topic names, store names),
`DEFAULT` (global only), `INTERSECTION` (overridable per intersection).  A
parameter that is `INTERSECTION`-updatable needs a `ConfigMap<T> fooMap` field
plus a `getFoo(IntersectionRegion)` accessor delegating to
`ConfigUtil.getIntersectionValue(...)`, as in `LaneDirectionOfTravelParameters`.

## Data model vocabulary

- **Event** (`models/events/`) - a single detected condition at a point in time.
- **EventAggregation** (`models/events/*EventAggregation.java`) - the same event rolled up over a time window to reduce volume.  Produced by `topologies/aggregation/*AggregationTopology` extending `BaseAggregationTopology`, driven by the `aggregation.eventTopicMap` / `aggregation.eventAlgorithmMap` in `application.yaml`.  State store names are derived by convention (`<eventName>EventStore`, `<eventName>KeyStore`).
- **Notification** (`models/notifications/`) - an operator-facing alert derived from events; `NotificationTopology` deduplicates them into a KTable.
- **Assessment** (`models/assessments/`) - a periodic statistical rollup (e.g. `StopLineStopAssessment`), built from an `*Aggregator`.

Kafka topic names all follow `topic.Cm<Thing>` for outputs and come from upstream (`topic.ProcessedSpat`, `topic.ProcessedMap`, `topic.OdeBsmJson`) for inputs.  All are defined in `application.yaml`.

## Other pieces

- `MessageIngestTopology` builds the shared MAP/SPaT/BSM state stores that `IntersectionEventTopology` joins against; most per-vehicle analyses hang off `IntersectionEventTopology`.
- `monitor/processors/` holds low-level `Processor` implementations used where the DSL is insufficient (punctuators, sequence tracking, timestamp deltas).
- `monitor/serialization/JsonSerdes` is the central registry of Kafka serdes; new message types need an entry here.
- `AppHealthMonitor` and `health/TopologyGraph` expose topology state; `SystemConfig`/`SystemConfigMBean` expose JMX controls.
- `BoundedMemoryRocksDBConfig` caps RocksDB off-heap usage, configured via `ROCKSDB_*` env vars.

# Testing

Tests use `TopologyTestDriver` (not embedded Kafka) for topology tests: construct the topology directly, set parameters as plain objects, pipe records into input topics, assert on output topics.  Helpers live in `src/test/java/us/dot/its/jpo/conflictmonitor/testutils/` (`TopologyTestUtils`, `SpatTestUtils`, `BsmTestUtils`, `ConfigTestUtils`, `ResourceUtils`).  Both JUnit 4 (`org.junit.Test`) and JUnit 5 appear in the suite; match whatever the neighboring tests use.  Spring context tests use the `test` / `testConfig` profiles to keep `MonitorServiceController` from starting.
