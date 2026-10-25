# Testing Strategy

## 1. Purpose

Testing will verify the requirements in SRS.md and the sampling
rules in ALGORITHM.md.

Checks will cover individual components, the complete command-line
demo, and frame-generation performance.

This document contains the testing plan and a chronological
execution record. A planned case is not considered verified
solely because it appears in the plan.

Historical entries describe the status at the time of execution.
Later entries may resolve previously pending checks.

## 2. Approach

Testing will use three levels:

- Component tests for volume reading, CSV parsing, geometry,
  and raw frame generation.
- Integration tests for the complete input-to-video workflow.
- Performance experiments on a 500 × 500 × 500 byte volume.

Repeatable numerical checks will be automated where practical.
Visual inspection will supplement these checks, but will not
replace them.

Expected numerical results will be calculated independently
of the implementation under test.

## 3. Test Data

The main datasets are listed in DATASETS.md.

The constant 10 × 10 × 10 volume will provide exact sampling
expectations: 100 inside the volume and 0 outside it.

The four course-shared files will exercise larger volumes and
unsigned 8-bit, 16-bit, and 32-bit data.

Additional small fixtures will be created for:

- Distinct values along each axis.
- Integer boundary values.
- Little-endian and big-endian data.
- Raw and gzip encodings.
- Malformed headers and incomplete payloads.

These fixtures and their creation procedures will be committed
with the corresponding tests.

## 4. NRRD Reader Tests

| ID  | Case                                                               | Expected result                                                   |
|-----|--------------------------------------------------------------------|-------------------------------------------------------------------|
| N01 | Read the constant volume                                           | Dimensions are 10 × 10 × 10; every voxel equals 100               |
| N02 | Read a 3 × 4 × 5 volume with M[x][y][z] = x + 10y + 100z           | Every value matches its coordinates; axes are not swapped         |
| N03 | Read unsigned boundary values                                      | Values are preserved, including 255, 65535, and 4294967295        |
| N04 | Read equivalent little-endian and big-endian files                 | Both produce identical voxel values                               |
| N05 | Read equivalent raw and gzip files                                 | Both produce identical voxel values                               |
| N06 | Read signed integer fixtures                                       | Negative and positive values are decoded correctly                |
| N07 | Invalid dimensions, missing required fields, or unsupported format | A descriptive error stops processing                              |
| N08 | Truncated payload or damaged gzip data                             | The read fails; incomplete data is not accepted as a valid volume |

Boundary fixtures will include values around the signed/unsigned
transition, such as 127/128 and 32767/32768.

## 5. CSV Reader Tests

| ID  | Case                                                    | Expected result                                             |
|-----|---------------------------------------------------------|-------------------------------------------------------------|
| C01 | Valid rows containing 9 and 12 values                   | Rows become arrays of three and four points                 |
| C02 | Multiple valid rows                                     | Point order and row order are preserved                     |
| C03 | Surrounding whitespace and a UTF-8 BOM at the beginning | Valid numeric fields are parsed successfully                |
| C04 | Wrong field count, empty fields, or nonnumeric text     | The row is reported and skipped                             |
| C05 | NaN or infinite coordinates                             | The row is reported and skipped                             |
| C06 | Valid and invalid rows mixed together                   | Valid rows remain in order and retain original line numbers |
| C07 | Empty file or no valid rows                             | No valid slice definitions are returned                     |
| C08 | Missing or unreadable input file                        | Processing stops with an error                              |

Numeric negative coordinates are valid CSV input. Whether a
sample lies outside the volume is handled during sampling.

## 6. Geometry Tests

| ID  | Case                                                           | Expected result                                       |
|-----|----------------------------------------------------------------|-------------------------------------------------------|
| G01 | Valid isosceles triangle with the first point as apex          | Accepted                                              |
| G02 | Triangle with unequal legs                                     | Rejected                                              |
| G03 | Repeated points or collinear triangle points                   | Rejected                                              |
| G04 | Valid widening or narrowing isosceles trapezoid                | Accepted                                              |
| G05 | Rectangle in the documented point order                        | Accepted                                              |
| G06 | Non-coplanar points, nonparallel bases, or unequal legs        | Rejected                                              |
| G07 | Crossing edges or invalid point correspondence                 | Rejected                                              |
| G08 | Valid shapes translated or rotated in 3D                       | Accepted                                              |
| G09 | Perturbations clearly below and above the documented tolerance | Accepted or rejected according to the tolerance rules |

