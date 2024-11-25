package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Controller
public class ImageController {

    @Autowired
    MosaicEffectService mosaicEffectService;

    @Autowired
    TransparencyEffectService transparencyEffectService;

    @Autowired
    BlurEffectService blurEffectService;

    @GetMapping("/")
    public String mainPage() {
        return "main";
    }

    @PostMapping("/upload")
    public ResponseEntity<byte[]> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("regions") String regionsJson,
            @RequestParam("steps") int steps,
            @RequestParam("totalDuration") int totalDuration,
            @RequestParam("initialBlockSize") int initialBlockSize,
            @RequestParam("maxBlockSize") int maxBlockSize,
            @RequestParam("weight") double weight,
            @RequestParam("mosaicEffect") boolean mosaicEffect,
            @RequestParam("transparencyEffect") boolean transparencyEffect,
            @RequestParam("blurEffect") boolean blurEffect,
            Model model) {
        try {
            // JSON 데이터에서 선택된 영역을 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            List<Region> regions = objectMapper.readValue(regionsJson, new TypeReference<List<Region>>() {
            });

            // 업로드된 파일을 BufferedImage로 변환
            BufferedImage inputImage = ImageIO.read(file.getInputStream());
            if (inputImage == null) {
                model.addAttribute("message", "Invalid image file.");
                return ResponseEntity.badRequest().build();
            }

            // WebP 결과를 저장할 스트림 생성
            ByteArrayOutputStream webpOutputStream = new ByteArrayOutputStream();

            // 모자이크 효과 처리
            if (mosaicEffect) {
                mosaicEffectService.applyEffect(inputImage, webpOutputStream, regions, steps, totalDuration,
                        initialBlockSize, maxBlockSize, weight);
            }
            // 반투명 효과 처리
            else if (transparencyEffect) {
                transparencyEffectService.applyEffect(inputImage, webpOutputStream, regions, steps, totalDuration,
                        maxBlockSize, maxBlockSize, weight);
            }
            // 블러 효과 처리
            else if (blurEffect) {
                blurEffectService.applyEffect(inputImage, webpOutputStream, regions, steps, totalDuration,
                        initialBlockSize, maxBlockSize, weight);
            }
            // 효과 선택 안 됨
            else {
                throw new IllegalArgumentException("No effect selected.");
            }

            // WebP 파일 반환
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=output.webp");
            return new ResponseEntity<>(webpOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Region {
        private int x;
        private int y;
        private int width;
        private int height;
        private String shape; // 추가
    
        // 기본 생성자, getter, setter, toString 메서드 등
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
                    ", shape='" + shape + '\'' +
                    '}';
        }
    }
    

}
