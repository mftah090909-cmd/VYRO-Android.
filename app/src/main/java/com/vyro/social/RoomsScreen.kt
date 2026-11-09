package com.vyro.social

import android.Manifest
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import kotlinx.coroutines.launch

@Composable
internal fun RoomsScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var room by remember { mutableStateOf<Room?>(null) }
    var roomName by remember { mutableStateOf("vyro-lounge") }
    var error by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    var micOn by remember { mutableStateOf(false) }
    val pulse by animateFloatAsState(if (room != null) 1.08f else 1f, tween(900), label = "room-pulse")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) error = "يجب السماح بالميكروفون للدخول إلى الروم" }
    DisposableEffect(Unit) { onDispose { room?.disconnect() } }
    val active = room != null
    Column(Modifier.fillMaxSize().background(Ink).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("VYRO ROOMS", color = Purple, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Text("رومات صوتية حقيقية عبر LiveKit", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.size(150.dp).scale(pulse).background(Brush.radialGradient(listOf(Cyan.copy(alpha = .9f), Purple.copy(alpha = .45f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Waves, null, tint = Color.White, modifier = Modifier.size(62.dp))
        }
        Spacer(Modifier.height(16.dp))
        AnimatedVisibility(visible = !active, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(roomName, { roomName = it }, label = { Text("اسم الروم") }, singleLine = true)
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    if (roomName.length < 3) { error = "اسم الروم قصير"; return@Button }
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                    connecting = true; error = ""
                    scope.launch {
                        runCatching {
                            val session = SupabaseApi.liveKitToken(prefs.getString("access_token", "") ?: "", roomName.replace(" ", "-"))
                            val liveRoom = LiveKit.create(context)
                            liveRoom.connect(session.url, session.token)
                            liveRoom.localParticipant.setMicrophoneEnabled(true)
                            liveRoom
                        }.onSuccess { connected -> room = connected; micOn = true }.onFailure { error = it.message ?: "تعذر فتح الروم. انشر Edge Function أولًا." }
                        connecting = false
                    }
                }, enabled = !connecting, colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(18.dp), modifier = Modifier.height(52.dp)) { Text(if (connecting) "جارٍ الاتصال..." else "دخول الروم") }
            }
        }
        AnimatedVisibility(visible = active, enter = fadeIn(), exit = fadeOut()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = Panel, shape = RoundedCornerShape(20.dp)) { Text("متصل الآن • الصوت مباشر", color = Cyan, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val micColor by animateColorAsState(if (micOn) Cyan else Pink, label = "mic-color")
                    IconButton(onClick = { val next = !micOn; scope.launch { room?.localParticipant?.setMicrophoneEnabled(next); micOn = next } }, modifier = Modifier.size(58.dp).background(micColor.copy(alpha = .2f), CircleShape)) { Icon(if (micOn) Icons.Default.Mic else Icons.Default.MicOff, null, tint = micColor) }
                    OutlinedButton(onClick = { room?.disconnect(); room = null; micOn = false }) { Text("خروج") }
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = Pink, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp).alpha(.95f))
        Spacer(Modifier.height(18.dp)); Text("يتم استخدام هوية المستخدم من Supabase، ولا يتم وضع LiveKit secret داخل التطبيق.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
