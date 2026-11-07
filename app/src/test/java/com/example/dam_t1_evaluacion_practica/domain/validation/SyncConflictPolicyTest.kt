package com.example.dam_t1_evaluacion_practica.domain.validation

import com.example.dam_t1_evaluacion_practica.domain.model.SyncState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Design section 6: `SyncConflictPolicy` applies a remote snapshot only when the local
 * record is `SYNCED` and newer, and deletes locally only when `SYNCED`.
 */
class SyncConflictPolicyTest {

    @Test
    fun `shouldApplyRemote is false when local is LOCAL_ONLY`() {
        assertFalse(shouldApplyRemote(SyncState.LOCAL_ONLY, 1_700_000_000_000L, 1_700_000_000_001L))
    }

    @Test
    fun `shouldApplyRemote is false when local is PENDING`() {
        assertFalse(shouldApplyRemote(SyncState.PENDING, 1_700_000_000_000L, 1_700_000_000_001L))
    }

    @Test
    fun `shouldApplyRemote is false when local is FAILED`() {
        assertFalse(shouldApplyRemote(SyncState.FAILED, 1_700_000_000_000L, 1_700_000_000_001L))
    }

    @Test
    fun `shouldApplyRemote is false when remote is not newer`() {
        assertFalse(shouldApplyRemote(SyncState.SYNCED, 1_700_000_000_010L, 1_700_000_000_005L))
        assertFalse(shouldApplyRemote(SyncState.SYNCED, 1_700_000_000_005L, 1_700_000_000_005L))
    }

    @Test
    fun `shouldApplyRemote is true when remote is newer`() {
        assertTrue(shouldApplyRemote(SyncState.SYNCED, 1_700_000_000_005L, 1_700_000_000_010L))
    }

    @Test
    fun `shouldApplyRemote is true when local has no updatedAt but remote does`() {
        assertTrue(shouldApplyRemote(SyncState.SYNCED, null, 1_700_000_000_001L))
    }

    @Test
    fun `shouldApplyRemote is false when remote has no updatedAt`() {
        assertFalse(shouldApplyRemote(SyncState.SYNCED, 1_700_000_000_001L, null))
    }

    @Test
    fun `shouldDeleteOnRemoved is true only when local is SYNCED`() {
        assertTrue(shouldDeleteOnRemoved(SyncState.SYNCED))
        assertFalse(shouldDeleteOnRemoved(SyncState.LOCAL_ONLY))
        assertFalse(shouldDeleteOnRemoved(SyncState.PENDING))
        assertFalse(shouldDeleteOnRemoved(SyncState.FAILED))
    }
}

/**
 * Conflict rule for cloud mirroring (design section 6).
 *
 * A remote snapshot is applied locally **only if** the local record is `SYNCED` and the
 * remote one is newer. A local record in `LOCAL_ONLY`, `PENDING` or `FAILED` ignores
 * remote snapshots entirely — the dirty local copy wins until its push is acknowledged.
 * A remote `REMOVED` deletes locally only when the local record is `SYNCED`.
 */
fun shouldApplyRemote(
    localSyncState: SyncState,
    localUpdatedAt: Long?,
    remoteUpdatedAt: Long?
): Boolean {
    if (localSyncState != SyncState.SYNCED) return false
    if (remoteUpdatedAt == null) return false
    if (localUpdatedAt == null) return true
    return remoteUpdatedAt > localUpdatedAt
}

fun shouldDeleteOnRemoved(localSyncState: SyncState): Boolean =
    localSyncState == SyncState.SYNCED
