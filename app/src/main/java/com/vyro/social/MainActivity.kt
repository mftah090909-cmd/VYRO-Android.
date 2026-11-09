package com.vyro.social

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Ink = Color(0xFF100F1B)
private val Panel = Color(0xFF1B1929)
private val Purple = Color(0xFF9A6BFF)
private val Cyan = Color(0xFF38D7D0)
private val Pink = Color(0xFFFF72B6)
private val Muted = Color(0xFFAAA6BC)

private data class VyroPost(val author: String, val handle: String, val body: String, val tag: String, var likes: Int, var liked: Boolean = false, val comments: MutableList<String> = mutableListOf())
private enum class Tab(val ar: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("الرئيسية", Icons.Default.Home), SHORTS("شورتز", Icons.Default.PlayCircle), ROOMS("رومات", Icons.Default.Groups), MESSAGES("الرسائل", Icons.Default.ChatBubble), PROFILE("حسابي", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VyroApp(getPreferences(Context.MODE_PRIVATE)) }
    }
}

@Composable
private fun VyroApp(prefs: android.content.SharedPreferences) {
    var session by remember { mutableStateOf(prefs.getString("access_token", null)) }
    var userName by remember { mutableStateOf(prefs.getString("name", "مستخدم VYRO") ?: "مستخدم VYRO") }
    if (!session) {
        AuthScreen(onAuthenticated = { name, remote ->
            userName = name.ifBlank { "مستخدم VYRO" }; session = remote.accessToken
            prefs.edit().putString("access_token", remote.accessToken).putString("user_id", remote.userId).putString("name", userName).apply()
        })
    } else {
        MainShell(userName = userName, onLogout = { prefs.edit().clear().apply(); session = null })
    }
}

@Composable
private fun AuthScreen(onAuthenticated: (String, RemoteSession) -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Ink).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(92.dp).background(Brush.linearGradient(listOf(Purple, Cyan, Pink)), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) { Text("V", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(18.dp)); Text("VYRO", color = Purple, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
        Text(if (register) "أنشئ مساحتك الاجتماعية" else "عالمك، ناسك، مساحتك", color = Muted)
        Spacer(Modifier.height(28.dp))
        if (register) Field(name, { name = it }, "الاسم الظاهر")
        Field(email, { email = it }, "البريد الإلكتروني")
        Field(password, { password = it }, "كلمة المرور", true)
        if (error.isNotBlank()) Text(error, color = Pink, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        Button(onClick = { busy = true; error = ""; scope.launch { runCatching { if (register) SupabaseApi.signUp(email.trim(), password, name.trim()) else SupabaseApi.signIn(email.trim(), password) }.onSuccess { onAuthenticated(name, it) }.onFailure { error = it.message ?: "تعذر الاتصال بـ Supabase" }; busy = false } }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple), shape = RoundedCornerShape(16.dp)) { Text(if (busy) "جارٍ الاتصال..." else if (register) "إنشاء حساب حقيقي" else "تسجيل الدخول", fontWeight = FontWeight.Bold) }
        TextButton(onClick = { register = !register; error = "" }) { Text(if (register) "لديك حساب؟ سجّل الدخول" else "ليس لديك حساب؟ أنشئ حسابًا", color = Cyan) }
        Text("الحسابات والبيانات تُحفظ في Supabase", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Field(value: String, onValueChange: (String) -> Unit, label: String, password: Boolean = false) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Cyan, unfocusedBorderColor = Muted, focusedLabelColor = Cyan, unfocusedLabelColor = Muted, focusedTextColor = Color.White, unfocusedTextColor = Color.White))
}

