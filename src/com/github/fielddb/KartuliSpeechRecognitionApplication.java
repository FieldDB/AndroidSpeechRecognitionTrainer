package com.github.fielddb;

import com.github.fielddb.service.KartuliSMSCorpusService;

import android.content.Intent;

public class KartuliSpeechRecognitionApplication extends FieldDBApplication {
  
  @Override
  public void onCreate() {
    mUpdateSampleData = new Intent(getApplicationContext(), KartuliSMSCorpusService.class);
    super.onCreate();
  }

}
