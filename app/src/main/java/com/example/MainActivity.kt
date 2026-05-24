package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.*
import com.example.ui.PredictionViewModel
import com.example.ui.UserScore
import com.example.ui.theme.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PredictionViewModelFactory(private val repository: PredictionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PredictionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PredictionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: PredictionRepository
    private lateinit var viewModel: PredictionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room DB using standard thread-safe singleton
        database = AppDatabase.getDatabase(applicationContext)
        repository = PredictionRepository(database.predictionDao())
        
        // Instantiate ViewModel with ViewModelProvider survival
        val factory = PredictionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PredictionViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: PredictionViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val matches by viewModel.allMatches.collectAsStateWithLifecycle()
    val predictions by viewModel.allPredictions.collectAsStateWithLifecycle()
    val selectedUserId by viewModel.selectedUserId.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val actualWinnerTeam by viewModel.actualChampion.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Leaderboard, 1: Make predictions, 2: Match result dashboard
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    val activeUser = users.find { it.id == selectedUserId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StadiumDarkBlue, StadiumLightBlue)
                )
            )
    ) {
        // App Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0F19))
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Trophy icon",
                            tint = StadiumGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ถ้วยทำนายผลบอลโลก",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "คู่มือและกฎหมาย",
                                tint = StadiumGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "ทัศนะฟุตบอลโลก สรุปผลคะแนนกลุ่มเพื่อน",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                // Add Player Button
                Button(
                    onClick = { showAddUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("add_user_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add user icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "สร้างกลุ่มทาย", 
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active Player Selector (Horizontal List)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131B2E))
                .padding(vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Players Icon",
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "สลับผู้ใช้งานเพื่อกรอกการทาย:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (users.isEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ยังไม่มีผู้เล่น หรือคุณลงทะเบียนด่วนด้วยบัญชี Google ด้านล่าง",
                        color = TextGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = { showGoogleLoginDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("🔑 สมัครใช้งาน/เข้าสู่ระบบด้วย Google", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            onClick = { showGoogleLoginDialog = true },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B233A)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PitchLine),
                            modifier = Modifier
                                .widthIn(min = 120.dp)
                                .testTag("google_login_quick_button")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "เชื่อมต่อ Google",
                                    color = StadiumGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "เข้าใช้งานด่วน",
                                    color = TextGray,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    items(users) { u ->
                        val isSelected = u.id == selectedUserId
                        val userScoreObj = leaderboard.find { it.user.id == u.id }
                        val scoreDisplay = userScoreObj?.points ?: 0

                        val userColor = try {
                            Color(android.graphics.Color.parseColor(u.colorHex))
                        } catch (e: Exception) {
                            StadiumGold
                        }

                        Card(
                            onClick = { viewModel.selectUser(u.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) StadiumLightBlue else Color(0xFF1A233A)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) userColor else PitchLine
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .widthIn(min = 100.dp)
                                .testTag("select_user_card_${u.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(userColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = u.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    if (u.isGoogleUser) {
                                        Box(
                                            modifier = Modifier
                                                .size(15.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "G",
                                                color = Color(0xFF4285F4),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = u.name,
                                    color = if (isSelected) TextWhite else TextGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "points",
                                        tint = StadiumGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$scoreDisplay คะแนน",
                                        color = StadiumGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Player Target Banner
        activeUser?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(1.dp, PitchLine))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(it.colorHex))
                                } catch (e: Exception) {
                                    StadiumGold
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "กำลังกรอกพยากรณ์สำหรับ: ",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = it.name,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (it.isGoogleUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEA4335))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Google บัญชี",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Delete User button
                IconButton(
                    onClick = { viewModel.deleteUser(it.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "ลบผู้ใช้",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // M3 Navigation Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0xFF0B0F19),
            contentColor = StadiumGold,
            indicator = { tabPositions ->
                if (activeTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = StadiumGold
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Leaderboard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("สรุปผล / ตาราง", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Predictions",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ส่งพยากรณ์ผล", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Results Manager",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ผลบอลสนามจริง", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Main Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> LeaderboardTab(
                    leaderboard = leaderboard,
                    actualWinnerTeam = actualWinnerTeam,
                    viewModel = viewModel
                )
                1 -> PredictionsTab(
                    activeUser = activeUser,
                    matches = matches,
                    predictions = predictions,
                    viewModel = viewModel
                )
                2 -> MatchResultsTab(
                    matches = matches,
                    viewModel = viewModel
                )
            }
        }
    }

    // Dialog for adding a user
    if (showAddUserDialog) {
        val colors = listOf("#FFC107", "#00E676", "#00B0FF", "#E91E63", "#9C27B0", "#FF3D00")
        val champions = listOf(
            "อาร์เจนตินา 🇦🇷", "ฝรั่งเศส 🇫🇷", "บราซิล 🇧🇷", "อังกฤษ 🏴󠁧󠁢󠁥󠁮󠁧󠁿", 
            "โปรตุเกส 🇵🇹", "เยอรมนี 🇩🇪", "สเปน 🇪🇸", "เนเธอร์แลนด์ 🇳🇱", 
            "โครเอเชีย 🇭🇷", "ญี่ปุ่น 🇯🇵"
        )

        var newName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf(colors.first()) }
        var selectedChamp by remember { mutableStateOf(champions.first()) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddUserDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = StadiumLightBlue,
                border = BorderStroke(1.dp, PitchLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏁 สมัครกลุ่มทำนายผลบอลโลก",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = {
                            showAddUserDialog = false
                            showGoogleLoginDialog = true
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, PitchLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(text = "G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "o", color = Color(0xFFEA4335), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "g", color = Color(0xFF4285F4), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "l", color = Color(0xFF34A853), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(text = "e", color = Color(0xFFEA4335), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "สมัครด่วนด้วยบัญชี Google",
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "— หรือกรอกชื่อด้วยตนเองด้านล่าง —",
                        color = TextGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("ชื่อผู้ใช้งาน (เพื่อนๆ โหวต)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = StadiumGold,
                            unfocusedBorderColor = PitchLine
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_user_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Choice Row
                    Text(
                        text = "เลือกสีสัญลักษณ์ผู้เล่น:",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.forEach { c ->
                            val parsedColor = Color(android.graphics.Color.parseColor(c))
                            val isSelected = selectedColorHex == c
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) TextWhite else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = c }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Champion Prediction Box
                    Text(
                        text = "ทายผลทีมแชมป์บอลโลก (โบนัส +5 คะแนน):",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(champions) { c ->
                            val isSelected = selectedChamp == c
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) StadiumGold else Color(0xFF131B2E))
                                    .border(1.dp, PitchLine, RoundedCornerShape(12.dp))
                                    .clickable { selectedChamp = c }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = c,
                                    color = if (isSelected) Color(0xFF0F172A) else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddUserDialog = false },
                            border = BorderStroke(1.dp, PitchLine),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ยกเลิก", color = TextGray)
                        }

                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    val formattedChamp = selectedChamp.substringBefore(" ").trim()
                                    viewModel.createNewUser(
                                        name = newName.trim(),
                                        colorHex = selectedColorHex,
                                        championGuess = formattedChamp
                                    )
                                    showAddUserDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_add_user_button")
                        ) {
                            Text("สร้างตัวช่วย", color = Color(0xFF0F172A))
                        }
                    }
                }
            }
        }
    }

    // Google Sign-In Simulation Dialog
    if (showGoogleLoginDialog) {
        Dialog(onDismissRequest = { showGoogleLoginDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google logo representation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(text = "o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(text = "o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(text = "g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(text = "l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Text(text = "e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ลงทะเบียนเข้าสู่ระบบด้วย Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "เลือกบัญชีเพื่อเริ่มต้นบันทึกการทายถ้วยบอลโลก 2026 ของคุณอย่างปลอดภัย",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Card for first Google Account (pang8577@gmail.com)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                viewModel.registerOrLoginWithGoogle(
                                    name = "Pang",
                                    email = "pang8577@gmail.com",
                                    colorHex = "#EA4335"
                                )
                                showGoogleLoginDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEA4335)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "P",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Pang",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "pang8577@gmail.com",
                                        color = Color(0xFF4B5563),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account 2: Another Mock account
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                viewModel.registerOrLoginWithGoogle(
                                    name = "คุณสิริวร",
                                    email = "siriwor.w@gmail.com",
                                    colorHex = "#34A853"
                                )
                                showGoogleLoginDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34A853)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ส",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "คุณสิริวร",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "siriwor.w@gmail.com",
                                        color = Color(0xFF4B5563),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(
                        onClick = { showGoogleLoginDialog = false }
                    ) {
                        Text("ยกเลิก", color = Color(0xFF4B5563), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dialog for help, user guidelines, and anti-gambling laws
    if (showHelpDialog) {
        Dialog(onDismissRequest = { showHelpDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = StadiumLightBlue,
                border = BorderStroke(1.dp, PitchLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📖 คู่มือใช้งาน & กฎหมายการพนัน",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                        border = BorderStroke(1.dp, PitchLine)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📘 แนะนำขั้นตอนการใช้งาน",
                                color = StadiumGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. แอดรายชื่อผู้เล่น: กดปุ่ม \"สร้างกลุ่มทาย\" เพื่อเพิ่มเพื่อนที่ร่วมสนุก\n" +
                                        "2. เลือกบันทึกพยากรณ์: กดเลือกชื่อเพื่อนแถบด้านบน แล้วไปที่แท็บ \"ส่งพยากรณ์ผล\" ทำการส่งคำพยากรณ์ผล (ชนะ/เสมอ) ของแมตช์การแข่งขัน และผู้ครองแชมป์โลก\n" +
                                        "3. บันทึกสกอร์จริง/จำลอง: ไปยังแท็บ \"ผลบอลสนามจริง\" เพื่อบันทึกผลจริงในสนาม หรือคลิก สุ่มผลการแข่งจริง ระบบจะสรุปคำนวณคะแนนให้อัตโนมัติในตารางหลัก!",
                                color = TextWhite,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B22)),
                        border = BorderStroke(1.dp, Color(0xFFF44336))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "คำแจ้งเตือนทางกฎหมายและข้อบังคับ",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• พ.ร.บ. การพนัน พ.ศ. 2478: การเล่นพนันทายผลบอลออนไลน์หรือออฟไลน์เป็นสิ่งที่ผิดกฎหมายอย่างชัดเจนในประเทศไทย มีโทษจำคุกและโทษปรับอย่างรุนแรง\n" +
                                        "• แอปพลิเคชันนี้ออกแบบมาเพื่อกิจกรรมเชิงสร้างสรรค์ สร้างมิตรสัมพันธ์ในหมู่เพื่อน และความเพลิดเพลินเท่านั้น ห้ามนำข้อมูลไปใช้วางเดิมพัน แข่งขันพนันแลกรับเงินสด หรือประโยชน์ทางการเงินอันไม่สุจริตอย่างเด็ดขาด\n" +
                                        "• มาร่วมอนุรักษ์ธรรมชาติกีฬาฟุตบอล ดูกีฬาให้เต็มอิ่ม ปลอดภัย ไร้พนันบอล!",
                                color = TextWhite,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showHelpDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("เข้าใจและปลอดภัยร่วมกัน 👍", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTab(
    leaderboard: List<UserScore>,
    actualWinnerTeam: String,
    viewModel: PredictionViewModel
) {
    var isGuideExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Scoring Rules Reminder Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, PitchLine)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Rules",
                        tint = StadiumGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "กติกาการนับคะแนนพยากรณ์",
                        color = StadiumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• ทายถูกว่าฝ่ายใดจะชนะ (ทีม A หรือ ทีม B) ได้รับ 3 คะแนน\n" +
                            "• หากผลการแข่งขันออกมา เสมอ (Draw) และผู้ใช้ทายว่าเสมอเฉลยถูกต้อง จะได้รับ 1 คะแนน\n" +
                            "• โบนัสทายแชมป์โลกได้ถูกต้อง เมื่อทัวร์นาเมนต์จบ ได้รับเพิ่มเป็นพิเศษ 5 คะแนน",
                    color = TextWhite,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Leaderboard স্ট্যান্ডిંગ Table
        // Collapsible User Guide Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { isGuideExpanded = !isGuideExpanded },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            border = BorderStroke(1.dp, PitchLine)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "คู่มือการใช้งาน",
                            tint = StadiumGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "คู่มือวิธีใช้งานแอป 📖",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        imageVector = if (isGuideExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = TextGray
                    )
                }
                if (isGuideExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. สร้างกลุ่มทายผล: กดปุ่ม \"สร้างกลุ่มทาย\" ด้านบนขวา กรอกชื่อและทายธงทีมแชมป์โลกเพื่อตั้งทีมแข่งทายผลสำหรับเพื่อนๆ\n" +
                                "2. สลับผู้เล่น: กดคลิกการ์ดรายชื่อเพื่อนๆ ด้านบนสุด เพื่อทำการเลือกสลับคนกรอก\n" +
                                "3. กรอกคำทำนาย: ไปที่แท็บ \"ส่งพยากรณ์ผล\" กดเลือก ชนะ/เสมอ ได้ตามชอบใจในแมตช์ที่เปิดให้ทาย\n" +
                                "4. ใส่ผลจริงเพื่อคิดคะแนน: ในแท็บ \"ผลบอลสนามจริง\" คณะกรรมการสามารถบันทึกสกอร์จริงที่เกิดขึ้น หรือคลิก \"สุ่มผลการแข่งจริง\" เพื่อจำลองผลลัพธ์และอัปเดตคะแนนรวมของทุกคนทันที!",
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Anti-Gambling Legal Warning Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B22)),
            border = BorderStroke(1.dp, Color(0xFFF44336))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "คำเตือนกฎหมาย",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "คำแจ้งเตือนทางกฎหมาย (พ.ร.บ. การพนัน พ.ศ. 2478) ⚖️",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• ร่วมสนุกอย่างสร้างสรรค์: แอปพลิเคชันนี้เพื่อความสนุกและการพยากรณ์ผลฟรีในกลุ่มเพื่อนฝูงเท่านั้น ห้ามชวนแทงบอลเด็ดขาด!\n" +
                            "• กฎหมายสูงสุดของประเทศ: การแทงบอลออนไลน์หรือการจัดตั้งโต๊ะพนันกีฬาฟุตบอล ถือเป็นความผิดอาญาตามมาตรา 12 แห่ง พ.ร.บ. การพนัน มีโทษจำคุกสูงสุดและปรับอย่างรุนแรง\n" +
                            "• ร่วมเชียร์กีฬาฟุตบอลในทางที่ถูกต้อง ปลอดภัย ไร้พนันบอลอย่างสมบูรณ์แบบ!",
                    color = TextWhite,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Leaderboard ตารางคะแนนรวม
        Text(
            text = "👑 ตารางเทียบคะแนนรวมกลุ่มเพื่อน",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        if (leaderboard.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Text(
                    text = "ยังไม่มีข้อมูลผู้เล่น\nกรุณาสร้างกลุ่มทายเพื่อเริ่มต้นนับคะแนน!",
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                border = BorderStroke(1.dp, PitchLine)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("อันดับ", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.18f), textAlign = TextAlign.Center)
                        Text("ผู้ทำนาย", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.42f))
                        Text("สถิติ (ชนะ/เสมอ)", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.25f), textAlign = TextAlign.Center)
                        Text("คะแนน", color = TextGray, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center)
                    }

                    // User scores details
                    leaderboard.forEachIndexed { index, userScore ->
                        val rank = index + 1
                        val isFirst = index == 0
                        val scoreColor = if (isFirst) StadiumGold else TextWhite

                        val userColor = try {
                            Color(android.graphics.Color.parseColor(userScore.user.colorHex))
                        } catch (e: Exception) {
                            StadiumGold
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank cell
                            Box(
                                modifier = Modifier
                                    .weight(0.18f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFirst) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Winner",
                                        tint = StadiumGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = "#$rank",
                                        color = TextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // User Profile name & champion prediction
                            Row(
                                modifier = Modifier.weight(0.42f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(userColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userScore.user.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = userScore.user.name,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (userScore.user.championGuess.isNotEmpty()) {
                                        Text(
                                            text = "🏆 ทายแชมป์: ${userScore.user.championGuess}",
                                            color = if (userScore.hasCorrectChampion) GrassGreen else TextGray,
                                            fontSize = 10.sp,
                                            fontWeight = if (userScore.hasCorrectChampion) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            // Stats breakdown
                            Text(
                                text = "🏆${userScore.correctWinnersCount}  🤝${userScore.correctDrawsCount}",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.25f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )

                            // Point score pill
                            Box(
                                modifier = Modifier
                                    .weight(0.15f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isFirst) StadiumGold.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${userScore.points}",
                                    color = scoreColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        if (index < leaderboard.size - 1) {
                            Divider(color = PitchLine, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        }

        // Champion Determination Banner
        if (actualWinnerTeam.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GrassGreen.copy(alpha = 0.15f)),
                border = BorderStroke(2.dp, GrassGreen)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 ขอแสดงความยินดีกับผู้ชนะถ้วยรางวัลเวิลด์คัพ! 🎉",
                        color = GrassGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ทีมแชมป์บอลโลกจริง: $actualWinnerTeam",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    val bestSpecs = leaderboard.filter { it.points == leaderboard.maxOfOrNull { l -> l.points } }
                    if (bestSpecs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ผู้ที่ทำคะแนนสะสมสูงสุดคือ: ${bestSpecs.joinToString { it.user.name }} (${bestSpecs.first().points} คะแนน)",
                            color = StadiumGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Simulation Controller Cards
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, PitchLine)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎛️ จำลองความสนุกฟุตบอลโลก",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "กดปุ่มเพื่อสุ่มคะแนนจำลองผลฟุตบอลเล่นจริงทันที เพื่อตรวจสอบว่าคะแนนสะสมของแต่ละคนจะเป็นอย่างไร!",
                    color = TextGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetAllMatchResults() },
                        border = BorderStroke(1.dp, PitchLine),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_matches_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("รีเซ็ตผลจริง", color = TextGray, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.simulateTournament() },
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("สุ่มผลการแข่งจริง", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionsTab(
    activeUser: User?,
    matches: List<Match>,
    predictions: List<Prediction>,
    viewModel: PredictionViewModel
) {
    if (activeUser == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "กรุณาเลือกหรือสร้างผู้เล่นด้านบน เพื่อกรอกพยากรณ์ผลการแข่งขัน",
                color = TextGray,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
        return
    }

    // Champions pool
    val champions = listOf(
        "อาร์เจนตินา 🇦🇷", "ฝรั่งเศส 🇫🇷", "บราซิล 🇧🇷", "อังกฤษ 🏴󠁧󠁢󠁥󠁮󠁧󠁿", 
        "โปรตุเกส 🇵🇹", "เยอรมนี 🇩🇪", "สเปน 🇪🇸", "เนเธอร์แลนด์ 🇳🇱", 
        "โครเอเชีย 🇭🇷", "ญี่ปุ่น 🇯🇵"
    )

    var ddChampExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Champion Guess configuration component
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("champion_predict_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, PitchLine)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "👑 ทายชาติแชมป์บอลโลก",
                        color = StadiumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "หากทีมนี้ได้แชมป์รับเพิ่ม +5 คะแนน",
                        color = TextGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(champions) { c ->
                            val teamNoFlag = c.substringBefore(" ").trim()
                            val isSelected = activeUser.championGuess == teamNoFlag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) StadiumGold else Color(0xFF131B2E))
                                    .border(1.dp, PitchLine, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateUserChampion(activeUser.id, teamNoFlag) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = c,
                                    color = if (isSelected) Color(0xFF0F172A) else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "⚽ รายชื่อแมตช์ที่ให้ร่วมทำนาย",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Match cards predictions
        items(matches) { match ->
            val prediction = predictions.find { it.userId == activeUser.id && it.matchId == match.id }
            val predictionResult = prediction?.predictedResult ?: "PENDING"

            Card(
                modifier = Modifier.fillMaxWidth()
                    .testTag("predict_card_match_${match.id}"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                border = BorderStroke(1.dp, PitchLine)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Match Stage Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = match.stage,
                                color = StadiumGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Display result indicator
                        if (match.isFinished()) {
                            val isCorrect = when {
                                match.actualResult == "A_WIN" && predictionResult == "A_WIN" -> true
                                match.actualResult == "B_WIN" && predictionResult == "B_WIN" -> true
                                match.actualResult == "DRAW" && predictionResult == "DRAW" -> true
                                else -> false
                            }

                            val scoreReward = if (isCorrect) {
                                if (match.actualResult == "DRAW") 1 else 3
                            } else 0

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCorrect) GrassGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isCorrect) "ทายถูก (+$scoreReward คะแนน)" else "ทายผิด (+0 คะแนน)",
                                    color = if (isCorrect) GrassGreen else Color.Red,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Gray.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "รอแข่งขันจริง",
                                    color = TextGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Match Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team A
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = match.teamA,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = match.teamAFlag, fontSize = 22.sp)
                        }

                        // VS block or actual scores
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (match.isFinished()) {
                                Text(
                                    text = " ${match.teamAScore} - ${match.teamBScore} ",
                                    color = StadiumGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    text = " VS ",
                                    color = TextGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Team B
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(text = match.teamBFlag, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = match.teamB,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3-choice Selection Segmented Row
                    Text(
                        text = "เลือกผลทำนายของคุณ:",
                        color = TextGray,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Team A Win choice
                        ChoiceButton(
                            text = "${match.teamAFlag} ชนะ",
                            isSelected = predictionResult == "A_WIN",
                            onClick = { viewModel.makePrediction(activeUser.id, match.id, "A_WIN") },
                            modifier = Modifier.weight(1f).testTag("choice_A_match_${match.id}")
                        )

                        // Draw choice
                        ChoiceButton(
                            text = "🤝 เสมอ",
                            isSelected = predictionResult == "DRAW",
                            onClick = { viewModel.makePrediction(activeUser.id, match.id, "DRAW") },
                            modifier = Modifier.weight(0.9f).testTag("choice_DRAW_match_${match.id}")
                        )

                        // Team B Win choice
                        ChoiceButton(
                            text = "${match.teamBFlag} ชนะ",
                            isSelected = predictionResult == "B_WIN",
                            onClick = { viewModel.makePrediction(activeUser.id, match.id, "B_WIN") },
                            modifier = Modifier.weight(1f).testTag("choice_B_match_${match.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) StadiumGold.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) StadiumGold else TextGray
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) StadiumGold else PitchLine
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MatchResultsTab(
    matches: List<Match>,
    viewModel: PredictionViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, PitchLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📣 แผงควบคุมผู้ตัดสินฟุตบอลโลก",
                        color = StadiumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ส่วนนี้มีไว้จำลองการบันทึกสกอร์การแข่งขั้นจริงในสนามจริงเมื่อผลบอลจบการแข่งขันแล้ว เมื่อท่านคลิกเลือกผลชนะที่นี่ ระบบจะจัดเกรดและให้คะแนนพยากรณ์รวมทุกๆ คนในกลุ่มทันที!",
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        items(matches) { match ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("result_card_match_${match.id}"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                border = BorderStroke(1.dp, PitchLine)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = match.stage,
                                color = StadiumGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (match.isFinished()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GrassGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "บันทึกผลแล้ว",
                                        color = GrassGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear result",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.clearMatchResult(match.id) }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Gray.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "รอรายงานผลการแข่งจริง",
                                    color = TextGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Teams title matchup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team A
                        Text(text = match.teamAFlag, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = match.teamA,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // Scores Setup Input Areas
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val scoreA = if (match.teamAScore >= 0) match.teamAScore else 0
                            val scoreB = if (match.teamBScore >= 0) match.teamBScore else 0

                            ScoreSelectorButton(text = "-", onClick = {
                                if (scoreA > 0) {
                                    val newRes = getResultString(scoreA - 1, scoreB, match.stage)
                                    viewModel.updateMatchResult(match.id, scoreA - 1, scoreB, newRes)
                                }
                            })

                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "$scoreA", color = StadiumGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            ScoreSelectorButton(text = "+", onClick = {
                                val newRes = getResultString(scoreA + 1, scoreB, match.stage)
                                viewModel.updateMatchResult(match.id, scoreA + 1, scoreB, newRes)
                            })

                            Text(" : ", color = TextWhite, fontWeight = FontWeight.Black)

                            ScoreSelectorButton(text = "-", onClick = {
                                if (scoreB > 0) {
                                    val newRes = getResultString(scoreA, scoreB - 1, match.stage)
                                    viewModel.updateMatchResult(match.id, scoreA, scoreB - 1, newRes)
                                }
                            })

                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "$scoreB", color = StadiumGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            ScoreSelectorButton(text = "+", onClick = {
                                val newRes = getResultString(scoreA, scoreB + 1, match.stage)
                                viewModel.updateMatchResult(match.id, scoreA, scoreB + 1, newRes)
                            })
                        }

                        // Team B
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = match.teamB,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = match.teamBFlag, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Shortcut buttons to quickly mark winner manually
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.updateMatchResult(match.id, 2, 0, "A_WIN") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (match.actualResult == "A_WIN") GrassGreen.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (match.actualResult == "A_WIN") GrassGreen else PitchLine),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "${match.teamA} ชนะ (2-0)", fontSize = 10.sp, color = if (match.actualResult == "A_WIN") GrassGreen else TextGray)
                        }

                        OutlinedButton(
                            onClick = { viewModel.updateMatchResult(match.id, 1, 1, "DRAW") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (match.actualResult == "DRAW") GrassGreen.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (match.actualResult == "DRAW") GrassGreen else PitchLine),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text(text = "เสมอ (1-1)", fontSize = 11.sp, color = if (match.actualResult == "DRAW") GrassGreen else TextGray)
                        }

                        OutlinedButton(
                            onClick = { viewModel.updateMatchResult(match.id, 0, 2, "B_WIN") },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (match.actualResult == "B_WIN") GrassGreen.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (match.actualResult == "B_WIN") GrassGreen else PitchLine),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "${match.teamB} ชนะ (0-2)", fontSize = 10.sp, color = if (match.actualResult == "B_WIN") GrassGreen else TextGray)
                        }
                    }
                }
            }
        }
    }
}

private fun getResultString(scoreA: Int, scoreB: Int, stage: String): String {
    return when {
        scoreA > scoreB -> "A_WIN"
        scoreA < scoreB -> "B_WIN"
        else -> {
            if (stage != "รอบแบ่งกลุ่ม") {
                // If it's a knockout stage, we default to A_WIN as simulated penalty decider or DRAW is handled differently.
                // In knockout, let's treat tie score as penalty decider. Users can tap shortcut buttons if they want a clean winner.
                "A_WIN"
            } else {
                "DRAW"
            }
        }
    }
}

@Composable
fun ScoreSelectorButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StadiumLightBlue)
            .border(1.dp, PitchLine, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
