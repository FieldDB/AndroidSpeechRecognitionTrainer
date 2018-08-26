package com.github.fielddb.lessons.ui;

import com.github.fielddb.BugReporter;
import com.github.fielddb.Config;
import com.github.fielddb.database.DatumContentProvider;
import com.github.fielddb.database.DatumContentProvider.DatumTable;
import com.github.opensourcefieldlinguistics.fielddb.speech.kartuli.R;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

public class SpeechRecognitionActivity extends FragmentActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    super.setContentView(R.layout.activity_speech_recognition);

    // savedInstanceState is non-null when there is fragment state
    // saved from previous configurations of this activity
    // (e.g. when rotating the screen from portrait to landscape).
    // In this case, the fragment will automatically be re-added
    // to its container so we don't need to manually add it.
    // For more information, see the Fragments API guide at:
    //
    // http://developer.android.com/guide/components/fragments.html
    //
    if (savedInstanceState == null) {
      // Create the detail fragment and add it to the activity
      // using a fragment transaction.
      ContentValues values = new ContentValues();
      values.put(DatumTable.COLUMN_VALIDATION_STATUS, "ToBeChecked,AutomaticallyRecognized");
      values.put(DatumTable.COLUMN_TAGS, "Gismet");
      Uri newDatum = this.getContentResolver().insert(DatumContentProvider.CONTENT_URI, values);
      if (newDatum == null) {
        BugReporter.sendBugReport("*** Error inserting a speech recognition datum in DB ***");
      }
      Bundle arguments = new Bundle();
      arguments.putString(DatumDetailFragment.ARG_ITEM_ID, newDatum.getLastPathSegment());
      DatumSpeechRecognitionHypothesesFragment fragment = new DatumSpeechRecognitionHypothesesFragment();
      fragment.setArguments(arguments);
      getSupportFragmentManager().beginTransaction().add(R.id.datum_detail_container, fragment).commit();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    Log.d(Config.TAG, "Permission callback called-------");
    switch (requestCode) {
      case DatumSpeechRecognitionHypothesesFragment.REQUEST_ID_MULTIPLE_PERMISSIONS: {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Unable to record audio, this app wont be able to hear you.", Toast.LENGTH_LONG).show();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Unable to copy the recognition model, this app wont be able to recognize your speech.", Toast.LENGTH_LONG).show();
        }

        // TODO: return to the user's intent
      }
    }
  }
}
