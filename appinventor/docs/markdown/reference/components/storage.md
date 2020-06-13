---
layout: documentation
title: Storage
---

[&laquo; Back to index](index.html)
# Storage

Table of Contents:

* [CloudDB](#CloudDB)
* [File](#File)
* [GoogleSheets](#GoogleSheets)
* [TinyDB](#TinyDB)
* [TinyWebDB](#TinyWebDB)

## CloudDB  {#CloudDB}

The `CloudDB` component is a Non-visible component that allows you to store data on a Internet
 connected database server (using Redis software). This allows the users of your App to share
 data with each other. By default data will be stored in a server maintained by MIT, however you
 can setup and run your own server. Set the [`RedisServer`](#CloudDB.RedisServer) property and
 [`RedisPort`](#CloudDB.RedisPort) property to access your own server.



### Properties  {#CloudDB-Properties}

{:.properties}

{:id="CloudDB.ProjectID" .text .ro} *ProjectID*
: Gets the ProjectID for this CloudDB project.

{:id="CloudDB.RedisPort" .number .ro} *RedisPort*
: The Redis Server port to use. Defaults to 6381

{:id="CloudDB.RedisServer" .text .ro} *RedisServer*
: The Redis Server to use to store data. A setting of "DEFAULT" means that the MIT server will be used.

{:id="CloudDB.Token" .text .ro .do} *Token*
: This field contains the authentication token used to login to the backed Redis server. For the
 "DEFAULT" server, do not edit this value, the system will fill it in for you. A system
 administrator may also provide a special value to you which can be used to share data between
 multiple projects from multiple people. If using your own Redis server, set a password in the
 server's config and enter it here.

{:id="CloudDB.UseSSL" .boolean .ro .do} *UseSSL*
: Set to true to use SSL to talk to CloudDB/Redis server. This should be set to True for the "DEFAULT" server.

### Events  {#CloudDB-Events}

{:.events}

{:id="CloudDB.CloudDBError"} CloudDBError(*message*{:.text})
: Indicates that an error occurred while communicating with the CloudDB Redis server.

{:id="CloudDB.DataChanged"} DataChanged(*tag*{:.text},*value*{:.any})
: Indicates that the data in the CloudDB project has changed. Launches an event with the
 `tag`{:.text.block} that has been updated and the `value`{:.variable.block} it now has.

{:id="CloudDB.FirstRemoved"} FirstRemoved(*value*{:.any})
: Event triggered by the [`RemoveFirstFromList`](#CloudDB.RemoveFirstFromList) function. The argument
 `value`{:.variable.block} is the object that was the first in the list, and which is now
 removed.

{:id="CloudDB.GotValue"} GotValue(*tag*{:.text},*value*{:.any})
: Indicates that a [`GetValue`](#CloudDB.GetValue) request has succeeded.

{:id="CloudDB.TagList"} TagList(*value*{:.list})
: Event triggered when we have received the list of known tags. Run in response to a call to the
 [`GetTagList`](#CloudDB.GetTagList) function.

### Methods  {#CloudDB-Methods}

{:.methods}

{:id="CloudDB.AppendValueToList" class="method"} <i/> AppendValueToList(*tag*{:.text},*itemToAdd*{:.any})
: Append a value to the end of a list atomically. If two devices use this function simultaneously, both will be appended and no data lost.

{:id="CloudDB.ClearTag" class="method"} <i/> ClearTag(*tag*{:.text})
: Remove the tag from CloudDB.

{:id="CloudDB.CloudConnected" class="method returns boolean"} <i/> CloudConnected()
: Returns `true`{:.logic.block} if we are on the network and will likely be able to connect to
 the `CloudDB` server.

{:id="CloudDB.GetTagList" class="method"} <i/> GetTagList()
: Asks `CloudDB` to retrieve all the tags belonging to this project. The
 resulting list is returned in the event [`TagList`](#CloudDB.TagList).

{:id="CloudDB.GetValue" class="method"} <i/> GetValue(*tag*{:.text},*valueIfTagNotThere*{:.any})
: `GetValue` asks `CloudDB` to get the value stored under the given tag.
 It will pass the result to the [`GotValue`](#CloudDB.GotValue) will be given.

{:id="CloudDB.RemoveFirstFromList" class="method"} <i/> RemoveFirstFromList(*tag*{:.text})
: Obtain the first element of a list and atomically remove it. If two devices use this function
 simultaneously, one will get the first element and the the other will get the second element,
 or an error if there is no available element. When the element is available, the
 [`FirstRemoved`](#CloudDB.FirstRemoved) event will be triggered.

{:id="CloudDB.StoreValue" class="method"} <i/> StoreValue(*tag*{:.text},*valueToStore*{:.any})
: Asks `CloudDB` to store the given `value`{:.variable.block} under the given
 `tag`{:.text.block}.

## File  {#File}

Non-visible component for storing and retrieving files. Use this component to write or read files
 on the device. The default behavior is to write files to the private data directory associated
 with the app. The Companion writes files to `/sdcard/AppInventor/data` for easy debugging. If
 the file path starts with a slash (`/`), then the file is created relative to `/sdcard`.
 For example, writing a file to `/myFile.txt` will write the file in `/sdcard/myFile.txt`.



### Properties  {#File-Properties}

{:.properties}
None


### Events  {#File-Events}

{:.events}

{:id="File.AfterFileSaved"} AfterFileSaved(*fileName*{:.text})
: Event indicating that the contents of the file have been written.

{:id="File.GotText"} GotText(*text*{:.text})
: Event indicating that the contents from the file have been read.

### Methods  {#File-Methods}

{:.methods}

{:id="File.AppendToFile" class="method"} <i/> AppendToFile(*text*{:.text},*fileName*{:.text})
: Appends text to the end of a file. Creates the file if it does not already exist. See the help
 text under [`SaveFile`](#File.SaveFile) for information about where files are written.
 On success, the [`AfterFileSaved`](#File.AfterFileSaved) event will run.

{:id="File.Delete" class="method"} <i/> Delete(*fileName*{:.text})
: Deletes a file from storage. Prefix the `fileName`{:.text.block} with `/` to delete a specific
 file in the SD card (for example, `/myFile.txt` will delete the file `/sdcard/myFile.txt`).
 If the `fileName`{:.text.block} does not begin with a `/`, then the file located in the
 program's private storage will be deleted. Starting the `fileName`{:.text.block} with `//` is
 an error because asset files cannot be deleted.

{:id="File.ReadFrom" class="method"} <i/> ReadFrom(*fileName*{:.text})
: Reads text from a file in storage. Prefix the `fileName`{:.text.block} with `/` to read from a
 specific file on the SD card (for example, `/myFile.txt` will read the file
 `/sdcard/myFile.txt`). To read assets packaged with an application (also works for the
 Companion) start the `fileName`{:.text.block} with `//` (two slashes). If a
 `fileName`{:.text.block} does not start with a slash, it will be read from the application's
 private storage (for packaged apps) and from `/sdcard/AppInventor/data` for the Companion.

{:id="File.SaveFile" class="method"} <i/> SaveFile(*text*{:.text},*fileName*{:.text})
: Saves text to a file. If the `fileName`{:.text.block} begins with a slash (`/`) the file is
 written to the sdcard (for example, writing to `/myFile.txt` will write the file to
 `/sdcard/myFile.txt`). If the `fileName`{:.text.block} does not start with a slash, it will be
 written in the program's private data directory where it will not be accessible to other
 programs on the phone. There is a special exception for the AI Companion where these files are
 written to `/sdcard/AppInventor/data` to facilitate debugging.

   Note that this block will overwrite a file if it already exists. If you want to add content
 to an existing file use the [`AppendToFile`](#File.AppendToFile) method.

## GoogleSheets  {#GoogleSheets}

Appinventor Google Sheets Component



### Properties  {#GoogleSheets-Properties}

{:.properties}

{:id="GoogleSheets.CredentialsJson" .text} *CredentialsJson*
: Property for CredentialsJson

{:id="GoogleSheets.SpreadsheetID" .text} *SpreadsheetID*
: The ID for the Google Sheets file you want to edit. You can find the spreadsheetID in the URL of the Google Sheets file.

### Events  {#GoogleSheets-Events}

{:.events}

{:id="GoogleSheets.ErrorOccurred"} ErrorOccurred(*errorMessage*{:.text})
: This event block is triggered whenever an API call encounters an error. Text with details about the error can be found in `errorMessage`

{:id="GoogleSheets.FinishedAddCol"} FinishedAddCol(*columnNumber*{:.number})
: This event will be triggered once the AddCol method has finished executing and the values on the spreadsheet have been updated. Additionally, this returns the column number for the column you've just appended.

{:id="GoogleSheets.FinishedAddRow"} FinishedAddRow(*rowNumber*{:.number})
: This event will be triggered once the AddRow method has finished executing and the values on the spreadsheet have been updated. Additionally, this returns the row number for the row you've just added.

{:id="GoogleSheets.FinishedRemoveCol"} FinishedRemoveCol()
: This event will be triggered once the RemoveCol method has finished executing and the column on the spreadsheet have been removed.

{:id="GoogleSheets.FinishedRemoveRow"} FinishedRemoveRow()
: This event will be triggered once the RemoveRow method has finished executing and the row on the spreadsheet have been removed.

{:id="GoogleSheets.FinishedWriteCell"} FinishedWriteCell()
: This event will be triggered once the WriteCell method has finished executing and the cell on the spreadsheet has been updated.

{:id="GoogleSheets.FinishedWriteCol"} FinishedWriteCol()
: This event will be triggered once the WriteCol method has finished executing and the values on the spreadsheet have been updated.

{:id="GoogleSheets.FinishedWriteRange"} FinishedWriteRange()
: This event will be triggered once the WriteRange method has finished executing and the range on the spreadsheet has been updated.

{:id="GoogleSheets.FinishedWriteRow"} FinishedWriteRow()
: This event will be triggered once the WriteRow method has finished executing and the values on the spreadsheet have been updated.

{:id="GoogleSheets.GotCellData"} GotCellData(*cellData*{:.text})
: After calling the ReadCell method, the data in the cell will be stored as text in `cellData`.

{:id="GoogleSheets.GotColData"} GotColData(*colDataList*{:.list})
: After calling the ReadCol method, the data in the column will be stored as a list of text values in `colDataList`.

{:id="GoogleSheets.GotRangeData"} GotRangeData(*rangeData*{:.list})
: After calling the ReadRange method, the data in the range will be stored as a list of rows, where every row is another list of text, in `rangeData`.

{:id="GoogleSheets.GotRowData"} GotRowData(*rowDataList*{:.list})
: After calling the ReadRow method, the data in the row will be stored as a list of text values in rowDataList.

{:id="GoogleSheets.GotSheetData"} GotSheetData(*sheetData*{:.list})
: After calling the ReadSheet method, the data in the range will be stored as a list of rows, where every row is another list of text, in `sheetData`.

### Methods  {#GoogleSheets-Methods}

{:.methods}

{:id="GoogleSheets.AddCol" class="method"} <i/> AddCol(*sheetName*{:.text},*data*{:.list})
: Given a list of values as `data`, this method will write the values to the next empty column in the sheet with the provided sheetName.

{:id="GoogleSheets.AddRow" class="method"} <i/> AddRow(*sheetName*{:.text},*data*{:.list})
: Given a list of values as `data`, this method will write the values to the next empty row in the sheet with the provided sheetName.

{:id="GoogleSheets.GetCellReference" class="method returns text"} <i/> GetCellReference(*row*{:.number},*col*{:.number})
: Converts the integer representation of rows and columns to the reference strings used in Google Sheets. For example, row 1 and col 2 corresponds to the string "B1".

{:id="GoogleSheets.GetRangeReference" class="method returns text"} <i/> GetRangeReference(*row1*{:.number},*col1*{:.number},*row2*{:.number},*col2*{:.number})
: Converts the integer representation of rows and columns for the corners of the range to the reference strings used in Google Sheets. For ex, selecting the range from row 1 col 2 to row 3 col 4 corresponds to the string "B1:D3"

{:id="GoogleSheets.ReadCell" class="method"} <i/> ReadCell(*sheetName*{:.text},*cellReference*{:.text})
: Begins an API call which will request the data stored in the cell with the provided cell reference. This cell reference can be the result of the getCellReference block, or a text block with the correct A1 notation. The resulting cell data will be sent to the GotCellData event block.

{:id="GoogleSheets.ReadCol" class="method"} <i/> ReadCol(*sheetName*{:.text},*colNumber*{:.number})
: Begins an API call which will request the data stored in the column with the provided `colNumber` (1-indexed). The resulting data will be sent to the GotColData event block.

{:id="GoogleSheets.ReadRange" class="method"} <i/> ReadRange(*sheetName*{:.text},*rangeReference*{:.text})
: Begins an API call which will request the data stored in the range with the provided range reference. This range reference can be the result of the getRangeReference block, or a text block with the correct A1 notation. The resulting range data will be sent to the GotRangeData event block.

{:id="GoogleSheets.ReadRow" class="method"} <i/> ReadRow(*sheetName*{:.text},*rowNumber*{:.number})
: On the sheet with the provided sheet name, this method will read the row with the given number and returns the text that is found in each cell.

{:id="GoogleSheets.ReadSheet" class="method"} <i/> ReadSheet(*sheetName*{:.text})
: Reads the *entire* Google Sheet document. It will then provide the values of the full sheet will be provided as a list of lists of text.

{:id="GoogleSheets.RemoveCol" class="method"} <i/> RemoveCol(*gridId*{:.number},*colNumber*{:.number})
: Deletes the column with the given column number (1-indexed) from the sheets page with the grid ID `gridId`. This does not clear the column, but removes it entirely. The sheet's grid id can be found at the end of the url of the Google Sheets document, right after the `gid=`.

{:id="GoogleSheets.RemoveRow" class="method"} <i/> RemoveRow(*gridId*{:.number},*rowNumber*{:.number})
: Deletes the row with the given row number (1-indexed) from the sheets page with the grid ID `gridId`. This does not clear the row, but removes it entirely. The sheet's grid id can be found at the end of the url of the Google Sheets document, right after the `gid=`.

{:id="GoogleSheets.WriteCell" class="method"} <i/> WriteCell(*sheetName*{:.text},*cellReference*{:.text},*data*{:.any})
: Assigns the text in `data` to the cell at the provided cell reference. If there is already a value in this range, the old value be overriden by the new text.

{:id="GoogleSheets.WriteCol" class="method"} <i/> WriteCol(*sheetName*{:.text},*colNumber*{:.number},*data*{:.list})
: Given a list of values as `data`, this method will write the values to the column of the sheet. It will always start from the top row and continue downwards. If there are alreaddy values in that column, this method will override them with the new data. It will not erase the entire column, only the bits that overlap with this.

{:id="GoogleSheets.WriteRange" class="method"} <i/> WriteRange(*sheetName*{:.text},*rangeReference*{:.text},*data*{:.list})
: Assigns the values in the data value, which is a list of lists, to the range that you specify. The number of rows and columns in the range reference must match the dimensions of the 2D list provided in data.

{:id="GoogleSheets.WriteRow" class="method"} <i/> WriteRow(*sheetName*{:.text},*rowNumber*{:.number},*data*{:.list})
: Given a list of values as `data`, this method will write the values to the row of the sheet. It will always start from the left most column and continue to the right. If there are alreaddy values in that row, this method will override them with the new data. It will not erase the entire row.

## TinyDB  {#TinyDB}

`TinyDB` is a non-visible component that stores data for an app.

 Apps created with App Inventor are initialized each time they run. This means that if an app
 sets the value of a variable and the user then quits the app, the value of that variable will
 not be remembered the next time the app is run. In contrast, TinyDB is a persistent data store
 for the app. The data stored in a `TinyDB` will be available each time the app is run. An
 example might be a game that saves the high score and retrieves it each time the game is played.

 Data items consist of tags and values. To store a data item, you specify the tag it should be
 stored under. The tag must be a text block, giving the data a name. Subsequently, you can
 retrieve the data that was stored under a given tag.

 You cannot use the `TinyDB` to pass data between two different apps on the phone, although you
 can use the `TinyDB` to share data between the different screens of a multi-screen app.

 When you are developing apps using the AI Companion, all the apps using that Companion will
 share the same `TinyDB`. That sharing will disappear once the apps are packaged and installed on
 the phone. During development you should be careful to clear the Companion app's data each time
 you start working on a new app.



### Properties  {#TinyDB-Properties}

{:.properties}

{:id="TinyDB.Namespace" .text} *Namespace*
: Namespace for storing data.

### Events  {#TinyDB-Events}

{:.events}
None


### Methods  {#TinyDB-Methods}

{:.methods}

{:id="TinyDB.ClearAll" class="method"} <i/> ClearAll()
: Clear the entire data store.

{:id="TinyDB.ClearTag" class="method"} <i/> ClearTag(*tag*{:.text})
: Clear the entry with the given `tag`{:.text.block}.

{:id="TinyDB.GetTags" class="method returns any"} <i/> GetTags()
: Return a list of all the tags in the data store.

{:id="TinyDB.GetValue" class="method returns any"} <i/> GetValue(*tag*{:.text},*valueIfTagNotThere*{:.any})
: Retrieve the value stored under the given `tag`{:.text.block}.  If there's no such tag, then
 return `valueIfTagNotThere`{:.variable.block}.

{:id="TinyDB.StoreValue" class="method"} <i/> StoreValue(*tag*{:.text},*valueToStore*{:.any})
: Store the given `valueToStore`{:.variable.block} under the given `tag`{:.text.block}.
 The storage persists on the phone when the app is restarted.

## TinyWebDB  {#TinyWebDB}

The `TinyWebDB` component communicates with a Web service to store
 and retrieve information.  Although this component is usable, it is
 very limited and meant primarily as a demonstration for people who
 would like to create their own components that talk to the Web.
 The accompanying Web service is at
 (http://tinywebdb.appinventor.mit.edu).  The component has methods to
 [store a value](#TinyWebDB.StoreValue) under a tag and to
 [retrieve the value](#TinyWebDB.GetValue) associated with
 the tag.  The interpretation of what "store" and "retrieve" means
 is up to the Web service.  In this implementation, all tags and
 values are strings (text).  This restriction may be relaxed in
 future versions.



### Properties  {#TinyWebDB-Properties}

{:.properties}

{:id="TinyWebDB.ServiceURL" .text} *ServiceURL*
: Specifies the URL of the  Web service.
 The default value is the demo service running on App Engine.

### Events  {#TinyWebDB-Events}

{:.events}

{:id="TinyWebDB.GotValue"} GotValue(*tagFromWebDB*{:.text},*valueFromWebDB*{:.any})
: Indicates that a [`GetValue`](#TinyWebDB.GetValue) server request has succeeded.

{:id="TinyWebDB.ValueStored"} ValueStored()
: Event indicating that a [`StoreValue`](#TinyWebDB.StoreValue)  server request has succeeded.

{:id="TinyWebDB.WebServiceError"} WebServiceError(*message*{:.text})
: Indicates that the communication with the Web service signaled an error.

### Methods  {#TinyWebDB-Methods}

{:.methods}

{:id="TinyWebDB.GetValue" class="method"} <i/> GetValue(*tag*{:.text})
: `GetValue` asks the Web service to get the value stored under the given `tag`{:.text.block}.
 It is up to the Web service what to return if there is no value stored under the
 `tag`{:.text.block}.  This component just accepts whatever is returned. The
 [`GotValue`](#TinyWebDB.GotValue) event will be run on completion.

{:id="TinyWebDB.StoreValue" class="method"} <i/> StoreValue(*tag*{:.text},*valueToStore*{:.any})
: Sends a request to the Web service to store the given `valueToStore`{:.variable.block} under
 the given `tag`{:.text.block}. The [`ValueStored`](#TinyWebDB.ValueStored) event will be run on completion.
