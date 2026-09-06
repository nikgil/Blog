package dev.sirnik.blog.utils.testing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import dev.sirnik.blog.models.BlogPost;
import dev.sirnik.blog.utils.HTMLParser;

/**
 * Creates deterministic development images at several responsive widths.
 *
 * NOTE: class was AI generated
 */
public final class TestImageGenerator {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    public static final List<Integer> IMAGE_WIDTHS = List.of(480, 960, 1440);

    private static final double ASPECT_RATIO = 16.0 / 9.0;
    private static final int FALLBACK_WIDTH = 960;
    private static final String RESPONSIVE_IMAGE_SIZES = "(max-width: 768px) calc(100vw - 3rem), 56rem";

    private final Path outputDirectory;
    private final String publicPath;

    public TestImageGenerator(Path outputDirectory, String publicPath) {
        this.outputDirectory = outputDirectory.toAbsolutePath().normalize();
        this.publicPath = normalizePublicPath(publicPath);

        try {
            Files.createDirectories(this.outputDirectory);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not create the generated image directory",
                    exception);
        }
    }

    public GeneratedTestImage generate(int seed, int imageIndex) {
        Scene scene = createScene(seed, imageIndex);
        String imageName = "test-image-"
                + Integer.toUnsignedString(seed) + "-" + imageIndex;
        List<String> sourceSetEntries = new ArrayList<>();
        String fallbackSource = null;

        for (int width : IMAGE_WIDTHS) {
            int height = heightFor(width);
            String fileName = imageName + "-" + width + ".jpg";
            Path outputFile = outputDirectory.resolve(fileName);
            render(scene, width, height, outputFile);

            String url = publicPath + fileName;
            sourceSetEntries.add(url + " " + width + "w");
            if (width == FALLBACK_WIDTH) {
                fallbackSource = url;
            }
        }

        return new GeneratedTestImage(
                fallbackSource,
                String.join(", ", sourceSetEntries),
                "Abstract geometric test illustration " + (imageIndex + 1),
                FALLBACK_WIDTH,
                heightFor(FALLBACK_WIDTH));
    }

    public void addRandomImage(Document document, BlogPost post, int seed,
            int postIndex, Random randomiser) {
        GeneratedTestImage image = this
                .generate(seed, postIndex);
        Element figure = new Element("figure")
                .addClass("generated-test-image");
        figure.appendElement("img")
                .attr("src", image.source())
                .attr("srcset", image.sourceSet())
                .attr("sizes", RESPONSIVE_IMAGE_SIZES)
                .attr("width", Integer.toString(image.width()))
                .attr("height", Integer.toString(image.height()))
                .attr("loading", "lazy")
                .attr("decoding", "async")
                .attr("alt", image.altText());

        boolean includeCaption = randomiser.nextBoolean();
        boolean includeCitation = randomiser.nextBoolean();
        if (includeCaption || includeCitation) {
            Element caption = figure.appendElement("figcaption");
            if (includeCaption) {
                caption.appendText("Generated sample image for “"
                        + post.getTitle() + "”.");
            }
            if (includeCaption && includeCitation) {
                caption.appendText(" ");
            }
            if (includeCitation) {
                caption.appendElement("cite")
                        .text("sirnik.Dev test generator");
            }
        }

        HTMLParser.insertAtRandomPosition(document.body(), figure, randomiser,
                false);
    }

    private Scene createScene(int seed, int imageIndex) {
        Random random = new Random((long) seed + imageIndex);
        float baseHue = random.nextFloat();
        Color start = Color.getHSBColor(baseHue, 0.55f, 0.88f);
        Color end = Color.getHSBColor(
                (baseHue + 0.18f + random.nextFloat() * 0.24f) % 1.0f,
                0.68f,
                0.62f);

        List<ShapeSpec> shapes = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            shapes.add(new ShapeSpec(
                    random.nextDouble(),
                    random.nextDouble(),
                    0.12 + random.nextDouble() * 0.32,
                    random.nextFloat(),
                    0.12f + random.nextFloat() * 0.24f,
                    random.nextBoolean()));
        }

        return new Scene(start, end, List.copyOf(shapes));
    }

    private void render(Scene scene, int width, int height, Path outputFile) {
        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setPaint(new GradientPaint(
                    0, 0, scene.startColor(),
                    width, height, scene.endColor()));
            graphics.fillRect(0, 0, width, height);

            for (ShapeSpec shape : scene.shapes()) {
                Color color = Color.getHSBColor(
                        shape.hue(), 0.42f, 1.0f);
                graphics.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, shape.opacity()));
                graphics.setColor(color);

                double size = shape.size() * width;
                double x = shape.x() * width - size / 2.0;
                double y = shape.y() * height - size / 2.0;
                if (shape.circle()) {
                    graphics.fill(new Ellipse2D.Double(x, y, size, size));
                } else {
                    Path2D triangle = new Path2D.Double();
                    triangle.moveTo(x + size / 2.0, y);
                    triangle.lineTo(x + size, y + size);
                    triangle.lineTo(x, y + size);
                    triangle.closePath();
                    graphics.fill(triangle);
                }
            }
        } finally {
            graphics.dispose();
        }

        try {
            if (!ImageIO.write(image, "jpg", outputFile.toFile())) {
                throw new IOException("No JPEG writer is available");
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Could not write generated image " + outputFile,
                    exception);
        }
    }

    private int heightFor(int width) {
        return (int) Math.round(width / ASPECT_RATIO);
    }

    private String normalizePublicPath(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private record Scene(
            Color startColor,
            Color endColor,
            List<ShapeSpec> shapes) {
    }

    private record ShapeSpec(
            double x,
            double y,
            double size,
            float hue,
            float opacity,
            boolean circle) {
    }

    public static record GeneratedTestImage(
            String source,
            String sourceSet,
            String altText,
            int width,
            int height) {
    }
}
