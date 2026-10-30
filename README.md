# Java Memory Profiling Lab

A small Java 17 lab for learning heap, allocation, garbage collection (GC), retention, JFR, and Java Mission Control (JMC). Every example is a separate, continuously running class. Stop each with **Ctrl+C**. The workloads are deliberately artificial; do not run them in production.

## Concepts

- **Heap** stores objects. Used heap can rise as objects are allocated and drop after GC; committed heap and maximum heap are different measurements.
- **Allocation** is creation of objects. High allocation can create GC pressure even if objects do not remain live.
- **GC** reclaims memory from objects no longer reachable. GC frequency and pauses depend on JVM, heap size, collector, and workload.
- **Retention** means objects remain reachable. Long-lived reachable objects are not automatically leaks; a leak is unwanted retention.
- JFR records JVM events. JMC opens and analyzes `.jfr` recordings. JFR allocation events are sampled/estimated, not a complete exact per-object accounting.

## What is Java Flight Recorder (JFR), and why use it?

**Java Flight Recorder (JFR)** is a low-overhead event recorder built into the JDK (including JDK 17). It captures diagnostic information from a *running* Java application, such as sampled object allocations, garbage collections and pauses, CPU-related events, and thread activity. It is useful when an application is slow, allocates too much memory, or pauses unexpectedly: rather than guessing which code is responsible, you can inspect recorded evidence.

**JFR vs JMC:** JFR collects events and writes a `.jfr` recording; **Java Mission Control (JMC)** is the separate graphical tool used to open and analyze that file. You do **not** install JFR separately when using a full JDK 17, but you **do** install JMC separately. The `jcmd` utility starts and stops JFR for an already-running Java process.

### How JFR fits into this lab

1. **Run the baseline:** start `AllocationPressure`. Its `createTemporaryObjects()` method repeatedly allocates short-lived buffers. This can increase allocation rate and GC work; high allocation alone is **not** a memory leak.
2. **Record while it runs:** find the process ID with `jcmd -l`, start JFR with `jcmd <PID> JFR.start name=lab settings=profile`, wait about 60 seconds, and stop it with `jcmd <PID> JFR.stop name=lab filename=allocation.jfr`.
3. **Inspect the evidence in JMC:** open `allocation.jfr` (File > Open File). In Memory/allocation views, inspect sampled allocation stack traces and look for `AllocationPressure.createTemporaryObjects` and `byte[]`. In Garbage Collections views, inspect GC events, pause durations, and heap behavior. Exact view names depend on the JMC version.
4. **Compare an improvement:** run `AllocationOptimized` with the same heap settings and a similar recording duration, save `optimized.jfr`, and compare allocation activity and GC behavior. The point is to test whether the change helped under comparable conditions, not to assume every run will show identical numbers.
5. **Investigate retained memory separately:** `RetainedObjects` keeps arrays reachable in a static list. JFR helps show *where allocations happen*, but it does not by itself prove *why objects remain reachable*. For that question, take a heap dump and inspect GC roots with a tool such as Eclipse MAT; compare with `BoundedRetention`.

**What to expect:** you may see sampled allocation hotspots, GC events, pause times, and heap trends. JFR allocation events are sampled rather than an exact list of every object. A sawtooth heap graph is often normal; a leak requires evidence of unwanted retention. The exercises below provide the complete commands.

## Requirements and setup (Windows)

1. Install a **JDK 17** distribution if needed. JFR, `javac`, and `jcmd` are part of a full JDK. No Maven, Spring, database, or external library is needed.
2. Open Command Prompt and run `java -version`, `javac -version`, and `jcmd -l`. If a command is missing, install a JDK and add its `bin` directory to PATH; open a new terminal.
3. Install **JDK Mission Control** separately from a trusted JMC distribution compatible with your system. Launch it; UI names can vary by release.
4. Clone this repository and open Command Prompt in its folder. Compile:

