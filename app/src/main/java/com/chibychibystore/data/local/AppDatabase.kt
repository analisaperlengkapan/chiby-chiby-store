package com.chibychibystore.data.local

import com.chibychibystore.data.local.database.ChibyChibyDatabase

// Backwards-compatible alias for tests and older code that referenced AppDatabase
// Kept intentionally simple as a lightweight adapter.
typealias AppDatabase = ChibyChibyDatabase
