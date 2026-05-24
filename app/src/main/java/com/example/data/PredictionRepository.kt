package com.example.data

import kotlinx.coroutines.flow.Flow

class PredictionRepository(private val predictionDao: PredictionDao) {
    val allUsers: Flow<List<User>> = predictionDao.getAllUsers()
    val allMatches: Flow<List<Match>> = predictionDao.getAllMatches()
    val allPredictions: Flow<List<Prediction>> = predictionDao.getAllPredictions()

    suspend fun insertUser(user: User): Long = predictionDao.insertUser(user)
    suspend fun updateUser(user: User) = predictionDao.updateUser(user)
    suspend fun deleteUserById(userId: Int) {
        predictionDao.deleteUserById(userId)
        predictionDao.deletePredictionsForUser(userId)
    }

    suspend fun getAllUsersDirectly(): List<User> = predictionDao.getAllUsersDirectly()
    suspend fun getAllMatchesDirectly(): List<Match> = predictionDao.getAllMatchesDirectly()

    suspend fun updateMatch(match: Match) = predictionDao.updateMatch(match)
    suspend fun updateMatches(matches: List<Match>) = predictionDao.insertMatches(matches)
    suspend fun resetAllMatchResults() = predictionDao.resetAllMatchResults()

    suspend fun insertPrediction(prediction: Prediction) = predictionDao.insertPrediction(prediction)
    suspend fun insertPredictions(predictions: List<Prediction>) = predictionDao.insertPredictions(predictions)

    suspend fun preseedMatchesIfNeeded() {
        val currentMatches = predictionDao.getAllMatchesDirectly()
        if (currentMatches.isEmpty()) {
            val initialMatches = listOf(
                Match(1, "เม็กซิโก", "🇲🇽", "ออสเตรเลีย", "🇦🇺", "รอบแบ่งกลุ่ม (กลุ่ม A)"),
                Match(2, "แคนาดา", "🇨🇦", "ญี่ปุ่น", "🇯🇵", "รอบแบ่งกลุ่ม (กลุ่ม B)"),
                Match(3, "สหรัฐอเมริกา", "🇺🇸", "โมร็อกโก", "🇲🇦", "รอบแบ่งกลุ่ม (กลุ่ม D)"),
                Match(4, "อาร์เจนตินา", "🇦🇷", "ซาอุดีอาระเบีย", "🇸🇦", "รอบแบ่งกลุ่ม (กลุ่ม F)"),
                Match(5, "ฝรั่งเศส", "🇫🇷", "ออสเตรีย", "🇦🇹", "รอบแบ่งกลุ่ม (กลุ่ม I)"),
                Match(6, "อังกฤษ", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "เกาหลีใต้", "🇰🇷", "รอบแบ่งกลุ่ม (กลุ่ม C)"),
                Match(7, "บราซิล", "🇧🇷", "เซอร์เบีย", "🇷🇸", "รอบแบ่งกลุ่ม (กลุ่ม G)"),
                Match(8, "สเปน", "🇪🇸", "สวิตเซอร์แลนด์", "🇨🇭", "รอบแบ่งกลุ่ม (กลุ่ม K)"),
                Match(9, "แคนาดา", "🇨🇦", "อาร์เจนตินา", "🇦🇷", "รอบ 32 ทีมสุดท้าย"),
                Match(10, "สหรัฐอเมริกา", "🇺🇸", "ฝรั่งเศส", "🇫🇷", "รอบ 16 ทีมสุดท้าย"),
                Match(11, "บราซิล", "🇧🇷", "เยอรมนี", "🇩🇪", "รอบรองชนะเลิศ"),
                Match(12, "อังกฤษ", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "โปรตุเกส", "🇵🇹", "รอบรองชนะเลิศ"),
                Match(13, "อาร์เจนตินา", "🇦🇷", "ฝรั่งเศส", "🇫🇷", "รอบชิงชนะเลิศ")
            )
            predictionDao.insertMatches(initialMatches)
        }
    }
}