```bat
mkdir out
javac -d out src\*.java
```

## Exercise 1: allocation and GC

1. Start the allocation-heavy example in one terminal:

```bat
java -Xms128m -Xmx256m -cp out AllocationPressure
```

2. In a second terminal run `jcmd -l`. Find the PID for `AllocationPressure`. Replace `<PID>` below with that number:

```bat
jcmd <PID> JFR.start name=lab settings=profile
```

3. Wait about 60 seconds while the application continues running. Then save and stop the recording:

```bat
jcmd <PID> JFR.stop name=lab filename=allocation.jfr
```

4. Open `allocation.jfr` in JMC (File > Open File). In the recording's navigation tree, look under **Memory** for allocation views, and under **Garbage Collections** for GC activity and pauses. Select an allocation view, group or inspect by allocation stack trace, and find `AllocationPressure.createTemporaryObjects`. Expand the stack and inspect allocated classes, especially `byte[]`. Names and locations of views vary across JMC versions. Profile settings provide allocation sampling; increase recording duration if no sample appears.
5. Inspect heap usage over time and GC events. A sawtooth graph can be normal for short-lived objects; do not infer a leak merely from rising heap between collections.
6. Stop the application with Ctrl+C. Repeat with `AllocationOptimized`, the same heap settings, recording duration and conditions; use `filename=optimized.jfr`. Compare allocation samples/rate and GC events. The examples demonstrate reduced buffer allocation; they are illustrative workloads, not equivalent business benchmarks. Results vary.

```bat
java -Xms128m -Xmx256m -cp out AllocationOptimized
```

## Exercise 2: retained heap and a bound

Run `java -Xms128m -Xmx256m -cp out RetainedObjects` and record it as above. `retainData` slowly adds arrays to a static list and stops at 96 MiB; the program stays alive. Observe heap usage and GC, including after GC where available. JFR can help identify allocation sites, but allocation alone does not prove which references retain objects.

After the count reaches 96, take a heap dump (it may pause the app and contain sensitive data):

```bat
jcmd <PID> GC.heap_dump retained.hprof
```

A heap dump needs a separate analyzer such as Eclipse Memory Analyzer (MAT); install it only for this exercise. Open the dump, inspect the largest `byte[]` objects and use paths to GC roots / dominator tree to find the static `RetainedObjects.CACHE` list. Dumping may trigger GC; the heap snapshot is not the same as JFR's allocation history. Delete recordings and dumps after use, and do not commit them.

Run `java -Xms128m -Xmx256m -cp out BoundedRetention` and record again. Its queue retains at most ten arrays, evicting older entries; observe the stabilized retention and continued allocations. A bounded cache is a design choice: actual eviction policy must match application requirements.

## Interview review

- **Heap vs stack?** Heap holds objects; each thread has a stack of frames and local variables. Exact object placement is a JVM implementation detail.
- **What causes an OutOfMemoryError?** Examples include Java heap exhaustion, metaspace exhaustion, or inability to create native threads; identify the specific message and evidence.
- **Leak vs high allocation?** A leak retains unwanted reachable objects; high allocation may involve short-lived objects. They can coexist.
- **How investigate?** Establish baseline and symptoms, inspect GC/heap and allocation with JFR/JMC, use a heap dump for retained objects and root paths, fix the cause, and compare under comparable load.
- **Does GC free every unused object immediately?** No. Eligibility and collection timing differ; GC does not reclaim objects that remain reachable.
- **JMM vs memory management?** The Java Memory Model defines inter-thread visibility and ordering; heap and GC management concern storage and reclamation. They are related but distinct.

## Troubleshooting

If `jcmd` cannot attach, run the app and command as the same user and use the JDK matching the target JVM. Verify PID with `jcmd -l`. If JMC shows few allocation samples, use `settings=profile` and record longer while the workload is active. GC and heap graphs differ by JVM and timing. JFR recordings and heap dumps may contain sensitive application data.
