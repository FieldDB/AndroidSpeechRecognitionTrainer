package com.github.fielddb.lessons.ui;

import java.util.ArrayList;
import java.util.List;

import com.github.fielddb.Config;
import com.github.fielddb.database.DatumContentProvider;
import com.github.fielddb.experimentation.ui.ProductionExperimentActivity;
import com.github.fielddb.BugReporter;
import com.github.fielddb.datacollection.DeviceDetails;
import com.github.fielddb.service.DownloadFilesService;
import com.github.opensourcefieldlinguistics.fielddb.speech.kartuli.R;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
  private static final int RETURN_FROM_VOICE_RECOGNITION_REQUEST_CODE = 341;
  protected static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 487;
  protected DeviceDetails mDeviceDetails;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    super.setContentView(R.layout.activity_welcome);

    if (!checkAndRequestPermissions()) {
      return;
    }

    if (this.mDeviceDetails == null) {
      this.mDeviceDetails = new DeviceDetails(this);
    }
    BugReporter.putCustomData("deviceDetails", this.mDeviceDetails.getCurrentDeviceDetails());
    com.github.fielddb.model.Activity.sendActivity("login", "KartuliSpeechRecognizer");
  }

  public void onTrainClick(View view) {
    com.github.fielddb.model.Activity.sendActivity("openedTrainer", "trainer");

    Intent openTrainer = new Intent(this, ProductionExperimentActivity.class);
    startActivity(openTrainer);
  }

  public void onRecognizeClick(View view) {
    // Intent openRecognizer = new Intent(this,
    // SpeechRecognitionActivity.class);
    // startActivity(openRecognizer);
    BugReporter.putCustomData("deviceDetails", this.mDeviceDetails.getCurrentDeviceDetails());
    com.github.fielddb.model.Activity
        .sendActivity("requestedRecognizeSpeech", RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.im_listening));
    startActivityForResult(intent, RETURN_FROM_VOICE_RECOGNITION_REQUEST_CODE);
  }

  /**
   * Handle the results from the voice recognition activity.
   */
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RETURN_FROM_VOICE_RECOGNITION_REQUEST_CODE) {
      String eventType = "receivedFinalHypotheses";
      String eventDetails = "";
      if (resultCode == Activity.RESULT_OK) {
        /*
         * Toast the first result to the user and say them out loud.
         */
        ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

        String result = "Try again...";
        if (matches.size() > 0 && matches.get(0) != null) {
          result = matches.get(0);
          eventDetails = matches.toString();
        } else {
          eventDetails = "recognition result contained no text";
        }
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
      } else {
        eventDetails = "recognition returned no result";
      }
      com.github.fielddb.model.Activity.sendActivity(eventType, eventDetails);
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    Log.d(Config.TAG, "Permission callback called-------");
    switch (requestCode) {
      case DatumSpeechRecognitionHypothesesFragment.REQUEST_ID_MULTIPLE_PERMISSIONS: {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Unable to show images, this app will be uninteresting.", Toast.LENGTH_LONG).show();
        }

        Intent transferFile = new Intent(getApplicationContext(), DownloadFilesService.class);
        transferFile.putExtra(Config.EXTRA_RESULT_FILENAME, "sms_selected.png");
        getApplicationContext().startService(transferFile);

        transferFile = new Intent(getApplicationContext(), DownloadFilesService.class);
        transferFile.putExtra(Config.EXTRA_RESULT_FILENAME, "search_selected.png");
        getApplicationContext().startService(transferFile);

        transferFile = new Intent(getApplicationContext(), DownloadFilesService.class);
        transferFile.putExtra(Config.EXTRA_RESULT_FILENAME, "legal_search_selected.png");
        getApplicationContext().startService(transferFile);
      }
    }
  }

  private  boolean checkAndRequestPermissions() {
    List<String> listPermissionsNeeded = new ArrayList<>();

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    if (!listPermissionsNeeded.isEmpty()) {
      ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
      return false;
    }
    return true;
  }

  public void goToWebSite(View view) {
    Intent go = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://batumi.github.io"));
    startActivity(go);
  }
}
