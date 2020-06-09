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
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchGetValuesByDataFilterRequest;
import com.google.api.services.sheets.v4.model.BatchGetValuesByDataFilterRequest;
import com.google.api.services.sheets.v4.model.DataFilter;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
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
import gnu.lists.LList;
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
  private String spreadsheetID = "";
  // (TODO) set application name to be the name of the project
  private String APPLICATION_NAME = "Test Application";

  //   private final Activity activity;
  private final ComponentContainer container;
  private final Activity activity;

  @SimpleProperty(
    description = "The Credentials JSON file")
  public String credentialsJson() {
    return credentialsPath;
  }

  @DesignerProperty(
    editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET,
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

  @DesignerProperty(
    editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
    defaultValue = "")
  @SimpleProperty(
    description="The ID for the Google Sheets file you want to edit. You can " +
      "find the spreadsheetID in the URL of the Google Sheets file.")
  public void spreadsheetID(String spreadsheetID) {
    this.spreadsheetID = spreadsheetID;
  }

  /* Utility Functions for Making Calls */

  private Credential authorize() throws IOException {

    // TODO - Only change cachedCredentialsFile if credentials path is different. See FusionTables
    if (cachedCredentialsFile == null) {
      cachedCredentialsFile = MediaUtil.copyMediaToTempFile(container.$form(), this.credentialsPath);
    }

    // Convert the above java.io.File -> InputStream
    InputStream in = new FileInputStream(cachedCredentialsFile);

    // TODO: Catch Malformed Credentials JSON
    Credential credential = GoogleCredential.fromStream(in)
      .createScoped(Arrays.asList(SheetsScopes.SPREADSHEETS));

    return credential;
  }

  private Sheets getSheetsService ()  throws IOException, GeneralSecurityException {
    Credential credential = authorize();

    return new Sheets.Builder(new com.google.api.client.http.javanet.NetHttpTransport(),
      JacksonFactory.getDefaultInstance(), credential)
      .setApplicationName(APPLICATION_NAME)
      .build();
  }

  /* Error Catching Handler */

  @SimpleEvent(
    description="This event block is triggered whenever an API call encounters " +
      "an error. Text with details about the error can be found in `errorMessage`")
  public void ErrorOccurred (final String errorMessage) {
    final GoogleSheets thisInstance = this;
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(thisInstance, "ErrorOccurred", errorMessage);
      }
    });
  }

  @SimpleEvent(
    description="This event block is triggered when any write method to Google " +
      "Sheets has completed. The name of the original write procedure is given " +
      "as the `procedureName`, like 'WriteRange' and 'AddRow' for example.")
  public void AfterWriting (final String procedureName) {
    final GoogleSheets thisInstance = this;
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(thisInstance, "AfterWriting", procedureName);
      }
    });
  }
  /* Helper Functions for the User */

  @SimpleFunction(
    description="Converts the integer representation of rows and columns to " +
      "the reference strings used in Google Sheets. For example, row 1 and " +
      "col 2 corresponds to the string \"B1\".")
  public String GetCellReference(int row, int col) {
    String[] alphabet = {
      "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R",
      "S","T","U","V","W","X","Y","Z"};
    String colRange = "";
    while (col > 0) {
      String digit = alphabet[(col-1) % 26];
      colRange = digit + colRange;
      col = (int) Math.floor((col-1) / 26);
    }
    return colRange + Integer.toString(row);
  }

  @SimpleFunction(
    description="Converts the integer representation of rows and columns for the "+
      "corners of the range to the reference strings used in Google Sheets. " +
      "For ex, selecting the range from row 1 col 2 to row 3 col 4 " +
      "corresponds to the string \"B1:D3\"")
  public String GetRangeReference(int row1, int col1, int row2, int col2) {
    return GetCellReference(row1, col1) + ":" + GetCellReference(row2, col2);
  }

  /* Filters and Methods that Use Filters */

  // @SimpleFunction
  // public DataFilter FilterRowsWhere (int columnNumber, String equals) {}

  // @SimpleFunction
  // public void GetRowsWithFilter (DataFilter filter) {}

  /* Row-wise Operations */

  @SimpleFunction(
    description="On the sheet with the provided sheet name, this method will " +
      "read the row with the given number and returns the text that is found " +
      "in each cell.")
  public void ReadRow (String sheetName, int rowNumber) {
    Log.d(LOG_TAG, "Reading Row: " + rowNumber);
    // Properly format the Range Reference
    final String rangeReference = sheetName +  "!" + rowNumber + ":" + rowNumber;

    // Asynchronously fetch the data in the cell
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {
          Sheets sheetsService = getSheetsService();

          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, rangeReference ).execute();
          // Get the actual data from the response
          List<List<Object>> values = readResult.getValues();
          // If the data we got is empty, then return so.
          if (values == null || values.isEmpty())
            ErrorOccurred("ReadRow: No data found");

          // Format the result as a list of strings and run the callback
          else {
            List<String> ret = new ArrayList<String>();
            for (Object obj : values.get(0)) {
              ret.add(String.format("%s", obj));
            }
            GotRowData(ret);
          }
        }
        // Handle Errors which may have occured while sending the Read Request!
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("ReadRow: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="After calling the ReadRow method, the data in the row will " +
      "be stored as a list of text values in rowDataList.")
  public void GotRowData (final List<String> rowDataList) {
    Log.d(LOG_TAG, "GotRowData got: " + rowDataList);
    final GoogleSheets thisInstance = this;
    // We need to re-enter the main thread before we can dispatch the event!
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(thisInstance, "GotRowData", rowDataList);
      }
    });
  }

  @SimpleFunction(
    description="Given a list of values as `data`, this method will write the " +
      "values to the row of the sheet. It will always start from the left most " +
      "column and continue to the right. If there are alreaddy values in that " +
      "row, this method will override them with the new data. It will not " +
      "erase the entire row.")
  public void WriteRow (String sheetName, int rowNumber, YailList data) {

    // Generates the A1 Reference for the operation
    String rangeReference = String.format("A%d", rowNumber);
    final String rangeRef = sheetName + "!" + rangeReference;
    Log.d(LOG_TAG, "Writing Row: " + rangeRef);

    // Generates the body, which are the values to assign to the range
    List<List<Object>> values = new ArrayList<>();
    List<Object> r = new ArrayList<Object>();
    for (Object o : (LList) data.getCdr()) {
      r.add(o);
    }
    values.add(r);
    final ValueRange body = new ValueRange()
      .setValues(values);

    // Wrap the API Call in an Async Utility
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        // Surround the operation with a try catch statement
        try {
          Sheets sheetsService = getSheetsService();
          // UpdateValuesResponse result =
          Sheets.Spreadsheets.Values.Update update = sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body);
          update.setValueInputOption("USER_ENTERED");
          update.execute();
          AfterWriting("WriteRow");
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteRow: " + e.getMessage());
        }
      }
    });
  }

  @SimpleFunction(
    description="Given a list of values as `data`, this method will write the " +
      "values to the next empty row in the sheet with the provided sheetName. ")
  public void AddRow (String sheetName, YailList data) {
    // Properly format the range
    final String rangeRef = sheetName + "!A1";
    // Generates the body, which are the values to assign to the range
    List<List<Object>> values = new ArrayList<>();
    List<Object> r = new ArrayList<Object>();
    for (Object o : (LList) data.getCdr()) {
      r.add(o);
    }
    values.add(r);
    final ValueRange body = new ValueRange()
      .setValues(values);

    // Run the API call asynchronously
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {
          Sheets sheetsService = getSheetsService();
          // Sends the append values request
          sheetsService.spreadsheets().values()
            .append(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED") // USER_ENTERED or RAW
            .setInsertDataOption("INSERT_ROWS") // INSERT_ROWS or OVERRIDE
            .execute();
          AfterWriting("AddRow");
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("AddRow: " + e.getMessage());
        }
      }
    });
  }

  @SimpleFunction(
    description="Deletes the row with the given row number (1-indexed) from " +
      "the sheets page with the grid ID `gridId`. This does not clear the row, " +
      "but removes it entirely. The sheet's grid id can be found at the " +
      "end of the url of the Google Sheets document, right after the `gid=`.")
  public void RemoveRow (final int gridId, final int rowNumber) {
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try{
          Sheets sheetsService = getSheetsService();

          DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
            .setRange(
              new DimensionRange()
                .setSheetId(gridId)
                .setDimension("ROWS")
                .setStartIndex(rowNumber-1)
                .setEndIndex(rowNumber)
            );
          List<Request> requests = new ArrayList<>();
          requests.add(new Request().setDeleteDimension(deleteRequest));

          BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest()
            .setRequests(requests);
          sheetsService.spreadsheets().batchUpdate(spreadsheetID, body).execute();
          AfterWriting("RemoveRow");
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("RemoveRow: " + e.getMessage());
        }
      }
    });
  }

  /* Column-wise Operations */

  @SimpleFunction(
    description="Begins an API call which will request the data stored in the " +
      "column with the provided `colNumber` (1-indexed). The resulting data " +
      "will be sent to the GotColData event block.")
  public void ReadCol (String sheetName, int colNumber) {

    // Converts the col number to the corresponding letter
    String[] alphabet = {
      "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R",
      "S","T","U","V","W","X","Y","Z"};
    String colReference = "";
    while (colNumber > 0) {
      String digit = alphabet[(colNumber-1) % 26];
      colReference = digit + colReference;
      colNumber = (int) Math.floor((colNumber-1) / 26);
    }
    final String rangeRef = sheetName + "!" + colReference + ":" + colReference;

    // Asynchronously fetch the data in the cell and trigger the callback
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {
          Sheets sheetsService = getSheetsService();

          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, rangeRef).execute();
          List<List<Object>> values = readResult.getValues();

          // If the data we got is empty, then throw an error
          if (values == null || values.isEmpty()) {
            ErrorOccurred("ReadCol: No data found.");
            return;
          }

          // Format the result as a list of strings and run the callback
          List<String> ret = new ArrayList<String>();
          for (List<Object> row : values)
            ret.add(String.format("%s", row.get(0)));
          GotColData(ret);
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("ReadCol: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="After calling the ReadCol method, the data in the column will " +
      "be stored as a list of text values in `colDataList`.")
  public void GotColData (final List<String> colDataList) {
    Log.d(LOG_TAG, "GotColData got: " + colDataList);
    final GoogleSheets thisInstance = this;
    // We need to re-enter the main thread before we can dispatch the event!
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(thisInstance, "GotColData", colDataList);
      }
    });
  }

  @SimpleFunction(
    description="Deletes the column with the given column number (1-indexed) " +
      "from the sheets page with the grid ID `gridId`. This does not clear the " +
      "column, but removes it entirely. The sheet's grid id can be found at the " +
      "end of the url of the Google Sheets document, right after the `gid=`.")
  public void RemoveCol (final int gridId, final int colNumber) {

    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try{
          Sheets sheetsService = getSheetsService();

          DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
            .setRange(
              new DimensionRange()
                .setSheetId(gridId)
                .setDimension("COLUMNS")
                .setStartIndex(colNumber-1)
                .setEndIndex(colNumber)
            );
          List<Request> requests = new ArrayList<>();
          requests.add(new Request().setDeleteDimension(deleteRequest));

          BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest()
            .setRequests(requests);
          sheetsService.spreadsheets().batchUpdate(spreadsheetID, body).execute();
          AfterWriting("RemoveCol");
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("RemoveCol: " + e.getMessage());
        }
      }
    });
  }

  /* Cell-wise Operations */

  @SimpleFunction(
    description="Begins an API call which will request the data stored in the " +
      "cell with the provided cell reference. This cell reference can be the " +
      "result of the getCellReference block, or a text block with the correct " +
      "A1 notation. The resulting cell data will be sent to the GotCellData " +
      "event block.")
  public void ReadCell (final String sheetName, final String cellReference) {

    // 1. Check that the Cell Reference is actually a single cell
    if (!cellReference.matches("[a-zA-Z]+[0-9]+")) {
      ErrorOccurred("ReadCell: Invalid Cell Reference");
      return;
    }

    // 2. Asynchronously fetch the data in the cell
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        Log.d(LOG_TAG, "Reading Cell: " + cellReference);

        try {
          Sheets sheetsService = getSheetsService();
          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, sheetName + "!" + cellReference).execute();
          List<List<Object>> values = readResult.getValues();

          // If the data we got is empty, then return so.
          if (values == null || values.isEmpty()) {
            ErrorOccurred("ReadCell: No data found");
            return;
          }

          // Format the result as a string and run the call back
          String result = String.format("%s", values.get(0).get(0));
          GotCellData(result);
        }
        // Handle Errors which may have occured while sending the Read Request
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("ReadCell: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="After calling the ReadCell method, the data in the cell will " +
      "be stored as text in `cellData`.")
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

  @SimpleFunction(
    description="Assigns the text in `data` to the cell at the provided cell " +
      "reference. If there is already a value in this range, the old value " +
      "be overriden by the new text.")
  public void WriteCell (String sheetName, String cellReference, Object data) {
    // Generates the A1 Reference for the operation
    final String rangeRef = sheetName + "!" + cellReference;
    // Form the body as a 2D list of Strings, with only one string
    final ValueRange body = new ValueRange()
      .setValues(Arrays.asList(
        Arrays.asList(data)
      ));
    Log.d(LOG_TAG, "Writing Cell: " + rangeRef);

    // Wrap the API Call in an Async Utility
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        // Running the getSheetsService and executing the command may cause
        // IOException's and GeneralSecurityException's
        try {
          Sheets sheetsService = getSheetsService();
          // UpdateValuesResponse result =
          sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED") // USER_ENTERED or RAW
            .execute();
          AfterWriting("WriteCell");
        }
        // Catch the two kinds of exceptions
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteCell: " + e.getMessage());
        }
      }
    });
  }

  /* Range-wise Operations */

  @SimpleFunction(
    description="Begins an API call which will request the data stored in the " +
      "range with the provided range reference. This range reference can be " +
      "the result of the getRangeReference block, or a text block with the " +
      "correct A1 notation. The resulting range data will be sent to the " +
      "GotRangeData event block.")
  public void ReadRange (final String sheetName, final String rangeReference) {

    // (TODO) Check if the rangeReference is a valid A1 Format

    // Asynchronously fetch the data in the cell
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        Log.d(LOG_TAG, "Reading Range: " + rangeReference);

        try {
          Sheets sheetsService = getSheetsService();
          // Spreadsheet sheet = sheetsService.spreadsheets().get(spreadsheetID).execute();
          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, sheetName + "!" + rangeReference).execute();
          // Get the actual data from the response
          List<List<Object>> values = readResult.getValues();

          // No Data Found
          if (values == null || values.isEmpty()) {
            ErrorOccurred("ReadRange: No data found.");
            return;
          }
          // Format the result as a string and run the call back
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
        // Handle Errors which may have occured while sending the Read Request!
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("ReadRange: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="After calling the ReadRange method, the data in the range will " +
      "be stored as a list of rows, where every row is another list of text, in " +
      "`rangeData`.")
  public void GotRangeData (final List<List<String>> rangeData) {
    Log.d(LOG_TAG, "GotRangeData got: " + rangeData);
    final GoogleSheets thisInstance = this;
    // We need to re-enter the main thread before we can dispatch the event!
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        EventDispatcher.dispatchEvent(thisInstance, "GotRangeData", rangeData);
      }
    });
  }

  @SimpleFunction(
    description="Assigns the values in the data value, which is a list of lists, " +
      "to the range that you specify. The number of rows and columns in the range " +
      "reference must match the dimensions of the 2D list provided in data.")
  public void WriteRange (String sheetName, String rangeReference, YailList data) {

    // (TODO) Check that the range reference is in A1 notatoin

    // Generates the A1 Reference for the operation
    final String rangeRef = sheetName + "!" + rangeReference;
    Log.d(LOG_TAG, "Writing Range: " + rangeRef);

    // Generates the body, which are the values to assign to the range
    List<List<Object>> values = new ArrayList<>();
    int cols = -1;
    for (Object elem : (LList) data.getCdr()) {
      if (!(elem instanceof YailList))
        continue;
      YailList row = (YailList) elem;
      // construct the row that we will add to the list of rows
      List<Object> r = new ArrayList<Object>();
      for (Object o : (LList) row.getCdr())
        r.add(o);
      values.add(r);
      // Catch rows of unequal length
      if (cols == -1) cols = r.size();
      if (r.size() != cols) {
        ErrorOccurred("WriteRange: Rows must have the same length");
        return;
      }
    }

    // Check that values has at least 1 row
    if (values.size() == 0) {
      ErrorOccurred("WriteRange: Data must be a list of lists.");
      return;
    }

    final ValueRange body = new ValueRange()
      .setValues(values);
    Log.d(LOG_TAG, "Body's Range in A1: " + body.getRange());
    // Wrap the API Call in an Async Utility
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {
          Sheets sheetsService = getSheetsService();
          // UpdateValuesResponse result =
          sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED") // USER_ENTERED or RAW
            .execute();
          AfterWriting("WriteRange");
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteRange: " + e.getMessage());
        }
      }
    });
  }

}
