package com.example.demo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.example.demo.ImageController.Region;

@Service
public class BlurEffectService implements ImageEffectService {

    @Override
    public void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
            int steps, int totalDuration, int initialBlockSize, int maxBlockSize, double weight)
            throws IOException, InterruptedException {

        int width = inputImage.getWidth();
        int height = inputImage.getHeight();

        // 1. 임시 디렉토리 생성
        File tempDir = ImageUtils.makeTempDir();

        // 2. 점진적으로 블러 처리하여 프레임 저장
        for (int step = 1; step <= steps; step++) {
            double progressRatio = (double) step / steps;
            int currentBlurRadius = initialBlockSize
                    + (int) ((maxBlockSize - initialBlockSize) * Math.pow(progressRatio, weight));

            BufferedImage blurFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = blurFrame.createGraphics();
            g2d.drawImage(inputImage, 0, 0, null);

            // 모든 선택된 영역에 대해 블러 처리
            for (Region region : regions) {
                BufferedImage subImage = inputImage.getSubimage(region.getX(), region.getY(),
                        Math.min(region.getWidth(), width - region.getX()),
                        Math.min(region.getHeight(), height - region.getY()));

                // 블러 처리된 영역
                BufferedImage blurredRegion = applyGaussianBlur(subImage, currentBlurRadius);

                g2d.drawImage(blurredRegion, region.getX(), region.getY(), null);
            }
            g2d.dispose();

            // 프레임 저장
            File frameFile = new File(tempDir, String.format("frame%d.png", step));
            ImageIO.write(blurFrame, "png", frameFile);

            // PNG를 WebP로 변환
            File webpFrameFile = new File(tempDir, String.format("frame%d.webp", step));
            new ProcessBuilder("cwebp", frameFile.getAbsolutePath(), "-o", webpFrameFile.getAbsolutePath()).start()
                    .waitFor();
        }

        // 3. WebP 애니메이션 생성
        ImageUtils.makeAnimatedWebP(tempDir, steps, totalDuration, outputStream);
    }

    /**
     * 지정된 반지름으로 가우시안 블러를 적용합니다.
     *
     * @param image      입력 이미지
     * @param blurRadius 블러 반지름 (커질수록 더 흐려짐)
     * @return 블러 처리된 이미지
     */
    private BufferedImage applyGaussianBlur(BufferedImage image, int blurRadius) {
        // 블러 반지름 기반의 커널 크기 계산
        int kernelSize = blurRadius * 2 + 1;

        // 가우시안 커널 생성
        float[] kernelData = createGaussianKernel(blurRadius, kernelSize);
        Kernel kernel = new Kernel(kernelSize, kernelSize, kernelData);

        // ConvolveOp를 사용한 블러 처리
        ConvolveOp blurOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        return blurOp.filter(image, null);
    }

    /**
     * 가우시안 커널 생성 메서드
     *
     * @param radius 블러 반지름
     * @param size   커널 크기
     * @return 가우시안 커널 데이터
     */
    private float[] createGaussianKernel(int radius, int size) {
        float[] kernel = new float[size * size];
        float sigma = radius / 3.0f;
        float mean = size / 2.0f;
        float sum = 0.0f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float value = (float) Math.exp(-0.5 * (Math.pow((x - mean) / sigma, 2.0)
                        + Math.pow((y - mean) / sigma, 2.0)));
                kernel[y * size + x] = value;
                sum += value;
            }
        }

        // 정규화
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }

}
