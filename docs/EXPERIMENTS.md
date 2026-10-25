# Experiments and Results

## Experiment 1 — Croissant Volume

Date: 2026-10-24

### Objective

Verify video generation from a 500 × 500 × 500 volume
and measure the projection algorithm's frame generation rate
for a fixed sequence of parallel trapezoidal slices.

### Environment

- CPU: Intel Core 7 240H (2.50 GHz).
- RAM: 24 GB.
- OS: Windows 11, 64-bit.
- Java: OpenJDK 26.0.1.
- IDE: IntelliJ IDEA 2026.1.3.
- Video library: JCodec 0.2.5.
- Execution: Main launched through IntelliJ IDEA.

### Input and Slice Sequence

- Volume: croissant.nrrd.
- Dimensions: 500 × 500 × 500.
- Voxel type: unsigned 8-bit integer.
- NRRD encoding: gzip.
- Source: file shared in the course group; original provenance
  is not verified.
- CSV: croissant_planes.csv, containing 300 rows without a header.

Each row defines an isosceles trapezoid using the point order
a1, a2, b, c:

- a1 = (150, 50, z)
- a2 = (350, 50, z)
- b  = (50, 450, z)
- c  = (450, 450, z)

The value of z increases from 100 to 399, inclusive.
All coordinates are voxel-index coordinates.

The generated projection matrix is 401 columns × 413 rows
before resizing and padding to the video dimensions.

### Procedure

1. Generate the CSV sequence.
2. Run Main once to check successful output and inspect the video.
3. Run the same configuration three additional times sequentially.
4. Record the reported frame generation rate for each run.

Each run starts a new Java process. The preliminary run is
a functional check, not a JVM warm-up for subsequent runs.

### Timing Scope

The reported rate is:

generated frames / accumulated FrameGenerator.generate time

It includes projection matrix allocation and sampling.
It excludes input loading, CSV parsing, geometry validation,
volume-wide maximum calculation, image resizing, video encoding,
and output finalization.

These results measure projection throughput for this workload,
not end-to-end video production throughput.

### Results

| Run | Frames | Skipped rows | Generation rate |
|---|---:|---:|---:|
| Preliminary check | 300 | 0 | 1148.30 FPS |
| Measurement 1 | 300 | 0 | 1146.62 FPS |
| Measurement 2 | 300 | 0 | 1202.26 FPS |
| Measurement 3 | 300 | 0 | 1180.64 FPS |

Arithmetic mean of the three measurement rates: **1176.51 FPS**.

Median rate: **1180.64 FPS**.

Range: **1146.62–1202.26 FPS**.

All three measured projection rates exceed both the 30 FPS
minimum and the 100 FPS target for this specific workload
on the tested machine.

### Output and Visual Inspection

- Output: croissant_demo.mp4.
- Codec: H.264.
- Resolution: 640 × 480.
- Frame count: 300.
- Playback rate: 30 FPS.
- Duration: 10 seconds.

The video properties were verified using ffprobe.
Sampled frames from the beginning, middle, and end show changes
in the layered structure as the slicing plane moves along Z.
The user also inspected the video and reported clear,
changing imagery.

Visual inspection complements the automated tests; it does
not establish numerical correctness for every output pixel.

### Reproduction

Create the CSV in PowerShell:

```powershell
100..399 | ForEach-Object {
    $z = $_
    "150,50,$z,350,50,$z,50,450,$z,450,450,$z"
} | Set-Content -LiteralPath "C:\OOP_Test\croissant_planes.csv" -Encoding ascii
```

Set Main's Program arguments in IntelliJ IDEA to:

```text
"C:/OOP_Test/croissant.nrrd" "C:/OOP_Test/croissant_planes.csv" "C:/OOP_Test/croissant_demo.mp4"
```

Adjust paths to match the local file locations.
Each run replaces the output video at the specified path.
Reproduction requires obtaining the same NRRD input file.

### Limitations

This experiment covers one volume and one sequence of
axis-aligned trapezoidal slices. It does not measure arbitrary
plane orientations, peak memory usage, or total execution time.

The three repeated runs provide a small observational sample;
they are not a controlled microbenchmark. Experiment 2 adds
a fixed warm-up procedure within a single Java process.

### Experiment Files

- [Slice coordinates (CSV)](../demo/croissant_planes.csv)
- [Demo video (MP4)](../demo/croissant_demo.mp4)

## Experiment 2 — Projection Benchmark with Warm-Up

Date: 2026-10-24

### Setup and Method

This benchmark uses the same machine, croissant volume,
and 300-slice CSV sequence described in Experiment 1.

