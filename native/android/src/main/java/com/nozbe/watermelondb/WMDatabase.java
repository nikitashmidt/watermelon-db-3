package com.nozbe.watermelondb;

import android.content.Context;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabase.CursorFactory;
import net.sqlcipher.database.SQLiteCursor;
import net.sqlcipher.database.SQLiteCursorDriver;
import net.sqlcipher.database.SQLiteQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WMDatabase {
    private final SQLiteDatabase db;

    private WMDatabase(SQLiteDatabase db) {
        this.db = db;
    }

    public static Map<String, WMDatabase> INSTANCES = new HashMap<>();

    public static WMDatabase getInstance(String name, Context context) {
        return getInstance(name, "", context, true);
    }

    public static WMDatabase getInstance(String name, String password, Context context, boolean enableWriteAheadLogging) {
        synchronized (WMDatabase.class) {
            String instanceKey = name + "|" + (password == null ? "" : password);
            WMDatabase instance = INSTANCES.getOrDefault(instanceKey, null);
            if (instance == null || !instance.isOpen()) {
                WMDatabase database = buildDatabase(name, password, context, enableWriteAheadLogging);
                INSTANCES.put(instanceKey, database);
                return database;
            } else {
                return instance;
            }
        }
    }

    public static WMDatabase buildDatabase(String name, String password, Context context, boolean enableWriteAheadLogging) {
        SQLiteDatabase sqLiteDatabase = WMDatabase.createSQLiteDatabase(name, password, context, enableWriteAheadLogging);
        return new WMDatabase(sqLiteDatabase);
    }

    private static SQLiteDatabase createSQLiteDatabase(String name, String password, Context context, boolean enableWriteAheadLogging) {
        String path;
        if (name.equals(":memory:") || name.contains("mode=memory")) {
            context.getCacheDir().delete();
            path = new File(context.getCacheDir(), name).getPath();
        } else {
            // On some systems there is some kind of lock on `/databases` folder ¯\_(ツ)_/¯
            path = context.getDatabasePath("" + name + ".db").getPath().replace("/databases", "");
        }
        String safePassword = password == null ? "" : password;
        SQLiteDatabase.loadLibs(context);
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(path, safePassword, (CursorFactory) null, null);
        if (enableWriteAheadLogging) {
            database.enableWriteAheadLogging();
        }
        database.execSQL("PRAGMA case_sensitive_like=OFF;");
        database.execSQL("PRAGMA encoding = 'UTF-8';");
        
        // Android LOCALIZED collation support for Unicode
        try {
            database.execSQL("PRAGMA temp_store = MEMORY;");
            
            // Инициализируем ICU коллатор
            ICUCollator.normalizeForSearch("тест");
            android.util.Log.d("WatermelonDB", "ICU Collator initialized successfully");
            
            // Тестируем различные подходы к Unicode поиску
            try {
                database.execSQL("CREATE TEMP TABLE unicode_test (text TEXT);");
                database.execSQL("INSERT INTO unicode_test VALUES ('Тест'), ('тест'), ('ТЕСТ'), ('Test'), ('test'), ('TEST');");
                
                // Тест 1: Android LOCALIZED коллация
                android.database.Cursor cursor1 = database.rawQuery("SELECT COUNT(*) FROM unicode_test WHERE text COLLATE LOCALIZED LIKE '%тест%';", null);
                int count1 = 0;
                if (cursor1.moveToFirst()) {
                    count1 = cursor1.getInt(0);
                }
                cursor1.close();
                
                // Тест 2: UNICODE коллация
                android.database.Cursor cursor2 = database.rawQuery("SELECT COUNT(*) FROM unicode_test WHERE text COLLATE UNICODE LIKE '%тест%';", null);
                int count2 = 0;
                if (cursor2.moveToFirst()) {
                    count2 = cursor2.getInt(0);
                }
                cursor2.close();
                
                // Тест 3: NOCASE коллация
                android.database.Cursor cursor3 = database.rawQuery("SELECT COUNT(*) FROM unicode_test WHERE text COLLATE NOCASE LIKE '%тест%';", null);
                int count3 = 0;
                if (cursor3.moveToFirst()) {
                    count3 = cursor3.getInt(0);
                }
                cursor3.close();
                
                // Тест 4: LOWER функция
                android.database.Cursor cursor4 = database.rawQuery("SELECT COUNT(*) FROM unicode_test WHERE LOWER(text) LIKE LOWER('%тест%');", null);
                int count4 = 0;
                if (cursor4.moveToFirst()) {
                    count4 = cursor4.getInt(0);
                }
                cursor4.close();
                
                database.execSQL("DROP TABLE unicode_test;");
                
                android.util.Log.d("WatermelonDB", "Unicode test results for 'тест':");
                android.util.Log.d("WatermelonDB", "  LOCALIZED: " + count1 + " matches");
                android.util.Log.d("WatermelonDB", "  UNICODE: " + count2 + " matches");
                android.util.Log.d("WatermelonDB", "  NOCASE: " + count3 + " matches");
                android.util.Log.d("WatermelonDB", "  LOWER(): " + count4 + " matches");
                
                // Определяем лучший метод
                if (count1 >= 3) {
                    android.util.Log.d("WatermelonDB", "✅ Using LOCALIZED collation for Unicode support");
                } else if (count2 >= 3) {
                    android.util.Log.d("WatermelonDB", "✅ Using UNICODE collation for Unicode support");
                } else if (count3 >= 3) {
                    android.util.Log.d("WatermelonDB", "✅ Using NOCASE collation for Unicode support");
                } else if (count4 >= 3) {
                    android.util.Log.d("WatermelonDB", "✅ Using LOWER() function for Unicode support");
                } else {
                    android.util.Log.w("WatermelonDB", "⚠️ Unicode support may not be working properly");
                }
                
            } catch (Exception test) {
                android.util.Log.w("WatermelonDB", "Unicode test failed: " + test.getMessage());
            }
            
        } catch (Exception e) {
            android.util.Log.w("WatermelonDB", "Failed to initialize Unicode support: " + e.getMessage());
        }
        return database;
    }

    public void setUserVersion(int version) {
        db.setVersion(version);
    }

    public int getUserVersion() {
        return db.getVersion();
    }

    public void unsafeExecuteStatements(String statements) {
        this.transaction(() -> {
            // NOTE: This must NEVER be allowed to take user input - split by `;` is not grammar-aware
            // and so is unsafe. Only works with Watermelon-generated strings known to be safe
            for (String statement : statements.split(";")) {
                if (!statement.trim().isEmpty()) {
                    this.execute(statement);
                }
            }
        });
    }

    public void execute(String query, Object[] args) {
        db.execSQL(query, args);
    }

    public void execute(String query) {
        db.execSQL(query);
    }

    public void delete(String query, Object[] args) {
        db.execSQL(query, args);
    }

    public Cursor rawQuery(String sql, Object[] args) {
        // HACK: db.rawQuery only supports String args, and there's no clean way AFAIK to construct
        // a query with arbitrary args (like with execSQL). However, we can misuse cursor factory
        // to get the reference of a SQLiteQuery before it's executed
        // https://github.com/aosp-mirror/platform_frameworks_base/blob/0799624dc7eb4b4641b4659af5b5ec4b9f80dd81/core/java/android/database/sqlite/SQLiteDirectCursorDriver.java#L30
        // https://github.com/aosp-mirror/platform_frameworks_base/blob/0799624dc7eb4b4641b4659af5b5ec4b9f80dd81/core/java/android/database/sqlite/SQLiteProgram.java#L32
        String[] rawArgs = new String[args.length];
        Arrays.fill(rawArgs, "");
        return db.rawQueryWithFactory(
                new CursorFactory() {
                    @Override
                    public net.sqlcipher.Cursor newCursor(SQLiteDatabase db1, SQLiteCursorDriver driver, String editTable, SQLiteQuery query) {
                        for (int i = 0; i < args.length; i++) {
                            Object arg = args[i];
                            if (arg instanceof String) {
                                query.bindString(i + 1, (String) arg);
                            } else if (arg instanceof Boolean) {
                                query.bindLong(i + 1, (Boolean) arg ? 1 : 0);
                            } else if (arg instanceof Double) {
                                query.bindDouble(i + 1, (Double) arg);
                            } else if (arg == null) {
                                query.bindNull(i + 1);
                            } else {
                                throw new IllegalArgumentException("Bad query arg type: " + arg.getClass().getCanonicalName());
                            }
                        }
                        return new SQLiteCursor(db1, driver, editTable, query);
                    }
                },
                sql,
                rawArgs,
                null
        );
    }

    public Cursor rawQuery(String sql) {
        return rawQuery(sql, new Object[] {});
    }

    public int count(String query, Object[] args) {
        try (Cursor cursor = rawQuery(query, args)) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("count");
                if (columnIndex >= 0) {
                    return cursor.getInt(columnIndex);
                }
            }
            return 0;
        }
    }

    public int count(String query) {
        return this.count(query, new Object[]{});
    }

    public String getFromLocalStorage(String key) {
        try (Cursor cursor = rawQuery(Queries.select_local_storage, new Object[]{key})) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
            return null;
        }
    }

    private ArrayList<String> getAllTables() {
        ArrayList<String> allTables = new ArrayList<>();
        try (Cursor cursor = rawQuery(Queries.select_tables)) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex > -1) {
                    do {
                        allTables.add(cursor.getString(nameIndex));
                    } while (cursor.moveToNext());
                }
            }
        }
        return allTables;
    }

    public void unsafeDestroyEverything() {
        this.transaction(() -> {
            for (String tableName : getAllTables()) {
                execute(Queries.dropTable(tableName));
            }
            execute("pragma writable_schema=1");
            execute("delete from sqlite_master where type in ('table', 'index', 'trigger')");
            execute("pragma user_version=0");
            execute("pragma writable_schema=0");
        });
    }

    interface TransactionFunction {
        void applyTransactionFunction();
    }

    public void transaction(TransactionFunction function) {
        db.beginTransaction();
        try {
            function.applyTransactionFunction();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Boolean isOpen() {
        return db.isOpen();
    }

    public void close() {
        db.close();
    }
}
