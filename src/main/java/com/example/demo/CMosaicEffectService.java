package com.example.demo;

import org.springframework.stereotype.Service;
import com.example.demo.BImageController.Region;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class CMosaicEffectService implements XImageEffectService {

    @Override
    public void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
            int steps, int totalDuration, int minCensored, int maxCensored, double weight)
            throws IOException, InterruptedException {

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        File tempDir = DImageUtils.makeTempDir();

        IntStream.rangeClosed(1, steps).parallel().forEach(step -> {
            try {
                double progressRatio = Math.pow((double) step / steps, weight);
                int currentCensored = minCensored
                        + (int) ((mosaicMapping(width, height, maxCensored) - minCensored) * progressRatio);

                BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = frame.createGraphics();
                g2d.drawImage(inputImage, 0, 0, null);
                long start = System.nanoTime();
                regions.parallelStream().forEach(
                        region -> processRegionMosaic(inputImage, frame, region, currentCensored));
                long end = System.nanoTime();
                System.out.println(String.format("Step %d, Parallel processing time: %d ns", step, (end - start)));

                g2d.dispose();

                File frameFile = new File(tempDir, String.format("frame%d.png", step));
                ImageIO.write(frame, "png", frameFile);

                System.out.println(String.format("Step %d, Block size: %d", step, currentCensored));

                DImageUtils.convertToWebP(frameFile, new File(tempDir, String.format("frame%d.webp", step)));
            } catch (IOException e) {
                throw new RuntimeException("Error processing step " + step, e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        DImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);
    }

    private void processRegionMosaic(BufferedImage inputImage, BufferedImage outputImage, Region region,
            int blockSize) {
        // 회전된 영역의 Bounding Box를 계산
        Rectangle boundingBox = DStampUtils.getBoundingBox(region, inputImage.getWidth(), inputImage.getHeight());
        Shape rotatedShape = DStampUtils.getRotatedShape(region);

        for (int y = boundingBox.y; y < boundingBox.y + boundingBox.height; y += blockSize) {
            for (int x = boundingBox.x; x < boundingBox.x + boundingBox.width; x += blockSize) {
                // 블록 중심이 회전된 영역 안에 있는지 확인
                if (rotatedShape.contains(x + blockSize / 2.0, y + blockSize / 2.0)) {
                    // 블록 내 평균 색상 계산
                    Color avgColor = calculateAverageColor(inputImage, x, y, blockSize);
        
                    // 픽셀 직접 수정
                    fillBlock(outputImage, x, y, blockSize, avgColor);
                }
            }}
        }
        

    /**
     * 블록 내부를 평균 색상으로 채우는 메서드
     */
    private void fillBlock(BufferedImage image, int startX, int startY, int blockSize, Color color) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = startY; y < startY + blockSize && y < height; y++) {
            for (int x = startX; x < startX + blockSize && x < width; x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
    }

    private static Color calculateAverageColor(BufferedImage image, int x, int y, int blockSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(width, x + blockSize);
        int endY = Math.min(height, y + blockSize);

        long sumRed = 0, sumGreen = 0, sumBlue = 0, pixelCount = 0;

        for (int row = startY; row < endY; row++) {
            for (int col = startX; col < endX; col++) {
                int rgb = image.getRGB(col, row);
                sumRed += (rgb >> 16) & 0xFF;
                sumGreen += (rgb >> 8) & 0xFF;
                sumBlue += rgb & 0xFF;
                pixelCount++;
            }
        }

        int avgRed = (int) (sumRed / pixelCount);
        int avgGreen = (int) (sumGreen / pixelCount);
        int avgBlue = (int) (sumBlue / pixelCount);

        return new Color(avgRed, avgGreen, avgBlue);
    }

    private static int mosaicMapping(int width, int height, int maxCensored) {

        int maxPossibleBlockSize = Math.min(width, height) / 10;

        // 사용자 입력 (1~100)을 블록 크기 (1~maxPossibleBlockSize)로 매핑
        return 1 + (maxCensored - 1) * (maxPossibleBlockSize - 1) / 99;

    }

}
