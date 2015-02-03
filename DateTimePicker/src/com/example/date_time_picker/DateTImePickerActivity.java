package com.example.date_time_picker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;


public class DateTImePickerActivity extends Activity {

	private TextView mDateDisplay;
	private Button mPickDate;
	private int mYear;
	private int mMonth;
	private int mDay;
	private Button mPickTime;
	private TextView mTimeDisplay;
	private int mHour;
	private int mMinute;
	private TextView mEpochTimeDisplay;
	private long epochTime;
	Date date;

	static final int DATE_DIALOG_ID = 0;
	static final int TIME_DIALOG_ID = 1;

	// The callback received when the user "sets" the date in the Dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			updateDisplay(0);
			updateDisplay(2);
		}
	};
	// The callback received when the user "sets" the time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mHour = hourOfDay;
			mMinute = minute;
			updateDisplay(1);
			updateDisplay(2);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_date_time_picker);

		// Capture our View elements
		mDateDisplay = (TextView) findViewById(R.id.dateDisplay);
		mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);
		mEpochTimeDisplay = (TextView) findViewById(R.id.epochTimeDisplay);
		mPickDate = (Button) findViewById(R.id.pickDate);
		mPickTime = (Button) findViewById(R.id.pickTime);

		// Set an OnClickListener on the Change The Date Button
		mPickDate.setOnClickListener(new View.OnClickListener() {
			@SuppressWarnings("deprecation")
			public void onClick(View v) {
				showDialog(DATE_DIALOG_ID);
			}
		});
		// Set an OnClickListener on the Change The Time Button
		mPickTime.setOnClickListener(new View.OnClickListener() {
					@SuppressWarnings("deprecation")
					public void onClick(View v) {
						showDialog(TIME_DIALOG_ID);
					}
				});

		// Get the current date
		final Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mHour = c.get(Calendar.HOUR_OF_DAY);
		mMinute = c.get(Calendar.MINUTE);
		//date = c.getTime();	//get the current date time
		//epochTime = date.getTime();	//get the epoch time
		// = setEpochTime(mYear, mMonth,  mDay,  mHour,mMinute);
		// Display the current date
		updateDisplay(0);
		//display the current time
		updateDisplay(1);
		//display Epoch time
		updateDisplay(2);
		
		
	}
	
	
	// Update the date in the TextView
	//replace this update display function with the access to DB
	private void updateDisplay(int type) {
		switch(type) {
		case 0:
			mDateDisplay.setText(new StringBuilder()
					// Month is 0 based so add 1
					.append(mMonth + 1).append("-").append(mDay).append("-")
					.append(mYear).append("   "));
			mEpochTimeDisplay.setText(new StringBuilder().append(epochTime));
			break;
		case 1:
			mTimeDisplay.setText(new StringBuilder().append(pad(mHour)).append(":").append((mMinute)));
			mEpochTimeDisplay.setText(new StringBuilder().append(epochTime));
			break;
		case 2:
			mEpochTimeDisplay.setText(Long.toString(setEpochTime(mYear, mMonth,  mDay,  mHour,
					 mMinute)));
			break;
		}
	}
	private long setEpochTime(int mYear2, int mMonth2, int mDay2, int mHour2,
			int mMinute2) {
		
		
		StringBuilder str = new StringBuilder().append(mYear2).append('-')
				.append(mMonth2).append('-').append(mDay2).append("T")
				.append(mHour2).append(':')
				.append(mMinute2).append(':')
				.append("00.000-0700");	//truncating 
		
		//String str = "Jun 13 2003 23:11:52.454 UTC";
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date date = null;
		try {
			date = df.parse(str.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long epoch = date.getTime();
		return epoch;
	}


		// Prepends a "0" to 1-digit minutes 
		private static String pad(int c) {
			if (c >= 10)
				return String.valueOf(c);
			else
				return "0" + String.valueOf(c);
		}

	// Create and return DatePickerDialog
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
					mDay);
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute,
					false);
		}
		return null;
	}
}
