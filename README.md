# Plane Projection of 3D Volume

A Java application that generates grayscale 2D slices from a 3D
NRRD volume and combines them into an MP4 video.

Developed for Exercise 1 in Introduction to Software Engineering
(OOP / OOD), with an emphasis on modular design, input validation,
and efficient frame generation.

## Problem Definition

Given a three-dimensional volume and a sequence of planar regions,
the application samples the volume within each region to produce
a two-dimensional image.

Each region is defined by either three points forming an isosceles
triangle or four points forming an isosceles trapezoid. The resulting
images are combined into a video in the order specified by the input.

Here, projection means sampling a planar slice of the volume;
it does not accumulate values along viewing rays.

## Input and Output

| File | Purpose |
| --- | --- |
| NRRD input | Stores the volume dimensions and voxel values. |
| CSV input | Defines one slice per row using three or four 3D points. |
| MP4 output | Contains one frame for each valid slice, preserving input order. |

Each point consists of three numeric coordinates: `x`, `y`, and `z`.
CSV rows contain 9 or 12 comma-separated values, without a header.
Point order is significant:

- Triangle: a, b, c, where a is the apex and
  distance(a, b) equals distance(a, c).
- Trapezoid: a1, a2, b, c, where a1–a2 and b–c are
  the parallel bases, and a1–b and a2–c are the equal legs.

Examples:

```csv
5,1,0,2,5,0,8,5,0
4,1,0,6,1,0,1,5,0,9,5,0
```

## Scope and Behavior

- Load the NRRD volume once and reuse it for all slices.
- Parse CSV rows and validate their coordinates and geometry.
- Support isosceles triangles and isosceles trapezoids.
- Interpret coordinates in voxel index space.
- Assign a value of zero to samples outside the volume.
- Report invalid CSV rows and skip them while processing valid rows.
- Generate grayscale frames and encode them as an MP4 video.
- Create no video when no valid slices are available.

Physical-coordinate transformations using NRRD space origin and
space directions are outside the current scope.

## Design

Responsibilities are separated into dedicated classes:

| Class | Responsibility |
| --- | --- |
| `Point3D` | Represents a point with x, y, and z coordinates. |
| `SliceRow` | Stores a row's points and original line number. |
| `NrrdReader` | Reads volume metadata and voxel data. |
| `CsvReader` | Parses CSV rows into slice definitions. |
| `GeometryValidator` | Checks whether the points define a supported shape. |
| `FrameGenerator` | Samples the volume to produce a 2D frame. |
| `VideoWriter` | Converts frames to grayscale images and writes the MP4. |
| `Application` | Coordinates the processing workflow. |
| `Main` | Provides the program entry point. |

## Technologies

- **Java** — application implementation.
- **Maven** — build configuration and dependency management.
- **JCodec** — MP4 video encoding.

## Performance

The assignment requires at least 30 FPS for a
500 × 500 × 500 byte volume, with 100 FPS or more as the target.

For the tested croissant volume and sequence of 300 parallel
trapezoidal slices, three repeated runs reported projection rates
of 1146.62, 1202.26, and 1180.64 FPS.
Their arithmetic mean was 1176.51 FPS.

These measurements cover FrameGenerator.generate only.
They exclude input loading, geometry validation, volume-wide
maximum calculation, image resizing, and video encoding.

The output video plays at 30 FPS. Projection throughput does
not represent end-to-end video production speed, and the measured
results apply to the documented workload and machine.

See [experiment details and limitations](docs/EXPERIMENTS.md).

## Project Status

Implemented and verified:

- NRRD loading, CSV parsing, geometry validation, frame generation,
  and MP4 video output.
- Command-line arguments for input and output paths.
- Successful execution from Windows CMD.
- 88 automated tests passed through Maven with no failures,
  errors, or skipped tests.
- A documented experiment on a 500 × 500 × 500 volume,
  including repeated measurements and a 10-second demo video.

Project documentation:

- [Software Requirements Specification](docs/SRS.md)
- [Detailed Algorithm](docs/ALGORITHM.md)
- [Datasets and Availability](docs/DATASETS.md)
- [Testing Strategy and Results](docs/TESTING.md)
- [Experiment and Results](docs/EXPERIMENTS.md)

Remaining verification includes reviewing consistency between
the requirements, implementation, and test coverage, and completing
any outstanding dataset checks.

The course-shared NRRD files are not included in the repository.
Reproducing the croissant experiment requires obtaining the same
input volume; its original provenance and public distribution
source remain unverified.

## Build and Run (Windows CMD)

Requirements:
- JDK 26 and Maven available in the terminal.
- An NRRD input file and a CSV file containing plane coordinates.
- An existing output directory.

Run the following commands from the project root
(the directory containing pom.xml).

### Build and prepare runtime dependencies

```bat
mvn compile dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
```

### Run

```bat
java -cp "target/classes;target/dependency/*" Exercise1_OOP.Main "path/to/input.nrrd" "path/to/planes.csv" "path/to/output.mp4"
```

Replace the three paths with your actual file paths.
Argument order: NRRD input, CSV input, MP4 output.

The output video uses 30 FPS. The reported frame generation
rate measures projection computation only, not total runtime
or video playback speed.

### Run tests

```bat
mvn test
```

## Experiment and Demo

See [experiment details and results](docs/EXPERIMENTS.md).

- [Slice coordinates](demo/croissant_planes.csv)
- [Download the demo video](demo/croissant_demo.mp4)

A reproducible synthetic experiment is available using the
included volume generator and slice sequence.
See [generation and execution instructions](docs/EXPERIMENTS.md#experiment-3--reproducible-synthetic-volume).

- [Synthetic demo video](demo/synthetic_demo.mp4)