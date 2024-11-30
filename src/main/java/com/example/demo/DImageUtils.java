package com.example.demo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class DImageUtils {

   public static File makeTempDir() throws IOException {
    // 프로젝트 내 temp 디렉토리 경로 지정
    Path baseDirPath = Paths.get("temp");
    
    // baseDirPath가 존재하지 않으면 생성
    if (!Files.exists(baseDirPath)) {
        Files.createDirectories(baseDirPath);
        System.out.println("Created base directory: " + baseDirPath.toAbsolutePath());
    }

    // temp 디렉토리 내부에 임시 디렉토리 생성
    Path tempDirPath = Files.createTempDirectory(baseDirPath, "webp_frames_");
    File tempDir = tempDirPath.toFile();
    System.out.println("Temporary directory: " + tempDir.getAbsolutePath());

    try {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
        Files.setPosixFilePermissions(tempDirPath, permissions);
        System.out.println("Directory permissions set to 777.");
    } catch (UnsupportedOperationException e) {
        System.err.println("POSIX file permission not supported on this file system.");
    }

    return tempDir;
}


    public static void convertToWebP(File inputPngFile, File outputWebPFile) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("cwebp", inputPngFile.getAbsolutePath(), "-o",
                outputWebPFile.getAbsolutePath());
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            // WebP 변환 성공 시 PNG 파일 삭제
            if (!inputPngFile.delete()) {
                throw new IOException("Failed to delete PNG file: " + inputPngFile.getAbsolutePath());
            }
        } else {
            throw new IOException("Failed to convert PNG to WebP: " + inputPngFile.getName());
        }
    }

    public static void makeAnimatedWebP(File tempDir, int steps, int totalDuration, ByteArrayOutputStream outputStream)
            throws IOException, InterruptedException {

        // 프레임 딜레이를 기본값으로 계산
        int totalDurationMs = totalDuration * 1000;
        int frameDelay = (int) Math.floor((double) totalDurationMs / steps);
        int totalCalculatedDuration = frameDelay * steps;

        // 마지막 프레임에 추가할 초과 딜레이 계산
        int lastFrameExtraDelay = totalDurationMs - totalCalculatedDuration;

        // 출력 파일 생성
        File outputFile = File.createTempFile("output", ".webp");
        outputFile.deleteOnExit();

        // 명령어 빌드
        StringBuilder command = new StringBuilder("webpmux");
        for (int step = 1; step <= steps; step++) {
            File frameFile = new File(tempDir, String.format("frame%d.webp", step));
            if (!frameFile.exists()) {
                throw new IOException("Missing frame file: " + frameFile.getAbsolutePath());
            }

            // 마지막 프레임만 초과 딜레이를 추가
            int delay = (step == steps) ? frameDelay + lastFrameExtraDelay : frameDelay;
            command.append(String.format(" -frame %s +%d", frameFile.getAbsolutePath(), delay));
        }
        command.append(String.format(" -loop 1 -o %s", outputFile.getAbsolutePath()));

        System.out.println("Generated command: " + command);

        // 프로세스 실행
        ProcessBuilder processBuilder = new ProcessBuilder(command.toString().split(" "));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // 명령어 출력 및 에러 수집
        try (InputStream inputStream = process.getInputStream()) {
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Command Output: " + output);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to create WebP animation. Process exited with code " + exitCode);
        }

        // 생성된 WebP 애니메이션을 스트림에 복사
        Files.copy(outputFile.toPath(), outputStream);
        System.out.println("WebP animation successfully created: " + outputFile.getAbsolutePath());
    }

    public static int blurMapping(int width, int height, int maxCensored) {

        return maxCensored;

    }

}
