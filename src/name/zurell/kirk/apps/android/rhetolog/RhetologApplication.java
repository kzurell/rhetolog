package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.util.Log;


/**
 * Application singleton for Rhetolog
 * @author kirk
 *
 */

public class RhetologApplication extends Application implements SessionActor {

	private String TAG = RhetologApplication.class.getSimpleName();
	
	public static String RHETOLOG_PREFERENCES = "RHETOLOG_PREFRENCES";
	public static String RHETOLOG_CURRENTSESSION = "RHETOLOG_CURRENTSESSION";
	
	public static String RHETOLOG_MAINSESSION = "RHETOLOG_MAINSESSION";
	
	/* (non-Javadoc)
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		
		// Load static fallacy records
		loadFallacies();
		
	}
	
	
	/** Manage Fallacy records */

	public static List<Fallacy> fallacies;
	public static LinkedHashMap<String, Fallacy> mFallacies;
	
	private void loadFallacies() {
		InputStream jsonStream = this.getResources().openRawResource(R.raw.fallacies);
		JSONObject jsonObject;
		JSONArray jsonFallacies;
		JSONObject jsonFallacy;
		Fallacy eachFallacy;
		
		try {
			
			jsonObject = new JSONObject(convertStreamToString(jsonStream));
			jsonFallacies = jsonObject.getJSONArray("fallacies");
			fallacies = new ArrayList<Fallacy>();
			mFallacies = new LinkedHashMap<String, Fallacy>();
			for (int i = 0, m = jsonFallacies.length(); i < m; i++) {
				jsonFallacy = jsonFallacies.getJSONObject(i);
				eachFallacy = new Fallacy();
				eachFallacy.setName(jsonFallacy.getString("name"));
				eachFallacy.setTitle(jsonFallacy.getString("title"));
				eachFallacy.setDescription(jsonFallacy.getString("description"));
				eachFallacy.setExample(jsonFallacy.getString("example"));
				eachFallacy.setSortOrder(jsonFallacy.getString("sortorder"));
				eachFallacy.setColor(jsonFallacy.getInt("color"));
				
				String iconName = jsonFallacy.getString("icon");
				
				try {
					Class<?> drawableClass = R.drawable.class; // replace package
					Field drawableField = drawableClass.getField(iconName);
					int drawableId = (Integer)drawableField.get(null);
					Drawable drawable = this.getResources().getDrawable(drawableId);
					eachFallacy.setIcon(drawable);
				} catch (Exception e) {
					// NoSuchFieldException, IllegalAccessException, IllegalArgumentException, NotFoundException
					// On most any exception, use placeholder
					Drawable backup = this.getResources().getDrawable(R.drawable.empty);
					eachFallacy.setIcon(backup);
				}
				
				//TODO One or the other
				fallacies.add(eachFallacy);
				mFallacies.put(eachFallacy.getName(), eachFallacy);
			}
			
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		
		
		
	}
	
	
	private String convertStreamToString(final InputStream is)
    {

        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        final StringBuilder stringBuilder = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null)
            {
                stringBuilder.append(line + "\n");
            }
        } catch (final IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            try {
                is.close();
            } catch (final IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return stringBuilder.toString();
    }


	/**
	 * @return the fallacies
	 */
	public static List<Fallacy> getFallacies() {
		return fallacies;
	}

	public static Fallacy getFallacyNamed(String name) {
		return mFallacies.get(name);	
	}
	

	
	
	
	/** Data management support routines */
	
	public Uri insertContactIntoParticipants(Uri contact, Uri session) {
		
		String[] contactProjection = {
			ContactsContract.Contacts.DISPLAY_NAME,
			ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
		};
		
		Cursor contactQuery = getContentResolver().query(contact, contactProjection, null, null, null);	
		
		if ((contactQuery == null) || (!contactQuery.moveToFirst())) {
			return null;
		}
		
		int nameCol = contactQuery.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		int photoCol = contactQuery.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
		
		String participantName = null;
		String participantPhoto = null;
		
		if(contactQuery.getType(nameCol) == Cursor.FIELD_TYPE_STRING)
			participantName = contactQuery.getString(nameCol);
		else
			participantName = getResources().getString(R.string.defaultParticipantCaption);
			
		if (contactQuery.getType(photoCol) == Cursor.FIELD_TYPE_STRING) {
			participantPhoto = contactQuery.getString(photoCol);
		} else {
			participantPhoto = "android.resource://" 
						+ this.getPackageName() + "/"
						+ Integer.toString(R.drawable.rhetolog_participant);
		}
		
		contactQuery.close();
				
		ContentValues values = new ContentValues();
		values.put(RhetologContract.ParticipantsColumns.NAME, participantName);
		values.put(RhetologContract.ParticipantsColumns.PHOTO, participantPhoto);
		values.put(RhetologContract.ParticipantsColumns.LOOKUP, contact.toString());
		
		// Learn session id, use in participants/session/id
//		String sessionId = session.getLastPathSegment();
//		long currentSessionId;
//		if (sessionId.contentEquals("currentsession")) {
//			Bundle currentSessionBundle = getContentResolver().call(RhetologContract.PROVIDER_URI, "getCurrentSession", null, null);
//			currentSessionId = currentSessionBundle.getLong(RhetologContentProvider.CURRENTSESSIONEXTRA);
//		} else {
//			currentSessionId = Long.valueOf(sessionId);
//		}
//		
//		// Ugly, not sure how to make less so.
//		values.put(RhetologContract.ParticipantsColumns.SESSION, currentSessionId);

		Uri newParticipant = getContentResolver().insert(session, values);
		
		return newParticipant;
		
	}
	
	public Uri insertEventByParticipantInSession(String fallacyName, int participant, Uri session, long timestamp) {
		
		ContentValues values = new ContentValues();
		values.put(RhetologContract.EventsColumns.FALLACY, fallacyName);
		values.put(RhetologContract.EventsColumns.PARTICIPANT, participant);
		values.put(RhetologContract.EventsColumns.TIMESTAMP, timestamp);
		
		Uri newEvent = getContentResolver().insert(session, values);
		return newEvent;
	}

	public String newSessionTitle() {
		String dateNow = DateFormat.getDateFormat(this).format(System.currentTimeMillis());
		String timeNow = DateFormat.getTimeFormat(this).format(System.currentTimeMillis());
		String newDefaultTitle = getResources().getString(R.string.newSessionDefaultTitle) + " - " + dateNow + " " + timeNow;
		return newDefaultTitle;
	}

	public Uri insertSession(String newTitle) {
		
		ContentValues values = new ContentValues();
		values.put(RhetologContract.SessionsColumns.TITLE, newTitle);
		values.put(RhetologContract.SessionsColumns.UUID, UUID.randomUUID().toString());
		
		Uri newSession = null;
		
		newSession = getContentResolver().insert(RhetologContract.SESSIONS_URI, values);
		
		return newSession;
	}
	
	/** Generate (placeholder) report for numbered session */
	
	String reportForSession(Uri session) {
		
		// Use own contentprovider streaming?
		
		Uri uri = Uri.withAppendedPath(RhetologContract.SESSIONSREPORT_URI, session.getLastPathSegment());
		try {
			InputStream inputStream = getContentResolver().openInputStream(uri);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			StringBuilder stringBuilder = new StringBuilder();
			
			stringBuilder.append("Report for session " + session.toString() + "\n\n" );
			
			String line;
			while( (line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append("\n"); // oh well.
			}
			
			return stringBuilder.toString();
			
		} catch (IOException e) {
			return null;
		}
		
	}
		
	
	
	/** Called when a session detail view asks to send session results somewhere. 
	 * Generates a link to the session report Uri, and lets the remote program read from it. 
	 * The {@link RhetologContentProvider} generates the result file on demand.
	 * 
	 * */
	
	@Override
	public void onSessionSend(Context context, Uri session) {
		
		// Send to the program selected by the chooser below.
		Intent sendSession = new Intent(Intent.ACTION_SEND);
		
		// Permit to read from Rhetolog URIs.
		sendSession.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		// Note type of send as report.
		sendSession.setType(RhetologContract.RHETOLOG_TYPE_SESSION_REPORT);

		sendSession.putExtra(Intent.EXTRA_SUBJECT, "Sending session " + session);

		// Send text as text.
		sendSession.putExtra(Intent.EXTRA_TEXT, reportForSession(session));
		
		// Send text as URI to be read.
		sendSession.putExtra(Intent.EXTRA_STREAM, Uri.parse(RhetologContract.SESSIONSREPORT + "/" + session.getLastPathSegment()));
		
		// Permit user to choose target of send.
		context.startActivity(Intent.createChooser(sendSession, "Send session results"));
	}


	@Override
	public void onSessionDelete(Context context, Uri session) {
		ContentResolver cr = getContentResolver();
		
		// Remove session from session list, and remove all related participant and event records.
		cr.delete(session, null, null);
	}

	@Override
	public void onSessionRename(Context context, Uri session, String newName) {
		ContentResolver cr = getContentResolver();
		
		ContentValues values = new ContentValues();
		values.put(RhetologContract.SessionsColumns.TITLE, newName);
		
		cr.update(session, values, null, null);

	}
	

	@Override
	public Uri onAddParticipant(Context context, Uri contactUri, Uri session) {
		return insertContactIntoParticipants(contactUri, session);
	}


	@Override
	public Uri onInsertEvent(String droppedFallacy, int participantId,
			Uri session, long timestamp) {
		return insertEventByParticipantInSession(droppedFallacy, participantId, session, timestamp);
	}
	
	
	public void setSessionStartTime(Uri sessionToSet, long timestamp) {
		ContentValues values = new ContentValues(1);
		values.put(RhetologContract.SessionsColumns.STARTTIME, timestamp);
		getContentResolver().update(sessionToSet, values, null, null);
	}
	
	public void setSessionEndTime(Uri sessionToSet, long timestamp) {
		ContentValues values = new ContentValues(1);
		values.put(RhetologContract.SessionsColumns.ENDTIME, timestamp);
		getContentResolver().update(sessionToSet, values, null, null);
	}


	
	
}
