package com.example.demo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.example.demo.ImageController.Region;

@Service
public class MosaicEffectService implements ImageEffectService {

    @Override
    public void applyEffect(
            BufferedImage inputImage,
            ByteArrayOutputStream outputStream,
            List<Region> regions,
            int steps,
            int totalDuration,
            int initialBlockSize,
            int maxBlockSize,
            double weight) throws IOException, InterruptedException {

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        File tempDir = ImageUtils.makeTempDir();

        // 병렬로 프레임 생성
        IntStream.rangeClosed(1, steps).parallel().forEach(step -> {
            try {
                double progressRatio = (double) step / steps;
                int currentBlockSize = initialBlockSize
                        + (int) ((maxBlockSize - initialBlockSize) * Math.pow(progressRatio, weight));

                BufferedImage mosaicFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = mosaicFrame.createGraphics();
                g2d.drawImage(inputImage, 0, 0, null);

                // 병렬로 블록 처리
                regions.parallelStream().forEach(region -> {
                    String shape = region.getShape(); // "rectangle", "circle" 등
                    for (int y = region.getY(); y < region.getY() + region.getHeight(); y += currentBlockSize) {
                        for (int x = region.getX(); x < region.getX() + region.getWidth(); x += currentBlockSize) {
                            // 블록의 중심을 계산
                            int centerX = x + currentBlockSize / 2;
                            int centerY = y + currentBlockSize / 2;

                            boolean isInShape = false;

                            switch (shape) {
                                case "circle":
                                    // 타원 검열 처리
                                    int radiusX = region.getWidth() / 2; // x축 반지름
                                    int radiusY = region.getHeight() / 2; // y축 반지름
                                    int ellipseCenterX = region.getX() + radiusX; // 타원의 중심 X 좌표
                                    int ellipseCenterY = region.getY() + radiusY; // 타원의 중심 Y 좌표

                                    // 타원 공식: (x - h)^2 / a^2 + (y - k)^2 / b^2 <= 1
                                    isInShape = Math.pow(centerX - ellipseCenterX, 2) / Math.pow(radiusX, 2)
                                            + Math.pow(centerY - ellipseCenterY, 2) / Math.pow(radiusY, 2) <= 1;
                                    break;

                                case "rectangle":
                                    // 사각형 검열 (기본 처리)
                                    isInShape = true;
                                    break;

                                case "triangle":
                                    // 삼각형 검열 (삼각형 내 포함 여부 계산)
                                    // 예시: 상단 꼭짓점 기준 좌표
                                    int triangleBaseX = region.getX();
                                    int triangleBaseY = region.getY() + region.getHeight();
                                    int apexX = region.getX() + region.getWidth() / 2;
                                    int apexY = region.getY();
                                    isInShape = isPointInTriangle(centerX, centerY, apexX, apexY, triangleBaseX,
                                            triangleBaseY, region.getX() + region.getWidth(), triangleBaseY);
                                    break;

                                default:
                                    throw new IllegalArgumentException("Unsupported shape: " + shape);
                            }

                            if (isInShape) {
                                // 평균 색상 계산 및 블록 그리기
                                Color avgColor = ImageUtils.calculateAverageColor(inputImage, x, y, currentBlockSize);
                                synchronized (g2d) {
                                    g2d.setColor(avgColor);
                                    g2d.fillRect(x, y, currentBlockSize, currentBlockSize);
                                }
                            }
                        }
                    }
                });

                g2d.dispose();

                // PNG 저장
                File frameFile = new File(tempDir, String.format("frame%d.png", step));
                ImageIO.write(mosaicFrame, "png", frameFile);

                // WebP로 변환
                File webpFrameFile = new File(tempDir, String.format("frame%d.webp", step));
                new ProcessBuilder("cwebp", frameFile.getAbsolutePath(), "-o", webpFrameFile.getAbsolutePath())

                        .start()
                        .waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // WebP 애니메이션 생성
        ImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);
    }

    public static boolean isPointInTriangle(int px, int py, int ax, int ay, int bx, int by, int cx, int cy) {
        double denominator = ((by - cy) * (ax - cx) + (cx - bx) * (ay - cy));
        double a = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / denominator;
        double b = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / denominator;
        double c = 1 - a - b;

        return a >= 0 && b >= 0 && c >= 0;
    }
}