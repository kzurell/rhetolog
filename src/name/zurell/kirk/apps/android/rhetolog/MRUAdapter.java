package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class MRUAdapter extends ArrayAdapter<Fallacy> implements OnDragListener {

	@SuppressWarnings("unused")
	private static String TAG = MRUAdapter.class.getSimpleName();
	
	
	private static final String MRUPREFSSECTION = "MRU";
	
	// Reference to adapter's own list.
	ViewGroup list;
	
	// Reference to adapters own context.
	Context context;
	
	// TextViewResourceId should not be used.
	public MRUAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	
		this.context = context;
		
		// Reload list contents from preferences
		SharedPreferences sp = context.getSharedPreferences(MRUPREFSSECTION, Context.MODE_PRIVATE);
		Map<String,?> map = sp.getAll();
		for (String each : map.keySet()) {
			String val = (String) map.get(each);
			Fallacy fal = RhetologApplication.getFallacyNamed(val);
			this.add(fal);
		}
		
		
		
	}
	
	/* (non-Javadoc)
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.mruentry, null);
		}
		
		Fallacy fallacyToUse = this.getItem(position);
		
		//inform view which fallacy to show.
		MRUView asMRUView = (MRUView) convertView;
		asMRUView.setFallacy(fallacyToUse);
		
		return convertView;
	}

	/**
	 * @return The list of MRU Fallacies.
	 */
	public ViewGroup getList() {
		return list;
	}

	/**
	 * @param list The list to set
	 */
	public void setList(ViewGroup list) {
		this.list = list;
		list.setOnDragListener(this);
	}

	
	
	/**
	 * Drag and Drop functionality for the MRU list: show states
	 */
	
	final LightingColorFilter mediumDropColor = new LightingColorFilter(0xFF404040, 0xFF101010);
	final LightingColorFilter lightDropColor = new LightingColorFilter(0xFF808080, 0xFF101010);
	
	final float DROP_POSSIBLE = 0.3f;
	final float DROP_PROBABLE = 0.6f;
	final float DROP_NONE = 1.0f;
	
	@Override
	public boolean onDrag(View v, DragEvent event) {
		
		boolean result = true;
		
		int action = event.getAction();
		
		if (v.equals(getList())) {
			
			// Handle for MRU list
			switch (action) {
			case DragEvent.ACTION_DRAG_STARTED:
				list.setAlpha(DROP_POSSIBLE);
				break;
			case DragEvent.ACTION_DRAG_ENTERED:
				list.setAlpha(DROP_PROBABLE);
				break;
			case DragEvent.ACTION_DRAG_EXITED:
				list.setAlpha(DROP_POSSIBLE);
				break;
			case DragEvent.ACTION_DROP:
				list.setAlpha(DROP_NONE);
				Fallacy payload = (Fallacy) event.getLocalState();
				addFallacyToMRU(payload);
				break;
			case DragEvent.ACTION_DRAG_ENDED:
				list.setAlpha(DROP_NONE);
				break;
			default:
				result = false;
			}
			
		} else {
			
			// Handle for drop on FallacyList (or anywhere else), indicates removal from MRU list.
			switch (action) {
			case DragEvent.ACTION_DRAG_STARTED:
				break;
			case DragEvent.ACTION_DROP:
				Fallacy payload = (Fallacy) event.getLocalState();
				if (payload != null)
					removeFallacyFromMRU(payload);
				break;
			case DragEvent.ACTION_DRAG_ENDED:
				break;
			default:
				result = false;
			}
			
		}
		
		return result;
	}
	
	
	private void addFallacyToMRU(Fallacy fallacy) {
		
		int curPosIfAny = this.getPosition(fallacy);
		if (curPosIfAny != -1) {
			return; // Already present, ignore addition
		}
		
		int second_last_position = this.getCount() - 1;
		if (second_last_position < 0) second_last_position = 0;
			
		this.insert(fallacy, second_last_position);
			
		commitMRUToPrefs();
	}
	
	private void removeFallacyFromMRU(Fallacy fallacy) {
		
		int curPosIfAny = this.getPosition(fallacy);
		if (curPosIfAny == -1) {
			return; // Not already present, ignore removal (?)
		}
		
		this.remove(fallacy);
		
		commitMRUToPrefs();
	}
	
	private void commitMRUToPrefs() {
		// Clear previous MRU preference, add new list
		SharedPreferences sp = context.getSharedPreferences(MRUPREFSSECTION, 
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = sp.edit();
		ed.clear();

		for (int i = 0; i < this.getCount(); i++) {
			Fallacy each = this.getItem(i);
			if (each == null) continue;
			ed.putString(each.getName(), each.getName());
		}

		ed.commit();
	}
	
}
