package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author kirk
 *
 */
public class SimpleGraphView extends View implements LoaderCallbacks<Cursor> {

	private String TAG = SimpleGraphView.class.getSimpleName(); 
	
	/* Delegate to perform intensive computation on statistics obtained on whatever this view is reporting on */
	
	public interface OnGenerateStatisticsListener {
		public ArrayList<Float> OnGenerateStatistics(Uri uri, Cursor cursor);
	}
	OnGenerateStatisticsListener mGenerateStatisticsListener = null;
	
	/**
	 * @return the mGenerateStatisticsListener
	 */
	public OnGenerateStatisticsListener getGenerateStatisticsListener() {
		return mGenerateStatisticsListener;
	}

	/**
	 * @param mGenerateStatisticsListener the mGenerateStatisticsListener to set
	 */
	public void setGenerateStatisticsListener(
			OnGenerateStatisticsListener mGenerateStatisticsListener) {
		this.mGenerateStatisticsListener = mGenerateStatisticsListener;
	}

	
		
	private Paint mPaint = new Paint();
	private Rect fixedRect = new Rect(); // Allocate once
	private Rect sectionRect = new Rect();
	
	
	private List<Float> values;
	
	
	
	
	private List<Integer> colors;
	
	/**
	 * @return the colours
	 */
	public List<Integer> getColors() {
		return colors;
	}

	/**
	 * @param colors the colours to set
	 */
	public void setColors(List<Integer> colors) {
		
		if(this.colors != null)
			this.colors.clear();
		
		for(Integer c: colors) {
			this.colors.add(c);
		}
			
	}

	
	
	public Uri dataSource = null;

	/**
	 * @return the dataSource
	 */
	public Uri getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(Uri dataSource) {
		this.dataSource = dataSource;
		
		if (mActivity == null) {
			throw new IllegalStateException(TAG + " must have activity initialized");
		}
		if (mGenerateStatisticsListener == null) {
			throw new IllegalStateException(TAG + " must have OnGenerateStatisticsListener initialized");
		}
		
		// Using my own unique snowflake
		mActivity.getLoaderManager().restartLoader(this.hashCode(), /* args */ null, this);
		
	}

	
	
	private Activity mActivity = null;
	
	/**
	 * @return the mActivity
	 */
	public Activity getActivity() {
		return mActivity;
	}

	/**
	 * @param mActivity the mActivity to set
	 */
	public void setActivity(Activity mActivity) {
		this.mActivity = mActivity;
	}

	
	
	
	public SimpleGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Retrieve XML attributes
		//TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.);
		//attributes.recycle()
		
		init();
		
		
	}
	
	public void init() {
		
		this.colors = new ArrayList<Integer>();
		
		mPaint.setAntiAlias(true);
		mPaint.setColor(0xFFFFCC88);
		
		
		this.values = new ArrayList<Float>();
		
		// Padding?
		//setpadding(int, int, int, int);
		
		
		//activity.getLoaderManager().initLoader(this.getId(), null, this);
		
	}

	
	/*
	 * Store local
	 * requestLayout(); if it could change dimensions
	 * invalidate(); if it changes appearance
	 */
	
	/* Handle changes in data store */
	
	private void doUpdate(Cursor cursor) {
		
		values.clear();
				
		if (mGenerateStatisticsListener != null) {
			ArrayList<Float> newStats = mGenerateStatisticsListener.OnGenerateStatistics(dataSource, cursor);
			if (newStats != null)
				values.addAll(newStats);			
		}
		
		// Invalidate as appropriate, loader methods
		
	}
	
	
	
	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		
		// No allocations here to avoid garbage collection.
		
		super.onDraw(canvas);
		
		if(colors == null)
			throw new IllegalArgumentException("SimpleGraphView requires a list of colors");
		
		// Note fixed dimension information, quit if not actually redrawing anything.
		
		if(!canvas.getClipBounds(fixedRect)) {
			return;
		}
		
		// Draw background color.
		Drawable background = getBackground();
		if(background != null)
			background.draw(canvas); // Bass-ackwards!?!?!
		
		
		// Progress across canvas.	
		int progress = 0;
		for (int i = 0; i < values.size(); i++) {
			int eachWidth = (int) (values.get(i) * fixedRect.width());
			
			// If no more colors, do nothing.
			if (i >= colors.size())
				continue;
			
			// Choose current color.
			mPaint.setColor(colors.get(i));
			
			// Calculate colored block.
			//Rect eachBlock = new Rect(progress, fixedRect.top, progress + eachWidth, fixedRect.bottom);
			sectionRect.set(progress, fixedRect.top, progress + eachWidth, fixedRect.bottom);
			
			//draw block
			canvas.drawRect(sectionRect, mPaint);
			
			//and move on
			progress += eachWidth;
		}
		
		
		
		
		
	}

	/* (non-Javadoc)
	 * @see android.view.View#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		/* From measureSpec to measuredWidth */
		int measuredWidth = 0;
		int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
		if (widthSpecMode == MeasureSpec.EXACTLY) {
			measuredWidth = widthSpecSize;
		} else {
			measuredWidth = widthSpecSize;
		}
		
		/* From measureSpec to measuredHeight */
		int measuredHeight = 0;
		int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
		if (heightSpecMode == MeasureSpec.EXACTLY) {
			measuredHeight = heightSpecSize;
		} else {
			measuredHeight = heightSpecSize;
		}
		
		setMeasuredDimension(measuredWidth, measuredHeight);
	}
	
	
	
	
	
	/* Loader for the graph view */
	
	public static final String[] EVENT_PROJECTION = {
			RhetologContract.EventsColumns._ID,
			RhetologContract.EventsColumns.FALLACY,
			RhetologContract.EventsColumns.PARTICIPANT,
			RhetologContract.EventsColumns.SESSION,
	};
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		
		if(this.dataSource == null)
			return null;
		
		CursorLoader dataLoader = new CursorLoader(mActivity, 
				this.dataSource,
				EVENT_PROJECTION,
				null,
				null,
				null);
		return dataLoader;

	}
	

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		
		if (data == null) return;
		
		doUpdate(data);
		data.close();
		
		postInvalidate();
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		
		values.clear();
		
		postInvalidate();
		
	}
	
	
	
	

}
