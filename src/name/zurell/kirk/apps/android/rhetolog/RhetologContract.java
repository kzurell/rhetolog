package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.net.Uri;
import android.provider.BaseColumns;

public class RhetologContract {

	public static final String AUTHORITY = "name.zurell.kirk.apps.android.Rhetolog.contentprovider";
	
	private static final String METHOD = "content://";
	
	private static final String PROVIDER = METHOD + AUTHORITY;
	
	public static final String EVENTS = METHOD + AUTHORITY + "/events";
	public static final String SESSIONS_TABLE = "sessions";
	public static final String SESSIONS = METHOD + AUTHORITY + "/" + SESSIONS_TABLE;
	public static final String PARTICIPANTS = METHOD + AUTHORITY + "/participants";
	
	public static final String SESSIONSREPORT = METHOD + AUTHORITY + "/sessions/report";
	
	
	public static final String EVENTSCOUNTSESSION = METHOD + AUTHORITY + "/events/count/session";
	
	public static final String SESSIONSPARTICIPANTSEVENTS = METHOD + AUTHORITY + "/sessions/participants/events";
	
	public static final String SESSIONSCURRENT = SESSIONS + "/current";
	public static final String PARTICIPANTSCURRENTSESSION = PARTICIPANTS + "/currentsession";
	public static final String EVENTSCURRENTSESSION = EVENTS + "/currentsession";
	
	/* Uri forms */
	
	public static final Uri PROVIDER_URI = Uri.parse(PROVIDER);
	
	public static final Uri SESSIONS_URI = Uri.parse(SESSIONS);
	public static final Uri EVENTS_URI = Uri.parse(EVENTS);
	public static final Uri PARTICIPANTS_URI = Uri.parse(PARTICIPANTS);
	
	public static final Uri SESSIONSREPORT_URI = Uri.parse(SESSIONSREPORT);
	
	public static final Uri EVENTSCOUNTSESSION_URI = Uri.parse(EVENTSCOUNTSESSION);
	
	public static final Uri SESSIONSPARTICIPANTSEVENTS_URI = Uri.parse(SESSIONSPARTICIPANTSEVENTS);
	
	public static final Uri SESSIONSCURRENT_URI = Uri.parse(SESSIONSCURRENT);
	public static final Uri PARTICIPANTSCURRENTSESSION_URI = Uri.parse(PARTICIPANTSCURRENTSESSION);
	public static final Uri EVENTSCURRENTSESSION_URI = Uri.parse(EVENTSCURRENTSESSION);
	
	
	/* Types */
	public static final String RHETOLOG_TYPE_EVENTTABLE = "vnd.com.cursor.dir/vnd.name.zurell.kirk.apps.android.rhetolog.events";
	public static final String RHETOLOG_TYPE_EVENT = "vnd.com.cursor.item/vnd.name.zurell.kirk.apps.android.rhetolog.events";
	
	public static final String RHETOLOG_TYPE_SESSIONTABLE = "vnd.com.cursor.dir/vnd.name.zurell.kirk.apps.android.rhetolog.sessions";
	public static final String RHETOLOG_TYPE_SESSION = "vnd.com.cursor.item/vnd.name.zurell.kirk.apps.android.rhetolog.sessions";
	
	public static final String RHETOLOG_TYPE_PARTICIPANTTABLE = "vnd.com.cursor.dir/vnd.name.zurell.kirk.apps.android.rhetolog.participants";
	public static final String RHETOLOG_TYPE_PARTICIPANT = "vnd.com.cursor.item/vnd.name.zurell.kirk.apps.android.rhetolog.participants";
	
	/* Reports */
	public static final String RHETOLOG_TYPE_SESSION_REPORT = "*/*";
	
	/* Statistics */
	public static final String RHETOLOG_TYPE_EVENTSCOUNTSESSION = "vnd.com.cursor.item/vnd.name.zurell.kirk.apps.android.rhetolog.eventscountsession";

	
	/* Columns */
	
	public class EventsColumns implements BaseColumns {
		public static final String _ID = "_id";
		public static final String _COUNT = "_count";
		
		public static final String TIMESTAMP = "TIMESTAMP";
		public static final String PARTICIPANT = "PARTICIPANT";
		public static final String FALLACY = "FALLACY";
		public static final String SESSION = "SESSION";
	}
	
	public class SessionsColumns implements BaseColumns {
		public static final String _ID = "_id";
		public static final String _COUNT = "_count";
		
		public static final String UUID = "SESSIONUUID";
		public static final String TITLE = "TITLE";
		public static final String STARTTIME = "STARTTIME";
		public static final String ENDTIME = "ENDTIME";
	}
	
	public class ParticipantsColumns implements BaseColumns {
		public static final String _ID = "_id";
		public static final String _COUNT = "_count";
		
		public static final String NAME = "NAME";
		public static final String PHOTO = "PHOTO";
		public static final String LOOKUP = "LOOKUP";
		public static final String SESSION = "SESSION";
	}
	
	public class EventsCountSessionColumns {
		public static final String SESSION = "SESSION";
		public static final String EVENTCOUNT = "EVENTCOUNT";
	}
	
	
	
}
