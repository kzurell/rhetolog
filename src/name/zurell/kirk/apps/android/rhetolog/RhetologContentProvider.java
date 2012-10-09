package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

public class RhetologContentProvider extends ContentProvider implements ContentProvider.PipeDataWriter<Cursor> {

	// Ensure content provider access methods are thread safe.
	
	
	@SuppressWarnings("unused")
	private static final String TAG = RhetologContentProvider.class.getSimpleName();
	
	private MainDatabaseHelper mOpenHelper;

	private static final class URI_TYPES {
		private static final int EVENTTABLE = 1; 		// Catalogue of event records.
		private static final int EVENT = 2;      		// Individual event record.
		private static final int SESSIONSTABLE = 3;  	// Catalogue of session records.
		private static final int SESSION = 4;			// Individual session record.
		private static final int PARTICIPANTTABLE = 5; 	// Catalogue of participant records.
		private static final int PARTICIPANT = 6;      	// Individual participant record.
		
		private static final int SESSIONPARTICIPANTTABLE = 7;	// Catalogue of participant records for a specific session.
		private static final int SESSIONPARTICIPANT = 8; 		 
		
		private static final int SESSIONEVENTTABLE = 9; 
		private static final int SESSIONEVENT = 10; 
		
		private static final int REPORT_CSV = 11;
		
		private static final int EVENTSCOUNTSESSION = 12;

		private static final int SESSIONPARTICIPANTEVENTTABLE = 13; // Catalogue of event records for a particular session and participant.
		
		private static final int SESSIONSCURRENT = 14;
		private static final int PARTICIPANTSCURRENTSESSION = 15;
		private static final int EVENTSCURRENTSESSION = 16;
		
	}
	
	// Add sdcard and write external storage permission from manifest.
	private static final String DBNAME = "rhetolog";
	
	
	public static final String EVENTS_TABLENAME = "events";
	public static final String SESSIONS_TABLENAME = "sessions";
	public static final String PARTICIPANTS_TABLENAME = "participants";
	
	
	private SQLiteDatabase db;

	/** The types of URIs serviced by this ContentProvider */
	private static UriMatcher sUriMatcher;
	static	{
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		// Events
		// content://authority/events  --- list of all events
		sUriMatcher.addURI(RhetologContract.AUTHORITY, EVENTS_TABLENAME, URI_TYPES.EVENTTABLE);
		// content://authority/events/# --- one event: one participant received one fallacy at one time.
		sUriMatcher.addURI(RhetologContract.AUTHORITY, EVENTS_TABLENAME + "/#", URI_TYPES.EVENT);
		
		// content://authority/sessions --- all sessions
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME, URI_TYPES.SESSIONSTABLE);
		// content://authority/sessions/# --- one session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/#", URI_TYPES.SESSION);
		
		// content://authority/participants --- all participants 
		sUriMatcher.addURI(RhetologContract.AUTHORITY, PARTICIPANTS_TABLENAME, URI_TYPES.PARTICIPANTTABLE);
		// content://authority/participants/# --- one participant
		sUriMatcher.addURI(RhetologContract.AUTHORITY, PARTICIPANTS_TABLENAME + "/#", URI_TYPES.PARTICIPANT);
		
		// content://authority/sessions/#/participant --- several participants from one session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/#/" + PARTICIPANTS_TABLENAME, URI_TYPES.SESSIONPARTICIPANTTABLE);
		// content://authority/sessions/#/participant/# --- one participant from one session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/#/" + PARTICIPANTS_TABLENAME + "/#", URI_TYPES.SESSIONPARTICIPANT);
		
		// content://authority/sessions/#/events --- all events from a particular session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/#/" + EVENTS_TABLENAME, URI_TYPES.SESSIONEVENTTABLE);
		// content://authority/sessions/#/events/fallacy/$ --- all events from a particular session that match a particular fallacy
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/#/" + EVENTS_TABLENAME + "/#", URI_TYPES.SESSIONEVENT);
		
		// content://authority/sessions/report/# --- all records from a particular session in CSV
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/report/#" , URI_TYPES.REPORT_CSV);
		
		// content://authority/events/count/session/# --- count of events for a particular session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, EVENTS_TABLENAME + "/count/session/#", URI_TYPES.EVENTSCOUNTSESSION);
		
