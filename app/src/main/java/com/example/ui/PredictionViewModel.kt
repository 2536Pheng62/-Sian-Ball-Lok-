package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserScore(
    val user: User,
    val points: Int,
    val correctWinnersCount: Int,
    val correctDrawsCount: Int,
    val hasCorrectChampion: Boolean,
    val totalPredictionsCount: Int
)

class PredictionViewModel(private val repository: PredictionRepository) : ViewModel() {

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMatches: StateFlow<List<Match>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPredictions: StateFlow<List<Prediction>> = repository.allPredictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks selected user ID for making predictions
    private val _selectedUserId = MutableStateFlow<Int?>(null)
    val selectedUserId: StateFlow<Int?> = _selectedUserId.asStateFlow()

    // Calculated champion (determined by the final match winner)
    val actualChampion: StateFlow<String> = allMatches.map { matches ->
        val finalMatch = matches.find { it.id == 13 }
        if (finalMatch != null && finalMatch.isFinished()) {
            if (finalMatch.actualResult == "A_WIN") finalMatch.teamA
            else if (finalMatch.actualResult == "B_WIN") finalMatch.teamB
            else "รอดวลจุดโทษ" // In case of final ending in draw, usually a winner is determined
        } else {
            ""
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Calculate leaderboard scores
    val leaderboard: StateFlow<List<UserScore>> = combine(
        allUsers,
        allMatches,
        allPredictions,
        actualChampion
    ) { users, matches, predictions, champion ->
        users.map { user ->
            var pts = 0
            var correctWins = 0
            var correctDraws = 0
            var predictsCount = 0

            val userPredictions = predictions.filter { it.userId == user.id }

            userPredictions.forEach { pred ->
                if (pred.predictedResult != "PENDING") {
                    predictsCount++
                    val match = matches.find { it.id == pred.matchId }
                    if (match != null && match.isFinished()) {
                        if (match.actualResult == "A_WIN" && pred.predictedResult == "A_WIN") {
                            pts += 3
                            correctWins++
                        } else if (match.actualResult == "B_WIN" && pred.predictedResult == "B_WIN") {
                            pts += 3
                            correctWins++
                        } else if (match.actualResult == "DRAW" && pred.predictedResult == "DRAW") {
                            pts += 1
                            correctDraws++
                        }
                    }
                }
            }

            // Bonus for correct world cup champion (+5 points)
            val correctChamp = champion.isNotEmpty() && 
                    user.championGuess.isNotEmpty() && 
                    user.championGuess.trim() == champion.trim()
            if (correctChamp) {
                pts += 5
            }

            UserScore(
                user = user,
                points = pts,
                correctWinnersCount = correctWins,
                correctDrawsCount = correctDraws,
                hasCorrectChampion = correctChamp,
                totalPredictionsCount = predictsCount
            )
        }.sortedByDescending { it.points }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-seed matches on startup
        viewModelScope.launch {
            repository.preseedMatchesIfNeeded()
            
            // Set default user if none exists
            val users = repository.getAllUsersDirectly()
            if (users.isEmpty()) {
                val defaultUserId1 = repository.insertUser(User(name = "คนรักบอล", colorHex = "#FFC107", championGuess = "อาร์เจนตินา"))
                val defaultUserId2 = repository.insertUser(User(name = "สมชาย", colorHex = "#00E676", championGuess = "ฝรั่งเศส"))
                _selectedUserId.value = defaultUserId1.toInt()
            } else if (_selectedUserId.value == null) {
                _selectedUserId.value = users.first().id
            }
        }
    }

    fun selectUser(userId: Int) {
        _selectedUserId.value = userId
    }

    fun registerOrLoginWithGoogle(name: String, email: String, colorHex: String = "#EA4335") {
        viewModelScope.launch {
            val existing = repository.getAllUsersDirectly().find { it.googleEmail?.trim()?.lowercase() == email.trim().lowercase() }
            if (existing != null) {
                _selectedUserId.value = existing.id
            } else {
                val newId = repository.insertUser(
                    User(
                        name = name,
                        colorHex = colorHex,
                        googleEmail = email,
                        isGoogleUser = true,
                        championGuess = ""
                    )
                )
                _selectedUserId.value = newId.toInt()
            }
        }
    }

    fun createNewUser(name: String, colorHex: String, championGuess: String) {
        viewModelScope.launch {
            val newId = repository.insertUser(User(name = name, colorHex = colorHex, championGuess = championGuess))
            if (_selectedUserId.value == null) {
                _selectedUserId.value = newId.toInt()
            }
        }
    }

    fun updateUserChampion(userId: Int, champion: String) {
        viewModelScope.launch {
            val user = allUsers.value.find { it.id == userId } ?: return@launch
            repository.updateUser(user.copy(championGuess = champion))
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            repository.deleteUserById(userId)
            val updatedUsers = allUsers.value.filter { it.id != userId }
            if (updatedUsers.isNotEmpty()) {
                _selectedUserId.value = updatedUsers.first().id
            } else {
                _selectedUserId.value = null
            }
        }
    }

    fun makePrediction(userId: Int, matchId: Int, predictedResult: String) {
        viewModelScope.launch {
            repository.insertPrediction(Prediction(userId, matchId, predictedResult))
        }
    }

    fun updateMatchResult(matchId: Int, teamAScore: Int, teamBScore: Int, result: String) {
        viewModelScope.launch {
            val match = allMatches.value.find { it.id == matchId } ?: return@launch
            repository.updateMatch(
                match.copy(
                    teamAScore = teamAScore,
                    teamBScore = teamBScore,
                    actualResult = result
                )
            )
        }
    }

    fun clearMatchResult(matchId: Int) {
        viewModelScope.launch {
            val match = allMatches.value.find { it.id == matchId } ?: return@launch
            repository.updateMatch(
                match.copy(
                    teamAScore = -1,
                    teamBScore = -1,
                    actualResult = "PENDING"
                )
            )
        }
    }

    fun resetAllMatchResults() {
        viewModelScope.launch {
            repository.resetAllMatchResults()
        }
    }

    // Beautiful simulation to populate actual results randomly/realistically to show how predictions get graded
    fun simulateTournament() {
        viewModelScope.launch {
            val currentMatches = allMatches.value
            val simulated = currentMatches.map { match ->
                // Simulate weights slightly biased towards historic expectations but mostly random
                val scoreA = (0..4).random()
                val scoreB = (0..4).random()
                val actualResult = when {
                    scoreA > scoreB -> "A_WIN"
                    scoreA < scoreB -> "B_WIN"
                    else -> {
                        if (match.stage != "รอบแบ่งกลุ่ม") {
                            // Non-group stage must have a winner, so we add a penalty shootout simulation
                            if ((0..1).random() == 0) "A_WIN" else "B_WIN"
                        } else {
                            "DRAW"
                        }
                    }
                }
                match.copy(
                    teamAScore = scoreA,
                    teamBScore = scoreB,
                    actualResult = actualResult
                )
            }
            repository.updateMatches(simulated)
        }
    }
}
