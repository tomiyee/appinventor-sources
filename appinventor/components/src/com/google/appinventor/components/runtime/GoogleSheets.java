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
import com.google.api.client.http.javanet.NetHttpTransport;
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
import com.google.appinventor.components.runtime.util.CsvUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.YailList;
import gnu.lists.LList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
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
  // private static final String ApplicationName = "Google Sheets API Java Quickstart";
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

  // Designer Properties
  private String apiKey;
  private String credentialsPath;
  private String tokensPath;
  private String spreadsheetID = "";
  // This gets changed to the name of the project by MockGoogleSheets by default
  private String ApplicationName = "App Inventor";

  // Variables for Authenticating the Google Sheets Component
  private File cachedCredentialsFile = null;
  private Sheets sheetsService = null;

  //   private final Activity activity;
  private final ComponentContainer container;
  private final Activity activity;

  /* Getter and Setters for Properties */

  @SimpleProperty(
    description = "The Credentials JSON file")
  public String CredentialsJson() {
    return credentialsPath;
  }

  @DesignerProperty(
    editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET,
    defaultValue = "")
  @SimpleProperty
  public void CredentialsJson (String credentialsPath) {
    this.credentialsPath = credentialsPath;
  }

  @SimpleProperty(
    description = "The ID you can find in the URL of the Google Sheets you want to edit")
  public String SpreadsheetID() {
    return spreadsheetID;
  }

  @DesignerProperty(
    editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
    defaultValue = "")
  @SimpleProperty(
    description="The ID for the Google Sheets file you want to edit. You can " +
      "find the spreadsheetID in the URL of the Google Sheets file.")
  public void SpreadsheetID(String spreadsheetID) {
    this.spreadsheetID = spreadsheetID;
  }

  /**
   * Specifies the name of the application given when doing an API call.
   *
   * @internaldoc
   * This is set programmatically
   * in {@link com.google.appinventor.client.editor.simple.components.MockGoogleSheets}
   * and consists of the current App Inventor project name.
   *
   * @param ApplicationName the name of the App
   */
  @SimpleProperty(
    userVisible = false)
  public String ApplicationName() {
    return ApplicationName;
  }

  @DesignerProperty(
    editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
    defaultValue = "App Inventor")
  @SimpleProperty(
    description="The name of your application, used when making API calls.")
  public void ApplicationName(String ApplicationName) {
    this.ApplicationName = ApplicationName;
  }

  /* Utility Functions for Making Calls */

  private Credential authorize() throws IOException {

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

  // Uses the Google Sheets Credentials to create a Google Sheets API instance
  // required for all other Google Sheets API calls
  private Sheets getSheetsService ()  throws IOException, GeneralSecurityException {
    // Generate a new sheets service only if there is not one already created
    if (sheetsService == null) {
      Credential credential = authorize();
      this.sheetsService = new Sheets.Builder(new NetHttpTransport(),
        JacksonFactory.getDefaultInstance(), credential)
        .setApplicationName(ApplicationName)
        .build();
    }
    return sheetsService;
  }

  // Yields the A1 notation for the column, e.g. col 1 = A, col 2 = B, etc
  private String getColString (int colNumber) {
    String[] alphabet = {
      "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R",
      "S","T","U","V","W","X","Y","Z"};
    String colReference = "";
    while (colNumber > 0) {
      String digit = alphabet[(colNumber-1) % 26];
      colReference = digit + colReference;
      colNumber = (int) Math.floor((colNumber-1) / 26);
    }
    return colReference;
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

  @SimpleFunction(
    description="(Requires that the Google Sheets document is public with link) " +
      "Uses SQL-like queries to fetch data For info on the query, see Google's " +
      "Query Language Reference.")
  public void ReadWithQuery(final int gridId, final String query) {

    // Google Query API
    // https://developers.google.com/chart/interactive/docs/querylanguage?hl=en

    // Converts the query into URL friendly encoding
    String encodedQuery = "";
    try {
      encodedQuery = URLEncoder.encode(query, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // If UTF-8 is not supported, we're in big trouble!
      // According to Javadoc and Android documentation for java.nio.charset.Charset, UTF-8 is
      // available on every Java implementation.
      Log.e(LOG_TAG, "UTF-8 is unsupported?", e);
      ErrorOccurred("GetRowsWithQuery: Something went wrong encoding the query.");
      return;
    }

    // Formats the url from the template
    final String selectUrl = String.format(
      "https://spreadsheet.google.com/tq?tqx=out:csv&key=%s&gid=%d&tq=%s",
      spreadsheetID, gridId, encodedQuery);

    // Asynchronously conduct the HTTP Request
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {

          // HTTP Request
          URL url = new URL(selectUrl);
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

          // Parse the Result
          // final int responseCode = connection.getResponseCode();
          // final String responseType = connection.getContentType();
          String responseContent = getResponseContent(connection);
          final YailList parsedCsv = CsvUtil.fromCsvTable(responseContent);

          // Dispatch the event.
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              GotQueryResult(parsedCsv);
            }
          });

        // Catch Various Errors
        } catch (MalformedURLException e) {
          e.printStackTrace();
          ErrorOccurred("ReadWithQuery: MalformedURLException - " + e.getMessage());
        } catch (IOException e) {
          e.printStackTrace();
          ErrorOccurred("ReadWithQuery: IOException - " + e.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("ReadWithQuery: Exception - " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="The result of the GetRowsWithQuery call. The response is a " +
      "list of lists, similar to the result of GetRange.")
  public void GotQueryResult (YailList response) {
    EventDispatcher.dispatchEvent(this, "GotQueryResult", response);
  }

  private static String getResponseContent(HttpURLConnection connection) throws IOException {
    // Use the content encoding to convert bytes to characters.
    String encoding = connection.getContentEncoding();
    if (encoding == null) {
      encoding = "UTF-8";
    }
    InputStreamReader reader = new InputStreamReader(getConnectionStream(connection), encoding);
    try {
      int contentLength = connection.getContentLength();
      StringBuilder sb = (contentLength != -1)
          ? new StringBuilder(contentLength)
          : new StringBuilder();
      char[] buf = new char[1024];
      int read;
      while ((read = reader.read(buf)) != -1) {
        sb.append(buf, 0, read);
      }
      return sb.toString();
    } finally {
      reader.close();
    }
  }

  private static InputStream getConnectionStream(HttpURLConnection connection) throws SocketTimeoutException {
    // According to the Android reference documentation for HttpURLConnection: If the HTTP response
    // indicates that an error occurred, getInputStream() will throw an IOException. Use
    // getErrorStream() to read the error response.
    try {
      return connection.getInputStream();
    } catch (SocketTimeoutException e) {
      throw e; //Rethrow exception - should not attempt to read stream for timeouts
    } catch (IOException e1) {
      // Use the error response for all other IO Exceptions.
      return connection.getErrorStream();
    }
  }

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
            final List<String> ret = new ArrayList<String>();
            for (Object obj : values.get(0)) {
              ret.add(String.format("%s", obj));
            }

            // We need to re-enter the main thread before we can dispatch the event!
            activity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                GotRowData(ret);
              }
            });
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
    EventDispatcher.dispatchEvent(this, "GotRowData", rowDataList);
  }

  @SimpleFunction(
    description="Given a list of values as `data`, this method will write the " +
      "values to the row of the sheet. It will always start from the left most " +
      "column and continue to the right. If there are alreaddy values in that " +
      "row, this method will override them with the new data. It will not " +
      "erase the entire row.")
  public void WriteRow (String sheetName, int rowNumber, YailList data) {

    // Generates the A1 Reference for the operation
    final String rangeRef = String.format("%s!A%d", sheetName, rowNumber);

    // Generates the 2D list, which are the values to assign to the range
    LList rowValues = (LList) data.getCdr();
    List<List<Object>> values = new ArrayList<>();
    List<Object> row = new ArrayList<Object>(rowValues);
    values.add(row);

    // Sets the 2D list above to be the values in the body of the API Call
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
          sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED")
            .execute();
          // Re-enter main thread to call the Event Block
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedWriteRow();
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteRow: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the WriteRow method has " +
      "finished executing and the values on the spreadsheet have been updated.")
  public void FinishedWriteRow () {
    EventDispatcher.dispatchEvent(this, "FinishedWriteRow");
  }

  @SimpleFunction(
    description="Given a list of values as `data`, this method will write the " +
      "values to the next empty row in the sheet with the provided sheetName. ")
  public void AddRow (String sheetName, YailList data) {
    // Properly format the range
    final String rangeRef = sheetName + "!A1";

    // Generates the 2D list, which are the values to assign to the range
    LList rowValues = (LList) data.getCdr();
    List<List<Object>> values = new ArrayList<>();
    List<Object> row = new ArrayList<Object>(rowValues);
    values.add(row);

    // Sets the 2D list above to be the values in the body of the API Call
    final ValueRange body = new ValueRange()
      .setValues(values);

    // Run the API call asynchronously
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        try {
          Sheets sheetsService = getSheetsService();
          // Sends the append values request
          AppendValuesResponse response = sheetsService.spreadsheets().values()
            .append(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED") // USER_ENTERED or RAW
            .setInsertDataOption("INSERT_ROWS") // INSERT_ROWS or OVERRIDE
            .execute();

          // getUpdatedRange returns the range that updates were applied in A1
          String updatedRange = response.getUpdates().getUpdatedRange();
          // updatedRange is in the form SHEET_NAME!A#:END# => We want #
          String cell = updatedRange.split("!")[1].split(":")[0];
          // Remove non-numeric characters from the string
          final int rowNumber = Integer.parseInt(cell.replaceAll("[^\\d.]", ""));
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedAddRow(rowNumber);
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("AddRow: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the AddRow method has " +
      "finished executing and the values on the spreadsheet have been updated. " +
      "Additionally, this returns the row number for the row you've just added.")
  public void FinishedAddRow (final int rowNumber) {
    EventDispatcher.dispatchEvent(this, "FinishedAddRow", rowNumber);
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

          // Run the callback event block
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedRemoveRow();
            }
          });

        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("RemoveRow: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the RemoveRow method has " +
      "finished executing and the row on the spreadsheet have been removed.")
  public void FinishedRemoveRow () {
    EventDispatcher.dispatchEvent(this, "FinishedRemoveRow");
  }

  /* Column-wise Operations */

  @SimpleFunction(
    description="Begins an API call which will request the data stored in the " +
      "column with the provided `colNumber` (1-indexed). The resulting data " +
      "will be sent to the GotColData event block.")
  public void ReadCol (String sheetName, int colNumber) {

    // Converts the col number to the corresponding letter
    String colReference = getColString(colNumber);
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
          final List<String> ret = new ArrayList<String>();
          for (List<Object> row : values)
            ret.add(String.format("%s", row.get(0)));

          // We need to re-enter the main thread before we can dispatch the event!
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              GotColData(ret);
            }
          });
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
    EventDispatcher.dispatchEvent(this, "GotColData", colDataList);
  }

  @SimpleFunction(
    description="Given a list of values as `data`, this method will write the " +
      "values to the column of the sheet. It will always start from the top " +
      "row and continue downwards. If there are alreaddy values in that " +
      "column, this method will override them with the new data. It will not " +
      "erase the entire column, only the bits that overlap with this.")
  public void WriteCol (String sheetName, int colNumber, YailList data) {

    // Converts the col number to the corresponding letter
    String colReference = getColString(colNumber);
    final String rangeRef = sheetName + "!" + colReference + ":" + colReference;

    // Generates the body, which are the values to assign to the range
    List<List<Object>> values = new ArrayList<>();
    for (Object o : (LList) data.getCdr()) {
      List<Object> r = new ArrayList<Object>(Arrays.asList(o));
      values.add(r);
    }

    // Sets the 2D list above to be the values in the body of the API Call
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
          sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED")
            .execute();
          // Run the callback function
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedWriteCol();
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteCol: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the WriteCol method has " +
      "finished executing and the values on the spreadsheet have been updated.")
  public void FinishedWriteCol () {
    EventDispatcher.dispatchEvent(this, "FinishedWriteCol");
  }

  @SimpleFunction(
    description="Given a list of values as `data`, this method will write the " +
      "values to the next empty column in the sheet with the provided sheetName. ")
  public void AddCol (final String sheetName, YailList data) {

    // Generates the body, which are the values to assign to the range
    List<List<Object>> values = new ArrayList<>();
    for (Object o : (LList) data.getCdr()) {
      List<Object> r = new ArrayList<Object>();
      r.add(o);
      values.add(r);
    }
    final ValueRange body = new ValueRange()
      .setValues(values);

    // Wrap the API Call in an Async Utility
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        // Surround the operation with a try catch statement
        try {
          Sheets sheetsService = getSheetsService();

          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, sheetName + "!1:1" ).execute();
          // Get the actual data from the response
          List<List<Object>> values = readResult.getValues();
          // If the data we got is empty, then return so.
          if (values == null || values.isEmpty())
            ErrorOccurred("ReadRow: No data found");

          // nextCol gets mutated, keep addedColumn as a constant
          int nextCol = values.get(0).size() + 1;
          final int addedColumn = nextCol;
          // Converts the col number to the corresponding letter
          String[] alphabet = {
            "A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R",
            "S","T","U","V","W","X","Y","Z"};
          String colReference = "";
          while (nextCol > 0) {
            String digit = alphabet[(nextCol-1) % 26];
            colReference = digit + colReference;
            nextCol = (int) Math.floor((nextCol-1) / 26);
          }
          String rangeRef = sheetName + "!" + colReference + "1";

          sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED")
            .execute();

          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedAddCol(addedColumn);
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("AddCol: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the AddCol method has " +
      "finished executing and the values on the spreadsheet have been updated. " +
      "Additionally, this returns the column number for the column you've just " +
      "appended.")
  public void FinishedAddCol (final int columnNumber) {
    EventDispatcher.dispatchEvent(this, "FinishedAddCol", columnNumber);
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
          // Run the callback event
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedRemoveCol();
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("RemoveCol: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the RemoveCol method has " +
      "finished executing and the column on the spreadsheet have been removed.")
  public void FinishedRemoveCol () {
    EventDispatcher.dispatchEvent(this, "FinishedRemoveCol");
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
            activity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                GotCellData("");
              }
            });
            return;
          }

          // Format the result as a string and run the call back
          final String result = String.format("%s", values.get(0).get(0));
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              GotCellData(result);
            }
          });
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
    EventDispatcher.dispatchEvent(this, "GotCellData", cellData);
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

          // You can use the UpdateValuesResponse to find more data on the
          // result of the Write method.

          // UpdateValuesResponse result =
          sheetsService.spreadsheets().values()
            .update(spreadsheetID, rangeRef, body)
            .setValueInputOption("USER_ENTERED") // USER_ENTERED or RAW
            .execute();
          // Trigger the Callback
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedWriteCell();
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteCell: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the WriteCell method has " +
      "finished executing and the cell on the spreadsheet has been updated.")
  public void FinishedWriteCell () {
    EventDispatcher.dispatchEvent(this, "FinishedWriteCell");
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
          final List<List<String>> ret = new ArrayList<List<String>>();
          // For every object in the result, convert it to a string
          for (List<Object> row : values) {
            List<String> cellRow = new ArrayList<String>();
            for (Object cellValue : row) {
              cellRow.add(String.format("%s", cellValue));
            }
            ret.add(cellRow);
          }

          // Run the callback event
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              GotRangeData(ret);
            }
          });
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
  public void GotRangeData (List<List<String>> rangeData) {
    Log.d(LOG_TAG, "GotRangeData got: " + rangeData);
    EventDispatcher.dispatchEvent(this, "GotRangeData", rangeData);
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
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              FinishedWriteRange();
            }
          });
        }
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("WriteRange: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="This event will be triggered once the WriteRange method has " +
      "finished executing and the range on the spreadsheet has been updated.")
  public void FinishedWriteRange () {
    EventDispatcher.dispatchEvent(this, "FinishedWriteRange");
  }

  /* Sheet-wise Operations */

  @SimpleFunction(
    description="Reads the *entire* Google Sheet document. It will then provide " +
      "the values of the full sheet will be provided as a list of lists of text.")
  public void ReadSheet (final String sheetName) {

    // Asynchronously fetch the data in the cell
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run () {
        Log.d(LOG_TAG, "Reading Sheet: " + sheetName);

        try {
          Sheets sheetsService = getSheetsService();
          // Spreadsheet sheet = sheetsService.spreadsheets().get(spreadsheetID).execute();
          ValueRange readResult = sheetsService.spreadsheets().values()
            .get(spreadsheetID, sheetName).execute();
          // Get the actual data from the response
          List<List<Object>> values = readResult.getValues();

          // No Data Found
          if (values == null || values.isEmpty()) {
            ErrorOccurred("ReadSheet: No data found.");
            return;
          }
          // Format the result as a string and run the call back
          final List<List<String>> ret = new ArrayList<List<String>>();
          // For every object in the result, convert it to a string
          for (List<Object> row : values) {
            List<String> cellRow = new ArrayList<String>();
            for (Object cellValue : row) {
              cellRow.add(String.format("%s", cellValue));
            }
            ret.add(cellRow);
          }

          // We need to re-enter the main thread before we can dispatch the event!
          activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              GotSheetData(ret);
            }
          });
        }
        // Handle Errors which may have occured while sending the Read Request!
        catch (Exception e) {
          e.printStackTrace();
          ErrorOccurred("ReadSheet: " + e.getMessage());
        }
      }
    });
  }

  @SimpleEvent(
    description="After calling the ReadSheet method, the data in the range will " +
      "be stored as a list of rows, where every row is another list of text, in " +
      "`sheetData`.")
  public void GotSheetData (final List<List<String>> sheetData) {
    Log.d(LOG_TAG, "GotSheetData got: " + sheetData);
    EventDispatcher.dispatchEvent(this, "GotSheetData", sheetData);
  }

}
