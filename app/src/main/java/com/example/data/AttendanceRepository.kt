package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AttendanceRecord
import com.example.model.Member
import com.example.model.SchoolSchedule
import com.example.server.LocalWebServer
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AttendanceRepository(
    private val db: FirebaseFirestore,
    val context: Context
) : LocalWebServer.DataProvider {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id)),
        context
    )

    private val auth = Firebase.auth
    private val scope = CoroutineScope(Dispatchers.IO)

    // Local in-memory caches for zero-latency camera matching & web server
    private val cachedMembers = mutableListOf<Member>()
    private val cachedAttendances = mutableListOf<AttendanceRecord>()
    private var cachedSchedule = SchoolSchedule(schoolName = "SMP Negeri 1")

    fun getUserId(): String? = auth.currentUser?.uid

    // LocalWebServer.DataProvider implementations
    override fun getSchoolSchedule(): SchoolSchedule = cachedSchedule
    override fun getMembers(): List<Member> = cachedMembers.toList()
    override fun getTodayAttendances(): List<AttendanceRecord> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return cachedAttendances.filter { it.date == todayStr }.sortedByDescending { it.time }
    }
    override fun getAllAttendances(): List<AttendanceRecord> {
        return cachedAttendances.sortedByDescending { it.date + it.time }
    }

    override fun onAddMemberFromWeb(member: Member) {
        val uid = getUserId() ?: return
        scope.launch {
            try {
                addMember(uid, member)
            } catch (e: Exception) {
                Log.e("AttendanceRepo", "Error adding member from web", e)
            }
        }
    }

    override fun onAddAttendanceFromWeb(record: AttendanceRecord) {
        val uid = getUserId() ?: return
        scope.launch {
            try {
                val docId = if (record.id.isNotBlank()) record.id else UUID.randomUUID().toString()
                val payload = mapOf(
                    "id" to docId,
                    "userId" to uid,
                    "memberId" to record.memberId,
                    "memberName" to record.memberName,
                    "memberRole" to record.memberRole,
                    "departmentOrClass" to record.departmentOrClass,
                    "date" to record.date,
                    "time" to record.time,
                    "type" to record.type,
                    "status" to record.status,
                    "verificationMethod" to record.verificationMethod,
                    "confidence" to record.confidence,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("attendances").document(docId).set(payload).await()
                cachedAttendances.add(0, record.copy(id = docId, userId = uid))
            } catch (e: Exception) {
                Log.e("AttendanceRepo", "Error recording attendance from web", e)
            }
        }
    }

    override fun onUpdateScheduleFromWeb(schedule: SchoolSchedule) {
        val uid = getUserId() ?: return
        scope.launch {
            try {
                saveSchedule(uid, schedule)
            } catch (e: Exception) {
                Log.e("AttendanceRepo", "Error saving schedule from web", e)
            }
        }
    }

    fun observeMembers(userId: String): Flow<List<Member>> = flow {
        val path = "users/$userId/members"
        val query = db.collection("users").document(userId).collection("members")
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Member::class.java)?.let { m ->
                                m.copy(id = m.id.ifBlank { doc.id })
                            }
                        } catch (e: Exception) {
                            Log.w("AttendanceRepo", "Error deserializing member ${doc.id}", e)
                            null
                        }
                    }
                    cachedMembers.clear()
                    cachedMembers.addAll(list)
                    list
                }
                .catch { e ->
                    Log.e("AttendanceRepo", "Error observing members at $path", e)
                    emit(cachedMembers.toList())
                }
        )
    }

    suspend fun addMember(userId: String, member: Member): String {
        val membersRef = db.collection("users").document(userId).collection("members")
        val docId = if (member.id.isNotBlank()) member.id else UUID.randomUUID().toString()
        val toSave = member.copy(
            id = docId,
            userId = userId,
            createdAt = null,
            updatedAt = null
        )

        val payload = mapOf(
            "id" to docId,
            "userId" to userId,
            "name" to toSave.name,
            "role" to toSave.role,
            "identifier" to toSave.identifier,
            "departmentOrClass" to toSave.departmentOrClass,
            "faceFeatureData" to toSave.faceFeatureData,
            "photoUri" to toSave.photoUri,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        membersRef.document(docId).set(payload).await()
        cachedMembers.removeAll { it.id == docId }
        cachedMembers.add(toSave)
        return docId
    }

    suspend fun deleteMember(userId: String, memberId: String) {
        db.collection("users").document(userId).collection("members").document(memberId).delete().await()
        cachedMembers.removeAll { it.id == memberId }
    }

    fun observeAttendances(userId: String): Flow<List<AttendanceRecord>> = flow {
        val path = "users/$userId/attendances"
        val query = db.collection("users").document(userId).collection("attendances")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(AttendanceRecord::class.java)?.let { a ->
                                a.copy(id = a.id.ifBlank { doc.id })
                            }
                        } catch (e: Exception) {
                            Log.w("AttendanceRepo", "Error deserializing attendance ${doc.id}", e)
                            null
                        }
                    }
                    cachedAttendances.clear()
                    cachedAttendances.addAll(list)
                    list
                }
                .catch { e ->
                    Log.e("AttendanceRepo", "Error observing attendances at $path", e)
                    emit(cachedAttendances.toList())
                }
        )
    }

    suspend fun recordAttendance(
        userId: String,
        member: Member,
        type: String = "CHECK_IN",
        verificationMethod: String = "FACE_RECOGNITION",
        confidence: Double = 0.96
    ): AttendanceRecord {
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

        // Check on-time vs late
        val timeSimple = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val isLate = if (type == "CHECK_IN") {
            timeSimple > cachedSchedule.checkInDeadline
        } else {
            false
        }
        val status = if (isLate) "LATE" else "ON_TIME"

        val docId = UUID.randomUUID().toString()
        val record = AttendanceRecord(
            id = docId,
            userId = userId,
            memberId = member.id,
            memberName = member.name,
            memberRole = member.role,
            departmentOrClass = member.departmentOrClass,
            date = dateStr,
            time = timeStr,
            type = type,
            status = status,
            verificationMethod = verificationMethod,
            confidence = confidence
        )

        val payload = mapOf(
            "id" to docId,
            "userId" to userId,
            "memberId" to member.id,
            "memberName" to member.name,
            "memberRole" to member.role,
            "departmentOrClass" to member.departmentOrClass,
            "date" to dateStr,
            "time" to timeStr,
            "type" to type,
            "status" to status,
            "verificationMethod" to verificationMethod,
            "confidence" to confidence,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId).collection("attendances").document(docId).set(payload).await()
        cachedAttendances.add(0, record)
        return record
    }

    fun observeSchedule(userId: String): Flow<SchoolSchedule> = flow {
        val docRef = db.collection("users").document(userId).collection("settings").document("schedule")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    val schedule = snapshot.toObject(SchoolSchedule::class.java) ?: SchoolSchedule(
                        userId = userId,
                        schoolName = "SMP Negeri 1"
                    )
                    cachedSchedule = schedule
                    schedule
                }
                .catch {
                    emit(cachedSchedule)
                }
        )
    }

    suspend fun saveSchedule(userId: String, schedule: SchoolSchedule) {
        val docRef = db.collection("users").document(userId).collection("settings").document("schedule")
        val payload = mapOf(
            "userId" to userId,
            "schoolName" to schedule.schoolName,
            "checkInStart" to schedule.checkInStart,
            "checkInDeadline" to schedule.checkInDeadline,
            "checkOutStart" to schedule.checkOutStart,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        cachedSchedule = schedule
    }

    /**
     * Mempersiapkan contoh data master jika pengguna belum memiliki anggota
     */
    fun seedSampleDataIfEmpty(userId: String) {
        scope.launch {
            try {
                val snapshot = db.collection("users").document(userId).collection("members").limit(1).get().await()
                if (snapshot.isEmpty) {
                    val samples = listOf(
                        Member(name = "Drs. H. Mulyadi, M.Pd", role = "TEACHER", identifier = "197205141998031002", departmentOrClass = "Kepala Sekolah / Guru Fisika"),
                        Member(name = "Siti Rahmawati, S.Pd", role = "TEACHER", identifier = "198502122009022005", departmentOrClass = "Guru Bahasa Indonesia"),
                        Member(name = "Bambang Wijaya, S.Kom", role = "STAFF", identifier = "199104082015041001", departmentOrClass = "Kepala Tata Usaha & IT"),
                        Member(name = "Ahmad Fadhil Prasetyo", role = "STUDENT", identifier = "NIS-24001", departmentOrClass = "Kelas 7A"),
                        Member(name = "Nabila Putri Cantika", role = "STUDENT", identifier = "NIS-24002", departmentOrClass = "Kelas 7A"),
                        Member(name = "Rizky Dwi Mahendra", role = "STUDENT", identifier = "NIS-24003", departmentOrClass = "Kelas 7B"),
                        Member(name = "Dewi Anjani", role = "STUDENT", identifier = "NIS-24004", departmentOrClass = "Kelas 8A"),
                        Member(name = "Faris Al-Ghifari", role = "STUDENT", identifier = "NIS-24005", departmentOrClass = "Kelas 9B")
                    )

                    for (m in samples) {
                        addMember(userId, m)
                    }

                    // Tambahkan schedule default
                    saveSchedule(userId, SchoolSchedule(userId = userId, schoolName = "SMP Negeri 1 Cerdas"))
                }
            } catch (e: Exception) {
                Log.w("AttendanceRepo", "Error seeding sample data", e)
            }
        }
    }
}
