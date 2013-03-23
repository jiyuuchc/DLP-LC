package edu.uchc.dlpij;

import ij.gui.Roi;
import ij.process.ByteProcessor;

import java.util.Arrays;
import java.util.prefs.Preferences;

import edu.uchc.DLPJava.DLPJava;
import edu.uchc.DLPJava.Display;
import edu.uchc.DLPJava.Img;
import edu.uchc.DLPJava.Misc;

public class DLPControl {
	int numOfDevices_ = 0;
	int deviceNum_ = 0;

	private static final int XGAWIDTH = 1024;
	private static final int XGAHEIGHT = 768;
	private static final int XGASIZE = XGAWIDTH * XGAHEIGHT /8 ;

	final static byte [] blackPattern_ = new byte[XGASIZE];
	final static byte [] whitePattern_ = new byte[XGASIZE];

	double t11, t12, t13, t21, t22, t23;
	
	double scalex_ = 2.0;
	double scaley_ = 2.0;
	double offsetx_ = - 285;
	double offsety_ = 242; 
	double rotation_ = 90;

	final static String SCALEX_KEY = "ScaleX";
	final static String SCALEY_KEY = "ScaleY";
	final static String OFFSETX_KEY = "OffsetX";
	final static String OFFSETY_KEY = "OffsetY";
	final static String ROTATION_KEY = "Rotation";

	static {
//		DLPJava.InitPortabilityLayer((short)0,(short)0, new DLPJava.OutputCallback() {
//
//			@Override
//			public void OnOutput(String s) {
//				IJ.log("DLP: " + s);
//
//			}
//		});

		DLPJava.InitSimple((short)0,(short)0);

		Arrays.fill(whitePattern_, (byte)0xff);
		Arrays.fill(blackPattern_, (byte) 0);
	}
	
	public static int getNumberOfDevices() {
		return Misc.GetTotalNumberOfUSBDevicesConnected();
	}
	
	public DLPControl(int deviceNum, int loglevel) throws IllegalArgumentException {
		numOfDevices_ = getNumberOfDevices();
		
		if (deviceNum >= numOfDevices_) {
			throw(new IllegalArgumentException("Wrong device number.")); 
		}

		deviceNum_ = deviceNum;
		Misc.SetUSBDeviceNumber((short)deviceNum);
		DLPJava.ChangeLogLevel((short) loglevel);

		Preferences pref = Preferences.userNodeForPackage(DLPControl.class);
		pref = pref.node(DLPControl.class.getName());
		
		scalex_ = pref.getDouble(SCALEX_KEY + deviceNum, 2.0);
		scaley_ = pref.getDouble(SCALEY_KEY + deviceNum, 2.0);
		offsetx_ = pref.getDouble(OFFSETX_KEY + deviceNum, -285);
		offsety_ = pref.getDouble(OFFSETY_KEY + deviceNum, 240);
		rotation_ = pref.getDouble(ROTATION_KEY + deviceNum, 90);
		
		calculateMatrix();
	}

	private int transformX(int x, int y) {
		double dx;
		dx = t11 * x + t12 * y + t13;
		return (int) Math.round(dx);
	}
	
	private int transformY(int x, int y) {
		double dy;
		dy = t21 * x + t22 * y + t23;
		return (int) Math.round(dy);
	}

	private void calculateMatrix() {
				
		double rotation = rotation_ * Math.PI / 180.0; 
			
 		t11 = scalex_ * Math.cos(rotation);
		t12 = - scaley_ * Math.sin(rotation);
		t21 = scalex_ * Math.sin(rotation);
		t22 = scaley_ * Math.cos(rotation_);
		t13 = (XGAWIDTH - t11 * XGAWIDTH - t12 * XGAHEIGHT) / 2 + offsetx_;  //keep central symmetry
		t23 = (XGAHEIGHT - t21 * XGAWIDTH - t22 * XGAHEIGHT) / 2 + offsety_;
	}

	public void startUp(){
		Misc.EnableCommunication();
		Display.UnparkDMD();
		Display.DisplayPatternManualForceFirstPattern();
	}

	public void shutDown() {
		Display.ParkDMD();
	}
	
	public void displayPattern() {
		Display.DisplayPatternManualForceFirstPattern();
	}

	public void setPattern(byte [] pattern) {
		Img.DownloadBitplanePatternToExtMem(pattern, 0);
	}

	public void turnFullyOn() {
		setPattern(whitePattern_);
		displayPattern();
	}
	
	public void turnFullyOff() {
		setPattern(blackPattern_);
		displayPattern();
	}
	
	private void setPatternToXGABinaryImage(ByteProcessor img) {
		byte [] bitData = new byte[XGASIZE];
		int bitDataIndex = 0;
		for (int y = 0; y < XGAHEIGHT; y++) {
			for (int x = 0; x < XGAWIDTH; x += 8) {
				int bitValue = 0;
				int currentBit = 1;
				for (int bit = 0; bit < 8; bit ++) {
					if ( img.get(x + bit, y)> 0) {
						bitValue |= currentBit;
					}
					currentBit = currentBit << 1;
				}
				bitData[bitDataIndex++] = (byte)bitValue; 
			}
		}
		setPattern(bitData);
	}

	public void setPatternToBinaryImage(ByteProcessor img) {
		int width = img.getWidth();
		int height = img.getHeight();
		if (width != XGAWIDTH || height != XGAHEIGHT) {
			throw(new IllegalArgumentException("Wrong image size"));
		}
		setPatternToXGABinaryImage(img);
	}

	public void setPatternToROI(Roi roi) {

		byte [] bitData = getBitdataFromROI(roi);
		setPattern(bitData);

	}
	
	 public byte [] getBitdataFromROI (Roi roi) {
		byte [] bitData = new byte[XGASIZE];
		int bitDataIndex = 0;
		//ImageProcessor img = new ByteProcessor(1024,768);
		
		for (int y = 0; y < XGAHEIGHT; y++) {
			for (int x = 0; x < XGAWIDTH; x += 8) {
				int bitValue = 0;
				int currentBit = 1;
				for (int offset = 0; offset < 8; offset ++) {
					if (roi.contains(
							transformX(x + offset, y),
							transformY(x + offset, y) )) {
						bitValue |= currentBit;
						//img.set(x,y,255);
					} else {
						//img.set(x,y,0);
					}
					currentBit = currentBit << 1;
				}
				bitData[bitDataIndex++] = (byte)bitValue;
			}
		}
		//ImagePlus newImp = new ImagePlus("mask",img);;
		//newImp.show();
		
		return bitData;
	}
	
	public void setTransformationMatrix(double scalex, double scaley, double rotation, double offsetx, double offsety) {
		scalex_ = scalex;
		scaley_ = scaley;
		offsetx_ = offsetx;
		offsety_ = offsety;
		rotation_ = rotation;
		
		Preferences pref = Preferences.userNodeForPackage(DLPControl.class);
		pref = pref.node(DLPControl.class.getName());
		
		pref.putDouble(OFFSETX_KEY, offsetx);
		pref.putDouble(OFFSETY_KEY, offsety);
		pref.putDouble(SCALEX_KEY, scalex);
		pref.putDouble(OFFSETY_KEY, offsety);
		pref.putDouble(ROTATION_KEY, rotation);
		
		calculateMatrix();
	}
	
	public double getOffsetX() {
		return offsetx_;
	}
	
	public double getOffsetY() {
		return offsety_;
	}
	
	public double getScaleX() {
		return scalex_;
	}
	
	public double getScaleY() {
		return scaley_;
	}
	
	public double getRotation() {
		return rotation_;
	}
}
