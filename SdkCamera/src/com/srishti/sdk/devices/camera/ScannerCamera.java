package com.srishti.sdk.devices.camera;


import com.srishti.sdk.devices.camera.*;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.content.Context;
import android.content.Intent;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.Toast;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import android.widget.TextView;
import android.graphics.ImageFormat;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;

/**
 * This class contains methods to open the camera and capture the Barcode/QRcode
 *
 */
public class ScannerCamera {
	
	    static ImageScanner scanner;
	    private SymbolSet syms;
	    private CameraPreview mPreview;
	    private Handler autoFocusHandler;
	    private static Camera mCamera;
	    private static boolean mpreviewing = true;
	    private boolean barcodeScanned = false;
	    private boolean previewing = true;
	    SrishtiErrors e;
	    String mContent;
	    static Context context;
	    int retCode;
	    private long startnow;
	    private long endnow;
	   
	   	  	 static {
	        System.loadLibrary("iconv");
	    } 
	  	 /**
	  	  * method returns BarcodeScannedStatus
	  	  * @return barcodeScanned - represents whether the Barcode/QRcode Scanned or not.
	  	  */
	  	 private boolean getBarcodeScannedStatus(){
	  		 return barcodeScanned;
	  	 }
	  	 
	  	 /**
	  	  * method set the BarcodeScannedStatus to the status of parameter
	  	  * * @param scannedStatus - This represents whether the Barcode/QRcode Scanned or not. 
	  	  */
	  	 private void setBarcodeScannedStatus(boolean scannedStatus){
	  		  barcodeScanned = scannedStatus;
	  	 }
	  	 
		 /**
		  *This method open the camera and returns the instance of the camera
		  *@return mCamera - returns the object of the camera.
		  */
		 public Camera initScannerCamera(Context context){
				this.context = context;
			 scanner = new ImageScanner();
			 scanner.setConfig(0, Config.X_DENSITY, 3);
			 scanner.setConfig(0, Config.Y_DENSITY, 3);
			    return openCamera();
			    
			    }
		 /**
		  * This method verifies whether the webcam is connected through the usb interface or not,
		  * if it is connected it opens the camera mode.
		  * @return mCamera
		  */
		 private static Camera openCamera(){
		        mCamera = null;
		        try {
		        	mCamera = Camera.open();
		        	
		        } catch (Exception e){
		        	Toast.makeText(context, "Unable to open camera", Toast.LENGTH_LONG).show();
		        }
		       
		        return mCamera;
		    }
		/**
		 *
		 * This method create the instances for autoFocusHandler and CameraPreview.
		 * It adds the Preview mode to the layout view and calls the method setCamera() 
		 * But while capturing it is recommended to maintain 2cm distance between QRcode and the camera.
	     * The distance varies depending on the QRcode/Barcode versions. 
		 * @param context - Application context.
		 * @param preview - FrameLayout preview.
		 * @return retCode - returns the error code
		 */
	  	 
	  	 public int captureBarcode(Context context, FrameLayout preview){
	  		 int retCode = e.SRISHTI_SDK_ERROR_DEV_CAMERA_FAIL;
	  		autoFocusHandler = new Handler();
	  		
	  		 mPreview = new CameraPreview(context, previewCb, autoFocusCB);
	        
	         preview.addView(mPreview);
	         Log.d("Srishti-sdk-camera", "Before set camera");
	         mPreview.setCamera(mCamera);
	         Log.d("Srishti-sdk-camera", "After set camera");
	         retCode = e.SRISHTI_SDK_ERROR_DEV_CAMERA_OK;
	         return retCode;
	  	 }
	  	 
	 	private Runnable doAutoFocus = new Runnable() {
            public void run() {
                if (previewing)
                	try{
                		if(mCamera != null)
                    mCamera.autoFocus(autoFocusCB);
                	}catch(Exception e){
                		Toast.makeText(context, "In doAutoFocus exception occured", Toast.LENGTH_LONG).show();
                	}
            }
        };
        
