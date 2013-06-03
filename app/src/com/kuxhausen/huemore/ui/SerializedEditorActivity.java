package com.kuxhausen.huemore.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.network.TransmitGroupMood;
import com.kuxhausen.huemore.nfc.HueNfcEncoder;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.GroupColumns;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.MoodColumns;
import com.kuxhausen.huemore.state.api.BulbState;

public class SerializedEditorActivity extends FragmentActivity implements
	LoaderManager.LoaderCallbacks<Cursor> {
	
	
	Context context;
	Gson gson = new Gson();

	// Identifies a particular Loader being used in this component
	private static final int GROUPS_LOADER = 0, MOODS_LOADER = 1;

	private SeekBar brightnessBar;
	private Spinner groupSpinner, moodSpinner;
	private SimpleCursorAdapter groupDataSource, moodDataSource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		
		// We need to use a different list item layout for devices older than
		// Honeycomb
		int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? android.R.layout.simple_list_item_activated_1
				: android.R.layout.simple_list_item_1;
		/*
		 * Initializes the CursorLoader. The GROUPS_LOADER value is eventually
		 * passed to onCreateLoader().
		 */
		LoaderManager lm = getSupportLoaderManager();
		lm.initLoader(GROUPS_LOADER, null, this);
		lm.initLoader(MOODS_LOADER, null, this);

		brightnessBar = (SeekBar) this.findViewById(R.id.brightnessBar);
		brightnessBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				preview();
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
			}
		});

		groupSpinner = (Spinner) this.findViewById(R.id.groupSpinner);
		String[] gColumns = { GroupColumns.GROUP, BaseColumns._ID };
		groupDataSource = new SimpleCursorAdapter(this, layout, null, gColumns,
				new int[] { android.R.id.text1 }, 0);
		groupDataSource
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupSpinner.setAdapter(groupDataSource);

		moodSpinner = (Spinner) this.findViewById(R.id.moodSpinner);
		String[] mColumns = { MoodColumns.MOOD, BaseColumns._ID };
		moodDataSource = new SimpleCursorAdapter(this, layout, null, mColumns,
				new int[] { android.R.id.text1 }, 0);
		moodDataSource
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		moodSpinner.setAdapter(moodDataSource);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initializeActionBar(true);

		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void initializeActionBar(Boolean value) {
		try {
			this.getActionBar().setDisplayHomeAsUpEnabled(value);
		} catch (Error e) {
		}
	}

	public void preview() {
		// Look up bulbs for that mood from database
		String[] groupColumns = { GroupColumns.BULB };
		String[] gWhereClause = { ((TextView) groupSpinner.getSelectedView())
				.getText().toString() };
		Cursor groupCursor = getContentResolver().query(
				DatabaseDefinitions.GroupColumns.GROUPBULBS_URI, // Use the
																	// default
																	// content
																	// URI
																	// for the
																	// provider.
				groupColumns, // Return the note ID and title for each note.
				GroupColumns.GROUP + "=?", // selection clause
				gWhereClause, // selection clause args
				null // Use the default sort order.
				);

		ArrayList<Integer> groupStates = new ArrayList<Integer>();
		while (groupCursor.moveToNext()) {
			groupStates.add(groupCursor.getInt(0));
		}
		Integer[] bulbS = groupStates.toArray(new Integer[groupStates.size()]);

		String[] moodColumns = { MoodColumns.STATE };
		String[] mWereClause = { ((TextView) moodSpinner.getSelectedView())
				.getText().toString() };
		Cursor moodCursor = getContentResolver().query(
				DatabaseDefinitions.MoodColumns.MOODSTATES_URI, // Use the
																// default
																// content URI
																// for the
																// provider.
				moodColumns, // Return the note ID and title for each note.
				MoodColumns.MOOD + "=?", // selection clause
				mWereClause, // election clause args
				null // Use the default sort order.
				);

		ArrayList<String> moodStates = new ArrayList<String>();
		while (moodCursor.moveToNext()) {
			moodStates.add(moodCursor.getString(0));
		}
		String[] moodS = moodStates.toArray(new String[moodStates.size()]);

		int brightness = brightnessBar.getProgress();
		for (int i = 0; i < moodS.length; i++) {
			BulbState bs = gson.fromJson(moodS[i], BulbState.class);
			bs.bri = brightness;
			moodS[i] = gson.toJson(bs);// put back into json string for Transmit
										// Group Mood
		}

		TransmitGroupMood tgm = new TransmitGroupMood(this, bulbS, moodS);
		tgm.execute();
	}

	public String getMessage() {
		String url = "kuxhausen.com/HueMore/nfc?";

		// Look up bulbs for that mood from database
		String[] groupColumns = { GroupColumns.BULB };
		String[] gWhereClause = { ((TextView) groupSpinner.getSelectedView())
				.getText().toString() };
		Cursor groupCursor = getContentResolver().query(
				DatabaseDefinitions.GroupColumns.GROUPBULBS_URI, // Use the
																	// default
																	// content
																	// URI
																	// for the
																	// provider.
				groupColumns, // Return the note ID and title for each note.
				GroupColumns.GROUP + "=?", // selection clause
				gWhereClause, // selection clause args
				null // Use the default sort order.
				);

		ArrayList<Integer> groupStates = new ArrayList<Integer>();
		while (groupCursor.moveToNext()) {
			groupStates.add(groupCursor.getInt(0));
		}
		Integer[] bulbS = groupStates.toArray(new Integer[groupStates.size()]);

		String[] moodColumns = { MoodColumns.STATE };
		String[] mWereClause = { ((TextView) moodSpinner.getSelectedView())
				.getText().toString() };
		Cursor moodCursor = getContentResolver().query(
				DatabaseDefinitions.MoodColumns.MOODSTATES_URI, // Use the
																// default
																// content URI
																// for the
																// provider.
				moodColumns, // Return the note ID and title for each note.
				MoodColumns.MOOD + "=?", // selection clause
				mWereClause, // election clause args
				null // Use the default sort order.
				);

		ArrayList<String> moodStates = new ArrayList<String>();
		while (moodCursor.moveToNext()) {
			moodStates.add(moodCursor.getString(0));
		}
		String[] moodS = moodStates.toArray(new String[moodStates.size()]);
		BulbState[] bsRay = new BulbState[moodS.length];

		int brightness = brightnessBar.getProgress();
		for (int i = 0; i < moodS.length; i++) {
			System.out.println(moodS[i]);
			bsRay[i] = gson.fromJson(moodS[i], BulbState.class);
			bsRay[i].bri = brightness;
			System.out.println(bsRay[i]);
		}

		String data = HueNfcEncoder.encode(bulbS, bsRay);
		return url + data;
	}

	/**
	 * Callback that's invoked when the system has initialized the Loader and is
	 * ready to start the query. This usually happens when initLoader() is
	 * called. The loaderID argument contains the ID value passed to the
	 * initLoader() call.
	 */
	@Override
	public Loader<Cursor> onCreateLoader(int loaderID, Bundle arg1) {
		/*
		 * Takes action based on the ID of the Loader that's being created
		 */
		switch (loaderID) {
		case GROUPS_LOADER:
			// Returns a new CursorLoader
			String[] gColumns = { GroupColumns.GROUP, BaseColumns._ID };
			return new CursorLoader(this, // Parent activity context
					DatabaseDefinitions.GroupColumns.GROUPS_URI, // Table
					gColumns, // Projection to return
					null, // No selection clause
					null, // No selection arguments
					null // Default sort order
			);
		case MOODS_LOADER:
			// Returns a new CursorLoader
			String[] mColumns = { MoodColumns.MOOD, BaseColumns._ID };
			return new CursorLoader(this, // Parent activity context
					DatabaseDefinitions.MoodColumns.MOODS_URI, // Table
					mColumns, // Projection to return
					null, // No selection clause
					null, // No selection arguments
					null // Default sort order
			);
		default:
			// An invalid id was passed in
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		/*
		 * Moves the query results into the adapter, causing the ListView
		 * fronting this adapter to re-display
		 */
		switch (loader.getId()) {
		case GROUPS_LOADER:
			if (groupDataSource != null) {
				groupDataSource.changeCursor(cursor);
			}
			break;
		case MOODS_LOADER:
			if (moodDataSource != null) {
				moodDataSource.changeCursor(cursor);
			}
			break;
		}

		// registerForContextMenu(getListView());
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		/*
		 * Clears out the adapter's reference to the Cursor. This prevents
		 * memory leaks.
		 */
		// unregisterForContextMenu(getListView());
		switch (loader.getId()) {
		case GROUPS_LOADER:
			if (groupDataSource != null) {
				groupDataSource.changeCursor(null);
			}
			break;
		case MOODS_LOADER:
			if (moodDataSource != null) {
				moodDataSource.changeCursor(null);
			}
			break;
		}
	}
}