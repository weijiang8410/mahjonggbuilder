package com.anoshenko.android.background;

import com.anoshenko.android.mahjongg.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class BackgroundActivity extends Activity {

	private Background mBackground;
	private GradientListAdapter mGradientAdapter;



	//--------------------------------------------------------------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		/*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		int new_orientation;
		String value = prefs.getString(getResources().getString(Res.string.pref_orientation), "3");
		switch(Integer.parseInt(value))
		{
		case 1:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			break;

		case 2:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
			break;

		case 3:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
			break;

		default:
			new_orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		}

		if (getRequestedOrientation() != new_orientation) {
			setRequestedOrientation(new_orientation);
		}*/

		super.onCreate(savedInstanceState);

		mBackground = new Background(this);

		setContentView(R.layout.background_view);

        Spinner spinner = (Spinner) findViewById(R.id.BackgroundType);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.background_type_item_values,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(BACKGROUND_TYPE_CHANGE);

		switch (mBackground.getType()) {
		case GRADIENT:
			spinner.setSelection(0);
			setCurrentPage(0);
			break;

		case BUILDIN:
			spinner.setSelection(1);
			setCurrentPage(1);
			break;

		//case EXTERN:
		//	spinner.setSelection(2);
		//	setCurrentPage(2);
		//	break;
		}

		Button button = (Button) findViewById(R.id.GradientColor0);
		button.setOnClickListener(COLOR1_BUTTON);

		button = (Button) findViewById(R.id.GradientColor1);
		button.setOnClickListener(COLOR2_BUTTON);

		mGradientAdapter = new GradientListAdapter(this);
		mGradientAdapter.setBackground(mBackground);

		GridView gradient_grid = (GridView) findViewById(R.id.GradientChooser);
		gradient_grid.setAdapter(mGradientAdapter);
		if (mBackground.getGradient() != null)
			gradient_grid.setSelection(mBackground.getGradient().Id);

		gradient_grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mBackground.setGradient(GradientType.values()[position], mBackground.getColor1(), mBackground.getColor2());
				((GridView)findViewById(R.id.GradientChooser)).invalidateViews();
			}
		});

		GridView buildin_grid = (GridView) findViewById(R.id.BuildinChooser);
		buildin_grid.setAdapter(new BuildinImageAdapter(this, Background.BUILDIN_IDS, mBackground));
		buildin_grid.setSelection(mBackground.getImageNumber());

		buildin_grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mBackground.setBuildin(position);
				((GridView)findViewById(R.id.BuildinChooser)).invalidateViews();
			}
		});


		buildin_grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mBackground.setBuildin(position);
				((GridView)findViewById(R.id.BuildinChooser)).invalidateViews();
			}
		});
	}

	//--------------------------------------------------------------------------
	@Override
	//protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	protected void onPause() {
		mBackground.Store();
		super.onPause();
	}

	//--------------------------------------------------------------------------
	private OnItemSelectedListener BACKGROUND_TYPE_CHANGE = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			switch(position) {
			case 0:
				mBackground.setGradient(mBackground.getGradient(),
						mBackground.getColor1(), mBackground.getColor2());
				break;

			case 1:
				mBackground.setBuildin(mBackground.getImageNumber());
				break;

			case 2:
				// TODO
				break;
			}
			setCurrentPage(position);
			//mBackground.Store(PreferenceManager.getDefaultSharedPreferences(BackgroundActivity.this));
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
    };

	//--------------------------------------------------------------------------
    private View.OnClickListener COLOR1_BUTTON = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			ColorChooser.show(BackgroundActivity.this, new ColorChoiceResult() {
				@Override
				public void setColor(int color) {
					mBackground.setColor1(color);
					GridView grid = (GridView)findViewById(R.id.GradientChooser);
					grid.invalidateViews();
				}
			});
		}
    };

	//--------------------------------------------------------------------------
    private View.OnClickListener COLOR2_BUTTON = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			ColorChooser.show(BackgroundActivity.this, new ColorChoiceResult() {
				@Override
				public void setColor(int color) {
					mBackground.setColor2(color);
					GridView grid = (GridView)findViewById(R.id.GradientChooser);
					grid.invalidateViews();
				}
			});
		}
    };

	//--------------------------------------------------------------------------
    private void setCurrentPage(int number) {
    	RelativeLayout gradient_page = (RelativeLayout)findViewById(R.id.GradientPage);
    	GridView buildin_page = (GridView)findViewById(R.id.BuildinChooser);

    	switch(number) {
        case 0: // GRADIENT
        	gradient_page.setVisibility(View.VISIBLE);
        	buildin_page.setVisibility(View.INVISIBLE);
        	break;

        case 1: // BUILDIN
        	gradient_page.setVisibility(View.INVISIBLE);
        	buildin_page.setVisibility(View.VISIBLE);
        	break;

        //case 2: // EXTERN
        //	gradient_page.setVisibility(View.INVISIBLE);
        //	buildin_page.setVisibility(View.INVISIBLE);
        //	break;
        }
    }
}
