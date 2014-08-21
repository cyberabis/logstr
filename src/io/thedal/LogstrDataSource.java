package io.thedal;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class LogstrDataSource {

  // Database fields
  private SQLiteDatabase database;
  private MySQLiteHelper dbHelper;
  private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
      MySQLiteHelper.COLUMN_LOG };

  public LogstrDataSource(Context context) {
    dbHelper = new MySQLiteHelper(context);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

  public Log createLog(String log) {
    ContentValues values = new ContentValues();
    values.put(MySQLiteHelper.COLUMN_LOG, log);
    long insertId = database.insert(MySQLiteHelper.TABLE_LOGSTR, null, values);
    Cursor cursor = database.query(MySQLiteHelper.TABLE_LOGSTR, allColumns,
        MySQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
    cursor.moveToFirst();
    Log newLog = cursorToLog(cursor);
    cursor.close();
    return newLog;
  }

  public void deleteLog(Log log) {
    long id = log.getId();
    database.delete(MySQLiteHelper.TABLE_LOGSTR, MySQLiteHelper.COLUMN_ID
        + " = " + id, null);
  }

  public List<Log> getAllLogs() {
    List<Log> logs = new ArrayList<Log>();

    Cursor cursor = database.query(MySQLiteHelper.TABLE_LOGSTR, allColumns,
        null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Log log = cursorToLog(cursor);
      logs.add(log);
      cursor.moveToNext();
    }
    // make sure to close the cursor
    cursor.close();
    return logs;
  }

  private Log cursorToLog(Cursor cursor) {
    Log log = new Log();
    log.setId(cursor.getLong(0));
    log.setLog(cursor.getString(1));
    return log;
  }
}