Tests near the tolerance boundary will use independently
calculated distances and directions.

## 7. Raw Frame Tests

Raw matrices will be checked before grayscale conversion,
resizing, and video encoding.

| ID  | Case                                                                           | Expected result                                                       |
|-----|--------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| F01 | Valid shape entirely inside the constant volume                                | Every sampled pixel equals 100; unused pixels remain 0                |
| F02 | Shape crossing a volume boundary                                               | Interior samples equal 100 and exterior samples equal 0               |
| F03 | Shape entirely outside the volume                                              | Every output value is 0                                               |
| F04 | Negative fractional coordinates                                                | Samples outside the volume return 0                                   |
| F05 | Coordinates at zero, just below an upper bound, and exactly at the upper bound | Bounds follow the documented half-open intervals                      |
| F06 | Fractional coordinates in a varying-value volume                               | Values match floor-based voxel selection                              |
| F07 | Small triangle and trapezoid with manually calculated output                   | Matrix dimensions and all pixel values match expectations             |
| F08 | Fractional leg lengths and scan-line widths                                    | First/last rows, endpoint handling, and placement follow ALGORITHM.md |
| F09 | Oblique slices through a varying-value volume                                  | Samples match independently calculated 3D positions                   |
| F10 | Requested frame exceeding the allocation limit                                 | Rejected before the frame buffer is allocated                         |

The constant-volume tests alone cannot verify axis order or
correct voxel selection. Varying-value fixtures are required
for those properties.

## 8. Image and Video Tests

| ID  | Case                                          | Expected result                                                              |
|-----|-----------------------------------------------|------------------------------------------------------------------------------|
| V01 | Known raw intensities                         | Grayscale mapping follows the documented global white level                  |
| V02 | Frames with different local maxima            | The same global intensity mapping is used throughout                         |
| V03 | Volume with no positive values                | Output images are black                                                      |
| V04 | Slices with different aspect ratios           | Images fit within 640 × 480, preserving aspect ratio and using black margins |
| V05 | A sequence of visually distinguishable slices | Decoded frame count and order match the valid input rows                     |
| V06 | Completed MP4                                 | Video is readable, has 640 × 480 frames, and uses 30 playback FPS            |

Exact voxel values will be checked in raw matrices.
MP4 encoding may alter pixel values, so decoded video checks
will focus on structure, order, dimensions, timing, and visual
agreement rather than exact pixel equality.

## 9. Command-Line Demo Tests

These tests address the complete demo required by the assignment.

| ID  | Case                                                   | Expected result                                                 |
|-----|--------------------------------------------------------|-----------------------------------------------------------------|
| D01 | Three valid paths: NRRD, CSV, MP4                      | A readable MP4 is generated                                     |
| D02 | Incorrect argument count                               | A usage message appears and processing does not start           |
| D03 | Input and output paths containing spaces               | Paths are handled correctly when quoted                         |
| D04 | Mixed valid, malformed, and geometrically invalid rows | Invalid rows are reported; valid frames appear in order         |
| D05 | No valid slices and no existing output                 | No output video is created                                      |
| D06 | No valid slices and an existing output file            | The existing file remains unchanged                             |
| D07 | Input read failure or output write failure             | Processing reports failure and does not claim success           |
| D08 | Run using each available main dataset                  | Loading and video generation complete without unexpected errors |

Input files will be checked for unintended modification.

A clean-checkout trial will verify that the documented build
and execution instructions are sufficient. Any unavailable
external dataset will be recorded as a reproducibility gap.

## 10. Performance Experiment

The primary benchmark will use croissant.nrrd:
a 500 × 500 × 500 volume of unsigned 8-bit values.

A fixed CSV sequence will be committed before measurement.
It will include different slice positions and orientations,
with documented generated frame sizes.

