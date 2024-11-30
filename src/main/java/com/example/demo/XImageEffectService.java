package com.example.demo;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.example.demo.BImageController.Region;

public interface XImageEffectService {

    
 void applyEffect(BufferedImage inputImage, ByteArrayOutputStream outputStream, List<Region> regions,
                     int steps, int totalDuration, int minCensored, int maxCensored, double weight) 
                     throws IOException, InterruptedException;
    
}
