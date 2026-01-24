cmake_minimum_required(VERSION 3.18.1)

project("watermelondb-icu")

# Добавляем поддержку ICU
find_package(PkgConfig REQUIRED)
pkg_check_modules(ICU REQUIRED icu-uc icu-i18n)

# Исходники SQLite с ICU
set(SQLITE_SOURCES
    sqlite3.c
    sqlite3_icu.c
)

# Создаем библиотеку SQLite с ICU
add_library(sqlite3-icu SHARED ${SQLITE_SOURCES})

# Настройки компиляции для ICU
target_compile_definitions(sqlite3-icu PRIVATE
    SQLITE_ENABLE_ICU=1
    SQLITE_HAS_CODEC=1
    SQLITE_TEMP_STORE=2
)

# Линкуем с ICU
target_link_libraries(sqlite3-icu ${ICU_LIBRARIES})
target_include_directories(sqlite3-icu PRIVATE ${ICU_INCLUDE_DIRS})

# Экспортируем библиотеку
set_target_properties(sqlite3-icu PROPERTIES
    ANDROID_ARM_MODE arm
    ANDROID_STL c++_shared
)