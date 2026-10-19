package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val schoolName: String = "",
    val adminName: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Member(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "STUDENT", // TEACHER, STAFF, STUDENT
    val identifier: String = "", // NIS atau NIP
    val departmentOrClass: String = "", // e.g. Kelas 7A, Guru Matematika
    val faceFeatureData: String = "", // Face embedding signature
    val photoUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val memberRole: String = "STUDENT",
    val departmentOrClass: String = "",
    val date: String = "", // YYYY-MM-DD
    val time: String = "", // HH:mm:ss
    val type: String = "CHECK_IN", // CHECK_IN, CHECK_OUT
    val status: String = "ON_TIME", // ON_TIME, LATE, EARLY_DEPARTURE, EXCUSED, SICK
    val verificationMethod: String = "FACE_RECOGNITION",
    val confidence: Double = 0.95,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SchoolSchedule(
    val userId: String = "",
    val schoolName: String = "SMP Negeri 1",
    val checkInStart: String = "06:30",
    val checkInDeadline: String = "07:15",
    val checkOutStart: String = "14:00",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
