package dev.sirnik.blog.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.sirnik.blog.utils.testing.TestImageGenerator;
import dev.sirnik.blog.utils.testing.TestImageGenerator.GeneratedTestImage;

class TestImageGeneratorTests {

        @TempDir
        private Path temporaryDirectory;

        @Test
        void generatesDeterministicResponsiveImageVariants()
                        throws IOException {
                Path firstDirectory = temporaryDirectory.resolve("first");
                Path secondDirectory = temporaryDirectory.resolve("second");
                TestImageGenerator firstGenerator = new TestImageGenerator(
                                firstDirectory, "/test-images");
                TestImageGenerator secondGenerator = new TestImageGenerator(
                                secondDirectory, "/test-images/");

                GeneratedTestImage first = firstGenerator.generate(42, 3);
                GeneratedTestImage second = secondGenerator.generate(42, 3);

                assertThat(first).isEqualTo(second);
                assertThat(first.source())
                                .isEqualTo("/test-images/test-image-42-3-960.jpg");
                assertThat(first.sourceSet())
                                .contains("-480.jpg 480w")
                                .contains("-960.jpg 960w")
                                .contains("-1440.jpg 1440w");
                assertThat(first.width()).isEqualTo(960);
                assertThat(first.height()).isEqualTo(540);

                for (int width : TestImageGenerator.IMAGE_WIDTHS) {
                        Path firstFile = firstDirectory.resolve(
                                        "test-image-42-3-" + width + ".jpg");
                        Path secondFile = secondDirectory.resolve(
                                        "test-image-42-3-" + width + ".jpg");

                        assertThat(firstFile).exists();
                        BufferedImage image = ImageIO.read(firstFile.toFile());
                        assertThat(image.getWidth()).isEqualTo(width);
                        assertThat(image.getHeight()).isEqualTo(width * 9 / 16);
                        assertThat(Files.readAllBytes(firstFile))
                                        .isEqualTo(Files.readAllBytes(
                                                        secondFile));
                }
        }
}
