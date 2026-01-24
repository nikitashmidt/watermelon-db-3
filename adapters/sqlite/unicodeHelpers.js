"use strict";

exports.__esModule = true;
exports.normalizeForSearch = exports.needsUnicodeProcessing = exports.createUnicodeLikeExpression = exports.createUnicodeIncludesExpression = exports.createUnicodeAwareExpression = exports.createLikePattern = void 0;
/**
 * Unicode helpers for SQLite queries
 * Since SQLCipher doesn't support ICU collations, we handle Unicode on JS level
 */
/**
 * Нормализует строку для поиска (приводит к нижнему регистру)
 */
var normalizeForSearch = exports.normalizeForSearch = function (text) {
  if (!text) return text;

  // Используем toLocaleLowerCase для правильной обработки кириллицы
  return text.toLocaleLowerCase('ru-RU');
};

/**
 * Создает LIKE паттерн с нормализацией
 */
var createLikePattern = exports.createLikePattern = function (pattern) {
  if (!pattern) return pattern;

  // Нормализуем паттерн
  return normalizeForSearch(pattern);
};

/**
 * Проверяет, нужна ли Unicode обработка для значения
 */
var needsUnicodeProcessing = exports.needsUnicodeProcessing = function (value) {
  if ('string' !== typeof value) return false;

  // Проверяем наличие кириллицы или других Unicode символов
  return /[а-яё]/i.test(value) || /[^\x00-\x7F]/.test(value);
};

/**
 * Создает SQL выражение для Unicode-aware поиска
 */
var createUnicodeAwareExpression = exports.createUnicodeAwareExpression = function (column, operator, value) {
  if (!needsUnicodeProcessing(value)) {
    // Для ASCII строк используем стандартный подход
    return {
      sql: "".concat(column, " ").concat(operator, " ?"),
      processedValue: value
    };
  }

  // Для Unicode строк используем LOWER()
  var processedValue = 'string' === typeof value ? normalizeForSearch(value) : value;
  return {
    sql: "LOWER(".concat(column, ") ").concat(operator, " ?"),
    processedValue: processedValue
  };
};

/**
 * Создает SQL выражение для LIKE операций с Unicode
 */
var createUnicodeLikeExpression = exports.createUnicodeLikeExpression = function (column, pattern) {
  if (!needsUnicodeProcessing(pattern)) {
    return {
      sql: "".concat(column, " LIKE ?"),
      processedPattern: pattern
    };
  }
  return {
    sql: "LOWER(".concat(column, ") LIKE ?"),
    processedPattern: createLikePattern(pattern)
  };
};

/**
 * Создает SQL выражение для INCLUDES операций с Unicode
 */
var createUnicodeIncludesExpression = exports.createUnicodeIncludesExpression = function (column, searchText) {
  if (!needsUnicodeProcessing(searchText)) {
    return {
      sql: "instr(".concat(column, ", ?)"),
      processedText: searchText
    };
  }
  return {
    sql: "instr(LOWER(".concat(column, "), ?)"),
    processedText: normalizeForSearch(searchText)
  };
};