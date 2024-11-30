package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;



@Controller
public class BImageController {

    @Autowired
    private CMosaicEffectService mosaicEffectService;

    @Autowired
    private CDarkerEffectService transparencyEffectService;

    @Autowired
    private CBlurEffectService blurEffectService;

    @GetMapping("/")
    public String mainPage() {
        return "main";
    }

@GetMapping("/bV")
public String bV() {
    return  "bV";
}


    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("regions") String regionsJson,
            @RequestParam("steps") int steps,
            @RequestParam("totalDuration") int totalDuration,
            @RequestParam("initialBlockSize") int minCensored,
            @RequestParam("maxBlockSize") int maxCensored,
            @RequestParam("weight") double weight,
            @RequestParam("mosaicEffect") boolean mosaicEffect,
            @RequestParam("transparencyEffect") boolean transparencyEffect,
            @RequestParam("blurEffect") boolean blurEffect) {

        try {
            // JSON 데이터에서 선택된 영역 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            List<Region> regions = objectMapper.readValue(regionsJson, new TypeReference<List<Region>>() {});
         
            System.out.print(regions);

            // 업로드된 이미지를 BufferedImage로 변환
            BufferedImage inputImage = ImageIO.read(file.getInputStream());
            if (inputImage == null) {
                return ResponseEntity.badRequest().body(null);
            }

            // WebP 결과를 저장할 스트림 생성
            ByteArrayOutputStream webpOutputStream = new ByteArrayOutputStream();

            // 선택된 효과 처리
            applySelectedEffect(mosaicEffect, transparencyEffect, blurEffect,
                    inputImage, webpOutputStream, regions, steps, totalDuration,
                    minCensored, maxCensored, weight);

            // WebP 파일 반환
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=output.webp");
            return new ResponseEntity<>(webpOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void applySelectedEffect(boolean mosaicEffect, boolean transparencyEffect, boolean blurEffect,
                                     BufferedImage inputImage, ByteArrayOutputStream outputStream,
                                     List<Region> regions, int steps, int totalDuration,
                                     int minCensored, int maxBlockSize, double weight) throws Exception {

        if (mosaicEffect) {
            mosaicEffectService.applyEffect(inputImage, outputStream, regions, steps, totalDuration,
                    minCensored, maxBlockSize, weight);
        } else if (transparencyEffect) {
            transparencyEffectService.applyEffect(inputImage, outputStream, regions, steps, totalDuration,
                    minCensored, maxBlockSize, weight);
        } else if (blurEffect) {
            blurEffectService.applyEffect(inputImage, outputStream, regions, steps, totalDuration,
                    minCensored, maxBlockSize, weight);
        } else {
            throw new IllegalArgumentException("No effect selected.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Region {
        private int x;
        private int y;
        private int width;
        private int height;
        private int rotation;
        private String shape;

        public Region() {}

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getRotation() {
            return rotation;
        }

        public void setRotation(int rotation) {
            this.rotation = rotation;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }

        @Override
        public String toString() {
            return "Region{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    ", rotation=" + rotation +
                    ", shape='" + shape + '\'' +
                    '}';
        }
    }
}

