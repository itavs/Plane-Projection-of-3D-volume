# NRRD Datasets

## Purpose

This dataset collection supports volume-loading checks, numerical
sampling tests, and performance measurements.

All paths below are relative to the project root.

## Dataset Overview

| File in `data/` | Dimensions | Voxel type | Encoding | Observed range | Purpose |
| --- | --- | --- | --- | --- | --- |
| `constant_100_10x10x10.nrrd` | 10 × 10 × 10 | uint8 | raw | 100–100 | Known-value sampling and boundary tests |
| `onion.nrrd` | 256 × 256 × 130 | uint8 | gzip | 0–232 | 8-bit volume processing |
| `biscuit_cake.nrrd` | 256 × 256 × 130 | uint16 | gzip | 0–3,986 | 16-bit volume processing |
| `swiss_roll.nrrd` | 256 × 256 × 130 | uint32 | gzip | 0–186,330 | 32-bit volume processing |
| `croissant.nrrd` | 500 × 500 × 500 | uint8 | gzip | 0–255 | Performance benchmark |

The 16-bit and 32-bit files use little-endian byte order.
All five files contain their voxel data within the NRRD file.

## Known-Value Dataset

`constant_100_10x10x10.nrrd` was generated for this project
with assistance from ChatGPT.

It contains exactly 1,000 unsigned 8-bit voxel values, all equal
to 100. Its header specifies three dimensions of size 10 and
raw encoding.

Expected sampling behavior:

- Samples inside the volume return 100.
- Samples outside the volume return 0.
- Unwritten pixels outside the sampled shape remain 0.

These expectations apply to the raw frame matrix before
grayscale conversion, resizing, and video encoding.

This dataset is included in the repository.

## Course-Shared Datasets

The onion, biscuit cake, Swiss roll, and croissant files were
shared by a student in the course group.

Their original source and generation method have not been
confirmed. They are therefore not identified here as real
scans or as AI-generated data.

These four files are currently used locally and are not included
in the repository. A public retrieval method or permission to
distribute them remains to be established.

Until then, cloning the repository alone does not provide
the complete dataset collection.

## Preliminary File Inspection

An independent inspection of the supplied files confirmed:

- Three-dimensional volume headers.
- The dimensions, types, encodings, and ranges listed above.
- Successful gzip decompression of the four course-shared files.
- Payload lengths matching the declared dimensions and types.
- Exactly 1,000 values equal to 100 in the known-value dataset.

This inspection does not establish that the Java application
processes every file correctly. Application-level checks and
their results will be documented separately.

## Coverage and Limitations

The collection covers unsigned 8-bit, 16-bit, and 32-bit data,
raw and gzip encodings, and the required 500³ byte-volume size.

The constant dataset provides a simple numerical reference,
but cannot by itself detect swapped axes or incorrect indexing
within the volume.

Additional small fixtures may be introduced for axis ordering,
integer boundary values, big-endian decoding, and malformed input
when the testing strategy is defined.