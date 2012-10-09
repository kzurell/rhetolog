package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class SessionListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	
	public static final int SESSION_LIST_LOADER_ID = 213;
	
	// Name of saved state
    private static final String SESSION_FROM_SAVEDINSTANCE = "SESSIONFROMSAVEDINSTANCE";
    
    /* Dual pane operation */
    boolean mDualPane = false;
    
    /* Callbacks for listener (one or the other activity) */
    
    private Callbacks mCallbacks = sDummyCallbacks;
    
    public interface Callbacks {
    	public void onInitialize(long sessId);
        public void onSessionSelected(long sessId);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onSessionSelected(long sessId) {
        }

		@Override
		public void onInitialize(long sessId) {
		}
    };

    
    
    
    /* Fragment's adapter */
    private SimpleCursorAdapter adapter = null; 
    
    String[] SESSIONS_LIST_PROJECTION = {
    		RhetologContract.SessionsColumns._ID,
    		RhetologContract.SessionsColumns.TITLE
    		};
    int[] SESSIONS_LIST_FIELDS = {
    		0,
    		android.R.id.text1
    };

    /* (non-Javadoc)
	 * @see android.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		
		// Set up adapter for cursor returned by loader.		
		adapter = new SimpleCursorAdapter(getActivity(), 
				android.R.layout.simple_list_item_activated_1, 
				null /* TBA */, 
				SESSIONS_LIST_PROJECTION, 
				SESSIONS_LIST_FIELDS, 
				0);
		setListAdapter(adapter);
				
		// Get the cursor at leisure.
		// Better restarted at onResume, permits recovery after sleep.
		//getActivity().getLoaderManager().initLoader(SESSION_LIST_LOADER_ID, null, this);
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		getActivity().getLoaderManager().restartLoader(SESSION_LIST_LOADER_ID, null, this);
		super.onResume();
	}

	
	/* (non-Javadoc)
	 * @see android.app.Fragment#onStop()
	 */
	@Override
	public void onStop() {
		setListShown(false);
		super.onStop();
	}





	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
        	mCallbacks = (Callbacks) activity;
		} catch (Exception e) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
	    }
        
	}

	
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    
    
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        
        // Activity shows details as possible.
        mCallbacks.onSessionSelected(id);
        
    }

    
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    
    
    
    
    

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader sessionListLoader = null;
		
		switch(arg0) {
		case SESSION_LIST_LOADER_ID:
			sessionListLoader = new CursorLoader(getActivity(), 
					RhetologContract.SESSIONS_URI, 
					SESSIONS_LIST_PROJECTION, 
					null, null, null);
			break;
		}
		
		return sessionListLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		
		int loaderID = arg0.getId();
		
		switch(loaderID) {
		
		case SESSION_LIST_LOADER_ID:
			//adapter.swapCursor(arg1);
			adapter.changeCursor(arg1);
			setListShown(true);
			
			Bundle selSessBundle = getActivity().getContentResolver().call(RhetologContract.PROVIDER_URI, "getCurrentSession", null, null);
			long selectedSessionId = selSessBundle.getLong(RhetologContentProvider.CURRENTSESSIONEXTRA);

			mCallbacks.onInitialize(selectedSessionId);
			break;
		}
		
		
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		
		int loaderID = arg0.getId();
		
		switch (loaderID) {
		case SESSION_LIST_LOADER_ID:
			//adapter.swapCursor(null);
			adapter.changeCursor(null);
			// List setListShown(false) in onStop, this reset happens later than list exists.
			if (mDualPane) {
				getListView().clearChoices();
			}
			break;

		default:
			break;
		}
		
		
	}
}
