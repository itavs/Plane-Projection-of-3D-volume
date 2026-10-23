package Exercise1_OOP;

import org.jcodec.api.transcode.PixelStore.LoanerPicture;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.VideoFrameWithPacket;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;

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
    private SinkImpl encoder;

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
        Picture picture = toVideoPicture(image);

        if (encoder == null) {
            openEncoder();
        }

        Packet packet = Packet.createPacket(
                null,
                frameCount,
                fps,
                1,
                frameCount,
                Packet.FrameType.KEY,
                null);

        encoder.outputVideoFrame(new VideoFrameWithPacket(
                packet, new LoanerPicture(picture, 0)));

        frameCount++;
    }

    private void openEncoder() throws IOException {
        channel = NIOUtils.writableChannel(outputFile);

        try {
            encoder = SinkImpl.createWithStream(
                    channel, Format.MOV, Codec.H264, null);

            encoder.init();
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

    // Package-private so tests can inspect the image before encoding.
    BufferedImage createImage(long[][] frame) {
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

    private Picture toVideoPicture(BufferedImage image) {
        /*
         * JCodec 0.2.5 requires the YUV420J input tag, but its H.264
         * encoder does not signal full-range video.
         *
         * For our grayscale images, write limited-range luma
         * directly and bypass its RGB-to-YUV conversion.
         */
        Picture picture = Picture.create(
                width, height, ColorSpace.YUV420J);

        byte[] luma = picture.getPlaneData(0);
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = image.getRGB(x, y) & 0xff;

                // Map grayscale 0..255 to video luma 16..235.
                int limitedY = 16 + (219 * gray + 127) / 255;

                // JCodec stores channel values with a -128 offset.
                luma[index++] = (byte) (limitedY - 128);
            }
        }

        /*
         * The chroma planes are initialized to zero.
         * With JCodec's offset, zero represents neutral chroma,
         * which is appropriate for grayscale.
         */
        return picture;
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