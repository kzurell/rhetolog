package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.content.Context;
import android.net.Uri;

/**
 * @author kirk
 * Interface for anything that sends sessions (Main Application)
 */
public interface SessionActor {
	public void onSessionSend (Context context, Uri session);
	public void onSessionDelete (Context context, Uri session);
	public void onSessionRename (Context context, Uri session, String newName);
	public Uri onAddParticipant (Context context, Uri contactUri, Uri session);
	public Uri onInsertEvent(String droppedFallacy, int participantId, Uri session, long timestamp);
}
