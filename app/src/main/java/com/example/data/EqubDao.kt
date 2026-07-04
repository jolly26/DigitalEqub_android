package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EqubDao {
    // Equb Group Queries
    @Query("SELECT * FROM equb_groups WHERE id = 1 LIMIT 1")
    fun getEqubGroupFlow(): Flow<EqubGroup?>

    @Query("SELECT * FROM equb_groups WHERE id = 1 LIMIT 1")
    suspend fun getEqubGroup(): EqubGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEqubGroup(equbGroup: EqubGroup)

    @Query("UPDATE equb_groups SET roleSetting = :role WHERE id = 1")
    suspend fun updateRole(role: String)

    @Query("UPDATE equb_groups SET currentCycleIndex = :cycleIndex WHERE id = 1")
    suspend fun updateCurrentCycle(cycleIndex: Int)

    @Query("UPDATE equb_groups SET currentRound = :round, currentCycleIndex = :cycleIndex WHERE id = 1")
    suspend fun updateRoundAndCycle(round: Int, cycleIndex: Int)

    // Member Queries
    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembersFlow(): Flow<List<Member>>

    @Query("SELECT * FROM members ORDER BY name ASC")
    suspend fun getAllMembers(): List<Member>

    @Query("SELECT * FROM members WHERE id = :memberId LIMIT 1")
    suspend fun getMemberById(memberId: Long): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMember(memberId: Long)

    @Query("UPDATE members SET payoutRound = :round, payoutCycleIndex = :cycleIndex, payoutDate = :date WHERE id = :memberId")
    suspend fun setMemberPayout(memberId: Long, round: Int?, cycleIndex: Int?, date: String?)

    // Installment Queries
    @Query("SELECT * FROM installments ORDER BY id DESC")
    fun getAllInstallmentsFlow(): Flow<List<Installment>>

    @Query("SELECT * FROM installments ORDER BY id DESC")
    suspend fun getAllInstallments(): List<Installment>

    @Query("SELECT * FROM installments WHERE memberId = :memberId AND round = :round AND cycleIndex = :cycleIndex")
    fun getInstallmentsForMemberFlow(memberId: Long, round: Int, cycleIndex: Int): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE memberId = :memberId AND round = :round AND cycleIndex = :cycleIndex")
    suspend fun getInstallmentsForMember(memberId: Long, round: Int, cycleIndex: Int): List<Installment>

    @Query("SELECT * FROM installments WHERE round = :round AND cycleIndex = :cycleIndex")
    fun getInstallmentsForCycleFlow(round: Int, cycleIndex: Int): Flow<List<Installment>>

    @Query("SELECT * FROM installments WHERE round = :round AND cycleIndex = :cycleIndex")
    suspend fun getInstallmentsForCycle(round: Int, cycleIndex: Int): List<Installment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: Installment): Long

    @Update
    suspend fun updateInstallment(installment: Installment)

    @Query("DELETE FROM installments WHERE id = :installmentId")
    suspend fun deleteInstallment(installmentId: Long)

    @Query("UPDATE installments SET isVerified = :isVerified WHERE id = :id")
    suspend fun updateInstallmentVerification(id: Long, isVerified: Boolean)

    // Audit Log Queries
    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    suspend fun getAllAuditLogs(): List<AuditLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long

    // Bulk / Sync support (Used during database restore / shared imports)
    @Transaction
    suspend fun clearAllData() {
        // Query-based cleanups
        // Note: RawQuery or deleting directly
    }

    @Query("DELETE FROM equb_groups")
    suspend fun clearEqubGroups()

    @Query("DELETE FROM members")
    suspend fun clearMembers()

    @Query("DELETE FROM installments")
    suspend fun clearInstallments()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()

    @Transaction
    suspend fun nukeDatabase() {
        clearEqubGroups()
        clearMembers()
        clearInstallments()
        clearAuditLogs()
    }
}
