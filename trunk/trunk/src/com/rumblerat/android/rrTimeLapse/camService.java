/*
 *                         rrTimeLapse 
 *  Copyright (c) 2009 by Jernej Kranjec <jernej.kranjec@gmail.com
 *    Licensed under GNU General Public License (GPL) 3 or later
*/

package com.rumblerat.android.rrTimeLapse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceView;

public class camService extends Service{
	
	private NotificationManager mNM;
	
	private Camera cam;
	
	private static final String STORAGE_PATH = new String("/sdcard/");
	private static final String DATA_PATH = new String("rrTimeLapse/");
	
	private Boolean isCamActive = false;
	private Boolean isCamRunning = false;
	
	private Handler mHandler = new Handler();
	private long mStartTime;
	
	private SurfaceView sv;
	
	private Boolean hasAutofocus;
	
	private Integer resWidth;
	private Integer resHeight;
	
	private Integer isType;
	
	private Integer hasLapse = 10;
	
	private Integer mYearStart;
	private Integer mMonthStart;
	private Integer mDayStart;
	private Integer mHourStart;
	private Integer mMinuteStart;

	private Integer mYearEnd;
	private Integer mMonthEnd;
	private Integer mDayEnd;
	private Integer mHourEnd;
	private Integer mMinuteEnd;
	
	private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {
    	
    	@Override
    	public void onShutter() {
    		
    	}
    };
    
