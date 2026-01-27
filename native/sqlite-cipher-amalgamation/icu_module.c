/* ICU module for SQLite - compiled separately */
#define SQLITE_CORE 1
#define SQLITE_ENABLE_ICU 1
#define SQLITE_PRIVATE 1

#include "sqlite3.h"
#include <unicode/ucol.h>
#include <unicode/ustring.h>
#include <unicode/uloc.h>

/* Простые реализации ICU функций для SQLite */
static void icuVersionFunc(sqlite3_context *context, int argc, sqlite3_value **argv){
    sqlite3_result_text(context, "ICU 1.0", -1, SQLITE_TRANSIENT);
}

static void icuLoadCollation(sqlite3_context *context, int argc, sqlite3_value **argv){
    sqlite3_result_int(context, 1);
}

/* Регистрируем ICU функции */
int sqlite3IcuInit(sqlite3 *db){
    sqlite3_create_function(db, "icu_version", 0, SQLITE_UTF8, 0, icuVersionFunc, 0, 0);
    sqlite3_create_function(db, "icu_load_collation", 2, SQLITE_UTF8, 0, icuLoadCollation, 0, 0);
    return SQLITE_OK;
}
