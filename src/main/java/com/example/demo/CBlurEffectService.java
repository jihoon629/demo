package com.example.demo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import com.example.demo.BImageController.Region;

@Service
public class CBlurEffectService implements XImageEffectService {

    @Override
    public void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
            int steps, int totalDuration, int minBlurRadius, int maxCensored, double weight)
            throws IOException, InterruptedException {

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        File tempDir = DImageUtils.makeTempDir();

        IntStream.rangeClosed(1, steps).forEach(step -> {
            try {
                double progressRatio = Math.pow((double) step / steps, weight);
                int currentCensored = minBlurRadius
                        + (int) ((mosaicMapping(width, height, maxCensored) - minBlurRadius) * progressRatio);

                BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = frame.createGraphics();
                g2d.drawImage(inputImage, 0, 0, null);
                long start = System.nanoTime();
                regions.parallelStream().forEach(region -> {
                    DGaussianBlurService.applyBlurToRegion(frame, frame, regions, currentCensored);
                });
                long end = System.nanoTime();
                System.out.println(String.format("Step %d, Parallel processing time: %d ns", step, (end - start)));
                g2d.dispose();

                File frameFile = new File(tempDir, String.format("frame%d.png", step));
                ImageIO.write(frame, "png", frameFile);

                System.out.println(String.format("Step %d, Blur radius: %d", step, currentCensored));

                DImageUtils.convertToWebP(frameFile, new File(tempDir, String.format("frame%d.webp", step)));
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error processing step " + step, e);
            }
        });

        DImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);
    }

    private static int mosaicMapping(int width, int height, int maxCensored) {
        // 최대 가능한 블록 크기 한 번 계산
        int maxPossibleBlockSize = Math.min(width, height) / 10;
    
        // 사용자 입력 (1~100)을 블록 크기 (1~maxPossibleBlockSize)로 매핑
        return 1 + (maxCensored - 1) * (maxPossibleBlockSize - 1) / 99;
    }
    
}
