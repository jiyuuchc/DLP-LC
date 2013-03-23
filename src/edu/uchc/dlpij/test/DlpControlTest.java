package edu.uchc.dlpij.test;

import edu.uchc.dlpij.DLPControl;

public class DlpControlTest {
	
	public static void main (String [] arg) {
		int n = DLPControl.getNumberOfDevices();
		System.out.println("Number of devices:" + n);
		
		if (n < 1) {  //didn't find device.
			return;
		}
		
		DLPControl dlp = new DLPControl(0,2);
		
		dlp.startUp();
		
		/////////////
		
		System.out.println("Now turning system fully on.");

		dlp.turnFullyOn();
		
		System.out.println("Press ENTER to go to next step.");
		
		try {
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//////////////
		
		System.out.println("Now turning system fully off.");

		dlp.turnFullyOff();
		
		System.out.println("Press ENTER to go to next step.");
		
		try {
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/////////////////
		
		System.out.println("Shutting down.");
		
		dlp.shutDown();
	}
}