ProjectionBenchmark loads the volume and parses and validates
the slices once. It then executes, within one Java process:

- 3 warm-up rounds: 900 frames, excluded from measurements.
- 5 measured rounds: 1,500 frames in total.

Each call to FrameGenerator.generate is timed using
System.nanoTime. Timed durations include frame allocation
and volume sampling. Input loading, geometry validation,
console output, and video encoding are excluded.

The latest generated frame is retained through a volatile
reference outside the timed region. Complete frame sequences
are not retained in memory.

### Results

| Round | Frames | Generation time (ms) | FPS |
|---|---:|---:|---:|
| 1 | 300 | 243.362 | 1232.73 |
| 2 | 300 | 240.944 | 1245.10 |
| 3 | 300 | 239.215 | 1254.10 |
| 4 | 300 | 243.564 | 1231.71 |
| 5 | 300 | 239.514 | 1252.54 |

- Arithmetic mean of round rates: **1243.24 FPS**.
- Median rate: **1245.10 FPS**.
- Combined throughput: **1243.16 FPS**.
- Range: **1231.71–1254.10 FPS**.
- Process exit code: 0.

Combined throughput is the total measured frame count divided
by the sum of measured generation durations.

### Interpretation and Limitations

All five measured rounds exceeded the 30 FPS minimum and
100 FPS target for this workload on the tested machine.

These measurements describe projection throughput after a
fixed warm-up procedure. They do not measure end-to-end video
production speed or establish performance for every slice
size and orientation.

The five rounds belong to one Java process and are not five
independent process launches. JVM activity, garbage collection,
and operating-system scheduling may affect the measurements.
The fixed warm-up does not guarantee full JIT stabilization.

### Reproduction

Run Exercise1_OOP.ProjectionBenchmark with these two arguments:

    "C:/OOP_Test/croissant.nrrd" "C:/OOP_Test/croissant_planes.csv"

Use normal execution, without the debugger.
No video is produced by this benchmark.

## Experiment 3 — Reproducible Synthetic Volume

Date: 2026-10-24

### Objective and Input

Provide an independently reproducible projection experiment
using a deterministic volume generated by the project.

- Generator: SyntheticVolumeGenerator.
- Volume: synthetic_500_v1.nrrd.
- Dimensions: 500 × 500 × 500.
- Type: unsigned 8-bit integer.
- Encoding: raw.
- Voxel payload: 125,000,000 bytes.
- Slice sequence: demo/croissant_planes.csv.

The CSV contains the same 300 parallel trapezoidal slices
used in Experiments 1 and 2. Its filename identifies its original
use; the coordinates also apply to the synthetic volume.

### Generation Rule

For integer coordinates x, y, z from 0 to 499:

    d2 = (x - 250)^2 + (y - 250)^2 + (z - 250)^2

Voxel values are assigned in this order:

- If d2 <= 80^2: value 255.
- Otherwise, if d2 <= 170^2: value 200.
- Otherwise: (x + 2*y + 3*z) modulo 128.

The file stores X fastest, then Y, then Z.
No random values are used.

### Video Verification

Main successfully generated synthetic_demo.mp4:

- 300 frames, with no skipped rows.
- H.264, 640 × 480 pixels.
- 30 FPS playback, 10 seconds.
- Exit code 0.

Video metadata was checked using ffprobe.
Sampled frames show the expected changing sphere cross-sections
against the gradient background, within the trapezoidal region.

The functional run reported 1172.65 FPS for frame generation.
This is recorded separately from the warmed benchmark below.

### Benchmark Method

The machine and runtime are the same as in Experiment 1.
The timing scope and method are the same as in Experiment 2:

- One Java process.
- 3 warm-up rounds, excluded from measurements.
- 5 measured rounds of 300 frames each.
- Only FrameGenerator.generate calls are timed.
- No video is encoded during the benchmark.

### Results

| Round | Frames | Generation time (ms) | FPS |
|---|---:|---:|---:|
| 1 | 300 | 241.494 | 1242.26 |
| 2 | 300 | 236.082 | 1270.74 |
| 3 | 300 | 238.728 | 1256.66 |
| 4 | 300 | 239.512 | 1252.55 |
| 5 | 300 | 240.156 | 1249.19 |

- Arithmetic mean of round rates: **1254.28 FPS**.
- Median rate: **1252.55 FPS**.
- Combined throughput: **1254.21 FPS**.
- Range: **1242.26–1270.74 FPS**.
- Measured frames: 1,500.
- Exit code: 0.

All measured rates exceed the assignment's projection throughput
targets for this workload on the tested machine.

These results do not measure total video production time.
The benchmark limitations described in Experiment 2 also apply.
The small difference from the croissant results is not sufficient
to establish a general performance difference between datasets.

