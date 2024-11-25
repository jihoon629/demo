package com.example.demo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import com.example.demo.ImageController.Region;


import java.io.File;
import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;


@Service
public class GaussianBlurEffectService implements ImageEffectService {
    @Override
    public void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
                            int steps, int totalDuration, int initialRadius, int maxRadius, double weight)
            throws IOException, InterruptedException {

        File tempDir = ImageUtils.makeTempDir();

        // 병렬로 프레임 생성
        IntStream.rangeClosed(1, steps).parallel().forEachOrdered(step -> {
            try {
                // 진행 비율에 따라 현재 반경 계산
                double progressRatio = (double) step / steps;
                int currentRadius = initialRadius
                        + (int) ((maxRadius - initialRadius) * Math.pow(progressRatio, weight));
                double sigma = currentRadius / 2.0;
        
                // 가우시안 커널 생성 및 블러 적용
                double[][] kernel = ImageUtils.createGaussianKernel(currentRadius, sigma);
                BufferedImage blurredImage = ImageUtils.applyGaussianBlur(inputImage, kernel);
        
                synchronized (GaussianBlurEffectService.class) {
                  //  PNG 저장
                    File frameFile = new File(tempDir, String.format("frame%d.png", step));
                    ImageIO.write(blurredImage, "png", frameFile);
        
                    // WebP 변환
                    File webpFrameFile = new File(tempDir, String.format("frame%d.webp", step));
                    new ProcessBuilder("cwebp", frameFile.getAbsolutePath(), "-o", webpFrameFile.getAbsolutePath())
                            .start()
                            .waitFor();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        

        // WebP 애니메이션 생성
        ImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);
    }
    }