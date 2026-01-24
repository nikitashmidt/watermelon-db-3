// @flow

/**
 * Unicode helpers for SQLite queries
 * Since SQLCipher doesn't support ICU collations, we handle Unicode on JS level
 */

/**
 * Нормализует строку для поиска (приводит к нижнему регистру)
 */
export const normalizeForSearch = (text: string): string => {
  if (!text) return text

  // Используем toLocaleLowerCase для правильной обработки кириллицы
  return text.toLocaleLowerCase('ru-RU')
}

/**
 * Создает LIKE паттерн с нормализацией
 */
export const createLikePattern = (pattern: string): string => {
  if (!pattern) return pattern

  // Нормализуем паттерн
  return normalizeForSearch(pattern)
}

/**
 * Проверяет, нужна ли Unicode обработка для значения
 */
export const needsUnicodeProcessing = (value: any): boolean => {
  if (typeof value !== 'string') return false

  // Проверяем наличие кириллицы или других Unicode символов
  return /[а-яё]/i.test(value) || /[^\x00-\x7F]/.test(value)
}

/**
 * Создает SQL выражение для Unicode-aware поиска
 */
export const createUnicodeAwareExpression = (
  column: string,
  operator: string,
  value: any
): { sql: string, processedValue: any } => {

  if (!needsUnicodeProcessing(value)) {
    // Для ASCII строк используем стандартный подход
    return {
      sql: `${column} ${operator} ?`,
      processedValue: value
    }
  }

  // Для Unicode строк используем LOWER()
  const processedValue = typeof value === 'string' ? normalizeForSearch(value) : value

  return {
    sql: `LOWER(${column}) ${operator} ?`,
    processedValue
  }
}

/**
 * Создает SQL выражение для LIKE операций с Unicode
 */
export const createUnicodeLikeExpression = (
  column: string,
  pattern: string
): { sql: string, processedPattern: string } => {

  if (!needsUnicodeProcessing(pattern)) {
    return {
      sql: `${column} LIKE ?`,
      processedPattern: pattern
    }
  }

  return {
    sql: `LOWER(${column}) LIKE ?`,
    processedPattern: createLikePattern(pattern)
  }
}

/**
 * Создает SQL выражение для INCLUDES операций с Unicode
 */
export const createUnicodeIncludesExpression = (
  column: string,
  searchText: string
): { sql: string, processedText: string } => {

  if (!needsUnicodeProcessing(searchText)) {
    return {
      sql: `instr(${column}, ?)`,
      processedText: searchText
    }
  }

  return {
    sql: `instr(LOWER(${column}), ?)`,
    processedText: normalizeForSearch(searchText)
  }
}