### Reproduction from Windows CMD

Run from the project root with JDK 26 and Maven available.
The data and demo directories must exist.

Build:

```bat
mvn compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
```

Generate the volume:

```bat
java -cp "target/classes" Exercise1_OOP.SyntheticVolumeGenerator "data/synthetic_500_v1.nrrd"
```

The generator refuses to overwrite an existing file.
Generate the volume once and reuse it.

Create the video:

```bat
java -cp "target/classes;target/dependency/*" Exercise1_OOP.Main "data/synthetic_500_v1.nrrd" "demo/croissant_planes.csv" "demo/synthetic_demo.mp4"
```

Run the benchmark:

```bat
java -cp "target/classes;target/dependency/*" Exercise1_OOP.ProjectionBenchmark "data/synthetic_500_v1.nrrd" "demo/croissant_planes.csv"
```

Measurements above were collected through IntelliJ IDEA.
The commands provide the equivalent command-line workflow.

### Files

- [Generator source](../src/main/java/Exercise1_OOP/SyntheticVolumeGenerator.java)
- [Slice sequence](../demo/croissant_planes.csv)
- [Demo video](../demo/synthetic_demo.mp4)

### Windows CMD Reproduction Check

The synthetic workflow was also executed from Windows CMD
using the generated volume and the repository's CSV sequence.

Video generation:
- Output: synthetic_cmd_check.mp4.
- Frames written: 300.
- Skipped rows: 0.
- Reported generation rate: 1188.68 FPS.

Benchmark:
- 3 warm-up rounds, excluded from measurements.
- 5 measured rounds, totaling 1,500 frames.

| Round | Generation time (ms) | FPS |
|---|---:|---:|
| 1 | 246.889 | 1215.12 |
| 2 | 238.104 | 1259.95 |
| 3 | 250.730 | 1196.51 |
| 4 | 243.491 | 1232.08 |
| 5 | 246.265 | 1218.20 |

Mean of round rates: 1224.37 FPS.
Median rate: **1218.20 FPS**.
Combined throughput: 1224.01 FPS.
Range: 1196.51–1259.95 FPS.

This verifies command-line execution on the development machine.
It is not a clean-machine installation test.

## Axis orientation benchmark — 2026-10-25

Compared three axis-aligned slice orientations using
synthetic_500_v1.nrrd, a 500 × 500 × 500 uint8 volume.

Each orientation used 300 congruent isosceles trapezoid slices,
with positions from 100 through 399 along the corresponding axis.
The generated frame dimensions were 401 × 413 pixels
(width × height).

CSV inputs:
- X-normal: demo/synthetic_x_planes.csv
- Y-normal: demo/synthetic_y_planes.csv
- Z-normal: demo/croissant_planes.csv

Each orientation was measured in a separate JVM invocation,
in X, Y, Z order. Each invocation performed three warm-up rounds
followed by five measured rounds of 300 frames each.
Warm-up frames were excluded.

Timing covered FrameGenerator.generate calls only, excluding
input loading, validation, resizing and video encoding.

| Slice normal | Mean FPS | Combined FPS | Median FPS | FPS range |
|---|---:|---:|---:|---:|
| X | 684.92 | 654.75 | 622.06 | 517.93–969.45 |
| Y | 1184.18 | 1182.90 | 1159.26 | 1146.76–1244.16 |
| Z | 1249.18 | 1248.83 | 1257.91 | 1208.47–1265.45 |

Measured round rates, in execution order:
- X: 969.45, 717.26, 597.90, 517.93, 622.06 FPS.
- Y: 1146.76, 1159.26, 1217.27, 1153.43, 1244.16 FPS.
- Z: 1208.47, 1261.31, 1252.75, 1265.45, 1257.91 FPS.

All measured rounds exceeded 100 FPS for these inputs.
X-normal slices showed lower throughput and greater variation.
The experiment does not isolate the cause of this difference.

These results cover three axis-aligned orientations, not arbitrary
oblique planes, and do not measure end-to-end video production speed.

### Reproduction reference

Repository snapshot containing the benchmark code and CSV inputs:
`95be1c7752d9bf72eaa8371acd91f71271d0a928`.

The X and Y CSV inputs were committed after the measurements.
This reference identifies the stored reproduction snapshot,
not a commit recorded before running the experiment.

Generate data/synthetic_500_v1.nrrd using SyntheticVolumeGenerator.
Run ProjectionBenchmark separately with that volume and each CSV:
- demo/synthetic_x_planes.csv
- demo/synthetic_y_planes.csv
- demo/croissant_planes.csv