@Composable
private fun MainShell(userName: String, prefs: android.content.SharedPreferences, onLogout: () -> Unit) {
    var selected by remember { mutableStateOf(Tab.HOME) }
    val posts = remember { mutableStateListOf<VyroPost>() }
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Purple, secondary = Cyan, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(containerColor = Ink, topBar = { TopBar(userName) }, bottomBar = { NavigationBar(containerColor = Panel) { Tab.values().forEach { tab -> NavigationBarItem(selected == tab, { selected = tab }, icon = { Icon(tab.icon, tab.ar) }, label = { Text(tab.ar, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, selectedTextColor = Cyan, indicatorColor = Purple.copy(alpha = .2f), unselectedIconColor = Muted, unselectedTextColor = Muted)) } } }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                when (selected) {
                    Tab.HOME -> Feed(posts, userName, prefs)
                    Tab.SHORTS -> Feature("VYRO SHORTS", "فيديوهات عمودية قصيرة", "سيتم ربط التخزين، الضغط، والتشغيل المتتابع عبر Supabase Storage.")
                    Tab.ROOMS -> Feature("VYRO ROOMS", "رومات صوتية مباشرة", "تحتاج هذه الميزة مزود صوت حقيقي مثل LiveKit أو Agora، وليست واجهة وهمية.")
                    Tab.MESSAGES -> Messages()
                    Tab.PROFILE -> Profile(userName, onLogout)
                }
            }
        }
    }
}

@Composable private fun TopBar(userName: String) { Row(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("VYRO", color = Purple, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp); Text("عالمك، ناسك، مساحتك", color = Muted, fontSize = 11.sp) }; IconButton({}) { Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = Color.White) }; Box(Modifier.size(36.dp).background(Brush.linearGradient(listOf(Purple, Cyan)), CircleShape), contentAlignment = Alignment.Center) { Text(userName.take(1), color = Color.White, fontWeight = FontWeight.Bold) } } }

@Composable private fun Feed(posts: MutableList<VyroPost>, userName: String, prefs: android.content.SharedPreferences) {
    var draft by remember { mutableStateOf("") }; var showComposer by remember { mutableStateOf(false) }; var commentFor by remember { mutableStateOf<Int?>(null) }; var comment by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }; val scope = rememberCoroutineScope(); val token = prefs.getString("access_token", "") ?: ""; val userId = prefs.getString("user_id", "") ?: ""
    LaunchedEffect(Unit) { runCatching { SupabaseApi.posts(token, userId) }.onSuccess { remote -> posts.clear(); posts.addAll(remote.map { VyroPost(it.author, it.handle, it.body, "منشور", it.likes, it.liked, remoteId = it.id) }) }.onFailure { error = it.message ?: "تعذر تحميل المنشورات" }; loading = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text("مساحتك اليوم", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("منشورات حقيقية من قاعدة بيانات VYRO", color = Muted, fontSize = 12.sp) }
        item { Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).clickable { showComposer = true }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AddCircle, null, tint = Purple); Spacer(Modifier.width(10.dp)); Text("شن في بالك؟ شارك منشور...", color = Muted, modifier = Modifier.weight(1f)); Text("نشر", color = Cyan, fontWeight = FontWeight.Bold) } }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan) }
        if (error.isNotBlank()) item { Text(error, color = Pink, fontSize = 12.sp) }
        if (showComposer) item { Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(14.dp)) { OutlinedTextField(draft, { draft = it }, label = { Text("اكتب منشورك") }, modifier = Modifier.fillMaxWidth()); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ showComposer = false }) { Text("إلغاء") }; Button({ scope.launch { runCatching { SupabaseApi.createPost(token, userId, draft) }.onSuccess { remote -> posts.add(0, VyroPost(remote.author, remote.handle, remote.body, "جديد", 0, remoteId = remote.id)); draft = ""; showComposer = false }.onFailure { error = it.message ?: "تعذر نشر المنشور" } } }, enabled = draft.isNotBlank()) { Text("نشر") } } } }
        itemsIndexed(posts) { index, post -> PostCard(post, onLike = { post.liked = !post.liked; post.likes += if (post.liked) 1 else -1; if (post.remoteId.isNotBlank()) scope.launch { runCatching { SupabaseApi.like(token, post.remoteId, userId, post.liked) } } }, onComment = { commentFor = index }, showComment = commentFor == index, comment = comment, onCommentChange = { comment = it }, submit = { if (comment.isNotBlank() && post.remoteId.isNotBlank()) scope.launch { runCatching { SupabaseApi.comment(token, post.remoteId, userId, comment) }.onSuccess { post.comments.add(comment); comment = ""; commentFor = null }.onFailure { error = it.message ?: "تعذر إضافة التعليق" } } }) }
    }
}

