#include "sqlite3.h"
#include <stdio.h>

int main() {
    sqlite3 *db;
    printf("Testing built-in ICU...\n");
    
    sqlite3_open(":memory:", &db);
    
    // Проверяем, доступны ли ICU функции
    sqlite3_stmt *stmt;
    const char *sql = "SELECT name FROM pragma_function_list WHERE name LIKE 'icu_%'";
    
    if(sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK) {
        while(sqlite3_step(stmt) == SQLITE_ROW) {
            printf("Found ICU function: %s\n", sqlite3_column_text(stmt, 0));
        }
        sqlite3_finalize(stmt);
    }
    
    // Пробуем использовать
    sqlite3_exec(db, 
        "SELECT icu_load_collation('ru_RU', 'russian'); \
         CREATE TABLE t(t TEXT); \
         INSERT INTO t VALUES ('Я'), ('А'), ('Б'); \
         SELECT t FROM t ORDER BY t COLLATE russian;",
        0, 0, 0);
    
    sqlite3_close(db);
    return 0;
}
