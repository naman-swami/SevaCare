package com.saathi.app.ui.archive

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saathi.app.data.model.StoryEntity
import com.saathi.app.ui.theme.SevaBodyTeal
import com.saathi.app.ui.theme.SevaCream
import com.saathi.app.ui.theme.SevaCreamContainer
import com.saathi.app.ui.theme.SevaDanger
import com.saathi.app.ui.theme.SevaDarkText
import com.saathi.app.ui.theme.SevaDeepTeal
import com.saathi.app.ui.theme.SevaMint
import com.saathi.app.ui.theme.SevaSaffron
import com.saathi.app.ui.theme.SevaWhite
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryHistorySheet(
    stories: List<StoryEntity>,
    onDeleteStory: (StoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var playingStoryId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPositionMs by remember { mutableFloatStateOf(0f) }
    var totalDurationMs by remember { mutableFloatStateOf(1f) }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        playingStoryId = null
        currentPositionMs = 0f
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPlayback()
        }
    }

    LaunchedEffect(playingStoryId) {
        while (playingStoryId != null) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    currentPositionMs = player.currentPosition.toFloat()
                }
            }
            delay(100)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopPlayback()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SevaCream
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recorded Stories",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SevaDeepTeal
                    )
                    Text(
                        text = "${stories.size} memories preserved safely",
                        fontSize = 14.sp,
                        color = SevaBodyTeal
                    )
                }

                IconButton(
                    onClick = {
                        stopPlayback()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(SevaMint, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close history",
                        tint = SevaDeepTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stories.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SevaWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No recorded stories yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevaDarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the round microphone button to record your first family audio memory.",
                            fontSize = 14.sp,
                            color = SevaBodyTeal,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stories, key = { it.id }) { story ->
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
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) SevaCreamContainer else SevaWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play / Pause Circle
                                    IconButton(
                                        onClick = {
                                            if (isCurrent) {
                                                stopPlayback()
                                            } else {
                                                stopPlayback()
                                                try {
                                                    val player = MediaPlayer().apply {
                                                        setDataSource(story.filePath)
                                                        prepare()
                                                        start()
                                                        setOnCompletionListener {
                                                            stopPlayback()
                                                        }
                                                    }
                                                    mediaPlayer = player
                                                    playingStoryId = story.id
                                                    totalDurationMs = maxOf(1f, player.duration.toFloat())
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(if (isCurrent) SevaSaffron else SevaDeepTeal, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrent) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isCurrent) "Pause story" else "Play story",
                                            tint = SevaWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = story.title,
                                            fontSize = 17.sp,
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

                                    // Share button
                                    IconButton(
                                        onClick = {
                                            shareStoryFile(context, story)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Share story",
                                            tint = SevaDeepTeal
                                        )
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            if (isCurrent) stopPlayback()
                                            onDeleteStory(story)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete story",
                                            tint = SevaDanger
                                        )
                                    }
                                }

                                if (isCurrent) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Slider(
                                        value = currentPositionMs.coerceIn(0f, totalDurationMs),
                                        onValueChange = { newVal ->
                                            currentPositionMs = newVal
                                            mediaPlayer?.seekTo(newVal.toInt())
                                        },
                                        valueRange = 0f..totalDurationMs,
                                        colors = SliderDefaults.colors(
                                            thumbColor = SevaSaffron,
                                            activeTrackColor = SevaDeepTeal
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun shareStoryFile(context: Context, story: StoryEntity) {
    try {
        val file = File(story.filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, story.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Family Audio Memory"))
    } catch (_: Exception) {}
}
