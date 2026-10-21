# Software Requirements Specification
## Plane Projection of 3D Volume

### 1. Purpose

This document defines the required behavior and quality goals of
the application. It provides a basis for implementation, testing,
and evaluation.

The application reads a three-dimensional NRRD volume and a CSV
sequence of planar regions, generates a grayscale image for each
valid region, and combines the images into an MP4 video.

Requirements describe the intended system. Their inclusion here
does not imply that they have already been implemented or verified.

### 2. Requirement Sources

The assignment requires:

- A three-dimensional volume with dimensions from 1 to 1023
  along each axis.
- Support for unsigned 8-bit, 16-bit, and 32-bit voxel values.
- Slices defined by isosceles triangles or isosceles trapezoids.
- Zero-valued samples outside the volume.
- An MP4 output preserving the order of the input slices.
- A command-line demo accepting NRRD, CSV, and MP4 paths.
- Frame-generation performance above 30 FPS, with a target of
  at least 100 FPS, for a 500 × 500 × 500 byte volume.

The following sections also define project decisions needed to
resolve details not fully specified in the assignment.

### 3. Input Requirements

#### FR-01 — Command-Line Interface

The application shall accept exactly three command-line arguments,
in this order:

1. Input NRRD file path.
2. Input CSV file path.
3. Output MP4 file path.

An incorrect argument count shall produce a usage message and
terminate execution without starting processing.

#### FR-02 — Volume Loading

The application shall read the volume dimensions, data type,
and voxel values from the NRRD file.

The volume shall be three-dimensional, with each dimension
between 1 and 1023.

The volume shall be loaded once and reused for all slices.

#### FR-03 — Supported NRRD Subset

The application shall support unsigned 8-bit, 16-bit, and 32-bit
integer voxel values.

As a project extension, signed integer values of the same widths
shall also be supported for datasets containing signed samples.

The supported file subset shall include:

- Voxel data stored inside the NRRD file.
- Raw and gzip encodings.
- Little-endian and big-endian multi-byte values.

Unsupported formats, invalid headers, and incomplete voxel data
shall produce a descriptive error and stop processing.

Detached data files, floating-point voxel types, and nonzero
data-skip settings are outside the supported subset.

#### FR-04 — CSV Structure

The CSV file shall use UTF-8 encoding and contain no header row.

Each row shall contain exactly 9 or 12 comma-separated numeric
values, representing three or four points respectively.

Each point shall contain coordinates in the order x, y, z.
Surrounding whitespace shall be permitted.

Empty fields, nonnumeric values, NaN, and infinite values
shall be rejected. Quoted CSV fields are outside the supported
format.

#### FR-05 — Point Order

For a triangle, the point order shall be:

a, b, c

Point a shall be the apex, so that dist(a,b) and dist(a,c)
are equal within the geometric tolerance.

For a trapezoid, the point order shall be:

a1, a2, b, c

The bases shall connect a1 to a2 and b to c.
The equal-length legs shall connect a1 to b and a2 to c.

### 4. Geometry and Sampling Requirements

#### FR-06 — Geometry Validation

A triangle shall contain three distinct, non-collinear points
and satisfy the equal-leg condition.

A trapezoid shall contain four distinct, coplanar points forming
a nondegenerate, non-self-intersecting isosceles trapezoid with
the specified base and leg correspondence.

As a project decision, rectangles shall be accepted as the
equal-base special case.

Floating-point comparisons shall use documented tolerances.
The formulas and tolerance values shall be specified in the
algorithm document.

#### FR-07 — Coordinate Interpretation

Coordinates shall be interpreted in voxel index space.

NRRD space origin and space directions shall not be used to
transform the input points.

A negative coordinate shall not invalidate a slice by itself;
the corresponding out-of-volume samples shall have value zero.

#### FR-08 — Frame Generation

The application shall generate one two-dimensional sample matrix
for each valid slice using the assignment's scan-line approach.

For a sample position p = (x,y,z):

- If any coordinate is outside its volume bounds, the sample
  value shall be zero.
- Otherwise, the voxel shall be selected using floor(x),
  floor(y), and floor(z).

Pixels outside the sampled shape shall remain zero.

Output sizing, scan-line placement, and endpoint handling shall
be defined in the algorithm document.

### 5. Output and Error Handling

#### FR-09 — Video Output

The application shall encode one frame per valid slice into
an MP4 file, preserving the relative order of valid CSV rows.

As project decisions:

- Video frames shall have a fixed size of 640 × 480 pixels.
- Each slice shall be fitted while preserving its aspect ratio.
- Unused image areas shall be filled with black.
- Video playback shall use 30 FPS.

Playback FPS shall be distinguished from frame-generation
throughput.

#### FR-10 — Grayscale Mapping

Raw sample values shall be converted to grayscale for video output.

The same intensity mapping shall be used for every frame:

- Values less than or equal to zero shall appear black.
- The maximum positive value in the volume shall appear white.
- Intermediate positive values shall be scaled linearly.
- If there are no positive values, all frames shall appear black.

This display conversion shall not modify the stored voxel values.

#### FR-11 — Invalid Rows

As a project decision, invalid CSV rows and invalid geometric
regions shall be reported with their original line numbers
and skipped.

Processing shall continue with the remaining rows.

If no valid slices are available, the application shall report
that outcome and shall not create or overwrite an MP4 file.

#### FR-12 — File and Encoding Errors

Failures to read input files or write the output video shall
terminate processing and produce a descriptive error.

The application shall not report successful completion after
such a failure. A video interrupted during writing may remain
as a partial output file.

Input files shall not be modified.

### 6. Non-Functional Requirements

#### NFR-01 — Performance

Frame-generation throughput shall exceed 30 FPS for the defined
500 × 500 × 500 byte-volume benchmark. The target shall be
at least 100 FPS.

The benchmark shall document:

- Hardware and Java runtime.
- Volume data and slice definitions.
- Generated frame dimensions and number of frames.
- Warm-up procedure and repeated measurements.
- The exact operations included in timing.

Frame-generation timing shall exclude file loading and video
encoding. Any end-to-end measurement shall be reported separately.

#### NFR-02 — Resource Management

Input streams and video-writing resources shall be closed
after use, including when processing fails.

Video frames shall be processed incrementally rather than
retaining the complete video as images in memory.

Documented allocation limits shall be checked before allocating
volume or frame buffers.

#### NFR-03 — Maintainability

Volume loading, CSV parsing, geometry validation, frame generation,
video encoding, and workflow coordination shall have separate
responsibilities.

Significant code and documentation changes shall be recorded
through descriptive Git commits.

#### NFR-04 — Reproducibility

The repository shall include dependency configuration and
execution instructions.

Test datasets shall be provided or accompanied by retrieval
or generation instructions.

### 7. Verification

A separate testing strategy shall define the test cases and
expected results for these requirements.

Verification shall cover volume decoding, CSV parsing, geometry,
sampling, boundary handling, video output, command-line execution,
and performance.

Test results and experiment reports shall record observed outcomes
separately from the requirements defined here.