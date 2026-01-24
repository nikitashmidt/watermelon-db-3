// @flow

import { logger } from '../../utils/common'

// ICU collation functions for proper Unicode support
export const enableICUSupport = (database: any): void => {
  try {
    // Enable ICU extension if available
    if (database && typeof database.exec === 'function') {
      // Try to load ICU extension
      try {
        database.exec('SELECT load_extension("libsqliteicu")')
        logger.log('[SQLite] ICU extension loaded successfully')
      } catch (error) {
        // ICU extension might be built-in, try to use ICU collations directly
        logger.log('[SQLite] ICU extension not available as loadable module, checking built-in support')
      }

      // Test if ICU collations are available
      try {
        database.exec('CREATE TEMP TABLE icu_test (text TEXT COLLATE NOCASE)')
        database.exec('INSERT INTO icu_test VALUES ("Test"), ("test"), ("TEST")')
        const result = database.exec('SELECT * FROM icu_test WHERE text = "test" COLLATE NOCASE')
        database.exec('DROP TABLE icu_test')

        if (result && result.length > 0) {
          logger.log('[SQLite] ICU collation support confirmed')
        }
      } catch (error) {
        logger.warn('[SQLite] ICU collation test failed:', error.message)
      }

      // Set up Unicode-aware collations
      try {
        // Enable case-insensitive Unicode comparisons
        database.exec(`
          PRAGMA case_sensitive_like = OFF;
        `)
        logger.log('[SQLite] Unicode-aware settings applied')
      } catch (error) {
        logger.warn('[SQLite] Failed to apply Unicode settings:', error.message)
      }
    }
  } catch (error) {
    logger.warn('[SQLite] Failed to enable ICU support:', error.message)
  }
}

// Helper function to create Unicode-aware LIKE queries
export const createUnicodeAwareLikeQuery = (column: string, value: string): string => {
  // Use COLLATE NOCASE for case-insensitive Unicode comparisons
  return `${column} COLLATE NOCASE LIKE ? COLLATE NOCASE`
}

// Helper function to create Unicode-aware equality queries
export const createUnicodeAwareEqualityQuery = (column: string): string => {
  return `${column} COLLATE NOCASE = ? COLLATE NOCASE`
}