Measurement procedure:

1. Record hardware, operating system, Java version, and commit.
2. Load the volume and prepare valid slice definitions.
3. Perform an untimed warm-up.
4. Measure a fixed batch of frame-generation calls.
5. Repeat the measurement at least five times.
6. Report each result and the median.

Throughput will be calculated as:

FPS = number of generated frames / elapsed generation seconds

Loading, validation, image resizing, and video encoding will
be excluded from this measurement. Any end-to-end duration
will be measured and reported separately.

The minimum acceptance criterion is above 30 generation FPS;
the target is at least 100 generation FPS.

Video playback FPS is a separate setting and is not evidence
of computational performance.

## 11. Recording Results

Each executed test or test group will record:

- Test ID.
- Input files or fixture definition.
- Expected result.
- Actual result.
- Status: PASS, FAIL, or NOT RUN.
- Relevant commit and execution environment.
- Supporting output where useful.

Performance results and the demo MP4 will be included in the
experiment report required by the assignment.

A failed test will lead to investigation, a documented fix,
and a rerun. Relevant earlier tests will also be rerun to
check for regressions.

## 12. Completion Criteria

Testing is complete when:

- All planned required checks have been executed.
- Failures affecting required behavior have been resolved.
- Remaining limitations are explicitly documented.
- The command-line demo is reproducible.
- Performance measurements and the output video are recorded.

Any change to expected behavior must be reflected consistently
in the SRS, algorithm description, tests, and README.

## 13. Execution Record

### Initial NRRD Reader Checks — 2026-10-22

Executed `NrrdReaderTest` using the IntelliJ JUnit runner.

Dataset: `data/constant_100_10x10x10.nrrd`

| Check                                              | Result |
|----------------------------------------------------|--------|
| Reads dimensions as 10 × 10 × 10                   | PASS   |
| Reads all 1,000 voxel values as 100                | PASS   |
| Returns zero at six tested out-of-bounds positions | PASS   |

Total: 3 tests passed.

This covers N01 and an initial check of integer-index boundary
handling. Other planned tests have not yet been executed.
Execution through Maven remains to be verified.

### Expanded NRRD Reader Checks — 2026-10-22

Executed `NrrdReaderTest` using the IntelliJ JUnit runner.

Coverage includes volume dimensions, axis ordering, signed and
unsigned integer decoding, byte order, raw/gzip encoding,
boundary access, and invalid input handling.

Initial result: 16 passed, 1 failed.

The incomplete-payload test revealed that the reader rejected
missing data but returned an exception without a descriptive
message.

The reader now catches EOFException during payload reading and
throws IOException with an explicit incomplete-data message,
preserving the original cause.

Result after the fix: 17 passed, 0 failed.

Gzip trailer corruption and execution through Maven have not
yet been verified.

### CSV Reader Checks — 2026-10-23

Executed `CsvReaderTest` using the IntelliJ JUnit runner
with OpenJDK 26.0.1.

Result: 12 passed, 0 failed.

Verified:

- Parsing three-point and four-point rows.
- Coordinate values, row order, and original line numbers.
- Whitespace, negative and fractional values, UTF-8 BOM,
  and Windows line endings.
- Rejection of incorrect field counts, empty fields,
  nonnumeric text, quoted fields, and non-finite values.
- Skipping invalid rows with line numbers and explanations.
- Empty files, entirely invalid files, and missing files.
- Separation of CSV parsing from geometry validation.

Temporary CSV fixtures were generated by the tests.
No production-code changes were required.

Unreadable-file permissions and execution through Maven
have not yet been verified.

### Geometry Validator Checks — 2026-10-23

Executed `GeometryValidatorTest` using the IntelliJ JUnit runner.

Result: 18 passed, 0 failed.

Verified:

- Valid isosceles triangles and correct apex ordering.
- Widening and narrowing isosceles trapezoids.
- Rectangles as the accepted equal-base special case.
- Translated and tilted shapes.
- Rejection of unequal legs, nonparallel bases,
  non-coplanar points, crossed legs, and zero-area shapes.
