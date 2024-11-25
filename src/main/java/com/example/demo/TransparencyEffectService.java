package com.example.demo;


import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import com.example.demo.ImageController.Region;

@Service
public class TransparencyEffectService implements ImageEffectService {

    @Override
    public void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
            int steps, int totalDuration, int initialBlockSize, int maxBlockSize, double weight)
            throws IOException, InterruptedException {



                int width = inputImage.getWidth();
    int height = inputImage.getHeight();

    // 투명도 관리 버퍼 생성
    int[][] transparencyBuffer = new int[height][width];

    File tempDir = ImageUtils.makeTempDir();




    for (int step = 1; step <= steps; step++) {
        // 가중치를 적용한 진행 비율 계산
        double progressRatio = Math.pow((double) step / steps, weight);
        int currentOpacity = (int) (maxBlockSize * progressRatio);

        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = frame.createGraphics();

        // 원본 이미지 복사
        g2d.drawImage(inputImage, 0, 0, null);

        // 선택된 영역에 반투명 검정색 적용
        for (Region region : regions) {
            for (int y = region.getY(); y < region.getY() + region.getHeight(); y++) {
                for (int x = region.getX(); x < region.getX() + region.getWidth(); x++) {
                    if (x < 0 || y < 0 || x >= width || y >= height) continue;

                    // 기존 픽셀 색상
                    int originalRgb = inputImage.getRGB(x, y);
                    Color originalColor = new Color(originalRgb, true);

                    // 기존 투명도와 현재 투명도를 비교하여 최종 투명도 계산
                    transparencyBuffer[y][x] = Math.max(transparencyBuffer[y][x], currentOpacity);
                    int alpha = transparencyBuffer[y][x];

                    // 알파 블렌딩: 원본 이미지와 검정색 혼합
                    int blendedRed = (originalColor.getRed() * (255 - alpha)) / 255;
                    int blendedGreen = (originalColor.getGreen() * (255 - alpha)) / 255;
                    int blendedBlue = (originalColor.getBlue() * (255 - alpha)) / 255;

                    Color blendedColor = new Color(blendedRed, blendedGreen, blendedBlue, 255);
                    frame.setRGB(x, y, blendedColor.getRGB());
                }
            }
        }

        g2d.dispose();

        // 프레임 저장
        File frameFile = new File(tempDir, String.format("frame%d.png", step));
        ImageIO.write(frame, "png", frameFile);

        // WebP 변환
        File webpFrameFile = new File(tempDir, String.format("frame%d.webp", step));
        ProcessBuilder convertBuilder = new ProcessBuilder(
                "cwebp",
                frameFile.getAbsolutePath(),
                "-o", webpFrameFile.getAbsolutePath());
        Process convertProcess = convertBuilder.start();
        if (convertProcess.waitFor() != 0) {
            throw new IOException("Failed to convert PNG to WebP for frame: " + step);
        }
    }

    ImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);

    }
    
    

}