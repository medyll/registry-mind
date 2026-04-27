package com.registry.mind.export

import com.registry.mind.data.EnrichedEntry

interface ExportConnector {
    val id: String
    suspend fun export(entry: EnrichedEntry): Result<Unit>
    suspend fun exportBatch(entries: List<EnrichedEntry>): List<Result<Unit>> =
        entries.map { export(it) }
}
