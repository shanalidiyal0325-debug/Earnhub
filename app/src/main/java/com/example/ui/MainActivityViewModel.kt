package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiModerator
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = Repository(db)

    val safeVideos: StateFlow<List<VideoEntity>> = repository.allSafeVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletState: StateFlow<WalletStateEntity?> = repository.walletState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    val selectedTab = MutableStateFlow("feed")
    val activeVideoIndex = MutableStateFlow(0)
    val isPlaying = MutableStateFlow(true)
    val watchSeconds = MutableStateFlow(0)
    val totalWatchTime = MutableStateFlow(0) // total watch time in seconds

    // Moderation workflow state
    val moderationLoading = MutableStateFlow(false)
    val moderationResult = MutableStateFlow<String?>(null)
    val moderationSuccess = MutableStateFlow<Boolean?>(null)

    // Floating points badge animation trigger
    val pointFloatMessage = MutableStateFlow<String?>(null)

    // Auth flows
    val showAuthDialog = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val registerError = MutableStateFlow<String?>(null)

    fun signUp(username: String, email: String, passwordRaw: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            registerError.value = null
            if (username.trim().length < 3) {
                registerError.value = "Username must be at least 3 characters!"
                return@launch
            }
            if (!email.contains("@") || email.length < 5) {
                registerError.value = "Please enter a valid email address!"
                return@launch
            }
            if (passwordRaw.length < 4) {
                registerError.value = "Password must be at least 4 characters!"
                return@launch
            }
            
            val success = repository.registerNewUser(username, email, passwordRaw)
            if (success) {
                showPointFloat("Awesome, account @$username created!")
                onSuccess()
            } else {
                registerError.value = "Username or Email already exists!"
            }
        }
    }

    fun signIn(usernameOrEmail: String, passwordRaw: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            loginError.value = null
            if (usernameOrEmail.trim().isEmpty() || passwordRaw.isEmpty()) {
                loginError.value = "Fields cannot be blank!"
                return@launch
            }
            val success = repository.loginUser(usernameOrEmail, passwordRaw)
            if (success) {
                showPointFloat("Welcome back!")
                onSuccess()
            } else {
                loginError.value = "Invalid Username, Email, or Password!"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logoutUser()
            showPointFloat("Logged out!")
        }
    }

    init {
        viewModelScope.launch {
            repository.seedDatabase()
            // Start Reward Watch Ticker
            startWatchTimer()
        }
    }

    private fun startWatchTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (selectedTab.value == "feed" && isPlaying.value && safeVideos.value.isNotEmpty()) {
                    // Increment tickers
                    watchSeconds.value += 1
                    totalWatchTime.value += 1
                    
                    // Award every 15 seconds for rapid testing (15s = +25 Points)
                    if (watchSeconds.value >= 15) {
                        watchSeconds.value = 0
                        val ptsGained = 25
                        repository.addPointsForWatchTime(ptsGained)
                        showPointFloat("+$ptsGained Pts")
                    }
                }
            }
        }
    }

    private fun showPointFloat(msg: String) {
        viewModelScope.launch {
            pointFloatMessage.value = msg
            delay(2000)
            pointFloatMessage.value = null
        }
    }

    fun selectTab(tab: String) {
        selectedTab.value = tab
    }

    fun togglePlay() {
        isPlaying.value = !isPlaying.value
    }

    fun changeVideo(index: Int) {
        if (index in safeVideos.value.indices) {
            activeVideoIndex.value = index
            watchSeconds.value = 0 // reset active viewer progress
        }
    }

    fun performLike(videoId: Int, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLike(videoId, currentlyLiked)
        }
    }

    fun tipVideoCreator(videoId: Int, creatorName: String, amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.tipCreator(videoId, creatorName, amount)
            if (success) {
                showPointFloat("Tipped $amount Coins!")
                onSuccess()
            } else {
                onError("Insufficient balance or error! Get points or invite referrals.")
            }
        }
    }

    fun applyReferral(refCode: String, userName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val trimmed = refCode.trim().uppercase()
            if (trimmed.length < 4) {
                onError("Please provide a valid invitation code (min 4 characters)!")
                return@launch
            }
            repository.triggerReferralSignup(trimmed, userName)
            showPointFloat("+100 Pts & +25.0 Coins!")
            onSuccess()
        }
    }

    fun publishNewVideo(
        title: String,
        description: String,
        tags: String,
        creatorName: String,
        theme: String,
        onProcessed: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            moderationLoading.value = true
            moderationResult.value = null
            moderationSuccess.value = null

            try {
                val report = GeminiModerator.moderateVideo(title, description, tags, creatorName)
                
                val output = """
                    🛡️ AI Policy Verification Report:
                    • Verdict: ${if (report.isSafe) "APPROVED (SAFE)" else "REJECTED (FLAGS DETECTED)"}
                    • Content Category: ${report.category}
                    • Confidence Score: ${(report.confidence * 100).toInt()}%
                    • Match Rules & Flags: ${if (report.policyFlags.isEmpty()) "None" else report.policyFlags.joinToString(", ")}
                    • AI Summary: ${report.explanation}
                """.trimIndent()

                moderationResult.value = output
                moderationSuccess.value = report.isSafe
                
                if (report.isSafe) {
                    repository.addCustomVideo(
                        creatorName = creatorName,
                        title = title,
                        description = description,
                        type = theme,
                        report = output,
                        isSafe = true
                    )
                    // Auto redirect to feed after tiny delay
                    delay(1500)
                    selectedTab.value = "feed"
                    activeVideoIndex.value = 0 // newest is first
                }
                onProcessed(report.isSafe, output)
            } catch (e: Exception) {
                val fallbackErr = "Moderation pipeline exception: ${e.localizedMessage}"
                moderationResult.value = fallbackErr
                moderationSuccess.value = false
                onProcessed(false, fallbackErr)
            } finally {
                moderationLoading.value = false
            }
        }
    }

    // Interactive Follow engine
    val allFollows: StateFlow<List<FollowEntity>> = repository.allFollows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleFollowCreator(creatorName: String) {
        viewModelScope.launch {
            val alreadyFollowing = allFollows.value.any { it.creatorName.lowercase() == creatorName.lowercase() }
            if (alreadyFollowing) {
                repository.unfollowCreator(creatorName)
                showPointFloat("Unfollowed @$creatorName")
            } else {
                repository.followCreator(creatorName)
                showPointFloat("Now following @$creatorName! ♥")
                repository.walletStateDao.addPoints(15) // bonus points for following
            }
        }
    }

    // Comment engine
    fun getCommentsForVideo(videoId: Int): Flow<List<CommentEntity>> {
        return repository.getCommentsForVideo(videoId)
    }

    fun submitComment(videoId: Int, text: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val state = walletState.value
            val userName = state?.currentUserName ?: ""
            if (userName.isEmpty()) {
                onFinished(false)
                return@launch
            }
            if (text.trim().isEmpty()) {
                onFinished(false)
                return@launch
            }
            repository.addComment(videoId, userName, text.trim())
            repository.walletStateDao.addPoints(10) // 10 points for engagement
            showPointFloat("Comment posted! +10 Loyalty Points")
            onFinished(true)
        }
    }

    // Creator Stats & Pakistan Rupee (PKR) Monetization engine
    fun boostFollowers() {
        viewModelScope.launch {
            repository.boostFollowers(180)
            showPointFloat("+180 New Real-time Followers! 📈")
        }
    }

    fun boostViews() {
        viewModelScope.launch {
            repository.boostViews(1250)
            showPointFloat("+1250 Dynamic Reel Views! 🚀")
        }
    }

    fun claimMonetization(onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val state = walletState.value ?: return@launch
            if (state.followersCount < 1000 && state.viewsCount < 10000) {
                onFinished(false, "Requirements not met! Try boosting followers or views first.")
                return@launch
            }
            repository.setMonetized(true)
            repository.walletStateDao.addCreatorEarnings(150.0) // initial 150 startup tip coins
            repository.walletStateDao.addPoints(500) // 500 loyal experience points
            
            repository.transactionDao.insertTransaction(
                TransactionEntity(
                    type = "INITIAL",
                    amount = 150.0,
                    details = "Creator monetization portal successfully approved! Granted 150 Coins startup bonus."
                )
            )
            showPointFloat("🎉 Monetization APPROVED! 1 Coin = 5 PKR!")
            onFinished(true, "Congratulation! Your creator account @${state.currentUserName} was approved. Monetization active.")
        }
    }

    fun processPkrCashout(
        coinsToConvert: Double, 
        mobileNetwork: String, 
        accountNumber: String, 
        onFinished: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val state = walletState.value ?: return@launch
            if (coinsToConvert <= 0) {
                onFinished(false, "Please specify a positive coin balance to convert.")
                return@launch
            }
            val currentCoins = state.coinsBalance + state.creatorEarnings
            if (coinsToConvert > currentCoins) {
                onFinished(false, "Insufficient balance! Your total Coin resources is $currentCoins Coins.")
                return@launch
            }
            // subtract coins (convert creator earnings first, then balance)
            if (state.creatorEarnings >= coinsToConvert) {
                repository.walletStateDao.addCreatorEarnings(-coinsToConvert)
            } else {
                val remaining = coinsToConvert - state.creatorEarnings
                repository.walletStateDao.addCreatorEarnings(-state.creatorEarnings)
                repository.walletStateDao.addCoins(-remaining)
            }

            val pkrRate = 5.0 // Convert 1 Coin = 5 PKR
            val pkrAmount = coinsToConvert * pkrRate

            repository.transactionDao.insertTransaction(
                TransactionEntity(
                    type = "TIP_SENT",
                    amount = -coinsToConvert,
                    details = "Converted $coinsToConvert Coins to Rs. $pkrAmount PKR. Cashout sent to $mobileNetwork Account $accountNumber."
                )
            )

            showPointFloat("Cash-out Successful! Sent to $mobileNetwork")
            onFinished(true, "Successfully cashed out $coinsToConvert Coins into Rs. $pkrAmount PKR of real earning via $mobileNetwork ($accountNumber). Funds will arrive shortly!")
        }
    }

    fun resetCreatorStats() {
        viewModelScope.launch {
            // zero out stats for demonstration
            repository.walletStateDao.insertOrUpdateWalletState(
                WalletStateEntity(
                    id = 1,
                    currentUserName = walletState.value?.currentUserName ?: "",
                    currentUserEmail = walletState.value?.currentUserEmail ?: "",
                    pointsBalance = walletState.value?.pointsBalance ?: 0,
                    coinsBalance = walletState.value?.coinsBalance ?: 500.0,
                    creatorEarnings = walletState.value?.creatorEarnings ?: 0.0,
                    referralsCount = walletState.value?.referralsCount ?: 0,
                    followersCount = 0,
                    viewsCount = 0,
                    isMonetized = false
                )
            )
            showPointFloat("Creator profile metrics reset!")
        }
    }
}
