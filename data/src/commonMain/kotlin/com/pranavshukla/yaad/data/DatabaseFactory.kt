package com.pranavshukla.yaad.data

/**
 * The database file. Each platform has a `DatabaseFactory` that opens it, creating or migrating it to the current
 * schema with foreign keys enforced; the app creates its platform's one and shares only the `SqlDriver`.
 */
internal const val DatabaseName = "yaad.db"
