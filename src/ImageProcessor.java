import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import edu.wpi.first.wpilibj.networktables.*;

public class ImageProcessor {
    // I'm assuming these are RGB values - Peter
    // Some deep purple (?)
    private final Scalar LOWER_BOUND = new Scalar(20, 10, 100);
    // Some bright blue (?)
    private final Scalar UPPER_BOUND = new Scalar(110, 255, 255);
    // Pure white
    private final Scalar WHITE = new Scalar(255, 255, 255);
    // Pure blue
    private final Scalar BLUE = new Scalar(0, 0, 255);
    
    public BufferedImage toBufferedImage(Mat matrix){
        // Set up network connection to RB Camera
        NetworkTable.setClientMode();
        NetworkTable.setIPAddress("10.40.79.2");
        NetworkTable table = NetworkTable.getTable("raspCameraTest");

        // Grayscale or BGR
        int type = getBufferedImageType(matrix);

        // Make a new Matrix using the HSV color space
        Mat hsv = new Mat();
        Imgproc.cvtColor(matrix, hsv, Imgproc.COLOR_RGB2HSV);
        //Imgproc.cvtColor(hsv, matrix, Imgproc.COLOR_HSV2RGB);
       // Scalar lower_bound = new Scalar(4,50,50); //blue
       // Scalar upper_bound = new Scalar(10,255,255); //blue

        // tbh no idea
        Mat mask = new Mat();
        Core.inRange(hsv, LOWER_BOUND, UPPER_BOUND, mask);
       //matrix.copyTo(matrix,mask);
       // Imgproc.cvtColor(matrix, matrix, Imgproc.COLOR_HSV2RGB);

        // Are we on Java 8? Can we use shorthand List<MatOfPoint> countours = new ArrayList<>();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat mHierarchy = new Mat();
        Mat blankImg = new Mat( matrix.rows(), matrix.cols(),  CvType.CV_8UC3, WHITE);
        Imgproc.findContours(mask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Create an iterator of countor mats
        Iterator<MatOfPoint> countoursIterator = contours.iterator();
        
        //int idx = 0;
        double greatestArea = 0;
        double secondGreatestArea = 0;

        // Java 8 ArrayList shorthand?
        ArrayList<MatOfPoint> contoursToDraw = new ArrayList<MatOfPoint>();
        //contoursToDraw.add(null);
        //contoursToDraw.add(null);

        // find contours to draw and the 1st / 2nd largest contours
        while (contoursIterator.hasNext()) {
            //idx=idx+1;
            MatOfPoint contour = contoursIterator.next();

            if (Imgproc.contourArea(contour) >= greatestArea){
                contoursToDraw = findBiggerCountour(contour, contoursToDraw);
                secondGreatestArea = greatestArea; 
                greatestArea = Imgproc.contourArea(contour);
            }
            else if (Imgproc.contourArea(contour) <= greatestArea && Imgproc.contourArea(contour) >= secondGreatestArea) {
                contoursToDraw = findMiddleContour(contour, contoursToDraw);
                secondGreatestArea = Imgproc.contourArea(contour);
            }
        }
        
        // Debug print of the above
        System.out.println("Greatest Area: " + greatestArea + "\nSecond Greatest Area: " + secondGreatestArea);

        // find sum of something (?)
        double sum = findSum(contoursToDraw);
        
        table.putNumber("Center", sum/contoursToDraw.size());
        //blankImg.copyTo(matrix);

        int bufferSize = matrix.channels() * matrix.cols() * matrix.rows();
        byte[] buffer = new byte[bufferSize];
        matrix.get(0, 0, buffer); // get all the pixels
        BufferedImage image = new BufferedImage(matrix.cols(),matrix.rows(), type);
        
        // why is a final variable declared here? - Peter
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
        
        return image;
    }
    
    private int getBufferedImageType(Mat matrix) {
        return matrix.channels() > 1
            ? Bufferedimage.TYPE_3BYTE_BGR
            : BufferedImage.TYPE_BYTE_GRAY;
    }
    
    private ArrayList<MatOfPoint> findBiggerContour (MatOfPoint contour, ArrayList<MatOfPoint> countourList) {
        if (contourList.size() == 2) {
            contourList.set(1, contourList.get(0));
            contourList.set(0, contour);
        }
        else if (contourList.size() == 1) {
            contourList.add(contourList.get(0));
            contourList.set(0, contour);
        }
        else if (contourList.size() == 0) {
            contourList.add(contour);
        }
        
        return contourList;
    }
    
    private ArrayList<MatOfPoint> findMiddleContour (MatOfPoint contour, ArrayList<MatOfPoint> contourList) {
        if (contourList.size() == 1) {
            contourList.add(contour);
        }
        else {
            contourList.set(1, contour);
        }
        
        return contourList;
    }
    
    private double findSum(ArrayList<MatOfPoint> contourList) {
        double sum = 0; 
        for (int i = 0; i < contourList.size(); i++) {
            // draw blue contours
            Imgproc.drawContours(matrix, contoursToDraw, i, BLUE);
            
            Moments moments = Imgproc.moments(contourList.get(i));
            
            // do center point
            Point centroid = new Point();
            centroid.x = moments.get_m10() / moments.get_m00();
            centroid.y = moments.get_m01() / moments.get_m00();

            // debug print center
            System.out.printf("%8.2f %8.2f\n", centroid.x, centroid.y);
            
            // add to sum
            sum += centroid.x;
        }
        
        return sum;
    }
}