- Rejection of repeated or overly close points,
  missing rows, invalid point counts, and non-finite coordinates.
- Triangle leg-length comparisons below and above tolerance.
- Descriptive messages for the tested rejection cases.

No production-code changes were required.
These results cover the selected cases, not every possible geometry.

### Frame Generator Checks — 2026-10-23

Executed `FrameGeneratorTest` using the IntelliJ JUnit runner.

Result: 12 passed, 0 failed.

Verified:

- Expected frame dimensions, pixel placement, and row order.
- Triangle, rectangle, widening and narrowing trapezoid sampling.
- Zero padding outside the sampled shape.
- Volume boundaries along all three axes.
- Negative fractional coordinates and floor-based voxel selection.
- Fractional scan-line widths and final-step clamping.
- Tilted slices sampling different Z layers.
- Entirely out-of-volume slices.
- Rejection of oversized frames, missing inputs,
  and incorrect point counts.

Expected raw matrices were specified explicitly using a constant
volume and a temporary volume with M[x][y][z] = x + 10y + 100z.

No production-code changes were required.
Video conversion and encoding were not tested in this group.

### VideoWriter — Test Results (2026-10-23)

- Result: 13 tests passed.
- Coverage includes input validation, file handling, frame counting,
  frame order, grayscale mapping, image orientation, aspect ratio,
  output dimensions, and playback rate.
- Initial testing found grayscale differences after video encoding
  and decoding: an expected value of 64 was decoded as 51.
- A separate test confirmed that grayscale values were correct
  before encoding.
- Fixed the JCodec 0.2.5 conversion path by writing limited-range
  grayscale luma directly, with neutral chroma.
- All tests passed after the fix without increasing the permitted
  color tolerance.
- Tests were run through IntelliJ IDEA; Maven verification is pending.

### Application — Integration Test Results (2026-10-23)

- Result: 11 tests passed through IntelliJ IDEA.
- Verified frame order and use of the volume-wide maximum
  for grayscale normalization.
- Verified processing of both triangles and trapezoids.
- Verified skipping malformed CSV rows, invalid geometry,
  and oversized frames while continuing with valid rows.
- Verified that empty or entirely invalid input does not
  create a video or overwrite an existing output file.
- Verified black output for volumes containing only
  non-positive values.
- Verified propagation of I/O errors for missing or malformed
  input files and an unavailable output directory.
- Tests create small temporary NRRD and CSV files.
- No changes to Application.java were required.
- Maven verification and large-dataset experiments are pending.

### Full Maven Test Run (2026-10-23)

- Executed the Maven test lifecycle through IntelliJ IDEA.
- Results: 83 tests, 0 failures, 0 errors, 0 skipped.
- Build result: BUILD SUCCESS.
- This completes the Maven verification previously marked pending.
- Large-dataset experiments and performance measurements remain pending.

### Main — Manual Argument Checks (2026-10-23)

Executed through IntelliJ IDEA using Program arguments:

- Three valid paths: created an MP4 containing 3 frames;
  process exited with code 0.
- No arguments: printed the expected-arguments message;
  process exited with code 2.
- Missing NRRD file: printed an error;
  process exited with code 1.

Standalone command-line execution remains to be verified.

### Standalone CMD Execution (2026-10-23)

- Environment: Windows, Java 26.0.1, Maven 3.9.11.
- Compilation and runtime dependency preparation: BUILD SUCCESS.
- Executed Exercise1_OOP.Main directly through Java from CMD.
- Inputs: test_volume_500.nrrd and a CSV containing three trapezoids.
- Output: cmd_result.mp4, with 3 frames and no skipped rows.
- The output was visually consistent with the IntelliJ run.
- This verifies the standalone execution previously marked pending.
- This short run is a functional check, not a formal benchmark.

### Input File Protection (2026-10-24)

- Added output-path validation before input loading.
- Output paths referring to either input file are rejected.
- Added two regression tests covering:
    - Output identical to the NRRD input path.
    - An equivalent path referring to the CSV input.
- Both tests verify that the contents of both input files
  remain unchanged.
