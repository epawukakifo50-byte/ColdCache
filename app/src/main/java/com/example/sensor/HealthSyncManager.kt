package com.example.sensor

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthSyncManager"
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    fun isHealthConnectAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            Log.d(TAG, "Health Connect SDK status: $status")
            // Accept both SDK_AVAILABLE and SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
            // On many Samsung devices the status is UPDATE_REQUIRED but the client still works
            status != HealthConnectClient.SDK_UNAVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Health Connect availability check failed: ${e.message}")
            // Try anyway — if getOrCreate works, we have a client
            try {
                HealthConnectClient.getOrCreate(context)
                true
            } catch (_: Exception) { false }
        }
    }

    private fun getClient(): HealthConnectClient? {
        return try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create HealthConnectClient: ${e.message}")
            null
        }
    }

    suspend fun hasPermissions(): Boolean {
        val client = getClient() ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val hasAll = granted.containsAll(permissions)
            Log.d(TAG, "Health Connect permissions granted: $granted, hasAll: $hasAll")
            hasAll
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check permissions: ${e.message}")
            false
        }
    }

    /**
     * Reads today's total steps from Samsung Health / Health Connect.
     * Uses readRecords + sumOf (official Samsung Health codelab approach).
     */
    suspend fun getTodayStepsFromHealth(): Long? = withContext(Dispatchers.IO) {
        val client = getClient() ?: run {
            Log.w(TAG, "HealthConnectClient unavailable")
            return@withContext null
        }
        try {
            val startOfDay: Instant = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
            val now: Instant = Instant.now()

            Log.d(TAG, "Reading steps from $startOfDay to $now")

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            val totalSteps = response.records.sumOf { it.count }
            Log.d(TAG, "Total steps from Health Connect: $totalSteps (records: ${response.records.size})")
            totalSteps
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read steps from Health Connect: ${e.message}")
            null
        }
    }
}
