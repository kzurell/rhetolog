package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import name.zurell.kirk.apps.android.rhetolog.FallacyAdapter.FallacyListDropListener;
import name.zurell.kirk.apps.android.rhetolog.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.devsmart.android.ui.*;




public class MainActivity extends Activity implements 
			OnCheckedChangeListener, 
			FallacyListDropListener ,
			LoaderManager.LoaderCallbacks<Cursor>
			{

	


	// Handler for this package
	Handler handler = new Handler();
	
	
	
	/* Miscellaneous arbitrary constants */
	
	public final static int MAINACTIVITYLOADER = 135;
	public final static int ADDCONTACTREQUEST = 234;
	public final static int CHANGESESSION = 124;
	
	private final static String STATECURRENTTIMESTAMP = "STATECURRENTTIMESTAMP";
	private final static String STATEBASETIMESTAMP = "STATEBASETIMESTAMP";
	private final static String STATEISCOUNTINGELAPSED = "STATEISCOUNTINGELAPSED";
	private final static String STATEISCLOCKRUNNING ="STATEISCLOCKRUNNING";
	private final static String STATESESSIONCHECKED ="STATESESSIONCHECKED";
	private final static String STATEPAUSEENABLED ="STATEPAUSEENABLED";
	private final static String STATEPAUSECHECKED ="STATEPAUSECHECKED";
	
	//cache my owning application
	RhetologApplication application;
	
	/* User interface objects */
	
	//list of fallacies
	ListView fallacyList;
	
	//list of participants for the current session
	HorizontalListView participantList;
	ParticipantAdapter participantAdapter; //was ParticipantAdapter
	
	//list of most-recently used fallacies (manually added now)
	HorizontalListView mruList;
	MRUAdapter mruAdapter;
	
	// Session time
	DigitalSessionClock sessionClock;
	ToggleButton sessionToggle;
	ToggleButton pauseToggle;
	
	

	
	
	
	
	
	
	/* Session displayed */

	//private int mCurrentSession;
	//private Uri mCurrentSessionUri;
	
	/* Initial configuration */
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove after debugging completed
        //LoaderManager.enableDebugLogging(true);
		
        
        application = (RhetologApplication) getApplication();
        
        
        // Display Rhetolog main activity layout.
        setContentView(R.layout.activity_main);
        
        
        // Convert menu to action bar.
        ActionBar actionBar = getActionBar();
        actionBar.show();
        
        //Set interim title
        setTitle("Rhetolog - Loading");
        
        
        
       
        
        
        
        /* List of participants */
        
        // List for participants.
        participantList = (HorizontalListView) findViewById(R.id.participantList);
        //TextView emptyParticipantListView = (TextView) findViewById(R.id.emptyParticipantListView);
        //participantList.setEmptyView(emptyParticipantListView);
        
        participantAdapter = new ParticipantAdapter(this, null, 0);
        participantList.setAdapter(participantAdapter);
        
        //getLoaderManager().initLoader(MAINACTIVITYLOADER, null, this);
        //participantAdapter.notifyDataSetChanged(); ???
        
        // Watch for Session data changes (Title)
        getContentResolver().registerContentObserver(RhetologContract.SESSIONSCURRENT_URI, true, this.sessionTitleObserver);
        // Watch for Session statistics changes (Event count).
        getContentResolver().registerContentObserver(RhetologContract.EVENTSCOUNTSESSION_URI, true, this.sessionStatsObserver);
       
        
        /* List of most recently used Fallacies */
        
        mruList = (HorizontalListView) findViewById(R.id.MostRecentlyUsed);
        mruAdapter = new MRUAdapter(this, 0);
        mruAdapter.setList(mruList);
        mruList.setAdapter(mruAdapter);
        mruAdapter.add(null); // Placeholder at end of list.
        
        
        /* List of Fallacies */
        
        fallacyList = (ListView) findViewById(R.id.fallacyList);
        FallacyAdapter fallacyAdapter = new FallacyAdapter(this, R.layout.fallacyentry);
        fallacyList.setAdapter(fallacyAdapter); 
        // MRU adapter handles drops on fallacyList as a "remove" action from mruList.
        fallacyList.setOnDragListener(mruAdapter);
        //fallacyAdapter.setDropListener(this);
        
        
       
        /* Configure clocks and clock control buttons. */
        sessionClock = (DigitalSessionClock) findViewById(R.id.sessionTime);
        sessionToggle = (ToggleButton) findViewById(R.id.sessionToggle);
        pauseToggle = (ToggleButton) findViewById(R.id.pauseToggle);

        
        
        boolean fromSavedInstance = savedInstanceState != null;
        
        if (fromSavedInstance) {
        	sessionClock.setSessionTime(
        			savedInstanceState.getLong(STATECURRENTTIMESTAMP, System.currentTimeMillis())
        			);
        	sessionClock.setBaseTime(
        			savedInstanceState.getLong(STATEBASETIMESTAMP, 0)
        			);
        	sessionClock.setCountingElapsed(
        			savedInstanceState.getBoolean(STATEISCOUNTINGELAPSED, false)
        			);
        	sessionClock.setRunning(
        			savedInstanceState.getBoolean(STATEISCLOCKRUNNING, true)
        			);
        	
        	sessionToggle.setChecked(
        			savedInstanceState.getBoolean(STATESESSIONCHECKED, false)
        			);
        	
        	pauseToggle.setEnabled(
        			savedInstanceState.getBoolean(STATEPAUSEENABLED, false)
        			); // Must enable by session toggle.
            pauseToggle.setChecked(
            		savedInstanceState.getBoolean(STATEPAUSECHECKED, false)
            		);
            
        } else {
        	sessionClock.reset();
        	sessionToggle.setChecked(false);
        	pauseToggle.setEnabled(false);
        	pauseToggle.setChecked(false);
        }
        
        sessionToggle.setOnCheckedChangeListener(this);
        pauseToggle.setOnCheckedChangeListener(this);
        
        
        // Cause one initial update of title.
        this.runOnUiThread(new Runnable() {
        	@Override
        	public void run() {
        		sessionStatsObserver.onChange(false);
        		sessionTitleObserver.onChange(false);
        	}
        });
        
        
    }
    
   
    

    /* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putLong(STATECURRENTTIMESTAMP, sessionClock.getSessionTime());
		outState.putLong(STATEBASETIMESTAMP, sessionClock.getBaseTime());
		outState.putBoolean(STATEISCOUNTINGELAPSED, sessionClock.isCountingElapsed());
		outState.putBoolean(STATEISCLOCKRUNNING, sessionClock.isRunning());
		outState.putBoolean(STATESESSIONCHECKED, sessionToggle.isChecked());
		outState.putBoolean(STATEPAUSEENABLED, pauseToggle.isEnabled());
		outState.putBoolean(STATEPAUSECHECKED, pauseToggle.isChecked());
		super.onSaveInstanceState(outState);
	}

    
    
    /* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// Anything to destroy loader/cursor?
		
		// No longer watch for title changes.
		getContentResolver().unregisterContentObserver(sessionTitleObserver);
		getContentResolver().unregisterContentObserver(sessionStatsObserver);
		
		super.onDestroy();
	}




	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
	
	
	private void addNewSessionHelper() {
		String newSessionTitle = application.newSessionTitle();
		Uri newSession;
		newSession = application.insertSession(newSessionTitle);
		String newSessionId;
		newSessionId = newSession.getLastPathSegment();

		getContentResolver().call(RhetologContract.PROVIDER_URI, "setCurrentSession", newSessionId, null);
	}
	

    /* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = false;
		
		switch(item.getItemId()) {
		
		case R.id.menu_setsessiondateandtime:
			
			// Apparently there's nothing as sensible as startFragmentForResult.
			
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			
			SessionDateTimeDialogFragment setSessionDateTimeFragment = SessionDateTimeDialogFragment.newInstance(sessionClock.getSessionTime());
			setSessionDateTimeFragment.show(ft, "setdatetime");
			
			break;
			
		case R.id.addparticipant:
			Intent pickParticipant = new Intent(Intent.ACTION_PICK, 
					ContactsContract.Contacts.CONTENT_URI);
			startActivityForResult(pickParticipant, ADDCONTACTREQUEST);
			result = true;
			break;
			
		case R.id.addsession:
			addNewSessionHelper();
			result = true;
			break;
			
		case R.id.menu_settings:
//			Intent settingsIntent = new Intent(getApplicationContext(), 
//					PreferencesActivity.class);
//			startActivity(settingsIntent);
//			result = true;
			break;
			
		case R.id.menu_sessions:
			Intent sessionsIntent = new Intent(getApplicationContext(), 
					SessionListActivity.class);
			// Pass currently appearing session to sessions list
//			if (mCurrentSessionUri != null) {
//				int sessionToSelect = Integer.valueOf(getCurrentSession().getLastPathSegment());
//				sessionsIntent.putExtra(SessionDetailFragment.ARG_ITEM_ID, sessionToSelect);
//			}
			startActivityForResult(sessionsIntent, CHANGESESSION);
			result = true;
			break;
			
		case R.id.webhelp:
			Uri webHelpUri = Uri.parse("http://rhetolog.kirk.zurell.name");
			Intent webHelpIntent = new Intent(Intent.ACTION_VIEW);
			webHelpIntent.setData(webHelpUri);
			startActivity(webHelpIntent);
			break;
			
		case R.id.about:
			
			WebView aboutWebView = new WebView(this);
			aboutWebView.loadUrl("file:///android_asset/about.html");
			
			AlertDialog.Builder aboutDialogBuilder = new AlertDialog.Builder(this);
			
			aboutDialogBuilder.setTitle("Rhetolog");
			aboutDialogBuilder.setView(aboutWebView);
			//aboutDialogBuilder.setMessage(R.string.aboutText);
			aboutDialogBuilder.setIcon(R.drawable.rhetolog_launcher);
			aboutDialogBuilder.setCancelable(false);
			aboutDialogBuilder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			
			AlertDialog aboutDialog = aboutDialogBuilder.create();
			aboutDialog.show();
			
			break;
		}
		
		
		return result | super.onOptionsItemSelected(item);
	}

	
    
	


	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ADDCONTACTREQUEST:
			//add new contact
			if (resultCode == RESULT_OK) {
				
				Uri contact = data.getData();
				
				application.insertContactIntoParticipants(contact, RhetologContract.PARTICIPANTSCURRENTSESSION_URI);
				
			}
			break;
		case CHANGESESSION:
			
			if (resultCode == RESULT_OK) {
				//Uri newSession = data.getData();
				
				// Reset clock
				sessionClock.reset();
				
				//setCurrentSession(newSession, true /* reset clock */);
			}
		default:
			break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}


	
	
	/**
	 * @return the mCurrentSession
	 */
