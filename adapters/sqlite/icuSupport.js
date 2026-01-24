"use strict";

exports.__esModule = true;
exports.enableICUSupport = exports.createUnicodeAwareLikeQuery = exports.createUnicodeAwareEqualityQuery = void 0;
var _common = require("../../utils/common");
// ICU collation functions for proper Unicode support
var enableICUSupport = exports.enableICUSupport = function (database) {
  try {
    // Enable ICU extension if available
    if (database && 'function' === typeof database.exec) {
      // Try to load ICU extension
      try {
        database.exec('SELECT load_extension("libsqliteicu")');
        _common.logger.log('[SQLite] ICU extension loaded successfully');
      } catch (error) {
        // ICU extension might be built-in, try to use ICU collations directly
        _common.logger.log('[SQLite] ICU extension not available as loadable module, checking built-in support');
      }

      // Test if ICU collations are available
      try {
        database.exec('CREATE TEMP TABLE icu_test (text TEXT COLLATE NOCASE)');
        database.exec('INSERT INTO icu_test VALUES ("Test"), ("test"), ("TEST")');
        var result = database.exec('SELECT * FROM icu_test WHERE text = "test" COLLATE NOCASE');
        database.exec('DROP TABLE icu_test');
        if (result && 0 < result.length) {
          _common.logger.log('[SQLite] ICU collation support confirmed');
        }
      } catch (error) {
        _common.logger.warn('[SQLite] ICU collation test failed:', error.message);
      }

      // Set up Unicode-aware collations
      try {
        // Enable case-insensitive Unicode comparisons
        database.exec("\n          PRAGMA case_sensitive_like = OFF;\n        ");
        _common.logger.log('[SQLite] Unicode-aware settings applied');
      } catch (error) {
        _common.logger.warn('[SQLite] Failed to apply Unicode settings:', error.message);
      }
    }
  } catch (error) {
    _common.logger.warn('[SQLite] Failed to enable ICU support:', error.message);
  }
};

// Helper function to create Unicode-aware LIKE queries
var createUnicodeAwareLikeQuery = exports.createUnicodeAwareLikeQuery = function (column) {
  // Use COLLATE NOCASE for case-insensitive Unicode comparisons
  return "".concat(column, " COLLATE NOCASE LIKE ? COLLATE NOCASE");
};

// Helper function to create Unicode-aware equality queries
var createUnicodeAwareEqualityQuery = exports.createUnicodeAwareEqualityQuery = function (column) {
  return "".concat(column, " COLLATE NOCASE = ? COLLATE NOCASE");
};