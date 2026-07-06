package com.example.utils

import org.junit.Assert.*
import org.junit.Test

class JsonBackupTest {
    @Test
    fun parse_minimal_backup_json() {
        val json = """
        {
          "equb": {"id":1,"name":"Test Equb","contribution":1000,"cycleType":"Monthly","startDate":"2026-07-01","currentRound":1,"currentCycleIndex":1,"roleSetting":"CHAIRMAN"},
          "members": [{"id":1,"name":"Alice","phone":"0911000000","isActive":true}],
          "installments": [],
          "auditLogs": []
        }
        """.trimIndent()

        val parsed = JsonBackup.parseBackup(json)
        assertNotNull(parsed)
        assertEquals("Test Equb", parsed!!.equbGroup?.name)
        assertEquals(1, parsed.members.size)
    }
}
