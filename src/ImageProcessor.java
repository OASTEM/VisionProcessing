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
	
	
        public BufferedImage toBufferedImage(Mat matrix){
        	
        	NetworkTable.setClientMode();
        	NetworkTable.setIPAddress("10.40.79.2");
        	NetworkTable table = NetworkTable.getTable("raspCameraTest");
        	
                int type = BufferedImage.TYPE_BYTE_GRAY;
                
                if ( matrix.channels() > 1 ) {
                	type = BufferedImage.TYPE_3BYTE_BGR;
                }
                
                Mat hsv = new Mat();
                Imgproc.cvtColor(matrix, hsv, Imgproc.COLOR_RGB2HSV);
                //Imgproc.cvtColor(hsv, matrix, Imgproc.COLOR_HSV2RGB);
               // Scalar lower_bound = new Scalar(4,50,50); //blue
               // Scalar upper_bound = new Scalar(10,255,255); //blue
                
                Scalar lower_bound = new Scalar(20,10,100);
                Scalar upper_bound = new Scalar(110,255,255);
                
                Mat mask = new Mat();
                Core.inRange(hsv, lower_bound, upper_bound, mask);
               //matrix.copyTo(matrix,mask);
               // Imgproc.cvtColor(matrix, matrix, Imgproc.COLOR_HSV2RGB);
                
                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                Mat mHierarchy = new Mat();
                Mat blankImg = new Mat( matrix.rows(), matrix.cols(),  CvType.CV_8UC3, new Scalar(255,255,255));
                Imgproc.findContours(mask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                
                
                Iterator<MatOfPoint> each = contours.iterator();
                //int idx = 0;
                double greatestArea = 0;
                double secondGreatestArea = 0;
                
                ArrayList<MatOfPoint> contoursToDraw = new ArrayList<MatOfPoint>();
                //contoursToDraw.add(null);
                //contoursToDraw.add(null);
                
                while (each.hasNext()) {
                	//idx=idx+1;
                	MatOfPoint contour = each.next();
                    	
                    if (Imgproc.contourArea(contour) >= greatestArea){
                    	if (contoursToDraw.size() == 2) {
                    		contoursToDraw.set(1, contoursToDraw.get(0));
                    		contoursToDraw.set(0, contour);
                    		secondGreatestArea = greatestArea; 
                    		greatestArea = Imgproc.contourArea(contour);
                    	}
                    	else if (contoursToDraw.size() == 1) {
                    		contoursToDraw.add(contoursToDraw.get(0));
                    		contoursToDraw.set(0, contour);
                    		secondGreatestArea = greatestArea; 
                    		greatestArea = Imgproc.contourArea(contour);
                    	}
                    	else if (contoursToDraw.size() == 0) {
                    		contoursToDraw.add(contour);
                    		secondGreatestArea = greatestArea; 
                    		greatestArea = Imgproc.contourArea(contour);
                    	}
                    	
                    }
                    else if (Imgproc.contourArea(contour) <= greatestArea && Imgproc.contourArea(contour) >= secondGreatestArea)
                    {
                    	if (contoursToDraw.size() == 1){
                        	contoursToDraw.add(contour);
                        	secondGreatestArea = Imgproc.contourArea(contour);
                    	}
                    	else{
                    		contoursToDraw.set(1, contour);
                    		secondGreatestArea = Imgproc.contourArea(contour);
                    	}
                    }
                }
                System.out.println("Greatest Area: " + greatestArea + "\nSecond Greatest Area: " + secondGreatestArea);
                
                double sum = 0; 
                for (int i = 0; i < contoursToDraw.size(); i++) {
                		Imgproc.drawContours(matrix, contoursToDraw, i, new Scalar(0,0,255));
                		Moments moments = Imgproc.moments(contoursToDraw.get(i));
                		Point centroid = new Point();
                		centroid.x = moments.get_m10()/moments.get_m00();
                		centroid.y = moments.get_m01()/moments.get_m00();
                    
                		System.out.printf("%8.2f %8.2f\n",centroid.x,centroid.y);
                		sum += centroid.x;
                }
                table.putNumber("Center", sum/contoursToDraw.size());
                //blankImg.copyTo(matrix);

                int bufferSize = matrix.channels()*matrix.cols()*matrix.rows();
                byte [] buffer = new byte[bufferSize];
                matrix.get(0,0,buffer); // get all the pixels
                BufferedImage image = new BufferedImage(matrix.cols(),matrix.rows(), type);
                final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);
                return image;
        }
}
