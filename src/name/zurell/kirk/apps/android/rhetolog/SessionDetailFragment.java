package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


public class SessionDetailFragment extends Fragment implements 
		SimpleCursorAdapter.ViewBinder, 
		SimpleGraphView.OnGenerateStatisticsListener {

	@SuppressWarnings("unused")
	private static final String TAG = SessionDetailFragment.class.getSimpleName();

	
		
	
	/** Implementation of SimpleCursorAdapter.ViewBinder */
	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		// Set onclick callback with int participantToDelete.

		if (view.getId() == R.id.participantStatsGraph) {
			
			int partId = cursor.getInt(columnIndex);

			Uri spe = Uri.withAppendedPath(RhetologContract.SESSIONSPARTICIPANTSEVENTS_URI, 
					Long.toString(mSelectedSession) + "/" + Integer.toString(partId));


			((SimpleGraphView) view).setActivity(getActivity());
			((SimpleGraphView) view).setColors(Arrays.asList(colors));
			((SimpleGraphView) view).setGenerateStatisticsListener(this);
			//trigger auto updates
			((SimpleGraphView) view).setDataSource(spe);

			return true;
		}
		
		return false;
	}




	
	/** Operations on sessions. */
	SessionActor mSessionActions = null;



	/* Loader ID */
	private static final int PARTICIPANTLISTLOADERID = 45;
	private static final int CONTACTPICKERRESULT = 23;

	
	
	public interface Callbacks {
		public void onDeleteParticipantClicked(int participantToDelete);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onDeleteParticipantClicked(int participantToDelete) {
		}
	};

	private Callbacks mCallbacks;

	
	public static final String ARG_ITEM_ID = "item_id";

	long mSelectedSession;
	Uri mSelectedSessionUri;

	SimpleCursorAdapter participantListAdapter = null;


	/** Factory method */
	public static SessionDetailFragment newInstance(long id) {
		SessionDetailFragment newSessDF = new SessionDetailFragment();

		Bundle args = new Bundle();
		args.putLong(ARG_ITEM_ID, id);
		newSessDF.setArguments(args);

		return newSessDF;
	}

	public long getShownIndex() {
		return getArguments().getLong(ARG_ITEM_ID, 0);
	}




	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (container == null)
			return null;

		mSelectedSession = getArguments().getLong(ARG_ITEM_ID);
		mSelectedSessionUri = Uri.withAppendedPath(RhetologContract.SESSIONS_URI, Long.toString(mSelectedSession));
		
		
		View rootView = inflater.inflate(R.layout.fragment_session_detail, container, false);


		// Load this session's metadata.
		String[] sessionProjection = {
				RhetologContract.SessionsColumns.TITLE,
				RhetologContract.SessionsColumns.STARTTIME,
				RhetologContract.SessionsColumns.ENDTIME
		};
		Cursor sessionData = getActivity().getContentResolver().query(mSelectedSessionUri, sessionProjection, null, null, null);
		if (sessionData == null) {
			return null;
		}
		if (!sessionData.moveToFirst()) {
			return null;
		}
		
		int sessTitleCol = sessionData.getColumnIndex(RhetologContract.SessionsColumns.TITLE);
		String sessTitle = sessionData.getString(sessTitleCol);
		int sessStartCol = sessionData.getColumnIndex(RhetologContract.SessionsColumns.STARTTIME);
		long sessStart = sessionData.getLong(sessStartCol);
		int sessEndCol = sessionData.getColumnIndex(RhetologContract.SessionsColumns.ENDTIME);
		long sessEnd = sessionData.getLong(sessEndCol);

		sessionData.close();
		
		
		// Static?
		Calendar sessionCal = Calendar.getInstance();
		String sessionDateFormat = "MM/dd/yy h:mm:ss";
		
		String sessionTimesDescription = "Session times: ";
		if (sessStart != 0) {
			sessionCal.setTimeInMillis(sessStart);
			sessionTimesDescription += "(Start) " + DateFormat.format(sessionDateFormat, sessionCal);
		} else {
			sessionTimesDescription += "(start not set)";
		}
		sessionTimesDescription += " ";
		if (sessEnd != 0) {
			sessionCal.setTimeInMillis(sessEnd);
			sessionTimesDescription += "(End) " + DateFormat.format(sessionDateFormat, sessionCal);
		} else {
			sessionTimesDescription += "(end not set)";
		}

		// Set title until session is loaded.
		getActivity().setTitle(sessTitle);


		TextView timeString = (TextView) rootView.findViewById(R.id.detailTitle);
		timeString.setText(sessionTimesDescription);




		// Configure action buttons
		
		// Add participant to session.
		((Button)rootView.findViewById(R.id.addParticipantButton)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent pickParticipant = new Intent("android.intent.action.PICK", 
						ContactsContract.Contacts.CONTENT_URI);
				startActivityForResult(pickParticipant, CONTACTPICKERRESULT);
			}

		});

		// Send session (data) to somewhere else.
		((Button)rootView.findViewById(R.id.sendsessionbutton)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mSessionActions.onSessionSend(getActivity(), mSelectedSessionUri);
			}

		});

		// Delete this session.
		((Button)rootView.findViewById(R.id.sessionDeleteButton)).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mSessionActions.onSessionDelete(getActivity(), mSelectedSessionUri);
				// Dismiss fragment.
			}
		});

		// Rename session.
		((Button)rootView.findViewById(R.id.sessionRenameButton)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Views for new session dialog
            	View sessionDetails = LayoutInflater.from(getActivity()).inflate(R.layout.sessiondetails, null);
            	final EditText sessionName = (EditText) sessionDetails.findViewById(R.id.sessionname);
            	
            	new AlertDialog.Builder(getActivity())
            	.setIcon(android.R.drawable.ic_menu_edit)
            	.setTitle("Rename Session")
            	.setView(sessionDetails)
            	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String newSessionName = sessionName.getText().toString();
						mSessionActions.onSessionRename(getActivity(), mSelectedSessionUri, newSessionName);
					}
				}).show();
            	
				
			}
		});
		
		
		
		
		
		// Set adapter for participantList.
		ListView participantList = (ListView) rootView.findViewById(R.id.participantList);

		final String[] PARTICIPANTS_LIST_PROJECTION = {
				RhetologContract.ParticipantsColumns._ID,
				RhetologContract.ParticipantsColumns.NAME, 
				RhetologContract.ParticipantsColumns.SESSION,
				RhetologContract.ParticipantsColumns.PHOTO 
			}; //NOTE SEE BELOW.
		int[] PARTICIPANTS_LIST_FIELDS = {
				R.id.participantStatsGraph, 
				R.id.participantlistname,
				0,
				R.id.participantlistimage };

		participantListAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.participantlistentry,
				null /* tba */, 
				PARTICIPANTS_LIST_PROJECTION, 
				PARTICIPANTS_LIST_FIELDS, 
				/* flags */ 0);
		participantListAdapter.setViewBinder(this);
		participantList.setAdapter(participantListAdapter);

		

		/* Loader for SessionDetail list of participants. */
		
		getActivity().getLoaderManager().initLoader(PARTICIPANTLISTLOADERID, null, new LoaderCallbacks<Cursor>() {

			String PARTICIPANTS_LIST_SELECTION = RhetologContract.ParticipantsColumns.SESSION + " = ? ";

			@Override
			public Loader<Cursor> onCreateLoader(int id, Bundle args) {
				Loader<Cursor> result = null;
				switch(id) {
				case PARTICIPANTLISTLOADERID:
					String[] selectionArgs = {Long.toString(mSelectedSession)};
					result = new CursorLoader(getActivity(), 
							RhetologContract.PARTICIPANTS_URI, 
							PARTICIPANTS_LIST_PROJECTION, 
							PARTICIPANTS_LIST_SELECTION, 
							selectionArgs, 
							null);
					break;
				}
				return result;
			}

			@Override
			public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
				//participantListAdapter.swapCursor(arg1);	
				participantListAdapter.changeCursor(arg1);
			}

			@Override
			public void onLoaderReset(Loader<Cursor> arg0) {
				//participantListAdapter.swapCursor(null);
				participantListAdapter.changeCursor(null);
			}
		});


		return rootView;
	}



	/* (non-Javadoc)
	 * @see android.app.Fragment#onDestroyView()
	 */
	@Override
	public void onDestroyView() {
		getActivity().getLoaderManager().destroyLoader(PARTICIPANTLISTLOADERID);
		super.onDestroyView();
	}



	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		//Note the enclosing application, fail if it won't work
		try {
			mSessionActions = (SessionActor) activity.getApplication();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.getApplication().toString() + " must implement SessionActions");
		}

		try {
			mCallbacks = (Callbacks) activity;
		} catch (ClassCastException e) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}


	/* (non-Javadoc)
	 * @see android.app.Fragment#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case CONTACTPICKERRESULT:

			if (data == null) return;

			if (resultCode == android.app.Activity.RESULT_OK) {

				Uri selectedSessionUri = Uri.withAppendedPath(RhetologContract.SESSIONS_URI, Long.toString(mSelectedSession));

				Uri contact = data.getData();
				mSessionActions.onAddParticipant(this.getActivity(), contact, selectedSessionUri);

			}
			break;
		}
	}

	
	
	public static Integer[] colors = {
		0xFF7E4F25, //see fallacies.json
		0xFFe20c18,
		0xFF9c92c5,
		0xFFe6007e,
		0xFF509b73,
		0xFF238dcd
	};
	public static ArrayList<Integer> colorsList = new ArrayList<Integer>();


	@Override
	public ArrayList<Float> OnGenerateStatistics(Uri uri, Cursor cursor) {


		
		int valOfA = Character.getNumericValue('a');

		// Count results
		int[] sixCounts = new int[6];
		int total = 0;
		
		// Return as percentages
		Float[] sixResults = new Float[6];
		Arrays.fill(sixResults, (float)0.0);

		// Column for fallacy.
		int nameCol = cursor.getColumnIndex(RhetologContract.EventsColumns.FALLACY);
		
		// Get ready.
		cursor.moveToFirst();

		// Read fallacies.
		while(!cursor.isAfterLast()) {
			total++;

			String eventFallacy = cursor.getString(nameCol);
			Fallacy eachFallacy = RhetologApplication.getFallacyNamed(eventFallacy);
			if (eachFallacy == null) continue;

			// Find sortorder letter category and count.
			Character c = eachFallacy.getSortOrder().charAt(0);
			int index = Character.getNumericValue(c) - valOfA;
			if (index > 5) 
				index = 5;
			if (index < 0)
				index = 0;
			sixCounts[index]++;

			cursor.moveToNext();
		}

		
		if (total > 0) {
			sixResults[0] = (float)sixCounts[0] / total;
			sixResults[1] = (float)sixCounts[1] / total;
			sixResults[2] = (float)sixCounts[2] / total;
			sixResults[3] = (float)sixCounts[3] / total;
			sixResults[4] = (float)sixCounts[4] / total;
			sixResults[5] = (float)sixCounts[5] / total;
		}
		
		
		ArrayList<Float> results = new ArrayList<Float>(6);
		results.addAll(Arrays.asList(sixResults));
		return results;
	}



}
