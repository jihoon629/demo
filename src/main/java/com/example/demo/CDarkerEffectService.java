package com.example.demo;

import org.springframework.stereotype.Service;

import com.example.demo.BImageController.Region;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class CDarkerEffectService implements XImageEffectService {

    @Override
    public void applyEffect(
            BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
            int steps, int totalDuration, int minCensored, int maxCensored, double weight)
            throws IOException, InterruptedException {

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        // 투명도 관리 배열
        int[][] transparencyBuffer = new int[height][width];
        File tempDir = DImageUtils.makeTempDir();

        // 병렬로 step 처리
        IntStream.rangeClosed(1, steps).parallel().forEach(step -> {
            try {
                // step에 기반하여 현재 opacity 계산
                double progressRatio = Math.pow((double) step / steps, weight);
                int currentCensored = minCensored + (int) (((int) (maxCensored * 2.54) - minCensored) * progressRatio);

                // 프레임 생성
                BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = frame.createGraphics();
                g2d.drawImage(inputImage, 0, 0, null);

                // 병렬로 영역 처리
                long start = System.nanoTime();

                regions.parallelStream()
                        .forEach(region -> processRegionDarker(inputImage, frame, region, transparencyBuffer,
                                currentCensored, width, height));
                long end = System.nanoTime();
                System.out.println(String.format("Step %d, Parallel processing time: %d ns", step, (end - start)));

                // 프레임 저장
                File frameFile = new File(tempDir, String.format("frame%d.png", step));
                ImageIO.write(frame, "png", frameFile);

                System.out.println(String.format("Step %d, Block size: %d", step, currentCensored));

                // WebP 변환
                DImageUtils.convertToWebP(frameFile, new File(tempDir, String.format("frame%d.webp", step)));

            } catch (IOException | InterruptedException e) {
                throw new UncheckedIOException((IOException) e);
            }
        });

        // WebP 애니메이션 생성
        DImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);
    }

    private void processRegionDarker(BufferedImage inputImage, BufferedImage frame, Region region,
            int[][] globalTransparencyBuffer, int currentOpacity, int width, int height) {

        // 회전된 영역의 경계 박스 계산
        Rectangle boundingBox = DStampUtils.getBoundingBox(region, width, height);

        // 지역 버퍼 초기화
        int[][] localBuffer = new int[boundingBox.height][boundingBox.width];

        Shape rotatedShape = DStampUtils.getRotatedShape(region);

        // 경계 박스 내의 픽셀 병렬 처리
        IntStream.range(0, boundingBox.height).parallel().forEach(localY -> {
            int globalY = boundingBox.y + localY;
            for (int localX = 0; localX < boundingBox.width; localX++) {
                int globalX = boundingBox.x + localX;
        
                // 회전된 영역 내부 픽셀만 처리
                if (rotatedShape.contains(globalX, globalY)) {
                    int newAlpha = Math.max(localBuffer[localY][localX], currentOpacity);
                    localBuffer[localY][localX] = newAlpha;
        
                    // 프레임에 적용
                    applyTransparency(inputImage, frame, globalX, globalY, newAlpha);
                }
            }
        });
        

        // 글로벌 버퍼와 병합
        IntStream.range(0, boundingBox.height).parallel().forEach(localY -> {
            int globalY = boundingBox.y + localY;
            for (int localX = 0; localX < boundingBox.width; localX++) {
                int globalX = boundingBox.x + localX;

                synchronized (globalTransparencyBuffer[globalY]) {
                    globalTransparencyBuffer[globalY][globalX] = Math.max(
                            globalTransparencyBuffer[globalY][globalX], localBuffer[localY][localX]);
                }
            }
        });
    }

    private void applyTransparency(BufferedImage inputImage, BufferedImage frame, int x, int y, int alpha) {
        int originalRgb = inputImage.getRGB(x, y);
        Color originalColor = new Color(originalRgb, true);

        // 투명도 블렌딩 계산
        int blendedRed = (originalColor.getRed() * (255 - alpha)) / 255;
        int blendedGreen = (originalColor.getGreen() * (255 - alpha)) / 255;
        int blendedBlue = (originalColor.getBlue() * (255 - alpha)) / 255;

        Color blendedColor = new Color(blendedRed, blendedGreen, blendedBlue, 255);
        frame.setRGB(x, y, blendedColor.getRGB());
    }

}
