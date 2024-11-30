package com.example.demo;

import com.example.demo.BImageController.Region;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

public class DStampUtils {

    public static Shape getRotatedShape(Region region) {
        int centerX = region.getX() + region.getWidth() / 2;
        int centerY = region.getY() + region.getHeight() / 2;
        double angle = Math.toRadians(region.getRotation());
    
        Shape baseShape;
    
        switch (region.getShape()) {
            case "rectangle":
                baseShape = new Rectangle(region.getX(), region.getY(), region.getWidth(), region.getHeight());
                break;
            case "circle":
                baseShape = new Ellipse2D.Double(region.getX(), region.getY(), region.getWidth(), region.getHeight());
                break;
            case "triangle":
                baseShape = createTriangle(region);
                break;
            default:
                throw new IllegalArgumentException("Unsupported shape: " + region.getShape());
        }
    
        // 회전을 적용
        AffineTransform transform = AffineTransform.getRotateInstance(angle, centerX, centerY);
        return transform.createTransformedShape(baseShape);
    }
    
    private static Shape createTriangle(Region region) {
        int baseX = region.getX();
        int baseY = region.getY();
        int width = region.getWidth();
        int height = region.getHeight();
    
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(baseX + width / 2.0, baseY); // 꼭짓점 (상단)
        triangle.lineTo(baseX, baseY + height);     // 좌측 하단
        triangle.lineTo(baseX + width, baseY + height); // 우측 하단
        triangle.closePath();
        return triangle;
    }


    public static Rectangle getBoundingBox(Region region, int imageWidth, int imageHeight) {
        Shape rotatedShape = getRotatedShape(region);
        Rectangle bounds = rotatedShape.getBounds();
    
        // 이미지 경계 내로 제한
        int minX = Math.max(0, bounds.x);
        int minY = Math.max(0, bounds.y);
        int maxX = Math.min(imageWidth, bounds.x + bounds.width);
        int maxY = Math.min(imageHeight, bounds.y + bounds.height);
    
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }
    
public static Point rotatePoint(int x, int y, int centerX, int centerY, double angle) {
    double radians = Math.toRadians(angle);
    double cos = Math.cos(radians);
    double sin = Math.sin(radians);

    int dx = x - centerX;
    int dy = y - centerY;

    int rotatedX = (int) (dx * cos - dy * sin + centerX);
    int rotatedY = (int) (dx * sin + dy * cos + centerY);

    return new Point(rotatedX, rotatedY);
}

static boolean isPointInTriangle(int px, int py, int ax, int ay, int bx, int by, int cx, int cy) {
    double epsilon = 1e-6; // 허용 오차
    double denominator = ((by - cy) * (ax - cx) + (cx - bx) * (ay - cy));
    if (Math.abs(denominator) < epsilon) return false; // 삼각형이 아닌 경우

    double a = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / denominator;
    double b = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / denominator;
    double c = 1 - a - b;

    return a >= -epsilon && b >= -epsilon && c >= -epsilon;
}

}
