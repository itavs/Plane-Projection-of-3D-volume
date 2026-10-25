# Plane Projection Algorithm

## 1. Purpose

The algorithm samples a planar region within a 3D volume and
produces a 2D matrix of voxel values.

The region is an isosceles triangle or an isosceles trapezoid.
Sampling follows successive scan lines between the two equal legs,
as described in the assignment.

Each output pixel represents a sampled voxel value.
Values are not accumulated along viewing rays.

## 2. Inputs and Preconditions

The algorithm receives:

- A loaded volume M with dimensions sx, sy, sz.
- An ordered array of three or four validated 3D points.

Coordinates are expressed in voxel index space.

For a triangle, the points are ordered as a, b, c:

- a is the apex.
- a→b and a→c are the equal-length legs.
- b→c is the base.

For a trapezoid, the points are ordered as a1, a2, b, c:

- a1→a2 is the starting base.
- b→c is the ending base.
- a1→b and a2→c are the equal-length legs.

Before sampling, geometry validation rejects repeated points,
degenerate shapes, unequal legs, and invalid trapezoid geometry.

Trapezoid validation also checks coplanarity, parallel bases,
consistent base direction, and centered bases. Rectangles are
accepted as the equal-base special case.

Geometry validation uses:

- ABS_EPS = 1e-8
- REL_EPS = 1e-8
- ANGLE_EPS = 1e-8

Length equality is checked using:

    abs(u - v) <= ABS_EPS + REL_EPS * max(abs(u), abs(v))

Points whose distance is at most ABS_EPS are rejected as
coincident or too close.

For coplanarity and base centering, the distance tolerance is:

    ABS_EPS + REL_EPS * scale

Here, scale is the maximum length among the two bases,
the two legs, and the vector from a1 to c.

Cross products of normalized directions are compared against
ANGLE_EPS to check parallelism and reject near-degenerate shapes.
The base directions must also have a positive dot product.

Geometry validation is performed by the caller before invoking
FrameGenerator.generate; the generator does not repeat the full
geometry validation.

## 3. Common Representation

Both shapes are represented by two starting points and two
ending points:

| Shape     | Start left | Start right | End left | End right |
|-----------|------------|-------------|----------|-----------|
| Triangle  | a          | a           | b        | c         |
| Trapezoid | a1         | a2          | b        | c         |

For a triangle, the starting base has zero length.
This allows both shapes to use the same sampling procedure.

Let:

- L = distance(startLeft, endLeft)
- B0 = distance(startLeft, startRight)
- B1 = distance(endLeft, endRight)
- B = max(B0, B1)

The validated right leg has approximately the same length as L.

## 4. Output Dimensions

Allocate a zero-initialized matrix E with:

- height = ceil(L)
- width = ceil(B) + 1

The matrix is indexed as E[row][column].

Height follows the leg length, as in the assignment, rather than
the perpendicular geometric height of the shape.

The extra column accommodates the inclusive horizontal loop,
which samples both endpoints when a scan-line length is an integer.

Using the larger base allows trapezoids that widen or narrow
along the scan direction.

Dimensions and allocation size must be checked before allocation.
The current frame-size limit is 16,000,000 pixels.

## 5. Constructing Each Scan Line

For each output row y from 0 to height - 1:

1. Compute the distance travelled along the left leg:

   travelled = min(y + 1, L)

2. Convert it to a relative position:

   t = travelled / L

3. Interpolate the scan-line endpoints:

   left = startLeft + t × (endLeft - startLeft)
   right = startRight + t × (endRight - startRight)

4. Compute the scan-line length:

   d = distance(left, right)

Interpolation uses the same relative position on both legs.

The first row is sampled after advancing along the legs, following
the update-before-sampling order in the assignment. Therefore,
the initial base, or the triangle's apex, is not sampled directly.

When L is not an integer, the final step is clamped to the ending
base instead of advancing beyond the shape.

## 6. Sampling Across a Scan Line

Compute the horizontal placement:

offset = floor((B - d) / 2)

Clamp offset to zero if floating-point rounding makes it negative.

When d is positive, compute the unit direction:

direction = (right - left) / d

For each integer i from 0 through floor(d), inclusive:

1. Compute the sample position:

   p = left + i × direction

2. Compute the output column:

   column = offset + i

3. If column is inside the output matrix, write:

   E[y][column] = sampleVolume(p)

