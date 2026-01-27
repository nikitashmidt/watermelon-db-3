#include "sqlite3.h"
#include <stdio.h>

/* Объявление функции инициализации ICU (она в sqlite3.o) */
int sqlite3IcuInit(sqlite3 *db);

int main() {
    sqlite3 *db;
    char *err_msg = 0;
    
    printf("Testing SQLite with explicit ICU initialization...\n");
    
    // Открываем базу
    if(sqlite3_open(":memory:", &db) != SQLITE_OK) {
        fprintf(stderr, "Cannot open database\n");
        return 1;
    }
    
    printf("SQLite version: %s\n", sqlite3_libversion());
    
    // 1. Пытаемся инициализировать ICU
    printf("\n1. Initializing ICU...\n");
    int rc = sqlite3IcuInit(db);
    if(rc == SQLITE_OK) {
        printf("ICU initialization: SUCCESS\n");
    } else {
        printf("ICU initialization: FAILED (code: %d)\n", rc);
    }
    
    // 2. Тестируем ICU функции
    printf("\n2. Testing ICU functions...\n");
    
    // Пробуем вызвать icu_version()
    rc = sqlite3_exec(db, "SELECT icu_version()", 
                     NULL, NULL, &err_msg);
    
    if(rc == SQLITE_OK) {
        printf("icu_version() works!\n");
    } else {
        printf("icu_version() failed: %s\n", err_msg);
        sqlite3_free(err_msg);
        err_msg = 0;
    }
    
    // Пробуем загрузить колляцию
    rc = sqlite3_exec(db, "SELECT icu_load_collation('en_US', 'english')", 
                     NULL, NULL, &err_msg);
    
    if(rc == SQLITE_OK) {
        printf("icu_load_collation() works!\n");
    } else {
        printf("icu_load_collation() failed: %s\n", err_msg);
        sqlite3_free(err_msg);
    }
    
    sqlite3_close(db);
    return 0;
}