@Composable private fun PostCard(post: VyroPost, onLike: () -> Unit, onComment: () -> Unit, showComment: Boolean, comment: String, onCommentChange: (String) -> Unit, submit: () -> Unit) { Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Purple, Cyan)), CircleShape), contentAlignment = Alignment.Center) { Text(post.author.take(1), color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(post.author, color = Color.White, fontWeight = FontWeight.Bold); Text("${post.handle} • الآن", color = Muted, fontSize = 10.sp) }; Text(post.tag, color = Cyan, fontSize = 10.sp) }; Spacer(Modifier.height(13.dp)); Text(post.body, color = Color.White, fontSize = 15.sp, lineHeight = 23.sp); if (post.comments.isNotEmpty()) Text("${post.comments.size} تعليق", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)); HorizontalDivider(color = Color.White.copy(alpha = .07f), modifier = Modifier.padding(top = 12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onLike) { Icon(if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (post.liked) Pink else Muted); Spacer(Modifier.width(4.dp)); Text("${post.likes}", color = Muted) }; TextButton(onComment) { Icon(Icons.Default.ChatBubbleOutline, null, tint = Muted); Spacer(Modifier.width(4.dp)); Text("تعليق", color = Muted) }; Spacer(Modifier.weight(1f)); IconButton({}) { Icon(Icons.Default.Share, "مشاركة", tint = Muted) } }; if (showComment) { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(comment, onCommentChange, label = { Text("اكتب تعليقًا") }, modifier = Modifier.weight(1f)); IconButton(submit) { Icon(Icons.Default.Send, "إرسال", tint = Cyan) } } } } }

@Composable private fun Messages() { Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.ChatBubble, null, tint = Cyan, modifier = Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); Text("رسائلك الخاصة", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("ابدأ محادثات آمنة مع مجتمعك. طبقة المحادثات البعيدة قيد الربط مع Supabase Realtime.", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(18.dp)); Button({ }, colors = ButtonDefaults.buttonColors(containerColor = Purple)) { Text("إنشاء محادثة") } } }

@Composable private fun Profile(userName: String, onLogout: () -> Unit) { Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(92.dp).background(Brush.linearGradient(listOf(Purple, Cyan, Pink)), CircleShape), contentAlignment = Alignment.Center) { Text(userName.take(1), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(12.dp)); Text(userName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("@vyro_user", color = Muted); Spacer(Modifier.height(22.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Stat("منشور", "3"); Stat("متابع", "128"); Stat("يتابع", "64") }; Spacer(Modifier.height(28.dp)); OutlinedButton(onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("تسجيل الخروج") }; Spacer(Modifier.height(12.dp)); Text("الملف الشخصي محلي حاليًا. أضف Supabase للمزامنة والصور.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center) } }
@Composable private fun Stat(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(label, color = Muted, fontSize = 11.sp) } }

@Composable private fun Feature(title: String, subtitle: String, description: String) { Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Box(Modifier.size(104.dp).background(Brush.linearGradient(listOf(Purple, Cyan, Pink)), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(48.dp)) }; Spacer(Modifier.height(24.dp)); Text(title, color = Purple, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp); Text(subtitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text(description, color = Muted, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center); Spacer(Modifier.height(20.dp)); Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Text("المرحلة التالية • Coming soon", color = Cyan, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) } } }