- ApplicationTest: 13 tests passed.
- Full Maven run: 85 tests, 0 failures, 0 errors, 0 skipped.
- Build result: BUILD SUCCESS.

### Verification Review (2026-10-25)

- The latest recorded full Maven run passed 85 tests,
  with no failures, errors, or skipped tests.
- Croissant and synthetic-volume video generation and warmed
  projection benchmarks are documented in EXPERIMENTS.md.
- The synthetic workflow was also executed from Windows CMD.

Outstanding checks or documentation:

- N08: - N08: Completed on 2026-10-25. Added regression tests for corrupted
  gzip checksum, corrupted size field, and truncated trailer.
  All 20 NrrdReader tests passed.
- C08: permission-denied input-file behavior.
- D03: Completed on 2026-10-25. Windows CMD execution with quoted
  NRRD, CSV and MP4 paths containing spaces succeeded.
  The application reported 110 frames written, zero rows skipped
  after CSV parsing, and exit code 0.
- D08: Completed on 2026-10-25. CLI runs on onion, biscuit_cake,
  and swiss_roll each produced 110 frames with zero rows skipped
  after CSV parsing. See "Additional dataset CLI checks" below.
- Fresh-clone build and execution: completed on 2026-10-25.
  See "Fresh-clone verification" below.
- Performance measurements with different slice orientations:
  completed for X-, Y- and Z-normal slices on 2026-10-25.
  See "Axis orientation benchmark" in EXPERIMENTS.md.
  Oblique-plane performance was not measured.
- Benchmark code/input revision references for reproducibility.

The geometry record demonstrates triangle leg-length tolerance
checks; it does not establish coverage of every tolerance used
for trapezoid validation.

Passing the current suite does not imply that every planned
case has been executed.

### NRRD gzip integrity validation — 2026-10-25

Fixed an issue where reading the expected voxel bytes did not
necessarily trigger gzip trailer validation. The reader now consumes
the gzip stream to its end and reports integrity failures as IOException.

Added three regression tests covering:

- Corrupted gzip checksum (CRC).
- Corrupted uncompressed-size field.
- Truncated gzip trailer.

Validation: all 20 NrrdReader tests passed.
Full Maven test suite: 88 tests passed, with no failures or errors.

### Additional dataset CLI checks — 2026-10-25

Ran Main from Windows CMD on onion.nrrd, biscuit_cake.nrrd,
and swiss_roll.nrrd using demo/small_volume_planes.csv.

The CSV contains 110 isosceles trapezoid slices at integer
Z coordinates from 10 through 119. Point order is:
(80,30,z), (170,30,z), (30,220,z), (220,220,z).

| Dataset           | Frames written | Rows skipped after CSV parsing | Frame generation rate |
|-------------------|---------------:|-------------------------------:|----------------------:|
| onion.nrrd        |            110 |                              0 |           3668.81 FPS |
| biscuit_cake.nrrd |            110 |                              0 |           3710.54 FPS |
| swiss_roll.nrrd   |            110 |                              0 |           3745.39 FPS |

All three output videos were verified to contain 110 frames
at 640 × 480, with a playback rate of 30 FPS and a duration
of approximately 3.67 seconds.

Visual inspection of the first, middle and last frames showed
different slice content. This is a basic integration check,
not a voxel-by-voxel correctness test.

Performance figures are single-run observations of frame generation
only, excluding input loading and video encoding. They are not
a warmed-up benchmark or a replacement for the 500³ experiment.

The input volumes and these three output videos remain local.
The CSV is included in the repository.

### Fresh-clone verification — 2026-10-25

Cloned the GitHub repository into C:/OOP_Test/clean_checkout
on the existing Windows development machine.

- All 88 automated tests passed.
- Compilation and runtime dependency copying succeeded.
- SyntheticVolumeGenerator created a new 500 × 500 × 500 volume.
- Main used that volume and the repository's demo/croissant_planes.csv.
- The application reported 300 frames written, zero rows skipped
  after CSV parsing, and exit code 0.
- Reported frame generation rate: 1187.17 FPS (single run).

This verifies a fresh repository checkout on the existing development
environment; it is not a clean-machine installation test.