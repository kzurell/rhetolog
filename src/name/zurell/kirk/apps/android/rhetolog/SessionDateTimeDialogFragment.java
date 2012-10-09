package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.util.Calendar;
import java.util.Date;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class SessionDateTimeDialogFragment extends DialogFragment 
											implements DatePicker.OnDateChangedListener, 
														TimePicker.OnTimeChangedListener, 
														View.OnClickListener {

	public static String TAG = SessionDateTimeDialogFragment.class.getSimpleName();
	
	static String DATETIMEARG = "DATETIMEARG";

	// References to my UI.
	private DatePicker sessionDate = null;
	private TimePicker sessionTime = null;
	private Button sessionDateTimeDialogButton = null;

	// Calendar for calculations.
	static Calendar workCalendar = null;
	static {
		workCalendar = Calendar.getInstance();
	}

	// Factory
	static SessionDateTimeDialogFragment newInstance(long timestamp) {

		SessionDateTimeDialogFragment newDateTimeDialogFragment = new SessionDateTimeDialogFragment();

		Bundle args = new Bundle();
		args.putLong(DATETIMEARG, timestamp);
		newDateTimeDialogFragment.setArguments(args);

		return newDateTimeDialogFragment;

	}

	
	/* (non-Javadoc)
	 * @see android.app.DialogFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		setStyle(STYLE_NORMAL, 0);
	}


	/* (non-Javadoc)
	 * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Store working copy of current timestamp
		if (savedInstanceState != null) {
			workCalendar.setTimeInMillis(savedInstanceState.getLong(DATETIMEARG));
		} else {
			workCalendar.setTimeInMillis(getArguments().getLong(DATETIMEARG));
		}

		updateTitle();
		
		View v = inflater.inflate(R.layout.sessiondatetimedialog, container, false);

		sessionDateTimeDialogButton = (Button) v.findViewById(R.id.sessiondatetimedialogsetbutton);
		sessionDateTimeDialogButton.setOnClickListener(this);

		sessionDate = (DatePicker) v.findViewById(R.id.sessionDatePicker);
		sessionDate.init(workCalendar.get(Calendar.YEAR), 
				workCalendar.get(Calendar.MONTH), 
				workCalendar.get(Calendar.DAY_OF_MONTH), 
				this /* listener */);

		sessionTime = (TimePicker) v.findViewById(R.id.sessionTimePicker);
		sessionTime.setCurrentHour(workCalendar.get(Calendar.HOUR_OF_DAY));
		sessionTime.setCurrentMinute(workCalendar.get(Calendar.MINUTE));
		sessionTime.setOnTimeChangedListener(this);

		return v;
	}




	/* (non-Javadoc)
	 * @see android.app.DialogFragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(DATETIMEARG, workCalendar.getTimeInMillis());
		super.onSaveInstanceState(outState);
	}



	@Override
	public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
		workCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		workCalendar.set(Calendar.MINUTE, minute);
		updateTitle();

	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		workCalendar.set(year, monthOfYear, dayOfMonth);
		updateTitle();
	}

	private void updateTitle() {

		Date workDate = workCalendar.getTime();
		
		String completeTitle = getActivity().getResources().getString(R.string.sessiontimedatedialogtitle) + " - " + workDate.toString();

		getDialog().setTitle(completeTitle);
	}

	@Override
	public void onClick(View v) {
		
		// Ugly way to set the result, should probably call a MainActivity method but that's not much better
		
		//getActivity().setResult(resultCode, data); ?
		((MainActivity)getActivity()).sessionClock.setSessionTime(workCalendar.getTimeInMillis());
		((MainActivity)getActivity()).sessionClock.setBaseTime(0);
		((MainActivity)getActivity()).sessionClock.setCountingElapsed(false);
		((MainActivity)getActivity()).sessionClock.setRunning(false);

		//((MainActivity)getActivity()).doSessionToggle(false);
		dismiss();

	}

}
