package com.example.data

import com.example.utils.BackupData
import kotlinx.coroutines.flow.Flow

class EqubRepository(private val dao: EqubDao) {

    val equbGroup: Flow<EqubGroup?> = dao.getEqubGroupFlow()
    val members: Flow<List<Member>> = dao.getAllMembersFlow()
    val installments: Flow<List<Installment>> = dao.getAllInstallmentsFlow()
    val auditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogsFlow()

    suspend fun getEqubGroup(): EqubGroup? = dao.getEqubGroup()
    
    suspend fun insertOrUpdateEqubGroup(equbGroup: EqubGroup) = dao.insertOrUpdateEqubGroup(equbGroup)
    
    suspend fun updateRole(role: String) = dao.updateRole(role)
    
    suspend fun updateCurrentCycle(cycleIndex: Int) = dao.updateCurrentCycle(cycleIndex)
    
    suspend fun updateRoundAndCycle(round: Int, cycleIndex: Int) = dao.updateRoundAndCycle(round, cycleIndex)

    suspend fun getAllMembers(): List<Member> = dao.getAllMembers()
    
    suspend fun getMemberById(memberId: Long): Member? = dao.getMemberById(memberId)
    
    suspend fun insertMember(member: Member): Long = dao.insertMember(member)
    
    suspend fun updateMember(member: Member) = dao.updateMember(member)
    
    suspend fun deleteMember(memberId: Long) = dao.deleteMember(memberId)
    
    suspend fun setMemberPayout(memberId: Long, round: Int?, cycleIndex: Int?, date: String?) = 
        dao.setMemberPayout(memberId, round, cycleIndex, date)

    suspend fun getAllInstallments(): List<Installment> = dao.getAllInstallments()
    
    suspend fun getInstallmentsForMember(memberId: Long, round: Int, cycleIndex: Int): List<Installment> =
        dao.getInstallmentsForMember(memberId, round, cycleIndex)

    suspend fun insertInstallment(installment: Installment): Long = dao.insertInstallment(installment)
    
    suspend fun updateInstallment(installment: Installment) = dao.updateInstallment(installment)
    
    suspend fun deleteInstallment(installmentId: Long) = dao.deleteInstallment(installmentId)
    
    suspend fun updateInstallmentVerification(id: Long, isVerified: Boolean) = 
        dao.updateInstallmentVerification(id, isVerified)

    suspend fun getAllAuditLogs(): List<AuditLog> = dao.getAllAuditLogs()
    
    suspend fun insertAuditLog(log: AuditLog): Long = dao.insertAuditLog(log)

    suspend fun nukeDatabase() = dao.nukeDatabase()

    suspend fun syncDatabase(incoming: BackupData, localRole: String) {
        dao.nukeDatabase()
        incoming.equbGroup?.let {
            dao.insertOrUpdateEqubGroup(it.copy(roleSetting = localRole))
        }
        incoming.members.forEach { dao.insertMember(it) }
        incoming.installments.forEach { dao.insertInstallment(it) }
        incoming.auditLogs.forEach { dao.insertAuditLog(it) }
    }
}
