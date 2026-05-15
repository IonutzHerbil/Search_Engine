package app.processor.strategy;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import javax.imageio.ImageIO;

public class ImageExtractionStrategy implements ExtractionStrategy {

  private static final int SAMPLE_STEPS = 50;

  @Override
  public boolean supports(String mimeType) {
    return mimeType.startsWith("image/");
  }

  @Override
  public ExtractionResult extract(Path file, BasicFileAttributes attrs) throws Exception {
    BufferedImage image = ImageIO.read(file.toFile());
    if (image == null) return new ExtractionResult(null, null, null);
    String color = dominantColor(image);
    String nameContent = file.getFileName().toString();
    return new ExtractionResult(nameContent, null, color);
  }

  private String dominantColor(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    int stepX = Math.max(1, width / SAMPLE_STEPS);
    int stepY = Math.max(1, height / SAMPLE_STEPS);

    long sumR = 0, sumG = 0, sumB = 0;
    int count = 0;

    for (int y = 0; y < height; y += stepY) {
      for (int x = 0; x < width; x += stepX) {
        int rgb = image.getRGB(x, y);
        sumR += (rgb >> 16) & 0xFF;
        sumG += (rgb >> 8) & 0xFF;
        sumB += rgb & 0xFF;
        count++;
      }
    }

    if (count == 0) return "unknown";
    return rgbToName((int) (sumR / count), (int) (sumG / count), (int) (sumB / count));
  }

  private String rgbToName(int r, int g, int b) {
    float rf = r / 255f;
    float gf = g / 255f;
    float bf = b / 255f;

    float max = Math.max(rf, Math.max(gf, bf));
    float min = Math.min(rf, Math.min(gf, bf));
    float delta = max - min;

    float saturation = (max == 0f) ? 0f : delta / max;
    float value = max;

    if (value > 0.90f && saturation < 0.12f) return "white";
    if (value < 0.15f) return "black";
    if (saturation < 0.15f) return "gray";

    float hue;
    if (delta == 0f) {
      hue = 0f;
    } else if (max == rf) {
      hue = 60f * (((gf - bf) / delta) % 6f);
    } else if (max == gf) {
      hue = 60f * (((bf - rf) / delta) + 2f);
    } else {
      hue = 60f * (((rf - gf) / delta) + 4f);
    }
    if (hue < 0f) hue += 360f;

    if (hue < 15f || hue >= 345f) return "red";
    if (hue < 45f) return "orange";
    if (hue < 75f) return "yellow";
    if (hue < 165f) return "green";
    if (hue < 195f) return "cyan";
    if (hue < 255f) return "blue";
    if (hue < 285f) return "purple";
    return "pink";
  }
}
