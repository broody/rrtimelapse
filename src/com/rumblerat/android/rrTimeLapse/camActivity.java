/*
 *                         rrTimeLapse 
 *  Copyright (c) 2009 by Jernej Kranjec <jernej.kranjec@gmail.com
 *    Licensed under GNU General Public License (GPL) 3 or later
*/

package com.rumblerat.android.rrTimeLapse;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class camActivity extends Activity {
	
	public SurfaceView sv;
	//private SurfaceHolder sh;

	private camService mBoundLocalService;

	private LinearLayout xView;

	private Button xButton;
	private ArrayAdapter<?> xAdaptor;

	private Spinner spinnerAutofocus;
	private Spinner spinnerResolution;
	private Spinner spinnerType;

	private Integer lpsTime = 10;

	private static final int DIALOG_GET_LAPSE_TIME = 0;
	private static final int DIALOG_TIME_START = 1;
	private static final int DIALOG_DATE_START = 2;
	private static final int DIALOG_TIME_END = 3;
	private static final int DIALOG_DATE_END = 4;
	
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
	
	private TextView lpsViewStart;
	private TextView lpsViewEnd;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Calendar c = Calendar.getInstance();

		mYearStart = c.get(Calendar.YEAR);
		mMonthStart = c.get(Calendar.MONTH);
		mDayStart = c.get(Calendar.DAY_OF_MONTH);
		mHourStart = c.get(Calendar.HOUR_OF_DAY);
		mMinuteStart = c.get(Calendar.MINUTE);

		mYearEnd = c.get(Calendar.YEAR);
		mMonthEnd = c.get(Calendar.MONTH);
		mDayEnd = c.get(Calendar.DAY_OF_MONTH);
		mHourEnd = c.get(Calendar.HOUR_OF_DAY);
		mMinuteEnd = c.get(Calendar.MINUTE);

		xButton = (Button)findViewById(R.id.grabCamera);
		xButton.setOnClickListener(mGrabLocalCamera);
		xButton.setVisibility(View.GONE);
		xButton = (Button)findViewById(R.id.startCamera);
		xButton.setOnClickListener(mStartLocalCamera);
		xButton.setVisibility(View.GONE);
		xButton = (Button)findViewById(R.id.releaseCamera);
		xButton.setOnClickListener(mReleaseLocalCamera);
		xButton.setVisibility(View.GONE);
		xButton = (Button)findViewById(R.id.stopCamera);
		xButton.setOnClickListener(mStopLocalCamera);
		xButton.setVisibility(View.GONE);

		xView = (LinearLayout)findViewById(R.id.view_settings);
		xView.setVisibility(View.VISIBLE);

		xButton = (Button)findViewById(R.id.getLapse);
		xButton.setText(getString(R.string.string_uiLapseTime) + " " + Integer.toString(lpsTime) + " " + getString(R.string.string_uiSeconds));
		xButton.setOnClickListener(mGetLapseTime);

		lpsViewStart = (TextView)findViewById(R.id.lapseStart);
		lpsViewStart.setText(getString(R.string.string_uiStartLapse) + " " + mYearStart.toString() + "." + String.format("%02d", mMonthStart + 1) + "." + String.format("%02d", mDayStart) + " " + String.format("%02d", mHourStart) + ":" + String.format("%02d", mMinuteStart) + ":00");

		lpsViewEnd = (TextView)findViewById(R.id.lapseEnd);
		lpsViewEnd.setText(getString(R.string.string_uiEndLapse) + " "  + mYearEnd.toString() + "." + String.format("%02d", mMonthEnd + 1) + "." + String.format("%02d", mDayEnd) + " " + String.format("%02d", mHourEnd) + ":" + String.format("%02d", mMinuteStart) + ":00");

		xButton = (Button)findViewById(R.id.startLapseTime);
		xButton.setOnClickListener(mStartLapseTime);
		xButton = (Button)findViewById(R.id.startLapseDate);
		xButton.setOnClickListener(mStartLapseDate);

		xButton = (Button)findViewById(R.id.endLapseTime);
		xButton.setOnClickListener(mEndLapseTime);
		xButton = (Button)findViewById(R.id.endLapseDate);
		xButton.setOnClickListener(mEndLapseDate);

		spinnerAutofocus = (Spinner) findViewById(R.id.spinner_autofocus);
		xAdaptor = ArrayAdapter.createFromResource(this, R.array.choice_autofocus, android.R.layout.simple_spinner_item);
		xAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAutofocus.setAdapter(xAdaptor);

		spinnerResolution = (Spinner) findViewById(R.id.spinner_resolution);
		xAdaptor = ArrayAdapter.createFromResource(this, R.array.choice_resolution, android.R.layout.simple_spinner_item);
		xAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerResolution.setAdapter(xAdaptor);

		spinnerType = (Spinner) findViewById(R.id.spinner_type);
		xAdaptor = ArrayAdapter.createFromResource(this, R.array.choice_type, android.R.layout.simple_spinner_item);
		xAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerType.setAdapter(xAdaptor);

		sv = (SurfaceView)findViewById(R.id.surfacePreview);

		//sh = sv.getHolder();
		//sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		sv.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(camActivity.this, camService.class), mLocalConnection, Context.BIND_AUTO_CREATE );
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mLocalConnection);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_GET_LAPSE_TIME:
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.lapse, null);
			return new AlertDialog.Builder(camActivity.this)
			.setTitle(getString(R.string.dialogTitleLapse_uiSeconds))
			.setView(textEntryView)
			.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					/* User clicked OK so do some stuff */
					AlertDialog ad = (AlertDialog) dialog;
					EditText et = (EditText) ad.findViewById(R.id.edit_lapse);

					lpsTime = Integer.valueOf(et.getText().toString());

					xButton = (Button)findViewById(R.id.getLapse);
					xButton.setText(getString(R.string.string_uiLapseTime) + " " + Integer.toString(lpsTime) + " " + getString(R.string.string_uiSeconds));
				}
			})
			.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					/* User clicked cancel so do some stuff */
				}
			})
			.create();

		case DIALOG_TIME_START:
			return new TimePickerDialog(this, mTimeStartSetListener, mHourStart, mMinuteStart, false);

		case DIALOG_DATE_START:
			return new DatePickerDialog(this, mDateStartSetListener, mYearStart, mMonthStart, mDayStart);

		case DIALOG_TIME_END:
			return new TimePickerDialog(this, mTimeEndSetListener, mHourEnd, mMinuteEnd, false);
		case DIALOG_DATE_END:
			return new DatePickerDialog(this, mDateEndSetListener, mYearEnd, mMonthEnd, mDayEnd);
		}
		return null;
	}

	private TimePickerDialog.OnTimeSetListener mTimeStartSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHourStart = hourOfDay;
			mMinuteStart = minute;
			lpsViewStart.setText(getString(R.string.string_uiStartLapse) + " " + mYearStart.toString() + "." + String.format("%02d", mMonthStart + 1) + "." + String.format("%02d", mDayStart) + " " + String.format("%02d", mHourStart) + ":" + String.format("%02d", mMinuteStart) + ":00");
		}
	};

	private DatePickerDialog.OnDateSetListener mDateStartSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYearStart = year;
			mMonthStart = monthOfYear;
			mDayStart = dayOfMonth;
			lpsViewStart.setText(getString(R.string.string_uiStartLapse) + " " + mYearStart.toString() + "." + String.format("%02d", mMonthStart + 1) + "." + String.format("%02d", mDayStart) + " " + String.format("%02d", mHourStart) + ":" + String.format("%02d", mMinuteStart) + ":00");
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeEndSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHourEnd = hourOfDay;
			mMinuteEnd = minute;
			lpsViewEnd.setText(getString(R.string.string_uiEndLapse) + " " + mYearEnd.toString() + "." + String.format("%02d", mMonthEnd + 1) + "." + String.format("%02d", mDayEnd) + " " + String.format("%02d", mHourEnd) + ":" + String.format("%02d", mMinuteEnd) + ":00");
		}
	};

	private DatePickerDialog.OnDateSetListener mDateEndSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYearEnd = year;
			mMonthEnd = monthOfYear;
			mDayEnd = dayOfMonth;
			lpsViewEnd.setText(getString(R.string.string_uiEndLapse) + " " + mYearEnd.toString() + "." + String.format("%02d", mMonthEnd + 1) + "." + String.format("%02d", mDayEnd) + " " + String.format("%02d", mHourEnd) + ":" + String.format("%02d", mMinuteEnd) + ":00");
		}
	};

	private OnClickListener mGrabLocalCamera = new OnClickListener() {
		public void onClick(View v) {

			if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				startService(new Intent(camActivity.this, camService.class));

				if (!mBoundLocalService.isCameraActive()) {
					mBoundLocalService.giveView(sv, spinnerAutofocus.getSelectedItemPosition(), spinnerResolution.getSelectedItemPosition(), spinnerType.getSelectedItemPosition(), lpsTime,
												mYearStart, mMonthStart, mDayStart, mHourStart, mMinuteStart, mYearEnd, mMonthEnd, mDayEnd, mHourEnd, mMinuteEnd);
				}

				xButton = (Button)findViewById(R.id.grabCamera);
				xButton.setVisibility(View.GONE);
				xButton = (Button)findViewById(R.id.releaseCamera);
				xButton.setVisibility(View.VISIBLE);
				xButton = (Button)findViewById(R.id.startCamera);
				xButton.setVisibility(View.VISIBLE);
				xButton = (Button)findViewById(R.id.stopCamera);
				xButton.setVisibility(View.GONE);

				xView.setVisibility(View.GONE);

			}
			else {
				DisplayToast(getString(R.string.error_sdCard));
			}
		}
	};

	private OnClickListener mReleaseLocalCamera = new OnClickListener() {
		public void onClick(View v) {
			if (mBoundLocalService.isCameraActive()) {
				mBoundLocalService.takeView();
			}

			stopService(new Intent(camActivity.this, camService.class));

			xButton = (Button)findViewById(R.id.grabCamera);
			xButton.setVisibility(View.VISIBLE);
			xButton = (Button)findViewById(R.id.releaseCamera);
			xButton.setVisibility(View.GONE);
			xButton = (Button)findViewById(R.id.startCamera);
			xButton.setVisibility(View.GONE);
			xButton = (Button)findViewById(R.id.stopCamera);
			xButton.setVisibility(View.GONE);

			xView.setVisibility(View.VISIBLE);
		}
	};

	private OnClickListener mStartLocalCamera = new OnClickListener() {
		public void onClick(View v) {
			mBoundLocalService.runCamera();

			xButton = (Button)findViewById(R.id.grabCamera);
			xButton.setVisibility(View.GONE);
			xButton = (Button)findViewById(R.id.releaseCamera);
			xButton.setVisibility(View.INVISIBLE);
			xButton = (Button)findViewById(R.id.startCamera);
			xButton.setVisibility(View.GONE);
			xButton = (Button)findViewById(R.id.stopCamera);
			xButton.setVisibility(View.VISIBLE);

			xView.setVisibility(View.GONE);
		}
	};

	private OnClickListener mStopLocalCamera = new OnClickListener() {
		public void onClick(View v) {
			mBoundLocalService.stopCamera();

			xButton = (Button)findViewById(R.id.grabCamera);
			xButton.setVisibility(View.GONE);
			xButton = (Button)findViewById(R.id.releaseCamera);
			xButton.setVisibility(View.VISIBLE);
			xButton = (Button)findViewById(R.id.startCamera);
			xButton.setVisibility(View.VISIBLE);
			xButton = (Button)findViewById(R.id.stopCamera);
			xButton.setVisibility(View.GONE);

			xView.setVisibility(View.GONE);
		}
	};

	private OnClickListener mGetLapseTime = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_GET_LAPSE_TIME);
		}
	};

	private OnClickListener mStartLapseTime = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_TIME_START);
		}
	};

	private OnClickListener mStartLapseDate = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_DATE_START);
		}
	};

	private OnClickListener mEndLapseTime = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_TIME_END);
		}
	};

	private OnClickListener mEndLapseDate = new OnClickListener() {
		public void onClick(View v) {
			showDialog(DIALOG_DATE_END);
		}
	};

	private ServiceConnection mLocalConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundLocalService = ((camService.LocalBinder)service).getService();

			if (mBoundLocalService.isCameraActive()) {

				if (mBoundLocalService.isCameraRunning()) {
					xButton = (Button)findViewById(R.id.grabCamera);
					xButton.setVisibility(View.GONE);
					xButton = (Button)findViewById(R.id.releaseCamera);
					xButton.setVisibility(View.INVISIBLE);
					xButton = (Button)findViewById(R.id.startCamera);
					xButton.setVisibility(View.GONE);
					xButton = (Button)findViewById(R.id.stopCamera);
					xButton.setVisibility(View.VISIBLE);

					xButton = (Button)findViewById(R.id.getLapse);
					xButton.setText(getString(R.string.string_uiLapseTime) + " " + Integer.toString(lpsTime) + " " + getString(R.string.string_uiSeconds));

					xView.setVisibility(View.GONE);
				}
				else {
					xButton = (Button)findViewById(R.id.grabCamera);
					xButton.setVisibility(View.GONE);
					xButton = (Button)findViewById(R.id.releaseCamera);
					xButton.setVisibility(View.VISIBLE);
					xButton = (Button)findViewById(R.id.startCamera);
					xButton.setVisibility(View.VISIBLE);
					xButton = (Button)findViewById(R.id.stopCamera);
					xButton.setVisibility(View.GONE);

					xButton = (Button)findViewById(R.id.getLapse);
					xButton.setText(getString(R.string.string_uiLapseTime) + " " + Integer.toString(lpsTime) + " " + getString(R.string.string_uiSeconds));

					xView.setVisibility(View.GONE);
				}
			}
			else {

				xButton = (Button)findViewById(R.id.grabCamera);
				xButton.setVisibility(View.VISIBLE);
				xButton = (Button)findViewById(R.id.releaseCamera);
				xButton.setVisibility(View.GONE);
				xButton = (Button)findViewById(R.id.startCamera);
				xButton.setVisibility(View.GONE);
				xButton = (Button)findViewById(R.id.stopCamera);
				xButton.setVisibility(View.GONE);

				xButton = (Button)findViewById(R.id.getLapse);
				xButton.setText(getString(R.string.string_uiLapseTime) + " " + Integer.toString(lpsTime) + " " + getString(R.string.string_uiSeconds));

				xView.setVisibility(View.VISIBLE);
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundLocalService = null;
		}
	};

	private void DisplayToast(String msg)
	{
		Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();        
	}  
}