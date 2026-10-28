package com.pranavshukla.yaad.data

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the current schema and foreign keys on, as on a device. */
expect fun testDriver(): SqlDriver
