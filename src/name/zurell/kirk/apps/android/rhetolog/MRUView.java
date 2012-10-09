package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.content.ClipData;
import android.content.Context;
import android.graphics.ColorFilter;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.MotionEvent;
import android.view.View;

/**
 * @author kirk
 *
 */
public class MRUView extends RelativeLayout {

	// MRUView's associated Fallacy
	Fallacy fallacy;
	
	// The text/icon image
	ImageView fallacyImage;
	
	// Colour filter to change appearance 
	ColorFilter colorFilter;
	
	
	public MRUView(Context context, AttributeSet attrs) {
		super(context, attrs);	
	}

	
	
	/* (non-Javadoc)
	 * @see android.view.View#onFinishInflate()
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		fallacyImage = (ImageView) findViewById(R.id.mruImage);
		//fallacyTitle = (TextView) findViewById(R.id.mruTitle);
		
		MRUTouchListener mruTouchListener = new MRUTouchListener();
		setOnTouchListener(mruTouchListener);

	}

	/**
	 * @return the fallacy
	 */
	public Fallacy getFallacy() {
		return fallacy;
	}

	/**
	 * @param fallacy the fallacy to set
	 */
	public void setFallacy(Fallacy fallacy) {
		this.fallacy = fallacy;
		if(fallacy != null) {
			fallacyImage.setImageDrawable(fallacy.getIcon());
			//fallacyTitle.setText(fallacy.getTitle());	
			setBackgroundColor(fallacy.getColor());
		} else {
			fallacyImage.setImageResource(R.drawable.empty);
			//fallacyTitle.setText(R.string.mruDefaultTitle);
		}
		
	}

	/**
	 * @return the colorFilter
	 */
	public ColorFilter getColorFilter() {
		return colorFilter;
	}

	/**
	 * @param colorFilter the colorFilter to set
	 */
	public void setColorFilter(ColorFilter colorFilter) {
		this.colorFilter = colorFilter;
		if (colorFilter != null)
			this.fallacyImage.setColorFilter(colorFilter);
		else
			this.fallacyImage.clearColorFilter();
	}

	
	private final class MRUTouchListener implements View.OnTouchListener {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			boolean result = false;
			int action = event.getAction();
			MRUView mruView = (MRUView) v;
			
			// If not a valid MRU setting, do nothing.
			if (mruView.getFallacy() == null) return false;
			
			switch (action) {
			case MotionEvent.ACTION_DOWN :
				// MRUView should be a child of FallacyView
				ClipData clipData = ClipData.newPlainText(FallacyView.FALLACY_DRAG_IDENTIFIER, fallacy.getName());
				
				View.DragShadowBuilder dsb = new View.DragShadowBuilder(fallacyImage);
				
				// Send selected Fallacy as payload.
				v.startDrag(clipData, dsb, fallacy, 0);
				
				result = true;
				break;
			}
			
			return result;
		}		
	}
	
	
	
	
	
//	/**
//	 * @return the fallacyImage
//	 */
//	public ImageView getFallacyImage() {
//		return fallacyImage;
//	}
//
//	/**
//	 * @param fallacyImage the fallacyImage to set
//	 */
//	public void setFallacyImage(ImageView fallacyImage) {
//		this.fallacyImage = fallacyImage;
//	}
	
	

}
