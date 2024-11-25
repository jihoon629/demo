package com.example.demo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class ImageUtils {

    /**
     * 이미지를 특정 블록 크기 기준으로 평균 색상을 계산합니다.
     */
    public static Color calculateAverageColor(BufferedImage image, int startX, int startY, int blockSize) {
        int r = 0, g = 0, b = 0, count = 0;
        for (int y = startY; y < startY + blockSize && y < image.getHeight(); y++) {
            for (int x = startX; x < startX + blockSize && x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y));
                r += pixel.getRed();
                g += pixel.getGreen();
                b += pixel.getBlue();
                count++;
            }
        }
        return new Color(r / count, g / count, b / count);
    }

    /**
     * 임시 디렉토리를 생성하고 POSIX 권한을 설정합니다.
     */
    public static File makeTempDir() throws IOException {
        Path tempDirPath = Files.createTempDirectory("webp_frames");
        File tempDir = tempDirPath.toFile();
        System.out.println("Temporary directory: " + tempDir.getAbsolutePath());

        try {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(tempDirPath, permissions);
            System.out.println("Directory permissions set to 777.");
        } catch (UnsupportedOperationException e) {
            System.err.println("POSIX file permission not supported on this file system.");
        }

        return tempDir;
    }

    /**
     * WebP 애니메이션 파일을 생성합니다.
     */
    public static void makeAnimatedWebP(File tempDir, int steps, int totalDuration, ByteArrayOutputStream outputStream)
            throws IOException, InterruptedException {

        File outputFile = File.createTempFile("output", ".webp");
        outputFile.deleteOnExit();

        StringBuilder command = new StringBuilder("webpmux ");
        for (int step = 1; step <= steps; step++) {
            command.append(
                    String.format("-frame %s/frame%d.webp +%d ", tempDir.getAbsolutePath(), step, totalDuration / 10));
        }
        command.append(String.format("-loop 1 -o %s", outputFile.getAbsolutePath()));

        System.out.println("Generated command: " + command);

        ProcessBuilder processBuilder = new ProcessBuilder(command.toString().split(" "));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (InputStream inputStream = process.getInputStream()) {
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Command Output: " + output);
        }

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            System.out.println("WebP animation created: " + outputFile.getAbsolutePath());
            Files.copy(outputFile.toPath(), outputStream);
        } else {
            throw new IOException("Failed to create WebP animation. Process exited with code " + exitCode);
        }
    }

 




    public static double[][] createGaussianKernel(int radius, double sigma) {
        int size = 2 * radius + 1; // 커널 크기
        double[][] kernel = new double[size][size];
        double sum = 0.0; // 가중치 합

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double weight = Math.exp(-(x * x + y * y) / (2 * sigma * sigma)) / (2 * Math.PI * sigma * sigma);
                kernel[y + radius][x + radius] = weight;
                sum += weight;
            }
        }

        // 가중치 정규화
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] /= sum;
            }
        }

        return kernel;
    }

    public static BufferedImage applyGaussianBlur(BufferedImage inputImage, double[][] kernel) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        int radius = kernel.length / 2;

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double r = 0, g = 0, b = 0;
                double weightSum = 0;

                // 커널 적용
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int pixelX = Math.min(Math.max(x + kx, 0), width - 1);
                        int pixelY = Math.min(Math.max(y + ky, 0), height - 1);

                        Color pixelColor = new Color(inputImage.getRGB(pixelX, pixelY), true);
                        double weight = kernel[ky + radius][kx + radius];

                        r += pixelColor.getRed() * weight;
                        g += pixelColor.getGreen() * weight;
                        b += pixelColor.getBlue() * weight;
                        weightSum += weight;
                    }
                }

                int newR = (int) Math.min(Math.max(r / weightSum, 0), 255);
                int newG = (int) Math.min(Math.max(g / weightSum, 0), 255);
                int newB = (int) Math.min(Math.max(b / weightSum, 0), 255);

                outputImage.setRGB(x, y, new Color(newR, newG, newB).getRGB());
            }
        }

        return outputImage;
    }

}
