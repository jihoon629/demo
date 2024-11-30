package com.example.demo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.IntStream;

import com.example.demo.BImageController.Region;

public class DGaussianBlurService {

    public static void applyBlurToRegion(BufferedImage inputImage, BufferedImage outputImage, List<Region> regions,
            int currentCensored) {
        BufferedImage blurredImage = applyGaussianBlur(inputImage, currentCensored);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, null); // 원본 이미지 복사

        regions.parallelStream().forEach(region -> {
            applyRegionMask(inputImage, blurredImage, region, g2d, inputImage.getWidth(), inputImage.getHeight());
        });

        g2d.dispose();
    }


    private static BufferedImage applyGaussianBlur(BufferedImage inputImage, int currentCensored) {
        int kernelSize = currentCensored * 2 + 1;
        float[] kernel = create1DGaussianKernel(kernelSize, currentCensored / 2.0f);
    
        // 한 번의 루프로 수직 및 수평 필터를 동시에 적용
        return applySeparableConvolve(inputImage, kernel);
    }
    
    private static BufferedImage applySeparableConvolve(BufferedImage inputImage, float[] kernel) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, inputImage.getType());
        int kernelMid = kernel.length / 2;
    
        // 픽셀 데이터를 한 번에 처리
        int[] srcPixels = inputImage.getRGB(0, 0, width, height, null, 0, width);
        int[] dstPixels = new int[srcPixels.length];
    
        // 가로 방향 블러 처리
        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                float redSum = 0, greenSum = 0, blueSum = 0;
                for (int k = 0; k < kernel.length; k++) {
                    int offset = x + k - kernelMid;
                    if (offset >= 0 && offset < width) {
                        int rgb = srcPixels[y * width + offset];
                        Color color = new Color(rgb, true);
    
                        float weight = kernel[k];
                        redSum += color.getRed() * weight;
                        greenSum += color.getGreen() * weight;
                        blueSum += color.getBlue() * weight;
                    }
                }
    
                int red = Math.min(Math.max((int) redSum, 0), 255);
                int green = Math.min(Math.max((int) greenSum, 0), 255);
                int blue = Math.min(Math.max((int) blueSum, 0), 255);
                dstPixels[y * width + x] = new Color(red, green, blue).getRGB();
            }
        });
    
        // 세로 방향 블러 처리
        int[] finalPixels = new int[dstPixels.length];
        IntStream.range(0, width).parallel().forEach(x -> {
            for (int y = 0; y < height; y++) {
                float redSum = 0, greenSum = 0, blueSum = 0;
                for (int k = 0; k < kernel.length; k++) {
                    int offset = y + k - kernelMid;
                    if (offset >= 0 && offset < height) {
                        int rgb = dstPixels[offset * width + x];
                        Color color = new Color(rgb, true);
    
                        float weight = kernel[k];
                        redSum += color.getRed() * weight;
                        greenSum += color.getGreen() * weight;
                        blueSum += color.getBlue() * weight;
                    }
                }
    
                int red = Math.min(Math.max((int) redSum, 0), 255);
                int green = Math.min(Math.max((int) greenSum, 0), 255);
                int blue = Math.min(Math.max((int) blueSum, 0), 255);
                finalPixels[y * width + x] = new Color(red, green, blue).getRGB();
            }
        });
    
        outputImage.setRGB(0, 0, width, height, finalPixels, 0, width);
        return outputImage;
    }
    
    private static float[] create1DGaussianKernel(int size, float sigma) {
        float[] kernel = new float[size];
        float sum = 0;
        int half = size / 2;
        double sigma2 = 2 * sigma * sigma;

        for (int i = -half; i <= half; i++) {
            float value = (float) Math.exp(-(i * i) / sigma2);
            kernel[i + half] = value;
            sum += value;
        }

        // Normalize kernel
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }


private static void applyRegionMask(BufferedImage inputImage, BufferedImage blurredImage, Region region,
                                    Graphics2D g2d, int width, int height) {
    Shape rotatedShape = DStampUtils.getRotatedShape(region);
    Rectangle boundingBox = rotatedShape.getBounds();

    for (int y = boundingBox.y; y < boundingBox.y + boundingBox.height; y++) {
        for (int x = boundingBox.x; x < boundingBox.x + boundingBox.width; x++) {
            if (rotatedShape.contains(x, y)) {
                int rgb = blurredImage.getRGB(x, y);
                inputImage.setRGB(x, y, rgb);
            }
        }
    }
}

}
