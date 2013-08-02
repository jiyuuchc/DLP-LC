package edu.uchc.dlpij;

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.gui.DialogListener;

import javax.swing.UIManager;

public class DLPPlugin implements PlugIn {
	static boolean deviceExist_ = false;
	static DLPControl ctl_ = null;
	static Roi roi_ = null;
	
	@Override
	public void run(String cmd) {
		
		if (! deviceExist_ ) {
			IJ.error("Can't find DLP device");
			return;
		}
		
		if (cmd.equals("black")) {
			ctl_.turnFullyOff();
			roi_ = null;
			//DLPControl.displayPattern();
			return;
		}
		if (cmd.equals("white")) {
			ctl_.turnFullyOn();
			roi_ = null;
			//DLPControl.displayPattern();
			return;
		}			
	
		// Control with ROI
		if (cmd.equals("ROI")) {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null) 
				return;
			roi_ = imp.getRoi();

			if (roi_ == null) {
				IJ.showMessage("No ROI in current image");
			} else {
				ctl_.setPatternToROI(roi_);
			}
			
			return;
		}
		
		//calibration
		if (cmd.equals("calibration")) {
			doCalibration();  
		}
	}

	private void doCalibration() {
		GenericDialog dlg = new GenericDialog("DLP calibration");
		
		double xoffset = ctl_.getOffsetX();
		double yoffset = ctl_.getOffsetY();
		double xscale = ctl_.getScaleX();
		double yscale = ctl_.getScaleY();
		double rot = ctl_.getRotation();
		
		dlg.addSlider("XOffset", -512, 512, - xoffset);
		dlg.addSlider("YOffset", -512, 512, - yoffset);
		dlg.addSlider("XScale", 20, 300, (1.0 / xscale) * 100);
		dlg.addSlider("YScale", 20, 300, (1.0 / yscale) * 100);
		dlg.addSlider("Rotation", -180, 180,  - rot);
		
		dlg.addDialogListener(new DialogListener() {
			@Override
			public boolean dialogItemChanged(GenericDialog dlg, AWTEvent evnt) {
				double newXOff = - dlg.getNextNumber();
				double newYOff = - dlg.getNextNumber();
				double newXScale= 100.0 / dlg.getNextNumber();
				double newYScale = 100.0 / dlg.getNextNumber();
				double newRot = - dlg.getNextNumber();
				
				ctl_.setTransformationMatrix(newXScale, newYScale, newRot, newXOff, newYOff);
				
				IJ.log(String.format("New matrix: %.2f %.2f %.2f   %.2f %.2f %.2f",
						ctl_.t11, ctl_.t12, ctl_.t13, ctl_.t21, ctl_.t22, ctl_.t23));

				
				if (roi_ != null) {
					//IJ.log("Set New ROI");
					ctl_.setPatternToROI(roi_);
				}
				return true;
			}
		});

		dlg.showDialog();
		
		if (dlg.wasCanceled()) {
			ctl_.setTransformationMatrix(xscale, yscale, rot, xoffset, yoffset); // restore old values
		} 
	}
	
	public DLPPlugin() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (ctl_ == null) {
			if (DLPControl.getNumberOfDevices() > 0) {
				ctl_ = new DLPControl(0, 2);
				ctl_.startUp();
				deviceExist_ = true;
			}
		}
	}

}
