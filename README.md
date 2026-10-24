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

## Performance Goal

The assignment requires a frame-generation rate of at least
30 FPS for a 500 × 500 × 500 byte volume, with 100 FPS or more
as the expected target.

Frame-generation throughput and video playback FPS are separate
measurements. Formal experiments will document the measured
operation, input data, and execution environment.

## Project Status

Initial versions of the requirements specification and detailed
algorithm description are available in [SRS](docs/SRS.md) and
[Algorithm](docs/ALGORITHM.md).

The selected datasets and their availability are documented
in [Datasets](docs/DATASETS.md).

The planned checks are documented in
[Testing Strategy](docs/TESTING.md).

Remaining work includes public access to the course-shared datasets,
implementation and execution of the planned tests, command-line
argument handling, and a documented performance experiment.

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