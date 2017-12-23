package com.github.fielddb.service;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;

import com.github.fielddb.Config;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.RecognitionService.Callback;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.Toast;

public class PocketSphinxRecognitionService extends Service implements RecognitionListener {

  private SpeechRecognizer recognizer;
  public static final String FREEFORM_SPEECH = "freeform";
  public static final String SMS_SPEECH = "sms";
  public static final String WEB_SEARCH = "websearch";
  public static final String LEGAL_SEARCH = "legalsearch";
  public static final String LANGUAGE_MODEL_SMS = "recognizesms";
  public static final String LANGUAGE_MODEL_LEGAL_SEARCH = "recognizelegalsearch";
  public static final String EXTRA_RESULT_AUDIO_FILE = "extra_result_audio_file";
  public static final int TIME_TO_STOP_LISTENING = 1000;

  protected ArrayList<String> mPreviousPartialHypotheses;
  protected long mLastPartialHypothesisChangeTimestamp;
  protected Boolean mAlreadyWaitingForEndOfSpeech = false;

  public PocketSphinxRecognitionService() {
    super();
  }

  /* TODO this might not be the way to get the onStartListening to be called */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    int result = super.onStartCommand(intent, flags, startId);
    onStartListening(intent, null);
    return result;
  }

  @Override
  public IBinder onBind(Intent intent) {
    onStartListening(intent, null);
    return null;
  }

  // @Override
  protected void onCancel(Callback arg0) {
    if (recognizer != null) {
      recognizer.stop();
    }
    Hypothesis completedHypoth = new Hypothesis(null, 0, 0);
    // Hypothesis completedHypoth = new Hypothesis("recognitionCancelled", "cancelled", 0);
    broadcast(completedHypoth, true);
  }

  @Override
  public void onDestroy() {
    onCancel(null);
    super.onDestroy();
  }

  @Override
  public boolean onUnbind(Intent intent) {
    onCancel(null);
    return super.onUnbind(intent);
  }

  // @Override
  protected void onStartListening(Intent recognizerIntent, Callback callback) {
    if (recognizer == null) {
      setupRecognizer();
    }
    if (recognizer == null) {
      Log.e(Config.TAG, "Recognizer failed to setup");
      return;
    }

    recognizer.setRecognitionCallback(callback);

    if (recognizerIntent != null && recognizerIntent.getData() != null) {
      Log.d(Config.TAG, "listener called with uri " + recognizerIntent.getData().toString());
    }

    if (recognizerIntent != null) {
      String requestedLanguageModel = recognizerIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL);
      if (RecognizerIntent.LANGUAGE_MODEL_FREE_FORM.equals(requestedLanguageModel)) {
        switchSearch(FREEFORM_SPEECH);
      } else if (RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH.equals(requestedLanguageModel)) {
        switchSearch(WEB_SEARCH);
      } else if (LANGUAGE_MODEL_SMS.equals(requestedLanguageModel)) {
        switchSearch(SMS_SPEECH);
      } else if (LANGUAGE_MODEL_LEGAL_SEARCH.equals(requestedLanguageModel)) {
        switchSearch(LEGAL_SEARCH);
      } else {
        switchSearch(FREEFORM_SPEECH);
        Toast.makeText(getApplicationContext(), "Speak naturally, I'm using your personal free form language model",
            Toast.LENGTH_LONG).show();
      }

    } else {
      Log.w(Config.TAG, "The intent to start the recognizer was not defined... this is odd.");
    }

  }

  // @Override
  protected void onStopListening(Callback callback) {
    Log.d(Config.TAG, "Stopping speech recognizer listening");
    onCancel(callback);
  }

  private void setupRecognizer() {
    try {
      (new File(Config.DEFAULT_OUTPUT_DIRECTORY + "/sync")).mkdirs();
      (new File(Config.DEFAULT_OUTPUT_DIRECTORY + "/tmp")).mkdirs();
      Assets assets;
      assets = new Assets(getApplicationContext(), Config.DEFAULT_OUTPUT_DIRECTORY + "/sync");
      File assetDir = assets.syncAssets();
      Log.d(Config.TAG, "Setting up recognizer models");
      File modelsDir = new File(assetDir, "models");
      recognizer = defaultSetup().setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
          .setDictionary(new File(modelsDir, "dict/sms_corpus.dic")).setRawLogDir(assetDir).setKeywordThreshold(1e-40f)
          .setRawLogDir(new File(Config.DEFAULT_OUTPUT_DIRECTORY + "/tmp")) // Saves utterances to sdcard
          .getRecognizer();
      recognizer.addListener(this);

      File smsLanguageModel = new File(modelsDir, "lm/sms_corpus.dmp");
      recognizer.addNgramSearch(SMS_SPEECH, smsLanguageModel);
      // File freeformLanguageModel = new File(modelsDir,
      // "lm/free_form_corpus.dmp");
      recognizer.addNgramSearch(FREEFORM_SPEECH, smsLanguageModel);
      // File webLanguageModel = new File(modelsDir,
      // "lm/web_search_corpus.dmp");
      recognizer.addNgramSearch(WEB_SEARCH, smsLanguageModel);
      // File legalLanguageModel = new File(modelsDir,
      // "lm/legal_search_corpus.dmp");
      recognizer.addNgramSearch(LEGAL_SEARCH, smsLanguageModel);
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(getApplicationContext(),
          "თქვენი sdcard არ არის მზად. Your voice model can't be loaded, please make sure your SDCARD is not in use.",
          Toast.LENGTH_LONG).show();
      return;
    }

  }

  private void switchSearch(String searchName) {
    recognizer.stop();
    recognizer.startListening(searchName);
  }

  public void onEvent(int eventType, Bundle params) {
    Log.d(Config.TAG, "onEvent " + eventType);
  }

  public void onPartialResults(Bundle params) {
    Log.d(Config.TAG, "onPartialResults ");
  }

  public void onBufferReceived(byte[] buffer) {
    Log.d(Config.TAG, "onBufferReceived ");
  }

  public void onResults(Bundle params) {
    Log.d(Config.TAG, "onResults ");
    Intent i = new Intent(Config.INTENT_PARTIAL_SPEECH_RECOGNITION_RESULT);
    i.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, params.getStringArrayList(RecognizerIntent.EXTRA_RESULTS));
    i.putExtra(Config.EXTRA_RECOGNITION_COMPLETED, params.getBoolean(RecognizerIntent.EXTRA_PARTIAL_RESULTS));

    i.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, params.getFloatArray(RecognizerIntent.EXTRA_CONFIDENCE_SCORES));
    i.putExtra(EXTRA_RESULT_AUDIO_FILE, params.getString((SpeechRecognizer.EXTRA_FILENAME)));
    getApplication().sendBroadcast(i);
  }

  // @Override
  public void onTimeout() {
    Log.d(Config.TAG, "onTimeout");
    onEndOfSpeech();
  }

  @Override
  public void onError(int error) {
    Log.d(Config.TAG, "onError " + error);
    onEndOfSpeech();
  }

  // @Override
  public void onError(Exception exception) {
    Log.d(Config.TAG, "onError");
    onEndOfSpeech();
  }

  @Override
  public void onReadyForSpeech(Bundle bundle) {
    Log.d(Config.TAG, "onReadyForSpeech");
  }

  @Override
  public void onRmsChanged(float v) {
    Log.d(Config.TAG, "onRmsChanged");
  }

  @Override
  public void onBeginningOfSpeech() {
    Log.d(Config.TAG, "   Beginning of speech");
  }

  @Override
  public void onEndOfSpeech() {
    if (mPreviousPartialHypotheses != null) {
      Log.d(Config.TAG, "   End of speech: " + mPreviousPartialHypotheses.toString());
    }
    onCancel(null);
    // TODO why?
    // if (DIGITS_SEARCH.equals(recognizer.getSearchName())
    // || FORECAST_SEARCH.equals(recognizer.getSearchName()))
    // switchSearch(KWS_SEARCH);
  }

  //  @Override
  public void onPartialResult(Hypothesis hypothesis) {
    if (hypothesis == null) {
      return;
    }

    // if (text.equals(KEYPHRASE)) {
    // switchSearch(MENU_SEARCH);
    // } else if (text.equals(DIGITS_SEARCH)) {
    // switchSearch(DIGITS_SEARCH);
    // } else if (text.equals(FORECAST_SEARCH)) {
    // switchSearch(FORECAST_SEARCH);
    // } else {

    broadcast(hypothesis, false);
    // }
  }

  //  @Override
  // Deprecated, using Android Recognizer interface instead
  public void onResult(Hypothesis hypothesis) {
    Log.d(Config.TAG, "Hypothesis result received");
    broadcast(hypothesis, true);
  }

  public void broadcast(Hypothesis hypothesis, boolean completedResult) {
    if (hypothesis == null) {
      return;
    }

    String text = hypothesis.getHypstr();
    if (text == null || "".equals(text)) {
      return;
    }

    ArrayList<String> confidences = new ArrayList<String>();
    confidences.add(hypothesis.getBestScore() + "");

    File directory = new File(Config.DEFAULT_OUTPUT_DIRECTORY + "/tmp");
    File[] files = directory.listFiles();
    String audioFile = files[files.length - 1].getName();
    String outFile = Config.DEFAULT_OUTPUT_DIRECTORY + "/audio_utterance_" + System.currentTimeMillis() + ".raw";

    try {
      InputStream in = new FileInputStream(audioFile);
      OutputStream out = new FileOutputStream(outFile);

      // Copy the bits from instream to outstream
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();

      audioFile = outFile;
      Log.d(Config.TAG, "renamed to " + outFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    Intent i = new Intent(Config.INTENT_PARTIAL_SPEECH_RECOGNITION_RESULT);
    if ("recognitionCancelled".equals(text)) {
      text = "";
      completedResult = true;
    }

    // Make first character upper case, and the rest lower case (this is
    // because the SMS corpus is uppercase)s
    if (text.length() > 1) {
      text = text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1).toLowerCase(Locale.getDefault());
    }
    if (!completedResult) {
      if (mPreviousPartialHypotheses == null) {
        mPreviousPartialHypotheses = new ArrayList<String>();
      }
      // If the guess is not in the top, append the previous info and
      // insert
      // at the top

      mLastPartialHypothesisChangeTimestamp = System.currentTimeMillis();
      // String newText = "";
      // if (mPreviousPartialHypotheses.size() > 0
      // && mPreviousPartialHypotheses.get(0) != null
      // && !"".equals(mPreviousPartialHypotheses.get(0))) {
      // newText = mPreviousPartialHypotheses.get(0) + " ";
      // }
      // if (!newText.equals(text)) {
      // newText = newText + text;
      // }
      mPreviousPartialHypotheses.add(0, text);
      // mPreviousPartialHypotheses.add(text);

      Log.d(Config.TAG, "Partial Hypothesis continued: " + text);
      /*
       * If it has been a while since the last hypothesis, send all of them as
       * completed
       */
      // if (!mAlreadyWaitingForEndOfSpeech) {
      // mAlreadyWaitingForEndOfSpeech = true;
      // Handler mainHandler = new Handler(getApplicationContext()
      // .getMainLooper());
      // Runnable myRunnable = new Runnable() {
      // @Override
      // public void run() {
      // if (System.currentTimeMillis()
      // - mLastPartialHypothesisChangeTimestamp > TIME_TO_STOP_LISTENING
      // - 100) {
      // Log.d(Config.TAG, "Partial results have stopped.");
      // onCancel(null);
      // } else {
      // Log.d(Config.TAG,
      // "Partial results still seem to be running.");
      // }
      // mAlreadyWaitingForEndOfSpeech = false;
      // }
      // };
      // mainHandler.postDelayed(myRunnable, TIME_TO_STOP_LISTENING);
      // }
      ArrayList<String> partialHypothesis = new ArrayList<String>();
      partialHypothesis.add(text);

      partialHypothesis.add("");
      confidences.add(0 + "");

      partialHypothesis.add("");
      confidences.add(0 + "");

      partialHypothesis.add("");
      confidences.add(0 + "");

      partialHypothesis.add("");
      confidences.add(0 + "");

      // partialHypothesis.add(mPreviousPartialHypotheses.toString());
      i.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, partialHypothesis);
    } else {
      i.putExtra(Config.EXTRA_RECOGNITION_COMPLETED, true);
      ArrayList<String> completedHypothesis = new ArrayList<String>();
      completedHypothesis.add(text);
      i.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, completedHypothesis);
    }

    i.putStringArrayListExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, confidences);
    if (!audioFile.contains("cancelled")) {
      i.putExtra(EXTRA_RESULT_AUDIO_FILE, audioFile);
    }
    getApplication().sendBroadcast(i);

  }
}
