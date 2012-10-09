package name.zurell.kirk.apps.android.rhetolog;

/*
 * Copyright (c) 2012 Kirk Zurell
 *
 * See the file LICENSE for copying permission.
 */

import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class FallacyView extends LinearLayout implements View.OnTouchListener {

	public static final String FALLACY_DRAG_IDENTIFIER = "identifier";
	
	Fallacy mfallacy;

	ImageView fallacyIcon;
	TextView fallacyTitle;
	TextView fallacyDescription;

	public FallacyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Not called during inflation
	}

	/* (non-Javadoc)
	 * @see android.view.View#onFinishInflate()
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		// References to View's own components
		fallacyIcon = (ImageView) findViewById(R.id.fallacyIcon);
		fallacyTitle = (TextView) findViewById(R.id.fallacyTitle);
		fallacyDescription = (TextView) findViewById(R.id.fallacyDescription);

		// The icon image is draggable
		fallacyIcon.setOnTouchListener(this);	

	}

	/**
	 * @return the view's associated fallacy
	 */
	public Fallacy getFallacy() {
		return mfallacy;
	}

	/**
	 * @param fallacy The fallacy to set.
	 */
	public void setFallacy(Fallacy fallacy) {
		mfallacy = fallacy;
		Drawable newIcon = fallacy.getIcon();
		fallacyIcon.setImageDrawable(newIcon);
		fallacyTitle.setText(fallacy.getTitle());
		fallacyDescription.setText(fallacy.getDescription());
		setBackgroundColor(fallacy.getColor());
	}


	/**
	 * Fallacies are draggable from the original list to the participants or to the MRU list.
	 */

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		boolean result = false;
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN :
			ClipData clipData = ClipData.newPlainText(FALLACY_DRAG_IDENTIFIER, mfallacy.getName());

			View.DragShadowBuilder dsb = new View.DragShadowBuilder(fallacyIcon);
			v.startDrag(clipData, dsb, mfallacy, 0);
			result = true;
			break;
		}
		return result;
	}


}
