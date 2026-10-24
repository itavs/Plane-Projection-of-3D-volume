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