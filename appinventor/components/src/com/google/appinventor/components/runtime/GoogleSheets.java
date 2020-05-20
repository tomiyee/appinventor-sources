// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0


package com.google.appinventor.components.runtime;

import static android.Manifest.permission.ACCOUNT_MANAGER;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesActivities;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.annotations.androidmanifest.ActionElement;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.annotations.androidmanifest.IntentFilterElement;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.YailList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
/**
 * Appinventor Google Sheets Component
 */
@DesignerComponent(version = YaVersion.GOOGLESHEETS_COMPONENT_VERSION,
    category = ComponentCategory.STORAGE,
    description = "<p>A non-visible component that communicates with Google Sheets.</p>",
    nonVisible = true,
    iconName = "images/googleSheets.png")
@SimpleObject
@UsesPermissions({
    INTERNET,
    ACCOUNT_MANAGER,
    GET_ACCOUNTS,
    WRITE_EXTERNAL_STORAGE,
    READ_EXTERNAL_STORAGE
})
@UsesLibraries({
    "googlesheets.jar",
    "jackson-core.jar",
    "google-api-client.jar",
    "google-api-client-jackson2.jar",
    "google-http-client.jar",
    "google-http-client-jackson2.jar",
    "google-oauth-client.jar",
    "google-oauth-client-jetty.jar",
    "grpc-context.jar",
    "opencensus.jar",
    "opencensus-contrib-http-util.jar",
    "guava.jar",
    "jetty.jar",
    "jetty-util.jar"
})
@UsesActivities(activities = {
    @ActivityElement(name = "com.google.appinventor.components.runtime.WebViewActivity",
       configChanges = "orientation|keyboardHidden",
       screenOrientation = "behind",
       intentFilters = {
           @IntentFilterElement(actionElements = {
               @ActionElement(name = "android.intent.action.MAIN")
           })
    })
})
public class GoogleSheets extends AndroidNonvisibleComponent implements Component {
  // private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
  // private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  // private static final String TOKENS_DIRECTORY_PATH = "tokens";
  //
  // // The Scopes and the Credentials File Path.
  // private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
  // private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
  private static final String LOG_TAG = "GOOGLESHEETS";

  private static final String WEBVIEW_ACTIVITY_CLASS = WebViewActivity.class
    .getName();
  private int requestCode;
  public GoogleSheets(ComponentContainer componentContainer) {
    super(componentContainer.$form());
    this.container = componentContainer;
    this.activity = componentContainer.$context();
  }

  /* Getter and Setters for Properties */
  private String apiKey;
  private String credentialsPath;
  private File cachedCredentialsFile = null;
  private String tokensPath;
  private String spreadsheetID = "1q0sM8BeBRL2n6EHEkkB54WbKn6pT-boChEbS3HzNe_g";
  String APPLICATION_NAME = "Test Application";


  //   private final Activity activity;
  private final ComponentContainer container;
  private final Activity activity;

  //   private final IClientLoginHelper requestHelper;
  @SimpleProperty(
    description = "API Key")
  public String ApiKey() {
    return apiKey;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
    defaultValue = "")
  @SimpleProperty
  public void ApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  @SimpleProperty(
    description = "The Credentials JSON file")
  public String credentialsJson() {
    return credentialsPath;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET,
    defaultValue = "")
  @SimpleProperty
  public void credentialsJson (String credentialsPath) {
    this.credentialsPath = credentialsPath;
  }


  @SimpleProperty(
    description = "The ID you can find in the URL of the Google Sheets you want to edit")
  public String spreadsheetID() {
    return spreadsheetID;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
    defaultValue = "")
  @SimpleProperty(description="The ID for the Google Sheets file you want to edit. " +
    "You can find the spreadsheetID in the URL of the Google Sheets file.")
  public void spreadsheetID(String spreadsheetID) {
    this.spreadsheetID = spreadsheetID;
  }

  /* Temporary testing functions */
  private Credential authorize() throws IOException {

    // TODO - Only change cachedCredentialsFile if credentials path is different. See FusionTables
    cachedCredentialsFile = MediaUtil.copyMediaToTempFile(container.$form(), this.credentialsPath);

    // Convert the above java.io.File -> InputStream
    InputStream in = new FileInputStream(cachedCredentialsFile);

    // TODO: Catch Malformed Credentials JSON
    Credential credential = GoogleCredential.fromStream(in)
      .createScoped(Arrays.asList(SheetsScopes.SPREADSHEETS));

    return credential;
  }

  public Sheets getSheetsService ()  throws IOException, GeneralSecurityException {
    Credential credential = authorize();

    return new Sheets.Builder(new com.google.api.client.http.javanet.NetHttpTransport(),
      JacksonFactory.getDefaultInstance(), credential)
      .setApplicationName(APPLICATION_NAME)
      .build();
  }

  @SimpleEvent
  public void GotText(String response) {
    EventDispatcher.dispatchEvent(this, "GotText", response);
  }


  /* Helper Functions for the User */

  @SimpleFunction(
    description="Converts the integer representation of rows and columns " +
      "to the reference strings used in Google Sheets. For example, " +
      "row 1 and col 2 corresponds to the string \"B1\".")
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

  @SimpleFunction(
    description="Converts the integer representation of rows and columns for the "+
      "corners of the range to the reference strings used in Google Sheets. " +
      "For ex, selecting the range from row 1 col 2 to row 3 col 4 " +
      "corresponds to the string \"B1:D3\"")
  public String GetRangeReference(int row1, int col1, int row2, int col2) {
    return GetCellReference(row1, col1) + ":" + GetCellReference(row2, col2);
  }

  /* Row-wise Operations */

