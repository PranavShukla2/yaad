package com.pranavshukla.yaad.core.scheduling

/** What to change so the scheduled alerts match the plan. */
data class SyncPlan(val toSchedule: List<Alert>, val toCancel: Set<String>)

object AlertSync {
    /** Compares alert ids already [scheduled] on the platform with the [desired] plan. Idempotent. */
    fun diff(scheduled: Set<String>, desired: List<Alert>): SyncPlan {
        val desiredIds = desired.mapTo(HashSet()) { it.id }
        return SyncPlan(
            toSchedule = desired.filter { it.id !in scheduled },
            toCancel = scheduled - desiredIds,
        )
    }
}
