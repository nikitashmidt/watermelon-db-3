#include "sqlite3.h"
#include <stdio.h>
#include <string.h>

static int callback(void *data, int argc, char **argv, char **colNames) {
    for(int i = 0; i < argc; i++) {
        printf("%s = %s\n", colNames[i], argv[i] ? argv[i] : "NULL");
    }
    printf("\n");
    return 0;
}

int main() {
    sqlite3 *db;
    char *err_msg = 0;
    
    printf("=== SQLite ICU Unit Test ===\n\n");
    
    // Open database
    if(sqlite3_open(":memory:", &db) != SQLITE_OK) {
        fprintf(stderr, "Cannot open database: %s\n", sqlite3_errmsg(db));
        return 1;
    }
    
    printf("1. SQLite version: %s\n", sqlite3_libversion());
    
    // Check ICU compile option
    printf("\n2. Checking ICU compile option:\n");
    sqlite3_exec(db, 
        "SELECT 'ICU_ENABLED' as option, \
                CASE WHEN compile_options LIKE '%ICU%' THEN 'YES' ELSE 'NO' END as value \
         FROM pragma_compile_options \
         WHERE compile_options LIKE '%ICU%'",
        callback, 0, &err_msg);
    
    // Try to use ICU functions
    printf("\n3. Testing ICU functions:\n");
    
    // First, let's see what functions are available
    printf("Available functions with 'icu' in name:\n");
    sqlite3_exec(db,
        "SELECT name FROM pragma_function_list WHERE name LIKE '%icu%'",
        callback, 0, &err_msg);
    
    // Try icu_load_collation
    printf("\n4. Loading ICU collations:\n");
    
    // Load English collation
    int rc = sqlite3_exec(db, 
        "SELECT icu_load_collation('en_US', 'english') as result",
        callback, 0, &err_msg);
    
    if(rc == SQLITE_OK) {
        printf("✓ English collation loaded\n");
        
        // Load Russian collation
        rc = sqlite3_exec(db, 
            "SELECT icu_load_collation('ru_RU', 'russian') as result",
            callback, 0, &err_msg);
        
        if(rc == SQLITE_OK) {
            printf("✓ Russian collation loaded\n");
        }
        
        // Test collations
        printf("\n5. Testing collations:\n");
        
        // Test 1: Case-insensitive English sorting
        printf("English sorting (case-insensitive):\n");
        sqlite3_exec(db,
            "CREATE TABLE words (word TEXT); \
             INSERT INTO words VALUES \
               ('zebra'), ('Apple'), ('banana'), ('apple'), ('Zebra'), ('Banana'); \
             SELECT word FROM words ORDER BY word COLLATE english;",
            callback, 0, &err_msg);
        
        // Test 2: Russian sorting
        printf("\nRussian sorting:\n");
        sqlite3_exec(db,
            "DELETE FROM words; \
             INSERT INTO words VALUES \
               ('Яблоко'), ('абрикос'), ('Бананы'), ('яблоко'), ('Абрикос'), ('бананы'); \
             SELECT word FROM words ORDER BY word COLLATE russian;",
            callback, 0, &err_msg);
        
        // Test 3: Turkish specific (dotless i)
        printf("\nTurkish collation:\n");
        rc = sqlite3_exec(db,
            "SELECT icu_load_collation('tr_TR', 'turkish')",
            callback, 0, &err_msg);
            
        if(rc == SQLITE_OK) {
            sqlite3_exec(db,
                "DELETE FROM words; \
                 INSERT INTO words VALUES ('İstanbul'), ('istanbul'), ('Istanbul'); \
                 SELECT word FROM words ORDER BY word COLLATE turkish;",
                callback, 0, &err_msg);
        }
        
    } else {
        printf("✗ Failed to load collation: %s\n", err_msg);
        sqlite3_free(err_msg);
    }
    
    // Clean up
    sqlite3_exec(db, "DROP TABLE IF EXISTS words", 0, 0, 0);
    
    sqlite3_close(db);
    printf("\n=== Test completed ===\n");
    return 0;
}
