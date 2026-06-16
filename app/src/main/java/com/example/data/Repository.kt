package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repository(private val db: AppDatabase) {
    val videoDao = db.videoDao()
    val transactionDao = db.transactionDao()
    val walletStateDao = db.walletStateDao()
    val userDao = db.userDao()
    val commentDao = db.commentDao()
    val followDao = db.followDao()

    val allSafeVideos: Flow<List<VideoEntity>> = videoDao.getAllSafeVideos()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val walletState: Flow<WalletStateEntity?> = walletStateDao.getWalletStateFlow()
    val allFollows: Flow<List<FollowEntity>> = followDao.getAllFollows()

    fun getCommentsForVideo(videoId: Int): Flow<List<CommentEntity>> = commentDao.getCommentsForVideo(videoId)

    suspend fun addComment(videoId: Int, username: String, text: String) = withContext(Dispatchers.IO) {
        commentDao.insertComment(CommentEntity(videoId = videoId, username = username, text = text))
    }

    suspend fun deleteComment(commentId: Int) = withContext(Dispatchers.IO) {
        commentDao.deleteComment(commentId)
    }

    fun isFollowing(creatorName: String): Flow<Boolean> = followDao.isFollowing(creatorName)

    suspend fun followCreator(creatorName: String) = withContext(Dispatchers.IO) {
        followDao.followCreator(FollowEntity(creatorName = creatorName))
    }

    suspend fun unfollowCreator(creatorName: String) = withContext(Dispatchers.IO) {
        followDao.unfollowCreator(FollowEntity(creatorName = creatorName))
    }

    suspend fun boostFollowers(delta: Int) = withContext(Dispatchers.IO) {
        walletStateDao.addFollowers(delta)
    }

    suspend fun boostViews(delta: Int) = withContext(Dispatchers.IO) {
        walletStateDao.addViews(delta)
    }

    suspend fun setMonetized(monetized: Boolean) = withContext(Dispatchers.IO) {
        walletStateDao.setMonetized(monetized)
    }

    suspend fun registerNewUser(username: String, email: String, passwordRaw: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedUser = username.trim().lowercase()
        val trimmedEmail = email.trim().lowercase()
        if (trimmedUser.isEmpty() || trimmedEmail.isEmpty() || passwordRaw.isEmpty()) return@withContext false
        
        val existingUser = userDao.getUserByUsername(trimmedUser) ?: userDao.getUserByEmail(trimmedEmail)
        if (existingUser != null) return@withContext false
        
        val newUser = UserEntity(
            username = trimmedUser,
            email = trimmedEmail,
            passwordHash = passwordRaw
        )
        userDao.registerUser(newUser)
        // Auto sign in right after successful registration!
        walletStateDao.updateCurrentUser(trimmedUser, trimmedEmail)
        true
    }

    suspend fun loginUser(usernameOrEmail: String, passwordRaw: String): Boolean = withContext(Dispatchers.IO) {
        val query = usernameOrEmail.trim().lowercase()
        val user = userDao.getUserByUsername(query) ?: userDao.getUserByEmail(query)
        if (user != null && user.passwordHash == passwordRaw) {
            walletStateDao.updateCurrentUser(user.username, user.email)
            transactionDao.insertTransaction(
                TransactionEntity(
                    type = "INITIAL",
                    amount = 0.0,
                    details = "User @${user.username} successfully signed into their account."
                )
            )
            return@withContext true
        }
        false
    }

    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        walletStateDao.updateCurrentUser("", "")
    }

    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val videoCount = videoDao.getVideosCount()
        if (videoCount == 0) {
            val defaultVideos = listOf(
                VideoEntity(
                    creatorName = "OceanDreamer",
                    title = "Golden Hour Coastline",
                    description = "Chasing peaceful waves as the sun sets over the Pacific. Take a deep breath and relax with this coastal visualizer.",
                    type = "SUNSET_COAST",
                    likesCount = 2450,
                    tipsCount = 15.0
                ),
                VideoEntity(
                    creatorName = "SynthRider",
                    title = "Retro Cyber Drive",
                    description = "Driving down the infinite wireframe grid under a giant magenta neon sun. Real-time synthesized visuals for retro vibes.",
                    type = "NEON_CYBERPUNK",
                    likesCount = 5890,
                    tipsCount = 42.5
                ),
                VideoEntity(
                    creatorName = "BuddhaMind",
                    title = "Enlightened Zen Lotus",
                    description = "Synchronize your breathing with these expanding geometric mandala lines. Inner tranquility is just one frame away.",
                    type = "ABSTRACT_ZEN",
                    likesCount = 1120,
                    tipsCount = 8.0
                ),
                VideoEntity(
                    creatorName = "RaindropLoFi",
                    title = "Cozy Lofi Rain Day",
                    description = "Warm lights reflecting on a rainy window sill with ambient lofi soundtrack. Perfect aesthetic for studying or sleeping.",
                    type = "COZY_RAIN",
                    likesCount = 3892,
                    tipsCount = 27.0
                ),
                VideoEntity(
                    creatorName = "NeoOne",
                    title = "Digital Sequence Grid",
                    description = "Cascading matrix green status streams falling through an engineered background. System is safe and ready to compile.",
                    type = "MATRIX_GRID",
                    likesCount = 980,
                    tipsCount = 4.5
                )
            )
            for (video in defaultVideos) {
                videoDao.insertVideo(video)
            }
            // Seed default interactive comments
            val sampleComments = listOf(
                CommentEntity(videoId = 1, username = "ali_dev", text = "This coastline looks super peaceful! PKR withdraw options are amazing!"),
                CommentEntity(videoId = 1, username = "ayesha_99", text = "Beautiful landscape vibes! Added as favorite!"),
                CommentEntity(videoId = 2, username = "zara_retrowave", text = "Cyberpunk theme is so cool! Lofi drive beat is perfect."),
                CommentEntity(videoId = 3, username = "kamran_shah", text = "Super relaxing. Cleared my daily work stress.")
            )
            for (comment in sampleComments) {
                commentDao.insertComment(comment)
            }
        }

        val wallet = walletStateDao.getWalletState()
        if (wallet == null) {
            walletStateDao.insertOrUpdateWalletState(WalletStateEntity())
            // Insert initial signup transaction
            transactionDao.insertTransaction(
                TransactionEntity(
                    type = "INITIAL",
                    amount = 500.0,
                    details = "Welcome rewards! Received free 500.0 coins startup tip balance."
                )
            )
        }
    }

    suspend fun addPointsForWatchTime(points: Int) = withContext(Dispatchers.IO) {
        walletStateDao.addPoints(points)
        transactionDao.insertTransaction(
            TransactionEntity(
                type = "WATCH_REWARD",
                amount = points.toDouble(),
                details = "Earned $points points for watching short video scrolls."
            )
        )
    }

    suspend fun tipCreator(videoId: Int, creatorName: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val wallet = walletStateDao.getWalletState() ?: return@withContext false
        if (wallet.coinsBalance >= amount) {
            // Deduct from user wallet
            walletStateDao.addCoins(-amount)
            // Save transaction
            transactionDao.insertTransaction(
                TransactionEntity(
                    type = "TIP_SENT",
                    amount = amount,
                    details = "Tipped $amount coins to creator @$creatorName"
                )
            )
            // Add to creator's earnings in database
            videoDao.addTipToVideo(videoId, amount)
            // Add to creator's global earnings inside our dashboard wallet
            walletStateDao.addCreatorEarnings(amount)
            true
        } else {
            false
        }
    }

    suspend fun triggerReferralSignup(refCode: String, newUserName: String) = withContext(Dispatchers.IO) {
        walletStateDao.incrementReferrals()
        walletStateDao.addCoins(25.0)
        walletStateDao.addPoints(100)
        
        transactionDao.insertTransaction(
            TransactionEntity(
                type = "REFERRAL_REWARD",
                amount = 25.0,
                details = "Referral code $refCode successfully redeemed by @$newUserName! You earned 25.0 Coins & 100 Points."
            )
        )
    }

    suspend fun addCustomVideo(creatorName: String, title: String, description: String, type: String, report: String, isSafe: Boolean) = withContext(Dispatchers.IO) {
        val newVideo = VideoEntity(
            creatorName = creatorName,
            title = title,
            description = description,
            type = type,
            isSafe = isSafe,
            moderationReport = report,
            likesCount = 0,
            tipsCount = 0.0
        )
        videoDao.insertVideo(newVideo)
    }

    suspend fun toggleLike(videoId: Int, currentLiked: Boolean) = withContext(Dispatchers.IO) {
        val delta = if (currentLiked) -1 else 1
        videoDao.updateLikes(videoId, delta, !currentLiked)
    }
}
