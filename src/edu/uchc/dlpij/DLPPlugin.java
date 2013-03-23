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
	boolean deviceExist_ = false;
	DLPControl ctl;
	
	@Override
	public void run(String cmd) {
		
		if (! deviceExist_ ) {
			IJ.error("Can't find DLP device");
			return;
		}
		
		if (cmd.equals("black")) {
			ctl.turnFullyOff();
			//DLPControl.displayPattern();
			return;
		}
		if (cmd.equals("white")) {
			ctl.turnFullyOn();
			//DLPControl.displayPattern();
			return;
		}			
	
		// Control with ROI
		if (cmd.equals("ROI")) {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null) 
				return;
			Roi roi = imp.getRoi();

			if (roi == null) {
				IJ.showMessage("No ROI in current image");
			} else {
				ctl.setPatternToROI(roi);
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
		
		double xoffset = ctl.getOffsetX();
		double yoffset = ctl.getOffsetY();
		double xscale = ctl.getScaleX();
		double yscale = ctl.getScaleY();
		double rot = ctl.getRotation();
		
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

				ctl.setTransformationMatrix(newXScale, newYScale, newRot, newXOff, newYOff);				
				
				return true;
			}
		});

		dlg.showDialog();
		
		if (! dlg.wasOKed()) {
			ctl.setTransformationMatrix(xscale, yscale, rot, xoffset, yoffset); // restore old values
		}
	}
	
	public DLPPlugin() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (DLPControl.getNumberOfDevices() > 0) {
			ctl = new DLPControl(0, 2);
			ctl.startUp();
			deviceExist_ = true;
		}
	}

}
