package com.github.fielddb.lessons.ui;

import java.util.ArrayList;

import com.github.fielddb.experimentation.ui.ProductionExperimentActivity;
import com.github.fielddb.BugReporter;
import com.github.fielddb.datacollection.DeviceDetails;
import com.github.opensourcefieldlinguistics.fielddb.speech.kartuli.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
  private static final int RETURN_FROM_VOICE_RECOGNITION_REQUEST_CODE = 341;
  protected DeviceDetails mDeviceDetails;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    super.setContentView(R.layout.activity_welcome);
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

  public void goToWebSite(View view) {
    Intent go = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://batumi.github.io"));
    startActivity(go);
  }
}
