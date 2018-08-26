package com.github.fielddb;

import com.github.fielddb.database.DatumContentProvider;
import com.github.fielddb.database.DatumContentProvider.DatumTable;
import com.github.fielddb.lessons.Config;
import com.github.fielddb.service.DownloadDatumsService;
import com.github.fielddb.service.KartuliSMSCorpusService;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.github.opensourcefieldlinguistics.fielddb.speech.kartuli.R;

public class KartuliSpeechRecognitionApplication extends FieldDBApplication {

  @Override
  public final void onCreate() {
    super.onCreate();

    /*
     * If we are in debug mode, or the user is connected to wifi, download
     * updates for samples and also register the user if they weren't registered
     * before
     */
    ConnectivityManager connManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    if (Config.APP_TYPE.equals("speechrecognition")) {
      Log.d(Config.TAG, "Not downloading samples, they are included in the training app");

      String[] datumProjection = { DatumTable.COLUMN_ID };
      CursorLoader loader = new CursorLoader(getApplicationContext(), DatumContentProvider.CONTENT_URI, datumProjection, null, null, null);
      Cursor datumCursor = loader.loadInBackground();
      if (datumCursor.getCount() == 0) {
        ContentValues values = new ContentValues();
        values.put(DatumTable.COLUMN_ID, "instructions");
        values.put(DatumTable.COLUMN_UTTERANCE, getString(R.string.training_instructions));
        values.put(DatumTable.COLUMN_ORTHOGRAPHY, getString(R.string.training_instructions));
        values.put(DatumTable.COLUMN_CONTEXT, getString(R.string.training_context));
        getContentResolver().insert(DatumContentProvider.CONTENT_URI, values);

        Intent updateSMSSamples = new Intent(getApplicationContext(), KartuliSMSCorpusService.class);
        getApplicationContext().startService(updateSMSSamples);
      }
      datumCursor.close();
    } else {
      if (wifi.isConnected() || BuildConfig.DEBUG) {
        Intent updateSamples = new Intent(getApplicationContext(), DownloadDatumsService.class);
        getApplicationContext().startService(updateSamples);
      }
    }
  }
}