	private Camera.PictureCallback mPictureCallbackRaw = new Camera.PictureCallback() {
		
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

        }
    };
    
    private Camera.PictureCallback mPictureCallbackJpeg= new Camera.PictureCallback() {
    	
    	OutputStream filoutputStream;
    	
    	@Override
        public void onPictureTaken(byte[] data, Camera camera) {    		
    		
    		try {
    			Time time = new Time();
    			time.setToNow();
    			
    			this.filoutputStream = new FileOutputStream(STORAGE_PATH + DATA_PATH + ((Integer)time.year).toString() + "-" + String.format("%02d", time.month + 1) + "-" + String.format("%02d", time.monthDay) + "-" + String.format("%02d", time.hour) + "." + String.format("%02d", time.minute) + "." + String.format("%02d", time.second) + ".jpg");
    		} catch (FileNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		};
    		
    		try {
    			filoutputStream.write(data);
    			filoutputStream.flush();
    			filoutputStream.close();
    		} catch(Exception ex) {
    			ex.printStackTrace();
    		}
    		
    		resetView();
    	}
    };
    
    private Camera.AutoFocusCallback mAutofocusCallback = new Camera.AutoFocusCallback() {
    	
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			cam.takePicture(mShutterCallback, mPictureCallbackRaw, mPictureCallbackJpeg);
		}
	};
	
	 @Override
	 public void onCreate() {
		 mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		 
		 showNotification_start();
	 }
	 
	 @Override
	 public void onStart(Intent intent, int startId) {
		 
		 if (!new File(STORAGE_PATH + DATA_PATH).exists()) {
			 new File(STORAGE_PATH + DATA_PATH).mkdir();
		 }
	 
	 }

	 @Override
	 public void onDestroy() {
		 unsetView();
		 
		 showNotification_stop();
	 }
	 
	 public class LocalBinder extends Binder {
		 camService getService() {
			 return camService.this;
		 }
	 }
	 
	 private final IBinder mBinder = new LocalBinder();
	 
	 @Override
	 public IBinder onBind(Intent intent) {
		 return mBinder;
	 }

	private void showNotification_start() {
        CharSequence text = getText(R.string.camService_text);
        
        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, camActivity.class), 0);
        
        notification.setLatestEventInfo(this, getText(R.string.camService_label), text, contentIntent);
        
        mNM.notify(R.string.camService_label, notification);
    }
	
	private void showNotification_stop() {
		mNM.cancel(R.string.camService_label);
    }
	
	private void setView() {
		if (!isCamActive) {
			cam = Camera.open();
			
			Camera.Parameters p = cam.getParameters();
			p.setPictureSize(resWidth, resHeight);
			cam.setParameters(p);

			isCamActive = true;
			
			try {
				cam.setPreviewDisplay(sv.getHolder());
				cam.startPreview();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void unsetView() {
		if (isCamActive) {
			cam.stopPreview();
			cam.release();			 
			isCamActive = false;
		}
	}
	
	private void resetView() {
		unsetView();
		setView();
		
		//cam.stopPreview();
		//cam.startPreview();
	}
	
	private Runnable mUpdateTimeTask = new Runnable() {
    	public void run() {
    		
    		Log.i("test", "start time");
    		final long start = 0;
    		long millis = SystemClock.uptimeMillis() - start;
    		int seconds = (int) (millis / 1000);
    		int minutes = seconds / 60;
    		seconds     = seconds % 60;
    		
    		takePicture();
    		
    		switch (isType) {
    		case 0:
    			mHandler.postAtTime(this, start + (( (0 * 60 * 60) + ((minutes + 0) * 60) + seconds + hasLapse) * 1000));
    			break;
    		case 1:
    			if (System.currentTimeMillis() < new Date((mYearEnd-1900), mMonthEnd, mDayEnd, mHourEnd, mMinuteEnd, 0).getTime()) {
        			mHandler.postAtTime(this, start + (( (0 * 60 * 60) + ((minutes + 0) * 60) + seconds + hasLapse) * 1000));
        		}
    			else {
    				stopCamera();
    			}
    			break;
    		}
    	}
    };
	
    private void takePicture() {    	
    	if (hasAutofocus) {
    		cam.autoFocus(mAutofocusCallback);
    	}
    	else {
    		cam.takePicture(mShutterCallback, mPictureCallbackRaw, mPictureCallbackJpeg);
    	}
    }
    
	// // //
    
    public void giveView(SurfaceView x, Integer autofocus, Integer resolution, Integer type, Integer lapse,
    						Integer yearStart, Integer monthStart, Integer dayStart, Integer hourStart, Integer minuteStart, Integer yearEnd, Integer monthEnd, Integer dayEnd, Integer hourEnd, Integer minuteEnd) {
		sv = x;
		
		if (autofocus == 0) {
			hasAutofocus = false;
		}
		else {
			hasAutofocus = true;
		}
		
		switch (resolution) {
		case 0:
			resWidth = 640;
			resHeight = 416;
			break;
		case 1:
			resWidth = 640;
			resHeight = 480;
			break;
		case 2:
			resWidth = 1280;
			resHeight = 848;
			break;
		case 3:
			resWidth = 1280;
			resHeight = 960;
			break;
		case 4:
			resWidth = 2048;
			resHeight = 1360;
			break;
		case 5:
			resWidth = 2048;
			resHeight = 1536;
			break;
		case 6:
			resWidth = 2592;
			resHeight = 1728;
			break;
		case 7:
			resWidth = 2592;
			resHeight = 1952;
			break;
		}
		
		isType = type;
		
		hasLapse = lapse;
		
		mYearStart = yearStart;
		mMonthStart = monthStart;
		mDayStart = dayStart;
		mHourStart = hourStart;
		mMinuteStart = minuteStart;

		mYearEnd = yearEnd;
		mMonthEnd = monthEnd;
		mDayEnd = dayEnd;
		mHourEnd = hourEnd;
		mMinuteEnd = minuteEnd;
		
		setView();
	}
	
	public void takeView() {
		unsetView();
	}
	
	public Boolean isCameraActive() {
		return isCamActive;
	}
	
	public Boolean isCameraRunning() {
		return isCamRunning;
	}
	
	public void runCamera() {
		isCamRunning = true;
		
		switch (isType) {
		case 0:
			mHandler.postDelayed(mUpdateTimeTask, 0);
			break;
		case 1:
			mStartTime = System.currentTimeMillis();
			mHandler.postDelayed(mUpdateTimeTask, (new Date((mYearStart-1900), mMonthStart, mDayStart, mHourStart, mMinuteStart, 0).getTime() - mStartTime));
			break;
		}
	}
	
	public void stopCamera() {
		isCamRunning = false;
		
		mHandler.removeCallbacks(mUpdateTimeTask);
	}	
}
