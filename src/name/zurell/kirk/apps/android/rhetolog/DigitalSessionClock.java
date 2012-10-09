package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */
import java.util.Calendar;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

public class DigitalSessionClock extends TextView {

	final String TAG = DigitalSessionClock.class.getSimpleName();
    
	// Time stores
	private long mSessionTime = System.currentTimeMillis();
	private long mBaseTime = mSessionTime;
	
	// States
	private boolean mCountingElapsed = false;
	private boolean mRunning = true;
	
	
	/**
	 * Reset the timer to current time, do not count elapsed time, and let clock run free.
	 */
	public void reset() {
		this.setSessionTime(System.currentTimeMillis());
		this.setBaseTime(0);
		this.setCountingElapsed(false);
		this.setRunning(true);
	}
	
	
	/**
	 * @return the mSessionTime in milliseconds
	 */
	public long getSessionTime() {
		return mSessionTime;
	}

	/**
	 * @param mSessionTime the mSessionTime to set in milliseconds
	 * 
	 * Setting a session (start) time sets the stored session time, clears elapsed time, and controls the clock.
	 */
	public void setSessionTime(long mSessionTime) {
		this.mSessionTime = mSessionTime;
		updateDisplay();
	}
	
	
	/**
	 * Retrieve the base time for counting elapsed time.
	 * @return The base time for counting elapsed time.
	 */
	public long getBaseTime() {
		return this.mBaseTime;
	}
	
	/**
	 * Set the base time for elapsed time calculation.
	 * @param mBaseTime Timestamp in milliseconds since the epoch to use as base for elapsed time.
	 */
	public void setBaseTime(long mBaseTime) {
		this.mBaseTime = mBaseTime;
		updateDisplay();
	}
	
	/**
	 * Set the base time and start counting elapsed time.
	 * @param startCountingElapsed
	 */
	public void setCountingElapsed(boolean startCountingElapsed) {
		this.mCountingElapsed = startCountingElapsed;
	}
	
	public boolean isCountingElapsed() {
		return this.mCountingElapsed;
	}
	
	/**
	 * Report whether the clock is running or not.
	 * @return the mRunning
	 */
	public boolean isRunning() {
		return mRunning;
	}

	/**
	 * Set the clock to running or no.
	 * @param mRunning the mRunning to set
	 */
	public void setRunning(boolean newRunState) {
		//record current state
		this.mRunning = newRunState;
	}

	
	
	
	/**
	 * Handler and Runnable for timekeeping behind main thread.
	 */
	private Handler mHandler;
	private Runnable mTicker;
	
	/**
	 * Private calendar for time conversion.
	 */
	private Calendar mCalendar;

	/**
	 * Time formats.
	 */
	String mFormat = "MM/dd/yy h:mm:ss aa";
	String mElapsedFormat = "kk:mm:ss";
	
	
	
	public DigitalSessionClock(Context context) {
		super(context);
		initClock(context);
	}

	public DigitalSessionClock(Context context, AttributeSet attrs) {
		super(context, attrs);
		initClock(context);
	}

	/**
	 * Initialiser
	 * @param context The UI context
	 */
	public void initClock(Context context) {
		if (mCalendar == null) {
			mCalendar = Calendar.getInstance();
		}
		mHandler = new Handler();
	}
	

	
	/* (non-Javadoc)
	 * @see android.widget.DigitalClock#onAttachedToWindow()
	 */
	@Override
	protected void onAttachedToWindow() {
		//mRunning = false;
        super.onAttachedToWindow();
        
        //if not already running & want to run, start running
        /**
         * requests a tick on the next hard-second boundary
         */
        mTicker = new Runnable() {
        	public void run() {
        		
        		if (mRunning) {
        			// Increment session time.
        			mSessionTime += 1000;
        		}
        	
        		// Every time, forever, solely to ensure elapsed time is refreshed after orientation recreate while paused. Yuck.
        		updateDisplay();
        		invalidate();	
			
        		long now = SystemClock.uptimeMillis();
        		long next = now + (1000 - now % 1000);
        		mHandler.postAtTime(mTicker, next);
        	}
        };
        mTicker.run();
       
	}
	
	@Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRunning = false;
    }

	
	private void updateDisplay() {
		
		String baseTimePart = getResources().getString(R.string.defaultClockValue);
		String wallTimePart = getResources().getString(R.string.defaultClockValue);
		
		//setText("Session Time: " + Long.toString(mSessionTime) + " / Elapsed " + Long.toString(mBaseTime));
		
		long elapsedSinceBase = mSessionTime - mBaseTime;
		if (elapsedSinceBase < 0 ) elapsedSinceBase = 0;
		
		// Determine elapsed time in HH:MM:SS
		if (mCountingElapsed) {
			
			long elapsedSeconds = elapsedSinceBase / 1000;
			long elapsedMinutes = elapsedSeconds / 60;
			elapsedSeconds -= elapsedMinutes * 60;
			long elapsedHours = elapsedMinutes / 60;
			elapsedMinutes -= elapsedHours * 60;
			baseTimePart = String.format("%02d:%02d:%02d", elapsedHours, elapsedMinutes, elapsedSeconds);
			
		}

		// Convert current millisecond time to human readable timestamp.
		mCalendar.setTimeInMillis(mSessionTime);
		
		if (mSessionTime > 0) {
			wallTimePart = DateFormat.format(mFormat, mCalendar).toString();
		}
		
		setText("Session Time: " + wallTimePart + " / Elapsed Time: " + baseTimePart);
		
	}
	
}