  @SimpleFunction
  public void ReadRow (String sheetName, int rowNumber) {
    AsynchUtil.runAsynchronously(new Runnable () {
      @Override
      public void run () {
        try {

          // ... Read Row Implementation
          // ... End with ReadRow(YailList response)

        } catch (Exception e) {
          // Unforeseen Error
        }
      }
    });
  }

  @SimpleEvent
  public void GotRowData (YailList rowDataList) {
    EventDispatcher.dispatchEvent(this, "GotRowData", rowDataList);
  }

  @SimpleFunction
  public void WriteRow (String sheetName, int rowNumber, YailList data) {}

  @SimpleFunction
  public void AddRow (String sheetName, YailList data) {}

  @SimpleFunction
  public void RemoveRow (String sheetName, int rowNumber) {}

  /* Column-wise Operations */

  @SimpleFunction
  public void ReadCol (String sheetName, int colNumber) {
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {

          // ... Read Column Implementation
          // ... End with ReadCol(YailList response)

        } catch (Exception e) {
          // Unforeseen Error
        }
      }
    });
  }

  @SimpleEvent
  public void GotColData (YailList colDataList) {
    EventDispatcher.dispatchEvent(this, "ReadCol", colDataList);
  }

  @SimpleFunction
  public void WriteCol (String sheetName, int colNumber, YailList data) {
  }

  @SimpleFunction
  public void AddCol (String sheetName, YailList data) {}

  @SimpleFunction
  public void RemoveCol (String sheetName, int colNumber) {}

  /* Cell-wise Operations */

  @SimpleFunction
  public void ReadCell (String sheetName, final String cellReference) {
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        Log.d(LOG_TAG, "Reading Cell: " + cellReference);

        // 1. Check that the Cell Reference is actually a single cell
        if (!cellReference.matches("[a-zA-Z]+[0-9]+")) {
            GotCellData("Invalid Cell Reference.");
            return;
        }

        // 2. Asynchronously fetch the data in the cell
        try {
          Sheets sheetsService = getSheetsService();
          // Spreadsheet sheet = sheetsService.spreadsheets().get(spreadsheetID).execute();
          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, cellReference).execute();
          // Get the actual data from the response
          List<List<Object>> values = readResult.getValues();
          // If the data we got is empty, then return so.
          if (values == null || values.isEmpty()) {
              GotCellData("No data found.");
          }
          // Format the result as a string and run the call back
          else {
            String result = String.format("%s", values.get(0).get(0));
            GotCellData(result);
          }
        }
        // Handle Errors which may have occured while sending the Read Request!
        catch (Exception e) {
          e.printStackTrace();
          GotCellData(e.getMessage());
        }
      }
    });
  }

  @SimpleEvent
  public void GotCellData(final String cellData) {
    Log.d(LOG_TAG, "GotCellData got: " + cellData);
    final GoogleSheets thisInstance = this;
    // We need to re-enter the main thread before we can dispatch the event!
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(thisInstance, "GotCellData", cellData);
      }
    });
  }

  @SimpleFunction
  public void WriteCell (String sheetName, String cellReference, String data) {}

  /* Range-wise Operations */
  // TODO Always Return 2D YailLists, currently only ret 2D if correct

  @SimpleFunction
  public void ReadRange (String sheetName, final String rangeReference) {
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        Log.d(LOG_TAG, "Reading Cell: " + rangeReference);

        // 1. TODO Check for valid range with regex (ex A1:B2)
        // if (!rangeReference.matches("[a-zA-Z]+[0-9]+:[a-zA-Z]+[0-9]+")) {
        //   List<List<String>> ret = new ArrayList<List<String>>();
        //   ret.add(new ArrayList<String>());
        //   ret.get(0).add("Invalid Cell Reference");
        //   GotRangeData(ret);
        //   return;
        // }

        // 2. Asynchronously fetch the data in the cell
        try {
          Sheets sheetsService = getSheetsService();
          // Spreadsheet sheet = sheetsService.spreadsheets().get(spreadsheetID).execute();
          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, rangeReference).execute();
          // Get the actual data from the response
          List<List<Object>> values = readResult.getValues();

          // If the data we got is empty, then return so.
          if (values == null || values.isEmpty()) {
            List<List<String>> ret = new ArrayList<List<String>>();
            ret.add(new ArrayList<String>());
            ret.get(0).add("No Data Found.");
            GotRangeData(ret);
          }
          // Format the result as a string and run the call back
          else {

            List<List<String>> ret = new ArrayList<List<String>>();
            // For every object in the result, convert it to a string
            for (List<Object> row : values) {
              List<String> cellRow = new ArrayList<String>();
              for (Object cellValue : row) {
                cellRow.add(String.format("%s", cellValue));
              }
              ret.add(cellRow);
            }

            GotRangeData(ret);
          }
        }
        // Handle Errors which may have occured while sending the Read Request!
        catch (Exception e) {
          e.printStackTrace();
          List<List<String>> ret = new ArrayList<List<String>>();
          ret.add(new ArrayList<String>());
          ret.get(0).add(e.getMessage());
          GotRangeData(ret);
        }
      }
    });
  }

  @SimpleEvent
  public void GotRangeData (final List<List<String>> rangeData) {

      Log.d(LOG_TAG, "GotCellData got: " + rangeData);
      final GoogleSheets thisInstance = this;
      // We need to re-enter the main thread before we can dispatch the event!
      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          EventDispatcher.dispatchEvent(thisInstance, "GotRangeData", rangeData);
        }
      });
  }

  @SimpleFunction
  public void WriteRange (String sheetName, String rangeReference, List<List<String>> data) {
    // 1. Check that the dimensions of the YailList is the same as the Range Reference
  }

}
