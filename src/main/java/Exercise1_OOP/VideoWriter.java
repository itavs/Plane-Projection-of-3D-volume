package Exercise1_OOP;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class VideoWriter implements AutoCloseable {

    private final File outputFile;
    private final int width;
    private final int height;
    private final int fps;
    private final double whiteValue;

    private SeekableByteChannel channel;
    private AWTSequenceEncoder encoder;

    private boolean closed;
    private int frameCount;

    public VideoWriter(
            String outputPath,
            int width,
            int height,
            int fps,
            double whiteValue) {

        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing output path.");
        }

        if (width <= 0 || height <= 0
                || width % 2 != 0 || height % 2 != 0) {

            throw new IllegalArgumentException(
                    "Video width and height must be positive and even.");
        }

        if (fps <= 0) {
            throw new IllegalArgumentException("FPS must be positive.");
        }

        if (!Double.isFinite(whiteValue) || whiteValue <= 0) {
            throw new IllegalArgumentException(
                    "White value must be finite and positive.");
        }

        this.outputFile = new File(outputPath);
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.whiteValue = whiteValue;
    }

    public void writeFrame(long[][] frame) throws IOException {
        if (closed) {
            throw new IllegalStateException(
                    "The video writer is already closed.");
        }

        validateFrame(frame);

        BufferedImage image = createImage(frame);

        if (encoder == null) {
            openEncoder();
        }

        encoder.encodeImage(image);
        frameCount++;
    }

    private void openEncoder() throws IOException {
        channel = NIOUtils.writableChannel(outputFile);

        try {
            encoder = new AWTSequenceEncoder(
                    channel, Rational.R(fps, 1));
        } catch (IOException | RuntimeException e) {
            try {
                channel.close();
            } catch (IOException closeError) {
                e.addSuppressed(closeError);
            }

            channel = null;
            closed = true;
            throw e;
        }
    }

    private void validateFrame(long[][] frame) {
        if (frame == null || frame.length == 0
                || frame[0] == null || frame[0].length == 0) {

            throw new IllegalArgumentException(
                    "Frame must not be empty.");
        }

        int expectedWidth = frame[0].length;

        for (long[] row : frame) {
            if (row == null || row.length != expectedWidth) {
                throw new IllegalArgumentException(
                        "Frame must be a rectangular matrix.");
            }
        }
    }

    private BufferedImage createImage(long[][] frame) {
        int sourceHeight = frame.length;
        int sourceWidth = frame[0].length;

        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);

        double scale = Math.min(
                (double) width / sourceWidth,
                (double) height / sourceHeight);

        int targetWidth = Math.min(width, Math.max(
                1, (int) Math.round(sourceWidth * scale)));

        int targetHeight = Math.min(height, Math.max(
                1, (int) Math.round(sourceHeight * scale)));

        int offsetX = (width - targetWidth) / 2;
        int offsetY = (height - targetHeight) / 2;

        for (int y = 0; y < targetHeight; y++) {
            int sourceY = Math.min(
                    sourceHeight - 1,
                    (int) ((y + 0.5) * sourceHeight / targetHeight));

            for (int x = 0; x < targetWidth; x++) {
                int sourceX = Math.min(
                        sourceWidth - 1,
                        (int) ((x + 0.5) * sourceWidth / targetWidth));

                int gray = toGray(frame[sourceY][sourceX]);

                int rgb = (gray << 16) | (gray << 8) | gray;

                image.setRGB(offsetX + x, offsetY + y, rgb);
            }
        }

        return image;
    }

    private int toGray(long value) {
        if (value <= 0) {
            return 0;
        }

        if (value >= whiteValue) {
            return 255;
        }

        return (int) Math.round((value / whiteValue) * 255.0);
    }

    public int getFrameCount() {
        return frameCount;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        if (channel == null) {
            return;
        }

        try {
            if (encoder != null) {
                encoder.finish();
            }
        } finally {
            channel.close();
        }
    }
}