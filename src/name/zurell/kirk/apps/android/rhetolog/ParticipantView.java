package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ParticipantView extends RelativeLayout implements OnDragListener {
	
	// The View's related participant by _ID
	private int mParticipant;

	// Reference to the owning Activity.
	public Activity activity;
	
	// Reference to the Application (or otherwise) for performing actions
	SessionActor application = null;
	
	/**
	 * @return the application
	 */
	public SessionActor getApplication() {
		return application;
	}

	/**
	 * @param application the application to set
	 */
	public void setApplication(SessionActor application) {
		this.application = application;
	}


	Context context;
	
	ImageView participantPhoto;
	TextView participantCaption;
	
	/**
	 * @return the mParticipant
	 */
	public int getParticipant() {
		return mParticipant;
	}

	/**
	 * @param mParticipant the mParticipant to set
	 */
	public void setParticipant(int mParticipant) {
		this.mParticipant = mParticipant;
	}

	
	
	public ParticipantView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		// Not sure one can get application context here in design mode.
		application = (SessionActor) ((MainActivity)context).getApplication();
	}
	
	/* (non-Javadoc)
	 * @see android.view.View#onFinishInflate()
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		participantPhoto = (ImageView) findViewById(R.id.participantPhoto);
		participantCaption = (TextView) findViewById(R.id.participantCaption);
		
		this.setOnDragListener(this);
		
	}
	
	
	final static LightingColorFilter mediumDropColor = new LightingColorFilter(0xFF404040, 0xFF101010);
	final static LightingColorFilter lightDropColor = new LightingColorFilter(0xFF808080, 0xFF101010);
	

	// Run by activity onDrag
	@Override
	public boolean onDrag(View v, DragEvent event) {
		
		boolean result = false;
		int action = event.getAction();
		
		switch (action) {
		case DragEvent.ACTION_DRAG_STARTED:
			ParticipantView.this.participantPhoto.setColorFilter(mediumDropColor);
			result = true;
			break;
		case DragEvent.ACTION_DRAG_ENTERED:
			ParticipantView.this.participantPhoto.setColorFilter(lightDropColor);
			result = true;
			break;
		case DragEvent.ACTION_DRAG_EXITED:
			ParticipantView.this.participantPhoto.setColorFilter(mediumDropColor);
			result = true;
			break;
		case DragEvent.ACTION_DROP:
			ParticipantView.this.participantPhoto.clearColorFilter();
			
			// Create new event.
			Fallacy droppedFallacy = (Fallacy) event.getLocalState();
			
			long timestamp = ((MainActivity)this.getContext()).sessionClock.getSessionTime();
			
			//Uri session = ((MainActivity)this.getContext()).getCurrentSession();
			Uri session = RhetologContract.EVENTSCURRENTSESSION_URI;
			
			// Record event.
			if (application != null) {
				application.onInsertEvent(droppedFallacy.getName(), this.getParticipant(), session, timestamp);
			}
			
			result = true;
			break;
		case DragEvent.ACTION_DRAG_ENDED:
			ParticipantView.this.participantPhoto.clearColorFilter();
			result = true;
			break;
		}
			
		return result;
	}

}
