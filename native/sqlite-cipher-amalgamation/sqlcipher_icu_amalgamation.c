/* Основной SQLCipher с поддержкой ICU */
#define SQLITE_ENABLE_ICU 1
#define SQLITE_HAS_CODEC 1
#define SQLITE_CORE 1

/* Включаем основной SQLite */
#include "sqlite3.c"

/* Включаем реализацию ICU */
#include "icu.c"