        /**
         * This method sets the preview mode that was initialized
         * in the method captureBarcode(). In case if it is unable to set the 
         * preview mode then it will return the error saying that 'Unable to return Preview mode
         */
        public int setPreviewMode(){
        	 retCode = e.SRISHTI_SDK_ERROR_DEV_CAMERA_FAIL;
        	if (getBarcodeScannedStatus()) {
                setBarcodeScannedStatus(false);
                mCamera.setPreviewCallback(previewCb);
                mCamera.startPreview();
                previewing = true;
                mCamera.autoFocus(autoFocusCB);
                retCode = e.SRISHTI_SDK_ERROR_DEV_CAMERA_OK;
            }
        	return retCode;
        }
        
        
        PreviewCallback previewCb = new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera mCamera) {
            	 Log.d("Srishti-sdk-camera", "Before getparameters");
                Camera.Parameters parameters = mCamera.getParameters();
                Log.d("Srishti-sdk-camera", "After getparameters");
                System.out.println("data value is:" + data);
                startnow = android.os.SystemClock.uptimeMillis();
                mContent = capture(data, parameters);
                if(mContent != null){
                endnow = android.os.SystemClock.uptimeMillis();
                Log.d("MYTAG", "Excution time: "+(endnow-startnow)+" ms");
                }
                Log.d("Srishti-sdk-camera", "After captureBarcode in PreviewCallback");

               //scanText.setText("barcode result " + content);
                         }
        };
        
        AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 1000);
            }
        };

	  /**
	  *	 This method captures the Barcode/QRcode and returns the decoded result.
	  *  @param data
	  *  @param parameters
	  *
	  */
	 private String capture(byte[] data, Parameters parameters){
		 Size size = parameters.getPreviewSize();
		 String barCodeScannedText = null;
         Image barcode = new Image(size.width, size.height, "Y800");
         barcode.setData(data);

         int result = scanner.scanImage(barcode);
         
         System.out.println("after getting result"+ result);
         if (result != 0) {
        	 Log.d("Srishti-sdk-camera", "Inside if condition");                
         	mpreviewing = false;
             mCamera.setPreviewCallback(null);
             mCamera.stopPreview();
             Log.d("Srishti-sdk-camera", "after stop preview");
            
             syms = scanner.getResults();
             Log.d("Srishti-sdk-camera", "Before for loop");
            
             barcodeScanned = true;
             for (Symbol sym : syms) {
            	 Log.d("Srishti-sdk-camera", "entered for loop");
            	
                //scanText.setText("barcode result " + sym.getData());
                barCodeScannedText = sym.getData();
                System.out.println("String is:"+ barCodeScannedText);
                 
                // System.out.println("At the end of capture Barcode"+ barcodeScanned);
             }
         }
         return barCodeScannedText;
	 }
	 
	 /**
	  * This method returns the content decoded from a bar code.
	  * If the bar code was not captured properly, this method
	  * returns a null string, otherwise returns a valid content
	  * @return - String that contains the content as decoded from a barcode
	  */
	  public String getDecodedBarcode(){
		 return mContent;
	 }
	  
	
	  /**
	  * This method is resposible for releasing the camera 
	  * @return retCode - (int) that shows the status of the camera which represents
	  *  whether the camera is still connected or released
	  */
	 public int releaseCamera() {
			 int retCode = e.SRISHTI_SDK_ERROR_DEV_CAMERA_FAIL;
		        if (mCamera != null) {
		            mpreviewing = false;
		            mCamera.setPreviewCallback(null);
		            //mPreview.getHolder().removeCallback(mPreview);
		            Log.d("Srishti-sdk-camera", "Before releasing camera");
		            mCamera.stopPreview();
		           mCamera.release();
		            Log.d("Srishti-sdk-camera", "After camera release");
		            mCamera = null;
		        }
		        if (mPreview != null) {
		            //mFlCameraPreview.removeView(mPreview);
		            mPreview = null;
		        }
		            retCode = e.SRISHTI_SDK_ERROR_DEV_CAMERA_OK;
		        
		       return retCode;
		    }
}

