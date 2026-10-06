package com.example.dam_t1_evaluacion_practica.data.repository

import com.example.dam_t1_evaluacion_practica.data.local.dao.IncidenceDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.InspectionDao
import com.example.dam_t1_evaluacion_practica.data.local.dao.UserDao
import com.example.dam_t1_evaluacion_practica.data.mapper.toDomainModel
import com.example.dam_t1_evaluacion_practica.data.mapper.toEntity
import com.example.dam_t1_evaluacion_practica.domain.model.Incidence
import com.example.dam_t1_evaluacion_practica.domain.model.InspectionSessionRecord
import com.example.dam_t1_evaluacion_practica.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class InspectionRepository(
    private val inspectionDao: InspectionDao,
    private val incidenceDao: IncidenceDao,
    private val userDao: UserDao
) {

    fun getAllInspections(): Flow<List<InspectionSessionRecord>> {
        return inspectionDao.getAllInspections().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getInspectionsByUser(userEmail: String): Flow<List<InspectionSessionRecord>> {
        return inspectionDao.getInspectionsByUser(userEmail).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getInspectionByFolio(folio: String): Flow<InspectionSessionRecord?> {
        return inspectionDao.getInspectionByFolio(folio).map { entity ->
            entity?.toDomainModel()
        }
    }

    suspend fun saveInspection(record: InspectionSessionRecord): Long = withContext(Dispatchers.IO) {
        inspectionDao.insertInspection(record.toEntity())
    }

    suspend fun updateInspection(record: InspectionSessionRecord) = withContext(Dispatchers.IO) {
        inspectionDao.updateInspection(record.toEntity())
    }

    fun getAllIncidences(): Flow<List<Incidence>> {
        return incidenceDao.getAllIncidences().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun saveIncidence(incidence: Incidence): Long = withContext(Dispatchers.IO) {
        incidenceDao.insertIncidence(incidence.toEntity())
    }

    suspend fun getUserByEmail(email: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)?.toDomainModel()
    }

    suspend fun login(email: String, password: String): User? = withContext(Dispatchers.IO) {
        userDao.login(email, password)?.toDomainModel()
    }

    suspend fun registerUser(user: User, password: String): Long = withContext(Dispatchers.IO) {
        userDao.insertUser(user.toEntity(password))
    }
}
