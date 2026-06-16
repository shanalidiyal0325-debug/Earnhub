package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val creatorName: String,
    val title: String,
    val description: String,
    val type: String, // e.g. "SUNSET_COAST", "NEON_CYBERPUNK", "ABSTRACT_ZEN", "COZY_RAIN", "MATRIX_GRID", "AURORA", "OCEAN"
    val isSafe: Boolean = true,
    val moderationReport: String = "AI Verified Safe",
    val likesCount: Int = 120,
    val tipsCount: Double = 0.0,
    val isLiked: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "WATCH_REWARD", "TIP_SENT", "TIP_RECEIVED", "REFERRAL_REWARD", "INITIAL"
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val email: String,
    val passwordHash: String,
    val registrationTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: Int,
    val username: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val creatorName: String
)

@Entity(tableName = "wallet_state")
data class WalletStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentUserName: String = "", // Empty means not logged in
    val currentUserEmail: String = "",
    val pointsBalance: Int = 0,
    val coinsBalance: Double = 500.0, // Initial coins
    val creatorEarnings: Double = 0.0, // Earned from tipping
    val referralCode: String = "TOKREF88",
    val referralsCount: Int = 0,
    val followersCount: Int = 0,
    val viewsCount: Int = 0,
    val isMonetized: Boolean = false
)
