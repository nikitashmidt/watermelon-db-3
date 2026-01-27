#include "sqlite3.h"
#include <stdio.h>

int main() {
    sqlite3 *db;
    sqlite3_open(":memory:", &db);
    
    sqlite3_stmt *stmt;
    const char *sql = "SELECT * FROM pragma_compile_options WHERE compile_options LIKE '%ICU%'";
    
    if(sqlite3_prepare_v2(db, sql, -1, &stmt, 0) == SQLITE_OK) {
        while(sqlite3_step(stmt) == SQLITE_ROW) {
            printf("Found: %s\n", sqlite3_column_text(stmt, 0));
        }
    }
    
    sqlite3_finalize(stmt);
    sqlite3_close(db);
    return 0;
}
