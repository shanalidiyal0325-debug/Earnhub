package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE isSafe = 1 ORDER BY id DESC")
    fun getAllSafeVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET likesCount = likesCount + :delta, isLiked = :liked WHERE id = :videoId")
    suspend fun updateLikes(videoId: Int, delta: Int, liked: Boolean)

    @Query("UPDATE videos SET tipsCount = tipsCount + :amount WHERE id = :videoId")
    suspend fun addTipToVideo(videoId: Int, amount: Double)

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideosCount(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: UserEntity)
}

@Dao
interface WalletStateDao {
    @Query("SELECT * FROM wallet_state WHERE id = 1")
    fun getWalletStateFlow(): Flow<WalletStateEntity?>

    @Query("SELECT * FROM wallet_state WHERE id = 1")
    suspend fun getWalletState(): WalletStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWalletState(walletState: WalletStateEntity)

    @Query("UPDATE wallet_state SET currentUserName = :username, currentUserEmail = :email WHERE id = 1")
    suspend fun updateCurrentUser(username: String, email: String)

    @Query("UPDATE wallet_state SET pointsBalance = pointsBalance + :delta WHERE id = 1")
    suspend fun addPoints(delta: Int)

    @Query("UPDATE wallet_state SET coinsBalance = coinsBalance + :delta WHERE id = 1")
    suspend fun addCoins(delta: Double)

    @Query("UPDATE wallet_state SET creatorEarnings = creatorEarnings + :delta WHERE id = 1")
    suspend fun addCreatorEarnings(delta: Double)

    @Query("UPDATE wallet_state SET referralsCount = referralsCount + 1 WHERE id = 1")
    suspend fun incrementReferrals()

    @Query("UPDATE wallet_state SET followersCount = followersCount + :delta WHERE id = 1")
    suspend fun addFollowers(delta: Int)

    @Query("UPDATE wallet_state SET viewsCount = viewsCount + :delta WHERE id = 1")
    suspend fun addViews(delta: Int)

    @Query("UPDATE wallet_state SET isMonetized = :monetized WHERE id = 1")
    suspend fun setMonetized(monetized: Boolean)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY timestamp DESC")
    fun getCommentsForVideo(videoId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Int)
}

@Dao
interface FollowDao {
    @Query("SELECT * FROM follows")
    fun getAllFollows(): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followCreator(follow: FollowEntity)

    @Delete
    suspend fun unfollowCreator(follow: FollowEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE creatorName = :creatorName LIMIT 1)")
    fun isFollowing(creatorName: String): Flow<Boolean>
}
