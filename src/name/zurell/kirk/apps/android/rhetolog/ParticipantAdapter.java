package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ParticipantAdapter extends CursorAdapter implements LoaderCallbacks<Cursor> {

	@SuppressWarnings("unused")
	private static final String TAG = ParticipantAdapter.class.getSimpleName();

	// Arbitrary constants
	private static final int PARTICIPANT_LOADER_ID = 111;
	
	private static final String LOADER_SESSION_ARG = "session_arg";

	
	LayoutInflater mInflater;
	Activity mActivity;

	

	public ParticipantAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mInflater = LayoutInflater.from(context);
		mActivity = (Activity) context;

		// We're using participants/currentsession
		mActivity.getLoaderManager().restartLoader(PARTICIPANT_LOADER_ID, null, this);
	}

	
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		// Note participant _ID.
		int participantIdCol = cursor.getColumnIndex(RhetologContract.ParticipantsColumns._ID);
		int participant = cursor.getInt(participantIdCol);
		((ParticipantView)view).setParticipant(participant); 

		
		// Bind the participants' caption.
		TextView partCaption;
		partCaption = (TextView) view.findViewById(R.id.participantCaption);

		String caption = cursor.getString(cursor.getColumnIndex(RhetologContract.ParticipantsColumns.NAME));
		partCaption.setText(caption);


		// Bind participant thumbnail or placeholder.
		String thumbUriString;
		thumbUriString = cursor.getString(cursor.getColumnIndex(RhetologContract.ParticipantsColumns.PHOTO));

		ImageView partPhoto = (ImageView) view.findViewById(R.id.participantPhoto);
		partPhoto.setContentDescription(caption);

		if (thumbUriString == null || thumbUriString.isEmpty()) {
			partPhoto.setImageResource(R.drawable.rhetolog_participant);
		} else {
			Uri thumbUri = Uri.parse(thumbUriString);
			partPhoto.setImageURI(thumbUri);
		}

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ParticipantView workView = (ParticipantView) mInflater.inflate(R.layout.participantentry, parent, false);
		
		// Let each participant know their owning activity for drag & drop, their application as session actor for actions.
		workView.activity = mActivity;
		workView.setApplication((SessionActor) mActivity.getApplication());
		
		return workView;
	}

	/**
	 * Loader to populate participants list in main view.
	 */
	
	// Projection for adding participants.
	private static final String[] PARTICIPANT_PROJECTION = {
		RhetologContract.ParticipantsColumns.NAME,
		RhetologContract.ParticipantsColumns.SESSION,
		RhetologContract.ParticipantsColumns.PHOTO,
		RhetologContract.ParticipantsColumns._ID
	};
	// Where clause, actual session number provided by init/restart above whenever the selected session changes
	//private static final String PARTICIPANT_SELECTION = RhetologContract.ParticipantsColumns.SESSION + " = ? ";

	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		
		CursorLoader result = null;

		switch (id) {
		case PARTICIPANT_LOADER_ID:
			result = new CursorLoader(mActivity, 
						RhetologContract.PARTICIPANTSCURRENTSESSION_URI,
						PARTICIPANT_PROJECTION, 
						null, //PARTICIPANT_SELECTION, 
						null, 
						null);
			
			break;
		default:
			break;
		}

		return result;

	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
		
		switch(loader.getId()) {
		
		case PARTICIPANT_LOADER_ID:
			//swapCursor(newCursor);
			changeCursor(newCursor);
			break;
		default:
			break;
		}
		
	}

	

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
		case PARTICIPANT_LOADER_ID:
			//swapCursor(null);
			changeCursor(null);
			break;
		default:
			break;
		}
		
	}

}