		// content://authority/sessions/#/participants/#/events --- all event records for a particular session and participant
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME 
														+ "/" + PARTICIPANTS_TABLENAME 
														+ "/" + EVENTS_TABLENAME
														+ "/#/#" 
														, 
													URI_TYPES.SESSIONPARTICIPANTEVENTTABLE);
		
		
		// content://authority/sessions/current --- session record of current session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, SESSIONS_TABLENAME + "/current", URI_TYPES.SESSIONSCURRENT);
		// content://authority/participants/currentsession --- participants of current session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, PARTICIPANTS_TABLENAME + "/currentsession", URI_TYPES.PARTICIPANTSCURRENTSESSION);
		// content://authority/events/currentsession --- events from current session
		sUriMatcher.addURI(RhetologContract.AUTHORITY, EVENTS_TABLENAME + "/currentsession", URI_TYPES.EVENTSCURRENTSESSION);
		
	}
	
	
	/** Session management API, additional to ContentProvider 
	 *  The contentprovider maintains a "current" session for the MainActivity to use.
	 *  This is simpler than passing URIs and longs around.
	 *  
	 */
	
	private long currentSession = 0;
	
	public static final String CURRENTSESSIONEXTRA = "CURRENTSESSIONEXTRA";
	private static final String RHETOLOGCONTENTPREFS = "RHETOLOGCONTENTPREFS";
	private static final String CURRENTSESSIONPREFERENCE = "CURRENTSESSIONPREFERENCE";
	
	public Bundle /* unused */ setCurrentSession(String arg, Bundle extraArgs) {
		long toset = Long.valueOf(arg);
		doSetCurrentSession(toset);
		return null;
	}
	
	private void doSetCurrentSession(long newSession) {
		currentSession = newSession;
		
		// Store in shared preference
		SharedPreferences sp  = getContext().getSharedPreferences(RHETOLOGCONTENTPREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = sp.edit();
		ed.putLong(CURRENTSESSIONPREFERENCE, currentSession);
		ed.commit();
		
		// Notify onlookers of new (current) session record.
		getContext().getContentResolver().notifyChange(RhetologContract.SESSIONSCURRENT_URI, null);
		
		// Notify onlookers watching count of events
		getContext().getContentResolver().notifyChange(RhetologContract.EVENTSCOUNTSESSION_URI, null);
		
		// Notify onlookers of new (current) list of participants.
		getContext().getContentResolver().notifyChange(RhetologContract.PARTICIPANTSCURRENTSESSION_URI, null);
	}
	
	
	public Bundle getCurrentSession(String unusedArg, Bundle unusedExtraArgs) {
		
		Bundle returnSessionLong = new Bundle();
		returnSessionLong.putLong(CURRENTSESSIONEXTRA, currentSession);
		
		return returnSessionLong;
	}
	
	
	/** Helper method
	 * 
	 */
	
	private void createFirstSession() {
		// Create initial session record.
		ContentValues cv = new ContentValues();
		cv.put(RhetologContract.SessionsColumns.TITLE, "First Session");
		cv.put(RhetologContract.SessionsColumns.UUID, UUID.randomUUID().toString());
		long newCurrentSession = db.insert(SESSIONS_TABLENAME, null, cv);
		
		doSetCurrentSession(newCurrentSession);
	}
	
	
	/** ContentProvider methods. */
	
	@Override
	public boolean onCreate() {

		mOpenHelper = new MainDatabaseHelper(getContext());

		db = mOpenHelper.getWritableDatabase();
		
		if (db == null) {
			return false;
		}
			
		// Retrieve current session long from preferences, or create new session record and use.
		SharedPreferences sp = getContext().getSharedPreferences(RHETOLOGCONTENTPREFS, Context.MODE_PRIVATE);
		long possibleSession = sp.getLong(CURRENTSESSIONPREFERENCE, 0);
		if(possibleSession == 0) {
			createFirstSession();
		} else {
			doSetCurrentSession(possibleSession);
		}
		
		return true;
	}

	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#call(java.lang.String, java.lang.String, android.os.Bundle)
	 */
	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		
		if (method.contentEquals("getCurrentSession")) {
			return getCurrentSession(arg, extras);
		} else if (method.contentEquals("setCurrentSession")) {
			return setCurrentSession(arg, extras);
		}
		
		return super.call(method, arg, extras);
	}
	
	
	

	@Override
	synchronized public String getType(Uri uri) {
		
		String result = null;
		
		switch(sUriMatcher.match(uri)) {
		
		case URI_TYPES.EVENTTABLE:
		case URI_TYPES.SESSIONEVENTTABLE:
		case URI_TYPES.SESSIONPARTICIPANTEVENTTABLE:
		case URI_TYPES.EVENTSCURRENTSESSION:
			result = RhetologContract.RHETOLOG_TYPE_EVENTTABLE;
			break;
			
		case URI_TYPES.EVENT:
		case URI_TYPES.SESSIONEVENT:
			result = RhetologContract.RHETOLOG_TYPE_EVENT;
			break;
			
		case URI_TYPES.SESSIONSTABLE:
			result = RhetologContract.RHETOLOG_TYPE_SESSIONTABLE;
			break;
			
		case URI_TYPES.SESSION:
		case URI_TYPES.SESSIONSCURRENT:
			result = RhetologContract.RHETOLOG_TYPE_SESSION;
			break;
			
		case URI_TYPES.PARTICIPANTTABLE:
		case URI_TYPES.SESSIONPARTICIPANTTABLE:
		case URI_TYPES.PARTICIPANTSCURRENTSESSION:
			result = RhetologContract.RHETOLOG_TYPE_PARTICIPANTTABLE;
			break;
			
		case URI_TYPES.PARTICIPANT:
		case URI_TYPES.SESSIONPARTICIPANT:
			result = RhetologContract.RHETOLOG_TYPE_PARTICIPANT;
			break;
			
		case URI_TYPES.REPORT_CSV:
			result = RhetologContract.RHETOLOG_TYPE_SESSION_REPORT;
			break;
			
		case URI_TYPES.EVENTSCOUNTSESSION:
			result = RhetologContract.RHETOLOG_TYPE_EVENTSCOUNTSESSION;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		return result;
	}
	
	
	
	@Override
	public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
	
		String[] result = null; 
		
		switch (sUriMatcher.match(uri)) {
		
		case URI_TYPES.REPORT_CSV:	
			String[] csvResult = {RhetologContract.RHETOLOG_TYPE_SESSION_REPORT};
			result = csvResult;
			break;

		default:
			return super.getStreamTypes(uri, mimeTypeFilter);
			
		}
		
		return result;
		
	}
	
	

		
	// Could make this recursive?
	@Override
	synchronized public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// To add to selectionArgs.
		ArrayList<String> completeArgs = new ArrayList<String>();
		if (selectionArgs != null) {
			completeArgs.addAll(Arrays.asList(selectionArgs));
		}
		
		boolean session_stats_changed = false;
		
		Uri notifyUri = null;
		int count = 0;
		
		switch(sUriMatcher.match(uri)) {
		
		case URI_TYPES.EVENTTABLE:
			// All events of particular session
			notifyUri = RhetologContract.EVENTS_URI;
			session_stats_changed = true;
			
			String[] finalETArgs = new String[completeArgs.size()];
			finalETArgs = completeArgs.toArray(finalETArgs);
			
			count = db.delete(EVENTS_TABLENAME, selection, finalETArgs);
			break;

		case URI_TYPES.EVENT:
			String eventId = uri.getPathSegments().get(1);
			if (!TextUtils.isEmpty(selection)) {
				selection = " ( " + selection + " ) AND ";
			} else {
				selection = "";
			}
			selection = selection + " ( " + RhetologContract.EventsColumns._ID + " = ? ) ";
			completeArgs.add(eventId);
			String[] finalEArgs = new String[completeArgs.size()];
			finalEArgs = completeArgs.toArray(finalEArgs);
			count = db.delete(EVENTS_TABLENAME, selection, finalEArgs);
			if (count > 0) {
				session_stats_changed = true;
				notifyUri = RhetologContract.EVENTS_URI;
			}
			break;

//		case URI_TYPES.SESSIONSTABLE:
//			break;

		case URI_TYPES.SESSION: // Checked
			String sessionId = uri.getPathSegments().get(1);
			boolean amDeletingCurrentSession = (Long.valueOf(sessionId) == currentSession);
			
			if (!TextUtils.isEmpty(selection)) {
				selection = " ( " + selection + " ) AND ";
			} else
				selection = "";
			selection += " ( " + RhetologContract.SessionsColumns._ID + " = ? ) ";
			completeArgs.add(sessionId);
			String[] finalSArgs = new String[completeArgs.size()];
			finalSArgs = completeArgs.toArray(finalSArgs);
			
			// Delete session.
			count = db.delete(SESSIONS_TABLENAME, selection, finalSArgs);	
			if (count > 0) {
				notifyUri = RhetologContract.SESSIONS_URI;
			
				// Delete session events.
				String eventSelection = " ( " + RhetologContract.EventsColumns.SESSION + " = ? ) ";
				String[] eventSelectionArgs = {sessionId};
				delete(RhetologContract.EVENTS_URI, eventSelection, eventSelectionArgs);
				
				// Delete session participants.
				String participantSelection = " ( " + RhetologContract.ParticipantsColumns.SESSION + " = ? ) ";
				String[] participantSelectionArgs = {sessionId};
				delete(RhetologContract.PARTICIPANTS_URI, participantSelection, participantSelectionArgs);
			}
			
						
			// If no sessions left, create new one. Must always be a session.
			String[] countColumns = {RhetologContract.SessionsColumns._ID};
			Cursor countQuery = db.query(SESSIONS_TABLENAME, countColumns, null, null, null, null, null);
			if(countQuery != null) {
				if(countQuery.getCount() == 0) {
					createFirstSession();
				} else if(amDeletingCurrentSession) {
					countQuery.moveToFirst();
					int idCol = countQuery.getColumnIndex(RhetologContract.SessionsColumns._ID);
					long firstId = countQuery.getLong(idCol);
					doSetCurrentSession(firstId);
				}
				countQuery.close();
			}
			
			break;

		case URI_TYPES.PARTICIPANTTABLE:
			notifyUri = RhetologContract.PARTICIPANTS_URI;
			String[] finalPTArgs = new String[completeArgs.size()];
			finalPTArgs = completeArgs.toArray(finalPTArgs);
			count = db.delete(PARTICIPANTS_TABLENAME, selection, finalPTArgs);
			break;
			
		case URI_TYPES.PARTICIPANT:
			String participantId = uri.getPathSegments().get(1);
			if (!TextUtils.isEmpty(selection)) {
				selection = " ( " + selection + " ) AND ";
			} else {
				selection = "";
			}
			selection = selection + " ( " + RhetologContract.ParticipantsColumns._ID + " = ? ) ";
			completeArgs.add(participantId);
			String[] finalPArgs = new String[completeArgs.size()];
			finalPArgs = completeArgs.toArray(finalPArgs);
			
			count = db.delete(PARTICIPANTS_TABLENAME, selection, selectionArgs);
			
			if (count > 0) {
				notifyUri = RhetologContract.PARTICIPANTS_URI;
			}
			
			//delete matching events and participants
			String participantEventSelection = " ( " + RhetologContract.EventsColumns.PARTICIPANT + " = ? ) ";
			String[] participantEventSelectionArgs = {participantId};
			delete(RhetologContract.EVENTS_URI, participantEventSelection, participantEventSelectionArgs);
			
			break;
		
		
			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		
		// Notify all interested watchers that a delete has occurred in the original Uri.
		//getContext().getContentResolver().notifyChange(uri, null);
		
		// Notify all interested observers that a delete has occurred as noted
		if (notifyUri != null) {
			getContext().getContentResolver().notifyChange(notifyUri, null);
		}
		
		// Other notifications
		if (session_stats_changed) {
			getContext().getContentResolver().notifyChange(RhetologContract.EVENTSCOUNTSESSION_URI, null);
		}
		
		return count;
	}

	
	@Override
	synchronized public Uri insert(Uri uri, ContentValues values) {
		
		String table_to_insert = null;
		Uri resultUriPrefix = null;
		
		boolean notify_original_uri = false;
		boolean session_stats_changed = false;
		
		
		long sessionToNotify = 0;
		
		
		switch(sUriMatcher.match(uri)) {
		
			// Insert an event to any session
		case URI_TYPES.EVENTTABLE:
			table_to_insert = EVENTS_TABLENAME;
			resultUriPrefix = RhetologContract.EVENTS_URI;
			sessionToNotify = values.getAsLong(RhetologContract.EventsColumns.SESSION);
			session_stats_changed = true;
			break;
			// Insert a session
		case URI_TYPES.SESSIONSTABLE:
			table_to_insert = SESSIONS_TABLENAME;
			resultUriPrefix = RhetologContract.SESSIONS_URI;
			break;
			// Insert a participant to any session, specified in values
		case URI_TYPES.PARTICIPANTTABLE:
			table_to_insert = PARTICIPANTS_TABLENAME;
			resultUriPrefix = RhetologContract.PARTICIPANTS_URI;
			sessionToNotify = values.getAsLong(RhetologContract.ParticipantsColumns.SESSION);
			break;
			
			// Insert an event in the current session
		case URI_TYPES.EVENTSCURRENTSESSION:
			table_to_insert = EVENTS_TABLENAME;
			values.put(RhetologContract.EventsColumns.SESSION, currentSession);
			resultUriPrefix = RhetologContract.EVENTS_URI;
			notify_original_uri = true;
			session_stats_changed = true;
			break;
			// Insert a participant in the current session
		case URI_TYPES.PARTICIPANTSCURRENTSESSION:
			table_to_insert = PARTICIPANTS_TABLENAME;
			values.put(RhetologContract.ParticipantsColumns.SESSION, currentSession);
			resultUriPrefix = RhetologContract.PARTICIPANTS_URI;
			notify_original_uri = true;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		// Do insert
		long rowID = db.insert(table_to_insert, null, values);
		if (rowID <= 0) {
			return null;
		}
		
		// Set result URI and notify
		Uri resultUri = ContentUris.withAppendedId(resultUriPrefix, rowID);
		getContext().getContentResolver().notifyChange(resultUri, null);
		
		// Notify original uri if desired
		if (notify_original_uri) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		
		
		// Notify associated session
		
		if (sessionToNotify > 0) {
			getContext().getContentResolver().notifyChange(Uri.withAppendedPath(RhetologContract.SESSIONS_URI, Long.toString(sessionToNotify)), null);
		}
		
		// Notify that statistics have changed.
		if (session_stats_changed) {
			getContext().getContentResolver().notifyChange(RhetologContract.EVENTSCOUNTSESSION_URI, null);
		}
		
		// Notify anyone watching the session and participant for new events
		if (sUriMatcher.match(uri) == URI_TYPES.EVENTTABLE) {
			String sessionStr = values.getAsString(RhetologContract.EventsColumns.SESSION);
			String partStr = values.getAsString(RhetologContract.EventsColumns.PARTICIPANT);
			Uri precise = Uri.withAppendedPath(RhetologContract.SESSIONSPARTICIPANTSEVENTS_URI, sessionStr + "/" + partStr);
			getContext().getContentResolver().notifyChange(precise, null);
		}
		
		
		return resultUri;

	}

	@Override
	synchronized public Cursor query(Uri uri, String[] projection, String selection,
										String[] selectionArgs, String sortOrder) {

		Uri notificationUri = null;
		
		ArrayList<String> completeProjection = new ArrayList<String>();
		if (projection != null) {
			completeProjection.addAll(Arrays.asList(projection));
		}
		
		// Pre-treat empty selection?
		
		ArrayList<String> completeSelectionArgs = new ArrayList<String>();
		if (selectionArgs != null) {
			completeSelectionArgs.addAll(Arrays.asList(selectionArgs));
		}
		
		Cursor c = null;
		
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		
		
		int uriToMatch = sUriMatcher.match(uri);
		
		switch(uriToMatch) {
		
			// Query for a particular event.
		case URI_TYPES.EVENT:
			builder.setTables(EVENTS_TABLENAME); 
			builder.appendWhere(RhetologContract.EventsColumns._ID + " = " + uri.getPathSegments().get(1));
			if(sortOrder == null || sortOrder.isEmpty()) {
				sortOrder = RhetologContract.EventsColumns.TIMESTAMP;
			}
			notificationUri = RhetologContract.EVENTS_URI;
			break;
			// Query for all events.
		case URI_TYPES.EVENTTABLE: 
			builder.setTables(EVENTS_TABLENAME);
			if(sortOrder == null || sortOrder.isEmpty()) {
				sortOrder = RhetologContract.EventsColumns.TIMESTAMP;
			}
			notificationUri = RhetologContract.EVENTS_URI;
			break;
			// Query for a particular session by ID.
		case URI_TYPES.SESSION: 
			builder.setTables(SESSIONS_TABLENAME);
			String sessionFromUri = uri.getLastPathSegment();
			builder.appendWhere(RhetologContract.SessionsColumns._ID + " = " + sessionFromUri);
			notificationUri = RhetologContract.SESSIONS_URI;
			break;
			// Query for all sessions.
		case URI_TYPES.SESSIONSTABLE: 
			builder.setTables(SESSIONS_TABLENAME);
			notificationUri = RhetologContract.SESSIONS_URI;
			break;
			// Query for a particular participant by ID.
		case URI_TYPES.PARTICIPANT:
			builder.setTables(PARTICIPANTS_TABLENAME);
			String participantFromUri = uri.getLastPathSegment();
			builder.appendWhere(RhetologContract.ParticipantsColumns._ID + " = " + participantFromUri);
			notificationUri = RhetologContract.PARTICIPANTS_URI;
			break;
			// Query for all participants.
		case URI_TYPES.PARTICIPANTTABLE:
			builder.setTables(PARTICIPANTS_TABLENAME);
			notificationUri = RhetologContract.PARTICIPANTS_URI;
			break;
			
		
			
		case URI_TYPES.REPORT_CSV:
			builder.setTables(EVENTS_TABLENAME);
			//completeProjection.add("'Session Report' AS _display_name");
			//completeProjection.add("23 as _size");
			String reportSessionId = uri.getPathSegments().get(2);
			builder.appendWhere(RhetologContract.EventsColumns.SESSION + " = " + reportSessionId);
			break;
			
			// Query for a count of all events of a particular session
		case URI_TYPES.EVENTSCOUNTSESSION: 
			builder.setTables(EVENTS_TABLENAME);
			
			// Find virtual column, replace with expression that generates it.
			int virt = -1;
			for (int i = 0; i < completeProjection.size(); i++) {
				String eachProjColumn = completeProjection.get(i);
				if (eachProjColumn.contentEquals(RhetologContract.EventsCountSessionColumns.EVENTCOUNT)) {
					virt = i;
					break;
				}
			}
			if (virt >= 0) {
				completeProjection.remove(virt);
				completeProjection.add(virt, " COUNT( " + RhetologContract.EventsColumns._ID + ") AS " + RhetologContract.EventsCountSessionColumns.EVENTCOUNT);
			}
				
			builder.appendWhere(RhetologContract.EventsColumns.SESSION + " = ?");
			completeSelectionArgs.add(uri.getPathSegments().get(3));
			
			notificationUri = RhetologContract.EVENTSCOUNTSESSION_URI;
			break;
			// Query for all events from a particular session and participant
		case URI_TYPES.SESSIONPARTICIPANTEVENTTABLE:
			builder.setTables(EVENTS_TABLENAME);
			// An excellent example of why to read the GD docs.
			builder.appendWhere(RhetologContract.EventsColumns.SESSION + " = ? AND " + RhetologContract.EventsColumns.PARTICIPANT + " = ?");
			completeSelectionArgs.add(uri.getPathSegments().get(3));
			completeSelectionArgs.add(uri.getPathSegments().get(4));
			notificationUri = RhetologContract.SESSIONSPARTICIPANTSEVENTS_URI;
			break;
			// Query for a single record of the current session
		case URI_TYPES.SESSIONSCURRENT:
			builder.setTables(SESSIONS_TABLENAME);
			builder.appendWhere(RhetologContract.SessionsColumns._ID + " = ?");
			completeSelectionArgs.add(Long.toString(currentSession));
			notificationUri = RhetologContract.SESSIONSCURRENT_URI;
			break;
			// Query for all participants of the current session
		case URI_TYPES.PARTICIPANTSCURRENTSESSION:
			builder.setTables(PARTICIPANTS_TABLENAME);
			builder.appendWhere(RhetologContract.ParticipantsColumns.SESSION + " = ?");
			completeSelectionArgs.add(Long.toString(currentSession));
			notificationUri = RhetologContract.PARTICIPANTSCURRENTSESSION_URI;
			break;
			// Query for all events of the current session
		case URI_TYPES.EVENTSCURRENTSESSION:
			builder.setTables(EVENTS_TABLENAME);
			builder.appendWhere(RhetologContract.EventsColumns.SESSION + " = ?");
			completeSelectionArgs.add(Long.toString(currentSession));
			notificationUri = RhetologContract.EVENTSCURRENTSESSION_URI;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		
		String[] finalProjection = completeProjection.toArray(new String[completeProjection.size()]);
		String[] finalArgs = completeSelectionArgs.toArray(new String[completeSelectionArgs.size()]);
		
		try {
			c = builder.query(db, finalProjection, selection, finalArgs, null, null, sortOrder);	
		} catch (SQLiteException e) {
			return null;
		}
		
		Log.d(TAG, "builder.query == " + builder.buildQuery(null, null, null, null, null, null));
		
		if (c == null) {
			 return null;
		}
		
		Log.d(TAG, "cursor.count == " + c.getCount());
		
		if(notificationUri != null) {
			c.setNotificationUri(getContext().getContentResolver(), notificationUri);	
		}
		
		
		return c;
	}

	
	
	@Override
	synchronized public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
	
		Uri notifyUri = null;
		
		String updateTable = null;
		
		if (selection == null) selection = "";
		if (!selection.isEmpty())
			selection = " ( " + selection + " ) AND ";
	
		ArrayList<String> completeSelectionArgs = new ArrayList<String>();
		if (selectionArgs != null) {
			completeSelectionArgs.addAll(Arrays.asList(selectionArgs));
		}
	
		int count = 0;
		
		switch(sUriMatcher.match(uri)) {
		
		case URI_TYPES.EVENT:
			updateTable = EVENTS_TABLENAME;
			String whereClause = " ( " + RhetologContract.EventsColumns._ID + " = ? ) ";
			selection += whereClause;
			completeSelectionArgs.add(uri.getLastPathSegment());
			notifyUri = uri;
			break;
			
		case URI_TYPES.SESSION:
			updateTable = SESSIONS_TABLENAME;
			String sessionWhereClause = " ( " + RhetologContract.SessionsColumns._ID + " = ? ) ";
			selection += sessionWhereClause;
			completeSelectionArgs.add(uri.getLastPathSegment());
			notifyUri = uri;
			break;
			
		case URI_TYPES.SESSIONSCURRENT:
			updateTable = SESSIONS_TABLENAME;
			String sessionCWhereClause = " ( " + RhetologContract.SessionsColumns._ID + " = ? ) ";
			selection += sessionCWhereClause;
			completeSelectionArgs.add(Long.toString(currentSession));
			notifyUri = uri;
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);	
			//break;		
		}
		
		// Finalize selection args
		String[] finalSelectionArgs = new String[completeSelectionArgs.size()];
		completeSelectionArgs.toArray(finalSelectionArgs);
		
		// Commit update
		count = db.update(updateTable, values, selection, finalSelectionArgs);
		
		// Notify all observers of change
		if (notifyUri != null) {
			getContext().getContentResolver().notifyChange(notifyUri, null);
		}
		
		// Report to caller
		return count;
	}
	
	
	
	/** Support for CSV reports */
	
	
	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#openTypedAssetFile(android.net.Uri, java.lang.String, android.os.Bundle)
	 */
	@Override
	public AssetFileDescriptor openTypedAssetFile(Uri uri,
			String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
		
		
		switch (sUriMatcher.match(uri)) {
		case URI_TYPES.REPORT_CSV:
			
			String session = uri.getLastPathSegment();
			
			String[] columns = {
					PARTICIPANTS_TABLENAME + "." + RhetologContract.ParticipantsColumns.NAME,
					EVENTS_TABLENAME + "." + RhetologContract.EventsColumns.SESSION,
					EVENTS_TABLENAME + "." + RhetologContract.EventsColumns.PARTICIPANT,
					EVENTS_TABLENAME + "." + RhetologContract.EventsColumns.FALLACY,
					EVENTS_TABLENAME + "." + RhetologContract.EventsColumns.TIMESTAMP
			};
			String selection = EVENTS_TABLENAME + "." + RhetologContract.EventsColumns.SESSION + " = ? ";
			String[] selectionArgs = {session};
			String groupBy = null;
			String having = null;
			String orderBy = null;
			
			String UNIONTABLE = EVENTS_TABLENAME + " INNER JOIN " + PARTICIPANTS_TABLENAME 
					+ " ON " 
					+ EVENTS_TABLENAME + "." + RhetologContract.EventsColumns.PARTICIPANT
					+ " = "
					+ PARTICIPANTS_TABLENAME + "." + RhetologContract.ParticipantsColumns._ID;
			
			Cursor c = db.query(UNIONTABLE, columns, selection, selectionArgs, groupBy, having, orderBy);
			if (c == null || !c.moveToFirst()) {
				if (c != null) {
					c.close();
				}
				throw new FileNotFoundException("Unable to query " + uri.toString());
			}
			
			String mimeType = RhetologContract.RHETOLOG_TYPE_SESSION_REPORT; 
			
			return new AssetFileDescriptor(openPipeHelper(uri, mimeType, opts, c, this), 
					0 /* startOffset */, 
					AssetFileDescriptor.UNKNOWN_LENGTH);
			
			
			//break;

		default:
			return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
		}
		
	}
	
	
	
	/** Support for CSV */
	
	@Override
	public void writeDataToPipe(ParcelFileDescriptor output, Uri uri,
			String mimeType, Bundle opts, Cursor c) {
		FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
		
		PrintWriter pw = null;
		
		try {
			pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
			
			//write csv lines of report
		
			int name = c.getColumnIndex(RhetologContract.ParticipantsColumns.NAME);
			int fallacy = c.getColumnIndex(RhetologContract.EventsColumns.FALLACY);
			int participant = c.getColumnIndex(RhetologContract.EventsColumns.PARTICIPANT);
			int timestamp = c.getColumnIndex(RhetologContract.EventsColumns.TIMESTAMP);
			String line;
			
			while(!c.isAfterLast()) {
				line = c.getString(fallacy) 
						+ "," 
						+ c.getString(participant) 
						+ "," 
						+ c.getString(name) 
						+ "," 
						+ Integer.toString(c.getInt(timestamp))
						+ "\n";
				pw.print(line);
				c.moveToNext();
			}
			
			
		} catch (UnsupportedEncodingException e) {
			// No idea what to do with this
			e.printStackTrace();
		} finally {
			c.close();
			if (pw != null) {
				pw.flush();
			}
			try {
				fout.close();
			} catch (Exception e2) {
				//do nothing
			}
		}
		
	}

	
	

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#openFile(android.net.Uri, java.lang.String)
	 */
	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		
		switch (sUriMatcher.match(uri)) {
		case URI_TYPES.REPORT_CSV:
			break;

		default:
			break;
		}
		return super.openFile(uri, mode);
	}




	private static final String SQL_CREATE_EVENTS_TABLE = "CREATE TABLE "
			+ EVENTS_TABLENAME
			+ " ( "
			+ RhetologContract.EventsColumns._ID + " INTEGER PRIMARY KEY, " 
			+ RhetologContract.EventsColumns.PARTICIPANT + " TEXT, "
			+ RhetologContract.EventsColumns.FALLACY + " TEXT, " 
			+ RhetologContract.EventsColumns.TIMESTAMP + " INTEGER, "
			+ RhetologContract.EventsColumns.SESSION + " INTEGER " // Link to owning session
			+ " ) ";
	
	private static final String SQL_CREATE_SESSIONS_TABLE = "CREATE TABLE "
			+ SESSIONS_TABLENAME
			+ " ( "
			+ RhetologContract.SessionsColumns._ID + " INTEGER PRIMARY KEY, "
			+ RhetologContract.SessionsColumns.TITLE + " TEXT, "
			+ RhetologContract.SessionsColumns.UUID + " TEXT, "
			+ RhetologContract.SessionsColumns.STARTTIME + " TEXT, "
			+ RhetologContract.SessionsColumns.ENDTIME + " TEXT "
			+ " ) ";

	private static final String SQL_CREATE_PARTICIPANTS_TABLE = "CREATE TABLE "
			+ PARTICIPANTS_TABLENAME
			+ " ( "
			+ RhetologContract.ParticipantsColumns._ID + " INTEGER PRIMARY KEY, "
			+ RhetologContract.ParticipantsColumns.NAME + " TEXT, "
			+ RhetologContract.ParticipantsColumns.PHOTO + " TEXT, "
			+ RhetologContract.ParticipantsColumns.LOOKUP + " TEXT, "
			+ RhetologContract.ParticipantsColumns.SESSION + " INTEGER "
			+ " ) ";
	
	
	protected static final class MainDatabaseHelper extends SQLiteOpenHelper {

		private static SQLiteDatabase.CursorFactory cursorFactory = null;
		static {
			cursorFactory = new SQLiteDatabase.CursorFactory() {      
				@Override
				public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
					Log.d(TAG, "Query: "+query);

					return new SQLiteCursor(db, masterQuery, editTable, query);
				}
			};	
		}
		 
		
		public MainDatabaseHelper(Context context) {
			super(context, DBNAME, cursorFactory, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_EVENTS_TABLE);
			db.execSQL(SQL_CREATE_SESSIONS_TABLE);
			db.execSQL(SQL_CREATE_PARTICIPANTS_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
		}

	}

	
	

//	/* Support routines */
//	
//	private static String[] joinStringArrays(String[]...arrays) {
//
//        final List<String> output = new ArrayList<String>();
//
//        for(String[] array : arrays) {
//        	if(array == null) continue;
//        	output.addAll(Arrays.asList(array));
//        }
//
//        return output.toArray(new String[output.size()]);
//
//}
	
}
