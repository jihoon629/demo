package com.example.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


@Controller
public class ImageController {

    // 업로드 폼을 보여주는 메인 페이지
    @GetMapping("/")
    public String mainPage() {
        return "main"; // index.html을 반환 (src/main/resources/templates/)
    }

    // 업로드된 파일 처리
    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadImage(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // 1. 업로드된 파일을 BufferedImage로 변환
            BufferedImage inputImage = ImageIO.read(file.getInputStream());
            if (inputImage == null) {
                model.addAttribute("message", "Invalid image file.");
                return ResponseEntity.badRequest().build();
            }

            // 2. GIF 생성 (모자이크 처리)
            ByteArrayOutputStream gifOutputStream = new ByteArrayOutputStream();
            generateMosaicGif(inputImage, gifOutputStream);

            // 3. HTTP 응답으로 GIF 반환
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=output.gif");
            return new ResponseEntity<>(gifOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void generateMosaicGif(BufferedImage inputImage, ByteArrayOutputStream outputStream) throws IOException {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
    
        // GIF 인코더 초기화
        com.madgag.gif.fmsware.AnimatedGifEncoder gifEncoder = new com.madgag.gif.fmsware.AnimatedGifEncoder();
        gifEncoder.start(outputStream);
        gifEncoder.setRepeat(0); // 무한 반복
    
        int steps = 50;          // 모자이크 단계 수
        int totalDuration = 20000; // 총 20초
        int delayPerFrame = totalDuration / steps; // 프레임 딜레이 계산
        gifEncoder.setDelay(delayPerFrame);
    
        int initialBlockSize = 1; // 최소 블록 크기
        int maxBlockSize = 13;    // 최대 블록 크기
    
        double weight = 0.7; // 초반 속도 조정 (0.0 ~ 1.0, 작을수록 초반 빠름)
    
        // 1. 점진적으로 모자이크 처리
        for (int step = 1; step <= steps; step++) {
            // 블록 크기 계산 (속도 조정 추가)
            double progressRatio = (double) step / steps;
            int currentBlockSize = initialBlockSize + (int) ((maxBlockSize - initialBlockSize) * Math.pow(progressRatio, weight));
    
            // 현재 단계의 모자이크 프레임 생성
            BufferedImage mosaicFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = mosaicFrame.createGraphics();
    
            // 모자이크 그리기
            for (int y = 0; y < height; y += currentBlockSize) {
                for (int x = 0; x < width; x += currentBlockSize) {
                    Color avgColor = calculateAverageColor(inputImage, x, y, currentBlockSize);
                    g2d.setColor(avgColor);
                    g2d.fillRect(x, y, currentBlockSize, currentBlockSize);
                }
            }
            g2d.dispose();
    
            // 프레임 추가
            gifEncoder.addFrame(mosaicFrame);
    
            // 진행 상태 로그
            System.out.printf("Progress: %.2f%% - Mosaic Frame %d/%d%n", (step / (double) steps) * 100, step, steps);
        }
    
        // 2. 완전 모자이크 상태 유지 (10초)
        BufferedImage finalMosaicFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D finalG2D = finalMosaicFrame.createGraphics();
    
        // 완전 모자이크 상태 생성
        for (int y = 0; y < height; y += maxBlockSize) {
            for (int x = 0; x < width; x += maxBlockSize) {
                Color avgColor = calculateAverageColor(inputImage, x, y, maxBlockSize);
                finalG2D.setColor(avgColor);
                finalG2D.fillRect(x, y, maxBlockSize, maxBlockSize);
            }
        }
        finalG2D.dispose();
    
        // 동일한 프레임 반복 추가
        int holdFrames = (15 * 1000) / delayPerFrame; // 10초 동안 유지할 프레임 수
        for (int i = 0; i < holdFrames; i++) {
            gifEncoder.addFrame(finalMosaicFrame);
    
            // 진행 상태 로그
            double progress = 100.0 + (i + 1) / (double) holdFrames * 10;
            System.out.printf("Progress: %.2f%% - Holding Frame %d/%d%n", progress, i + 1, holdFrames);
        }
    
        gifEncoder.finish();
        System.out.println("GIF generation complete.");
    }
    
    private Color calculateAverageColor(BufferedImage image, int startX, int startY, int blockSize) {
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
    
        // 평균 색상 계산
        return new Color(r / count, g / count, b / count);
    }
    
    
    }
