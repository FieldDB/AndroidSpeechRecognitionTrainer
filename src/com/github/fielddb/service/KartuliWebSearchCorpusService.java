package com.github.fielddb.service;

import java.util.ArrayList;

import com.github.fielddb.database.DatumContentProvider;
import com.github.fielddb.database.DatumContentProvider.DatumTable;
import com.github.fielddb.Config;
import com.github.fielddb.model.Datum;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class KartuliWebSearchCorpusService extends IntentService {
	private ArrayList<Datum> smsSamples;

	public KartuliWebSearchCorpusService(String name) {
		super(name);
		initSmsSamples();
	}

	public KartuliWebSearchCorpusService() {
		super("DownloadDatumsService");
		initSmsSamples();
	}

	@Override
	protected void onHandleIntent(Intent arg0) {
		String id = "";
		Uri uri;
		String[] datumProjection = {DatumTable.COLUMN_ID};
		Cursor cursor;
		ContentValues datumAsValues;
		for (Datum datum : smsSamples) {
			id = datum.getId();
			uri = Uri.withAppendedPath(DatumContentProvider.CONTENT_URI, id);
			cursor = getContentResolver().query(uri, datumProjection, null,
					null, null);

			// TODO instead, update it, without losing info
			if (cursor == null || cursor.getCount() <= 0) {
				/* save it */
				try {
					datumAsValues = new ContentValues();
					datumAsValues.put(DatumTable.COLUMN_ID, id);
					datumAsValues.put(DatumTable.COLUMN_REV, datum.getRev());
					datumAsValues.put(DatumTable.COLUMN_UTTERANCE,
							datum.getUtterance());
					datumAsValues.put(DatumTable.COLUMN_ORTHOGRAPHY,
							datum.getOrthography());
					datumAsValues.put(DatumTable.COLUMN_CONTEXT,
							datum.getContext());
					datumAsValues.put(DatumTable.COLUMN_TAGS,
							datum.getTagsString());

					uri = getContentResolver().insert(
							DatumContentProvider.CONTENT_URI, datumAsValues);
				} catch (Exception e) {
					Log.d(Config.TAG,
							"Failed to insert this sample most likely something was missing from the server...");
					e.printStackTrace();
				}
			}
			if (cursor != null){
        cursor.close();
      }
		}

		Intent updateLegalSearchSamples = new Intent(getApplicationContext(),
				KartuliLegalSearchCorpusService.class);
		getApplicationContext().startService(updateLegalSearchSamples);

	}
	private void initSmsSamples() {
		this.smsSamples = new ArrayList<Datum>();

		Datum datum = new Datum("რა ტემპერატურაა დღეს?");
		datum.setUtterance("ra t'emp'erat'uraa dghes");
		datum.setId("web1");
		datum.setRev("");
		datum.setContext("");
		datum.setTagsFromSting("WebSearch");
		this.smsSamples.add(datum);

		datum = new Datum("როგორ აღვადგინო გაუქმებული ფეისბუქის გვერდი?");
		datum.setUtterance("rogor aghvadgino gaukmebuli peisbukis gverdi");
		datum.setId("web2");
		datum.setRev("");
		datum.setContext("");
		datum.setTagsFromSting("WebSearch");
		this.smsSamples.add(datum);

	}

}
