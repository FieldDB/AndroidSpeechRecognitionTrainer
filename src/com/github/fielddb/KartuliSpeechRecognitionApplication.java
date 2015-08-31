package com.github.fielddb;

/* https://github.com/ACRA/acralyzer/wiki/setup */
import org.acra.annotation.ReportsCrashes;

import com.github.fielddb.service.KartuliSMSCorpusService;

import android.content.Intent;

@ReportsCrashes(formKey = "", formUri = "", reportType = org.acra.sender.HttpSender.Type.JSON, httpMethod = org.acra.sender.HttpSender.Method.PUT, formUriBasicAuthLogin = "see_private_constants", formUriBasicAuthPassword = "see_private_constants")
public class KartuliSpeechRecognitionApplication extends FieldDBApplication {

  @Override
  public void onCreate() {
    mUpdateSampleData = new Intent(getApplicationContext(), KartuliSMSCorpusService.class);
    super.onCreate();
  }

}
