package com.saathi.app.ui.archive

import android.Manifest
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.data.db.SaathiDatabase
import com.saathi.app.data.model.StoryEntity
import com.saathi.app.data.repository.StoryRepository
import com.saathi.app.ui.components.FourBarAudioVisualizer
import com.saathi.app.ui.mascot.MascotState
import com.saathi.app.ui.mascot.SparrowMascotView
import com.saathi.app.ui.theme.SevaBodyTeal
import com.saathi.app.ui.theme.SevaCream
import com.saathi.app.ui.theme.SevaCreamContainer
import com.saathi.app.ui.theme.SevaDanger
import com.saathi.app.ui.theme.SevaDarkText
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaMint
import com.saathi.app.ui.theme.SevaPeach
import com.saathi.app.ui.theme.SevaSafeGreen
import com.saathi.app.ui.theme.SevaSaffron
import com.saathi.app.ui.theme.SevaWhite
import com.saathi.app.util.RememberPermissionGate
import com.saathi.app.util.StoryRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Living Family Archive Screen.
 *
 * Provides a dignified, voice-first oral history vault for older adults.
 * Implements real MediaRecorder producing .m4a files on disk, live 4-bar
 * audio visualizer driven by amplitude polling, and floating history button.
 */
@Composable
fun FamilyArchiveScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { SaathiDatabase.getInstance(context) }
    val repository = remember { StoryRepository(db.storyDao(), context) }
    val stories by repository.allStories.collectAsState(initial = emptyList())

    val recorder = remember { StoryRecorder(context, repository) }
    var isRecording by remember { mutableStateOf(false) }
    var currentAmplitude by remember { mutableIntStateOf(0) }
    var recordingStartMillis by remember { mutableLongStateOf(0L) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var currentOutputFile by remember { mutableStateOf<File?>(null) }

    var showHistorySheet by remember { mutableStateOf(false) }
    var playingStoryId by remember { mutableStateOf<String?>(null) }
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Live Amplitude & Timer ticker loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                currentAmplitude = recorder.pollAmplitude()
                elapsedSeconds = ((System.currentTimeMillis() - recordingStartMillis) / 1000).toInt()
                delay(60)
            }
        } else {
            currentAmplitude = 0
            elapsedSeconds = 0
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isCurrentlyRecording()) {
                recorder.stop()
            }
            try {
                activePlayer?.stop()
                activePlayer?.release()
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SevaCream
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(64.dp)
                            .background(SevaMint, CircleShape)
                            .semantics { contentDescription = "Go back to Home" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = SevaDeepTeal,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Living Family Archive",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDeepTeal
                        )
                        Text(
                            text = "Stories & memories in your voice",
                            fontSize = 16.sp,
                            color = SevaBodyTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Companion Banner
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SevaCreamContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SparrowMascotView(
                            state = if (isRecording) MascotState.LISTENING else MascotState.IDLE_SOFT,
                            size = 80.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRecording) "Listening to your story..." else "Preserve your life stories",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SevaDarkText
                            )
                            Text(
                                text = if (isRecording)
                                    "Take your time. Speak naturally in your own words."
                                else
                                    "Your voice and memories stay safely on your phone in high-quality audio.",
                                fontSize = 15.sp,
                                color = SevaBodyTeal,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Voice Record Studio Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecording) Color(0xFFFFDAD6) else SevaWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (isRecording) SevaDanger else SevaMint,
                            RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val formattedTime = remember(elapsedSeconds) {
                            val mins = elapsedSeconds / 60
                            val secs = elapsedSeconds % 60
                            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                        }

                        Text(
                            text = if (isRecording) "Recording Story ($formattedTime)" else "Record a New Memory",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) SevaDanger else SevaDeepTeal
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live 4-Bar Audio Visualizer
                        FourBarAudioVisualizer(
                            amplitude = currentAmplitude,
                            isRecording = isRecording,
                            barColor = if (isRecording) SevaDanger else SevaSaffron,
                            width = 120.dp,
                            maxHeight = 64.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Round Microphone Button (88dp touch target)
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(if (isRecording) SevaDanger else SevaDeepTeal)
                                .clickable {
                                    if (isRecording) {
                                        // Stop recording and save
                                        val (file, durationMs) = recorder.stop()
                                        isRecording = false
                                        if (file != null && file.exists() && file.length() > 0L) {
                                            scope.launch {
                                                val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                                repository.saveStory(
                                                    file = file,
                                                    durationMs = durationMs,
                                                    title = "Family Memory ($nowStr)"
                                                )
                                            }
                                            onSpeak("Story saved safely to your private device archive.")
                                        }
                                    } else {
                                        // Start real recording
                                        val newFile = repository.createNewStoryFile()
                                        currentOutputFile = newFile
                                        recordingStartMillis = System.currentTimeMillis()
                                        recorder.start(newFile)
                                        isRecording = true
                                        onSpeak("Started recording your story. Tap the red button when finished.")
                                    }
                                }
                                .semantics {
                                    contentDescription = if (isRecording) "Stop recording story" else "Start recording story"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = SevaWhite,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isRecording) "Tap red square to stop & save" else "Tap microphone and speak",
                            fontSize = 16.sp,
                            color = SevaBodyTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Saved Family Stories Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Saved Stories",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "${stories.size} saved",
                        fontSize = 14.sp,
                        color = SevaBodyTeal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (stories.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SevaWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No audio memories recorded yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SevaDarkText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Record your childhood tales, recipes, or life advice. Your recordings stay 100% on this phone.",
                                fontSize = 14.sp,
                                color = SevaBodyTeal,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    stories.take(5).forEach { story ->
                        val isCurrent = playingStoryId == story.id
                        val formattedDate = remember(story.createdAt) {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(story.createdAt))
                        }
                        val formattedDuration = remember(story.durationMs) {
                            val secs = (story.durationMs / 1000) % 60
                            val mins = (story.durationMs / 1000) / 60
                            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                        }

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SevaWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .border(1.5.dp, SevaMint, RoundedCornerShape(20.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isCurrent) {
                                            try {
                                                activePlayer?.stop()
                                                activePlayer?.release()
                                            } catch (_: Exception) {}
                                            activePlayer = null
                                            playingStoryId = null
                                        } else {
                                            try {
                                                activePlayer?.stop()
                                                activePlayer?.release()
                                                val p = MediaPlayer().apply {
                                                    setDataSource(story.filePath)
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        playingStoryId = null
                                                        activePlayer = null
                                                    }
                                                }
                                                activePlayer = p
                                                playingStoryId = story.id
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(if (isCurrent) SevaSaffron else SevaDeepTeal, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isCurrent) "Pause" else "Play",
                                        tint = SevaWhite
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = story.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SevaDarkText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$formattedDate • $formattedDuration",
                                        fontSize = 13.sp,
                                        color = SevaBodyTeal
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteStory(story)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete story",
                                        tint = SevaDanger
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Circular History / Playback Button at bottom-right corner
        FloatingActionButton(
            onClick = { showHistorySheet = true },
            containerColor = SevaSaffron,
            contentColor = SevaDeepTeal,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(68.dp)
                .semantics { contentDescription = "Open Family Story History and Playback" }
        ) {
            Icon(
                imageVector = Icons.Rounded.HistoryEdu,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }

        // Story History Sheet
        if (showHistorySheet) {
            StoryHistorySheet(
                stories = stories,
                onDeleteStory = { story ->
                    scope.launch {
                        repository.deleteStory(story)
                    }
                },
                onDismiss = { showHistorySheet = false }
            )
        }
    }
}