//	public Uri getCurrentSession() {
//		return mCurrentSessionUri;
//	}

	/**
	 * @param newSession the mCurrentSession to set
	 */
//	public void setCurrentSession(Uri newSession, boolean resetClock) {
//		
//		SharedPreferences sp = getSharedPreferences(RhetologApplication.RHETOLOG_PREFERENCES, Context.MODE_PRIVATE);
//		SharedPreferences.Editor ed = sp.edit();
//		
//		if(newSession == null)
//		{
//			ed.remove(RhetologApplication.RHETOLOG_CURRENTSESSION);
//		}
//		else
//		{
//			ed.putString(RhetologApplication.RHETOLOG_CURRENTSESSION, newSession.toString());
//		}
//		ed.commit();
//				
//		// Local record of current session.
//		mCurrentSessionUri = newSession;
//
//		// Pretend the database notified us about an updated event count.
//		sessionTitleObserver.onChange(true);
//		sessionStatsObserver.onChange(true);
//		
//		// Nudge everything MainActivity knows is watching.
//		participantAdapter.setCurrentSession(mCurrentSessionUri);
//		
//		if (resetClock) {
//			sessionClock.reset();
//		}
//		
//	}




//	/**
//	 * @return the mTimestamp
//	 */
//	public long getTimestamp() {
//		return sessionClock.getSessionTime();
//	}
//
//	public void setTimestamp(long timestamp) {
//		sessionClock.setSessionTime(timestamp);
//	}

	
	

	



	
	



	/* Control for DigitalSessionClock */

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (buttonView.equals(sessionToggle)) {
			
			if (isChecked) {
				// Store start time in current session.
				application.setSessionStartTime(RhetologContract.SESSIONSCURRENT_URI, sessionClock.getSessionTime());
				
				// Start clock at whatever it's set at.
				sessionClock.setBaseTime(sessionClock.getSessionTime());
				sessionClock.setCountingElapsed(true);
				sessionClock.setRunning(true);
				
				pauseToggle.setEnabled(true);
			} else {
				// Store end time in current session
				application.setSessionEndTime(RhetologContract.SESSIONSCURRENT_URI, sessionClock.getSessionTime());
				// Reset clock to current time & let freewheel.
				sessionClock.reset();
				// Unset pause button and disable.
				
				pauseToggle.setChecked(false);
				pauseToggle.setEnabled(false);
			}
			
		} else if (buttonView.equals(pauseToggle)) {
			
			// If checked, stop clock from running right now.
			boolean running = !isChecked;
			sessionClock.setRunning(running);
			
		} else { // No other togglebuttons.
			
		}
		
		
	}

	

	/* DYNAMIC TITLE CONTROL */

	/* ContentObserver class for title manipulation */
	
	private class MainTitleObserver extends ContentObserver {

		public MainTitleObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			
			// Retrieve and update title
			String[] titleProjection = {
					RhetologContract.SessionsColumns.TITLE,
			};
			Cursor titleCursor = getContentResolver().query(RhetologContract.SESSIONSCURRENT_URI, titleProjection, null, null, null);
			if (titleCursor != null) {
				if (titleCursor.moveToFirst()) {
					int titleCol = titleCursor.getColumnIndex(titleProjection[0]);
					String title = titleCursor.getString(titleCol);
					setCurrentTitle(title);
				}
				titleCursor.close();
			}
			
		}

	}

	MainTitleObserver sessionTitleObserver = new MainTitleObserver(handler);
	
	private class MainSessionStatsObserver extends ContentObserver {

		public MainSessionStatsObserver(Handler handler) {
			super(handler);
		}
	
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			
			String[] projection = {
					RhetologContract.EventsCountSessionColumns.EVENTCOUNT, //NB Pseudo-column
					RhetologContract.EventsCountSessionColumns.SESSION
			};
			Bundle sessionBundle = getContentResolver().call(RhetologContract.PROVIDER_URI, "getCurrentSession", null, null);
			long session = sessionBundle.getLong(RhetologContentProvider.CURRENTSESSIONEXTRA);
			Uri sessionToCount = Uri.withAppendedPath(
					RhetologContract.EVENTSCOUNTSESSION_URI, 
					Long.toString(session)
					);

			Cursor countCursor = getContentResolver().query(sessionToCount, projection, null, null, null);

			if (countCursor != null) {
				if (countCursor.moveToFirst()) {
					int eventCountCol = countCursor.getColumnIndex(RhetologContract.EventsCountSessionColumns.EVENTCOUNT);
					int eventCount = countCursor.getInt(eventCountCol);	
					setCurrentCount(eventCount);
				}
				countCursor.close();	
			}
		}
	}
	
	MainSessionStatsObserver sessionStatsObserver = new MainSessionStatsObserver(handler);
	

	/* Activity title composition */

	String mCurrentTitle;
	int mCurrentCount;
	
	private void setCurrentTitle(String mCurrentTitle) {
		this.mCurrentTitle = mCurrentTitle;
		updateCurrentTitle();
	}

	private void setCurrentCount(int mCurrentCount) {
		this.mCurrentCount = mCurrentCount;
		updateCurrentTitle();
	}

	private void updateCurrentTitle() {
		String compTitle = mCurrentTitle;
		if(mCurrentCount > 0) {
			int pluralId = (mCurrentCount == 1 ? R.string.entrypluralsingle : R.string.entrypluralplural);
			String plural = getResources().getString(pluralId);
			compTitle = compTitle + " - " + Integer.toString(mCurrentCount) + " " + plural;
		}
		setTitle(compTitle);
	}




	@Override
	public void OnFallacyListDrop(Fallacy fallacy) {
		// Nothing to do here now.
	}





	/*
	 * on() {
	 *   new Thread(new Runnable{ run() { ... handler.postxxx(new Runnable() { run() { updatemain }}) } })
	 * }
	 * 
	 */

	
	
	public void moveMRUleft(View v) {
		mruList.scrollTo(0);
	}
	
	public void moveMRUright(View v) {
		mruList.scrollTo(mruList.getWidth());
	}


	
	/* loader for */

	String[] participantProjection = {
			RhetologContract.ParticipantsColumns._ID,
			RhetologContract.ParticipantsColumns.NAME,
			RhetologContract.ParticipantsColumns.PHOTO,
			RhetologContract.ParticipantsColumns.SESSION
	};
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		CursorLoader cl = new CursorLoader(this, 
				RhetologContract.PARTICIPANTSCURRENTSESSION_URI, 
				participantProjection, 
				null, 
				null, 
				null);
		
		return cl;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		//participantAdapter.swapCursor(data);
		participantAdapter.changeCursor(data);
	}

	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		//participantAdapter.swapCursor(null);
		participantAdapter.changeCursor(null);
	}
	    
    
}
