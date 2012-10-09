package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

public class SessionDetailActivity extends Activity implements SessionDetailFragment.Callbacks {

	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        
        // Set detail page.
        setContentView(R.layout.activity_session_detail);

        // Allow return to previous activity.
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        
        // Session ID as long
        long sessionId = getIntent().getExtras().getLong(SessionDetailFragment.ARG_ITEM_ID);
        
        // Set up fragment, using either cached current result or original arguments.
        SessionDetailFragment fragment = SessionDetailFragment.newInstance(sessionId);
        
       
    	// No use for savedInstanceState right now,
    	// each recreate from original arguments.

        
        getFragmentManager().beginTransaction()
        	.add(R.id.session_detail_container, fragment)
        	.commit();
        
    }

    
    
    



	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        	return true;
        }

        return super.onOptionsItemSelected(item);
    }

    
	
	
	
	@Override
	public void onDeleteParticipantClicked(int participantToDelete) {
		
		Uri deleteParticipant = Uri.withAppendedPath(
				RhetologContract.PARTICIPANTS_URI, 
				Integer.toString(participantToDelete)
				);
		
		getContentResolver().delete(deleteParticipant, null, null);
		
	}
    
    
    


	
    
}
