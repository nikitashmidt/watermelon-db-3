#include "sqlite3.h"
#include <stdio.h>

int main() {
    printf("SQLite with ICU test\n");
    printf("Version: %s\n", sqlite3_libversion());
    
    sqlite3 *db;
    if(sqlite3_open(":memory:", &db) == SQLITE_OK) {
        printf("Database opened successfully\n");
        
        // Test ICU
        char *err = 0;
        int rc = sqlite3_exec(db, "SELECT icu_version()", 0, 0, &err);
        if(rc == SQLITE_OK) {
            printf("ICU support: ENABLED\n");
        } else {
            printf("ICU support: NOT enabled (%s)\n", err);
            sqlite3_free(err);
        }
        
        sqlite3_close(db);
    }
    return 0;
}
