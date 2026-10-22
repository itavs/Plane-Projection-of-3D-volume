# Testing Strategy

## 1. Purpose

Testing will verify the requirements in SRS.md and the sampling
rules in ALGORITHM.md.

Checks will cover individual components, the complete command-line
demo, and frame-generation performance.

This document defines planned tests. It does not claim that
these tests have already passed.

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

| ID | Case | Expected result |
| --- | --- | --- |
| N01 | Read the constant volume | Dimensions are 10 × 10 × 10; every voxel equals 100 |
| N02 | Read a 3 × 4 × 5 volume with M[x][y][z] = x + 10y + 100z | Every value matches its coordinates; axes are not swapped |
| N03 | Read unsigned boundary values | Values are preserved, including 255, 65535, and 4294967295 |
| N04 | Read equivalent little-endian and big-endian files | Both produce identical voxel values |
| N05 | Read equivalent raw and gzip files | Both produce identical voxel values |
| N06 | Read signed integer fixtures | Negative and positive values are decoded correctly |
| N07 | Invalid dimensions, missing required fields, or unsupported format | A descriptive error stops processing |
| N08 | Truncated payload or damaged gzip data | The read fails; incomplete data is not accepted as a valid volume |

Boundary fixtures will include values around the signed/unsigned
transition, such as 127/128 and 32767/32768.

## 5. CSV Reader Tests

| ID | Case | Expected result |
| --- | --- | --- |
| C01 | Valid rows containing 9 and 12 values | Rows become arrays of three and four points |
| C02 | Multiple valid rows | Point order and row order are preserved |
| C03 | Surrounding whitespace and a UTF-8 BOM at the beginning | Valid numeric fields are parsed successfully |
| C04 | Wrong field count, empty fields, or nonnumeric text | The row is reported and skipped |
| C05 | NaN or infinite coordinates | The row is reported and skipped |
| C06 | Valid and invalid rows mixed together | Valid rows remain in order and retain original line numbers |
| C07 | Empty file or no valid rows | No valid slice definitions are returned |
| C08 | Missing or unreadable input file | Processing stops with an error |

Numeric negative coordinates are valid CSV input. Whether a
sample lies outside the volume is handled during sampling.

## 6. Geometry Tests

| ID | Case | Expected result |
| --- | --- | --- |
| G01 | Valid isosceles triangle with the first point as apex | Accepted |
| G02 | Triangle with unequal legs | Rejected |
| G03 | Repeated points or collinear triangle points | Rejected |
| G04 | Valid widening or narrowing isosceles trapezoid | Accepted |
| G05 | Rectangle in the documented point order | Accepted |
| G06 | Non-coplanar points, nonparallel bases, or unequal legs | Rejected |
| G07 | Crossing edges or invalid point correspondence | Rejected |
| G08 | Valid shapes translated or rotated in 3D | Accepted |
| G09 | Perturbations clearly below and above the documented tolerance | Accepted or rejected according to the tolerance rules |

Tests near the tolerance boundary will use independently
calculated distances and directions.

## 7. Raw Frame Tests

Raw matrices will be checked before grayscale conversion,
resizing, and video encoding.

| ID | Case | Expected result |
| --- | --- | --- |
| F01 | Valid shape entirely inside the constant volume | Every sampled pixel equals 100; unused pixels remain 0 |
| F02 | Shape crossing a volume boundary | Interior samples equal 100 and exterior samples equal 0 |
| F03 | Shape entirely outside the volume | Every output value is 0 |
| F04 | Negative fractional coordinates | Samples outside the volume return 0 |
| F05 | Coordinates at zero, just below an upper bound, and exactly at the upper bound | Bounds follow the documented half-open intervals |
| F06 | Fractional coordinates in a varying-value volume | Values match floor-based voxel selection |
| F07 | Small triangle and trapezoid with manually calculated output | Matrix dimensions and all pixel values match expectations |
| F08 | Fractional leg lengths and scan-line widths | First/last rows, endpoint handling, and placement follow ALGORITHM.md |
| F09 | Oblique slices through a varying-value volume | Samples match independently calculated 3D positions |
| F10 | Requested frame exceeding the allocation limit | Rejected before the frame buffer is allocated |

The constant-volume tests alone cannot verify axis order or
correct voxel selection. Varying-value fixtures are required
for those properties.

## 8. Image and Video Tests

| ID | Case | Expected result |
| --- | --- | --- |
| V01 | Known raw intensities | Grayscale mapping follows the documented global white level |
| V02 | Frames with different local maxima | The same global intensity mapping is used throughout |
| V03 | Volume with no positive values | Output images are black |
| V04 | Slices with different aspect ratios | Images fit within 640 × 480, preserving aspect ratio and using black margins |
| V05 | A sequence of visually distinguishable slices | Decoded frame count and order match the valid input rows |
| V06 | Completed MP4 | Video is readable, has 640 × 480 frames, and uses 30 playback FPS |

Exact voxel values will be checked in raw matrices.
MP4 encoding may alter pixel values, so decoded video checks
will focus on structure, order, dimensions, timing, and visual
agreement rather than exact pixel equality.

## 9. Command-Line Demo Tests

These tests address the complete demo required by the assignment.

| ID | Case | Expected result |
| --- | --- | --- |
| D01 | Three valid paths: NRRD, CSV, MP4 | A readable MP4 is generated |
| D02 | Incorrect argument count | A usage message appears and processing does not start |
| D03 | Input and output paths containing spaces | Paths are handled correctly when quoted |
| D04 | Mixed valid, malformed, and geometrically invalid rows | Invalid rows are reported; valid frames appear in order |
| D05 | No valid slices and no existing output | No output video is created |
| D06 | No valid slices and an existing output file | The existing file remains unchanged |
| D07 | Input read failure or output write failure | Processing reports failure and does not claim success |
| D08 | Run using each available main dataset | Loading and video generation complete without unexpected errors |

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