package com.bah.paul_matthew;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Preprocessor {
	public Mat im;
	public Mat blurred_image;
	public Mat threshed_image;
	public Mat contoured_image;
	public Mat id_image;
	public int[] testFeatures;
	
	public Preprocessor() {
		
	}
	
	// Converts the image to greyscale, applys a gaussian filter, applies a thresholding function
	// to the image, and identifies the contours of the digits. Returns the contour values.
	public List<Rect> preprocess(String filename) {
		// Load the image into a greyscale buffer
	      im = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
	      System.out.println(im.size());
	      Imgcodecs.imwrite("greyscale_output.jpg", im);
	      // Apply Gaussian filtering
	      blurred_image = new Mat(im.rows(),im.cols(),im.type());
	      Imgproc.GaussianBlur(im, blurred_image, new Size(5,5), 0);
	      Imgcodecs.imwrite("gaussian_output.jpg", blurred_image);
	      // Threshold the image
	      threshed_image = new Mat(blurred_image.rows(), blurred_image.cols(), blurred_image.type());
	      Imgproc.threshold(blurred_image, threshed_image, 90, 255, Imgproc.THRESH_BINARY_INV);
	      Imgcodecs.imwrite("threshed_output.jpg", threshed_image);
	      // Find image contours
	      List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	      contoured_image = new Mat(threshed_image.rows(), threshed_image.cols(), threshed_image.type());
	      Imgproc.findContours(threshed_image, contours, contoured_image, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
	      List<Rect> rects = new ArrayList<Rect>();
	      for(int i=0; i<contours.size(); i++) {
	    	  rects.add(Imgproc.boundingRect(contours.get(i)));
	      }
	      return rects;
	}
	
	// This function draws the bounds of each digit, extracts each digit from the original, normalizes,
	// then returns a list of the extracted image file names.
	public List<String> drawDigitBounds(String filename, List<Rect> rects) {
		List<String> digitFiles = new ArrayList();
		for(int i = 0; i < rects.size(); i++) {
			// Draw the rectangle
			Rect rect = rects.get(i);
			Point p = new Point(rect.x, rect.y);
			Point p2 = new Point(rect.x+rect.width,rect.y+rect.height);
			int height = (int) ((int) p2.y - p.y);
			int width = (int) ((int) p2.x - p.x);
			Imgproc.rectangle(im, p, p2, new Scalar(0, 255, 0));
			Mat extraction = new Mat(height+1, width+1, threshed_image.type());
			blackOut(extraction);
			Mat src = Imgcodecs.imread("threshed_output.jpg", Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
			for(int j = (int) p.y; j< p.y + height; j++) {
				for(int k = (int) p.x; k<p.x+width; k++) {
					extraction.put(j- ((int) p.y), k - ((int) p.x), src.get(j, k));
				}
			}
			// NOTE: Dilate and Erode may be utilized for varying results. Possible TODO is
			// to build a function to automatically dilate or Erode the digit content based 
			// on pixel density
			//Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(1,1));
			Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(2,2));
			//Imgproc.dilate(extraction, extraction, kernelDilate);
			Imgproc.erode(extraction, extraction, kernelErode);
			Imgproc.resize(extraction, extraction, new Size(20,32),0,0,Imgproc.INTER_CUBIC);
			Mat finalDigit = new Mat(32, 32, threshed_image.type());
			blackOut(finalDigit);
			for(int j = 0; j < 32; j++) {
				for(int k = 6; k < 26; k++) {
					finalDigit.put(j, k, extraction.get(j, k-6));
				}
			}
			String newFileName = "digit_" + Integer.toString(i) + ".jpg";
			digitFiles.add(newFileName);
			Imgcodecs.imwrite(newFileName, finalDigit);
		}
		Imgcodecs.imwrite("id_output.jpg", im);
		return digitFiles;
	}
	
	public static void blackOut(Mat matrix) {
		int height = matrix.rows();
		int width = matrix.cols();
		for(int j = 0; j < height; j++) {
			for(int k = 0; k < width; k++) {
				double[] theBlackness = {(double) 0.0};
				matrix.put(j, k, theBlackness);
			}
		}
	}
	
	public List<int[]> extractFeatures(List<String> digitFiles) {
		List<int[]> featureList = new ArrayList<int[]>();
		for(int i=0; i<digitFiles.size(); i++) {
			Mat digit = Imgcodecs.imread(digitFiles.get(i), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
			int[] features = new int[64];
			// Based on the UCI Repository digits description, we need to move through
			// the digit in 4x4 sections and count the number of white pixels in 
			// each section. Output the count of white pixels as the feature.
			// There should be a total of 64 features.
			double whiteThreshold = 0.0;
			int block = 0;
			for(int j = 0; j < digit.rows(); j += 4) {
				for(int k = 0; k < digit.cols(); k += 4) {
					int count = 0;
					// Loop Unrolling
					if (digit.get(j, k)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j, k+1)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j, k+2)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j, k+3)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+1, k)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+1, k+1)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+1, k+2)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+1, k+3)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+2, k)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+2, k+1)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+2, k+2)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+2, k+3)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+3, k)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+3, k+1)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+3, k+2)[0] > whiteThreshold) {
						count++;
					}
					if (digit.get(j+3, k+3)[0] > whiteThreshold) {
						count++;
					}
					features[block] = count;
					block++;
				}
			}
			// Print out our feature output
			String output = "";
			for(int j =0; j < features.length; j++) {
				if (j > 0) {
				output = output + ", " + String.valueOf(features[j]);
				}
				else output = output + String.valueOf(features[j]);
			}
			featureList.add(features);
		}
		return featureList;
	}
	
	public static void main( String[] args ) throws JSONException
	   {
	      System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
	      Preprocessor p = new Preprocessor();
	      String input = "photo_1.jpg";
	      List<Rect> rects = p.preprocess(input);
	      List<String> digitFiles = p.drawDigitBounds(input, rects);
	      List<int[]> digitFeatureList = p.extractFeatures(digitFiles);
	      HttpHandler handler = new HttpHandler();
	      Mat labeledImage = Imgcodecs.imread("id_output.jpg");
	      try {
	    	//handler.createPost("http://requestb.in/14xtiw91", digitFeatureList.get(0));
			JSONObject receivedMessage = handler.createPost("https://ussouthcentral.services.azureml.net/workspaces/55074e4b7c26459388d885c68143c343/services/e948813ae8194aabad9fbe2e3ec41788/execute?api-version=2.0&details=true", digitFeatureList);
			JSONArray valuesCollection = receivedMessage.getJSONObject("Results").getJSONObject("output1").getJSONObject("value").getJSONArray("Values");
			int fontFace = Core.FONT_HERSHEY_DUPLEX;
			double fontScale = 2;
			for(int i = 0; i < valuesCollection.length(); i++) {
				JSONArray values = valuesCollection.getJSONArray(i);
				int length = values.length();
				Rect currentRect = rects.get(i);
				Point location = new Point(currentRect.x, currentRect.y-10);
				System.out.println("Number identified as " + values.get(length-1));
				String digit = values.get(length-1).toString();
				Imgproc.putText(labeledImage, digit, location, fontFace, fontScale, new Scalar(97,227,32));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	      Imgcodecs.imwrite("labeled.jpg", labeledImage);
	      
	      java.util.Date date= new java.util.Date();
	      System.out.println("completed execution " + new Timestamp(date.getTime()));
	   }

}
