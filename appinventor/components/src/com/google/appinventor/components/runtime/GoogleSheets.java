// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
package com.google.appinventor.components.runtime;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.HttpTransport;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ClientLoginHelper;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.IClientLoginHelper;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.Math;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 * Appinventor Google Sheets Component
 */
@DesignerComponent(version = YaVersion.GOOGLESHEETS_COMPONENT_VERSION,
    category = ComponentCategory.STORAGE,
    description = "TBD",
    nonVisible = true,
    iconName = "images/googleSheets.png")
@SimpleObject
@UsesPermissions(permissionNames =
    "android.permission.INTERNET," +
    "android.permission.ACCOUNT_MANAGER," +
    "android.permission.MANAGE_ACCOUNTS," +
    "android.permission.GET_ACCOUNTS," +
    "android.permission.USE_CREDENTIALS," +
    "android.permission.WRITE_EXTERNAL_STORAGE," +
    "android.permission.READ_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "googlesheets.jar,google-api-client-1.20.0.jar")

public class GoogleSheets extends AndroidNonvisibleComponent implements Component {
  private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";

  // The Scopes and the Credentials File Path.
  private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
  private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

  public GoogleSheets(ComponentContainer componentContainer) {
    super(componentContainer.$form());
  }

  /**
   * Converts the integer representation of rows and columns
   * to the reference strings used in Google Sheets. For ex,
   * row 1 and col 2 corresponds to the string "B1".
   */
  @SimpleFunction
  public String GetCellReference(int row, int col) {
    String[] alphabet = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    String result = "";
    while (col > 0) {
      String digit = alphabet[(col-1) % 26];
      result = digit + result;
      col = (int) Math.floor((col-1) / 26);
    }
    result = result + Integer.toString(row);
    return result;
  }

  /**
   * Converts the integer representation of rows and columns for the
   * corners of the range to the reference strings used in Google Sheets.
   * For ex, selecting the range from row 1 col 2 to row 3 col 4
   * corresponds to the string "B1:D3"
   */
  @SimpleFunction
  public String GetRangeReference(int row1, int col1, int row2, int col2) {
    return GetCellReference(row1, col1) + ":" + GetCellReference(row2, col2);
  }

  /**
   *
   */
  /*@SimpleFunction
  public String[] ReadRow (String sheetName, int rowNumber) {
    return new String[1];
  }*/

  /**
   *
   */
  /*@SimpleFunction
  public void WriteRow (String sheetName, int rowNumber, String[] data) {}*/

  /**
   *
   */
  /*@SimpleFunction
  public void AddRow (String sheetName, String[] data) {}*/

  /**
   *
   */
  @SimpleFunction
  public void RemoveRow (String sheetName, int rowNumber) {}

  /**
   *
   */
  /*@SimpleFunction
  public String[] ReadCol (String sheetName, int colNumber) {
    return new String[1];
  }*/

  /**
   *
   */
  /*@SimpleFunction
  public void WriteCol (String sheetName, int colNumber, String[] data) {}*/

  /**
   *
   */
  /*@SimpleFunction
  public void AddCol (String sheetName, String[] data) {}*/

  /**
   *
   */
  @SimpleFunction
  public void RemoveCol (String sheetName, int colNumber) {}

  /**
   *
   */
  @SimpleFunction
  public int NumRows (String sheetName) {
    return 0;
  }

  /**
   *
   */
  @SimpleFunction
  public int NumCols (String sheetName) {
    return 0;
  }

  /**
   *
   */
  @SimpleFunction
  public String ReadCell (String sheetName, String cellReference) {
    return "";
  }

  /**
   *
   */
  @SimpleFunction
  public void WriteCell (String sheetName, String cellReference, String data) {}


}
