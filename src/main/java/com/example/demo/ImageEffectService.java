package com.example.demo;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.example.demo.ImageController.Region;

public interface ImageEffectService {

    
 void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
                     int steps, int totalDuration, int initialBlockSize, int maxBlockSize, double weight) 
                     throws IOException, InterruptedException;
    
}