When d is zero, sample the single endpoint once at offset.

Pixels not written by this procedure remain zero.

Scan lines are centered using an integer offset. Rounding can
produce a one-pixel difference between the side margins.

For noninteger d, the last sample is before the right endpoint;
an additional endpoint sample is not inserted.

## 7. Looking Up a Voxel

For p = (x, y, z), first check the original coordinates.

Return zero if any coordinate is non-finite or outside:

0 <= x < sx
0 <= y < sy
0 <= z < sz

Otherwise, select:

ix = floor(x)
iy = floor(y)
iz = floor(z)

Return M[ix][iy][iz].

Bounds are checked before conversion to integer indices, so
a negative fractional coordinate cannot become a valid index.

This is floor-based voxel selection. It does not perform
trilinear interpolation.

## 8. Pseudocode

```text
generateFrame(volume, points):
    identify startLeft, startRight, endLeft, endRight

    L = distance(startLeft, endLeft)
    B = max(
        distance(startLeft, startRight),
        distance(endLeft, endRight)
    )

    height = ceil(L)
    width = ceil(B) + 1
    validateAllocation(height, width)
    E = zeroMatrix(height, width)

    for y from 0 to height - 1:
        t = min(y + 1, L) / L

        left = startLeft + t * (endLeft - startLeft)
        right = startRight + t * (endRight - startRight)

        d = distance(left, right)
        offset = max(0, floor((B - d) / 2))

        if d == 0:
            if offset < width:
                E[y][offset] = sampleVolume(volume, left)
        else:
            direction = (right - left) / d

            for i from 0 to floor(d):
                column = offset + i

                if column >= width:
                    break

                p = left + i * direction
                E[y][column] = sampleVolume(volume, p)

    return E
```

## 9. Converting Frames into Video

Frame generation returns raw sample values. Video preparation
is a separate stage.

The application finds the maximum positive voxel value once
for the entire volume. This value defines the white level.

For a positive white level W, each sample v is mapped to:

gray = round(255 × clamp(v / W, 0, 1))

If the volume has no positive values, the output is black.

Each image is resized to fit a 640 × 480 frame while preserving
its aspect ratio, using nearest-neighbor resizing. Remaining
space is filled with black.

JCodec encodes the frames in valid CSV-row order at 30 playback FPS.
For JCodec 0.2.5, VideoWriter bypasses the default RGB-to-YUV
conversion for these grayscale images.

Each grayscale value is converted directly to limited-range luma:

    Y = 16 + floor((219 * gray + 127) / 255)

JCodec stores the luma byte as Y - 128. Chroma planes contain
zero bytes, representing neutral chroma in its internal format.

The encoder requires the YUV420J input tag in this version,
while the supplied samples use limited-range luma to match
the output's range signaling.

The decoded video may contain small intensity differences due
to lossy encoding and conversion rounding. Exact grayscale
mapping is checked before encoding; decoded-video tests allow
a bounded tolerance.

## 10. Complexity

Let N = sx × sy × sz and P = height × width.

- Reading and decoding the volume takes O(N) time.
- Finding the global white level takes O(N) time.
- Validating one shape takes O(1) time because it has at most
  four points.
- Allocating and generating one frame takes O(P) time.
- The raw frame matrix requires O(P) additional memory.
- Volume storage requires O(N) memory for a fixed voxel type.

Frames are generated and passed to the video writer one at a time.

CSV parsing, image resizing, and video encoding have additional
costs. Frame-generation FPS measures only frame generation;
it does not represent total application throughput.

## 11. Verification

The rounding and endpoint rules require checks covering:

- Fractional leg lengths and scan-line widths.
- The first and final scan lines.
- Triangles and both widening and narrowing trapezoids.
- Shapes partially or entirely outside the volume.
- Negative fractional coordinates and upper volume boundaries.
- Geometry close to the validation tolerances.

The zero-length scan-line branch is defensive. For valid,
nondegenerate inputs, sampling advances beyond the triangle's
apex and trapezoids have nonzero bases. Direct coverage of this
branch should therefore be distinguished from valid-input tests.

Automated tests and recorded results are documented in
[Testing Strategy and Results](TESTING.md).
Performance measurements and demonstrations are documented in
[Experiments](EXPERIMENTS.md).

Passing tests establish the checked cases, not every possible
floating-point input or geometric configuration.