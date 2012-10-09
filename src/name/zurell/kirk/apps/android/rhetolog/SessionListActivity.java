package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class SessionListActivity extends Activity
        implements SessionListFragment.Callbacks, 
        			SessionDetailFragment.Callbacks {

    private boolean mDualPane = false;

    private SessionListFragment sessionList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Layout that contains fragment session_list.
        setContentView(R.layout.activity_session_list);
        
        if (findViewById(R.id.session_detail_container) != null) {
            mDualPane = true;
        }
        
        // Enable up button
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().show();
        
        // Session list fragment is automatically created.
        sessionList = ((SessionListFragment) getFragmentManager().findFragmentById(R.id.session_list));
        sessionList.setActivateOnItemClick(mDualPane);
        
        // How to initialize listfragment?
        // sessionList.getListView().set
        
    }
    

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.sessions_main, menu);
		return true;
	}
    
	
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	finish(); // The activity
                return true;
            
            case R.id.newsession:
            	// Views for new session dialog.
            	View sessionDetails = LayoutInflater.from(this).inflate(R.layout.sessiondetails, null);
            	final EditText sessionName = (EditText) sessionDetails.findViewById(R.id.sessionname);
            	
            	new AlertDialog.Builder(this)
            	.setIcon(android.R.drawable.ic_menu_add)
            	.setTitle("New Session")
            	.setView(sessionDetails)
            	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String newSessionName = sessionName.getText().toString();
						
						RhetologApplication application = (RhetologApplication) SessionListActivity.this.getApplication();
						
						Uri resultSession = null;
						resultSession = application.insertSession(newSessionName);
						
						final long newSessId = Long.valueOf(resultSession.getLastPathSegment());
						SessionListActivity.this.onSessionSelected(newSessId);
						
						// Select new session in list once it appears.
						// Consider also postDelayed
//						sessionList.getListView().post(new Runnable() {
//							@Override
//							public void run() {
//								sessionList.setSelectedSession(newSessId);
//								SessionListActivity.this.onSessionSelected(newSessId);
//							}
//						});
						
					}
				}).show();
            	
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }

    
    /** SessionListFragment.Callbacks */
    
    private void displaySessionDetail(final long sessId) {
    	if (mDualPane) {
    		Handler why = new Handler(Looper.getMainLooper());
    		// Don't know why but it works.
    		why.post(new Runnable() {
				@Override
				public void run() {
					SessionDetailFragment fragment = SessionDetailFragment.newInstance(sessId);
		            getFragmentManager().beginTransaction()
		                    .replace(R.id.session_detail_container, fragment)
		                    .commit();		
				}
			});
        } else {
        	Intent detailIntent = new Intent(this, SessionDetailActivity.class);
            detailIntent.putExtra(SessionDetailFragment.ARG_ITEM_ID, sessId);
            startActivity(detailIntent);
        }
    }
    
    @Override
    public void onSessionSelected(long sessId) {
        
    	// Session selected becomes current session
    	this.getContentResolver().call(RhetologContract.PROVIDER_URI, "setCurrentSession", Long.toString(sessId), null);
    	
    	/* Display details */
    	// Needed if updated listview cursor loader runs onInitialize?
        displaySessionDetail(sessId);
    	
//        // Back to Uri land
//        Uri resultSession = Uri.withAppendedPath(RhetologContract.SESSIONS_URI, Long.toString(sessId));
        
        // Set result of session list selection for main activity
        Intent resultIntent = new Intent();
//        resultIntent.setData(resultSession);
        
        setResult(RESULT_OK, resultIntent);
    }

    @Override
	public void onInitialize(long sessId) {

    	// If using a dual pane layout, select the current session and display its detail.
    	if(mDualPane) {
			// Determine item position for specific ID
			ListView lv = sessionList.getListView();
			int positionFromID = -1;
			for (int i = 0; i < lv.getCount(); i++) {
				if (lv.getItemIdAtPosition(i) == sessId) {
					positionFromID = i; break;
				}
			}
			// Set the selection based on the ID
			//lv.setSelection(positionFromID);
			lv.setItemChecked(positionFromID, true);	
		
			displaySessionDetail(sessId);
		}	
		
	}

	/** SessionDetailFragment.Callbacks */

	@Override
	public void onDeleteParticipantClicked(int participantToDelete) {
		
		Uri deleteParticipant = Uri.withAppendedPath(
				RhetologContract.PARTICIPANTS_URI, 
				Integer.toString(participantToDelete)
				);
		
		getContentResolver().delete(deleteParticipant, null, null);
	}


	
	
}
