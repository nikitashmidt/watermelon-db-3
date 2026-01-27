/* sqlite3_icu.c - Обертка для компиляции SQLite с ICU */

/* Сначала определяем все типы ICU ДО включения sqlite3.c */
#define U_COMMON_IMPLEMENTATION

/* Определяем базовые типы ICU */
typedef int UBool;
typedef unsigned short UChar;
typedef uint32_t UChar32;
typedef uint8_t UErrorCode;
typedef void* UCollator;
typedef void* UConverter;
typedef void* UEnumeration;

/* Макросы экспорта */
#define U_EXPORT
#define U_EXPORT2
#define U_CAPI
#define U_CDECL_BEGIN
#define U_CDECL_END

/* Отключаем переименование функций ICU */
#define U_DISABLE_RENAMING 1

/* Включаем оригинальный sqlite3.c */
#include "sqlite3.c"
