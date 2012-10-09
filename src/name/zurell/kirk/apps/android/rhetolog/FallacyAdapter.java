package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.util.List;

import android.content.Context;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

/**
 * @author kirk
 *
 */


public class FallacyAdapter extends BaseAdapter implements ListAdapter, View.OnDragListener {

	@SuppressWarnings("unused")
	private String TAG = FallacyAdapter.class.getSimpleName();
	
	
	/**
	 * List of {@link Fallacy} objects to be displayed.
	 */
	private List<Fallacy> fallacies = RhetologApplication.getFallacies();
	
	
	private LayoutInflater inflater;
	private int layout;
	private Context context;
	public int HighlightedItem = 0;
	
	
	/**
	 * Constructor, receives the layout that the adapter uses to create views for its list.
	 */
	public FallacyAdapter(Context context, int layout) {
		super();
		this.context = context;
		this.layout = layout;
		
		this.inflater = LayoutInflater.from(this.context);
	}
	
	

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		if(fallacies == null) {
			return 0;
		}
		return fallacies.size();
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int arg0) {
		if (null == fallacies) {
			return null;
		}
		return fallacies.get(arg0); // Can't do anything if getCount() not respected
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		// Will only ever be FallacyViews
		FallacyView work = (FallacyView) convertView;

		if(work == null) {
			work = (FallacyView) inflater.inflate(this.layout, parent, false);
		}

		
		Fallacy selected = fallacies.get(position);
		
		// The FallacyView knows what Fallacy it displays, used for Drag
		work.setFallacy(selected);
		
		return work;
	}



	// Receive drags for the fallacy ListView. Accept them all.
	
	@Override
	public boolean onDrag(View v, DragEvent event) {

		boolean result = false;

		Fallacy fallacy = (Fallacy) event.getLocalState();

		switch (event.getAction()) {

		case DragEvent.ACTION_DRAG_STARTED:
			// Want the drop when it happens
			result = true;
			break;
		
		case DragEvent.ACTION_DROP:
			if(mDropListener != null) {
				if (fallacy != null) {
					mDropListener.OnFallacyListDrop(fallacy);
				}
			}
			result = true;
			break;

		default:
			break;
		
		}

		return result;
	}

	/**
	 * Main activity implements this interface to receive instructions to remove an entry from the MRU list
	 * @author kirk
	 *
	 */
	public interface FallacyListDropListener {
		public void OnFallacyListDrop(Fallacy fallacy);
	}
	
	
	// The adapter's drop listener
	private FallacyListDropListener mDropListener;

	/**
	 * @return the mDropListener
	 */
	public FallacyListDropListener getDropListener() {
		return mDropListener;
	}

	/**
	 * @param mDropListener the mDropListener to set
	 */
	public void setDropListener(FallacyListDropListener mDropListener) {
		this.mDropListener = mDropListener;
	}
	
	
	
}
