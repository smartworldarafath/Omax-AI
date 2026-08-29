package me.proton.android.lumo.ui.components.update

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.proton.android.lumo.R
import me.proton.android.lumo.ui.theme.LumoTheme
import me.proton.android.lumo.update.UpdateManager
import me.proton.android.lumo.update.model.DownloadStatus
import me.proton.android.lumo.update.model.GitHubRelease
import me.proton.android.lumo.utils.openExternalUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    updateManager: UpdateManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val downloadStatus by updateManager.downloadStatus.collectAsStateWithLifecycle()
    val isUpdateAvailable by updateManager.isUpdateAvailable.collectAsStateWithLifecycle()
    val latestRelease by updateManager.latestRelease.collectAsStateWithLifecycle()
    val recentReleases by updateManager.recentReleases.collectAsStateWithLifecycle()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var selectedQrImageRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        if (latestRelease == null) {
            isCheckingUpdates = true
            updateManager.checkForUpdates()
            isCheckingUpdates = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Omax Ai Updates & About",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LumoTheme.colors.textNorm
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LumoTheme.colors.textNorm
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isCheckingUpdates = true
                                updateManager.checkForUpdates()
                                isCheckingUpdates = false
                                Toast.makeText(context, "Checked for updates", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (isCheckingUpdates) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = LumoTheme.colors.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check for Updates",
                                tint = LumoTheme.colors.textNorm
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LumoTheme.colors.backgroundNorm
                )
            )
        },
        containerColor = LumoTheme.colors.backgroundNorm
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Version & Update Status Card
            item {
                UpdateStatusCard(
                    currentVersion = updateManager.currentVersion,
                    isUpdateAvailable = isUpdateAvailable,
                    latestRelease = latestRelease,
                    downloadStatus = downloadStatus,
                    onDownloadClick = {
                        latestRelease?.let { release ->
                            scope.launch {
                                updateManager.startDownload(release)
                            }
                        }
                    },
                    onInstallClick = { filePath ->
                        updateManager.installApk(filePath)
                    }
                )
            }

            // 2. App Information & Developer Attribution Card
            item {
                AppInfoCard(
                    onOpenGitHub = {
                        context.openExternalUrl("https://github.com/smartworldarafath/Omax-AI")
                    }
                )
            }

            // 3. Recent Update History (Last 5 Updates)
            item {
                Text(
                    text = "Recent Updates (Changelog)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LumoTheme.colors.textNorm,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (recentReleases.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LumoTheme.colors.backgroundWeak)
                    ) {
                        Text(
                            text = "Loading update history from GitHub...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LumoTheme.colors.textWeak,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(recentReleases) { release ->
                    ReleaseHistoryItem(release = release)
                }
            }

            // 4. Donation / Support Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DonationSupportCard(
                    onShowQr = { resId -> selectedQrImageRes = resId },
                    onCopyText = { text, label ->
                        copyToClipboard(context, text, label)
                    }
                )
            }
        }
    }

    // QR Code Fullscreen Preview Dialog
    selectedQrImageRes?.let { qrRes ->
        Dialog(onDismissRequest = { selectedQrImageRes = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E1E24)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = qrRes),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedQrImageRes = null },
                        colors = ButtonDefaults.buttonColors(containerColor = LumoTheme.colors.primary)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(
    currentVersion: String,
    isUpdateAvailable: Boolean,
    latestRelease: GitHubRelease?,
    downloadStatus: DownloadStatus,
    onDownloadClick: () -> Unit,
    onInstallClick: (String) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (downloadStatus is DownloadStatus.Downloading) downloadStatus.progress else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "DownloadProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUpdateAvailable) Color(0xFF261D36) else LumoTheme.colors.backgroundWeak
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LumoTheme.colors.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUpdateAvailable) Icons.Default.Refresh else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isUpdateAvailable) LumoTheme.colors.primary else Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Text(
                        text = "Omax Ai",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = LumoTheme.colors.textNorm
                    )
                    Text(
                        text = "Installed Version: v$currentVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LumoTheme.colors.textWeak
                    )
                }
            }

            if (isUpdateAvailable && latestRelease != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B2456))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🚀 New Version Available: ${latestRelease.tagName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE2B6FF)
                        )
                        Text(
                            text = latestRelease.name.ifEmpty { "Latest update with performance and UI improvements" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD1C4E9)
                        )
                    }
                }

                // Download / Install Status Section
                when (val status = downloadStatus) {
                    is DownloadStatus.Idle -> {
                        Button(
                            onClick = onDownloadClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LumoTheme.colors.primary)
                        ) {
                            Text("Download Update (ডাউনলোড করুন)", fontWeight = FontWeight.Bold)
                        }
                    }

                    is DownloadStatus.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = LumoTheme.colors.primary,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val percent = (status.progress * 100).toInt()
                                val downloadedMb = status.downloadedBytes / (1024f * 1024f)
                                val totalMb = status.totalBytes / (1024f * 1024f)
                                Text(
                                    text = "$percent% • ${"%.1f".format(downloadedMb)} MB / ${"%.1f".format(totalMb)} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LumoTheme.colors.textNorm
                                )
                                if (status.speedBytesPerSec > 0) {
                                    val speedMb = status.speedBytesPerSec / (1024f * 1024f)
                                    Text(
                                        text = "${"%.1f".format(speedMb)} MB/s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LumoTheme.colors.textWeak
                                    )
                                }
                            }
                        }
                    }

                    is DownloadStatus.Downloaded -> {
                        Button(
                            onClick = { onInstallClick(status.filePath) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Install Update (ইন্সটল করুন)", fontWeight = FontWeight.Bold)
                        }
                    }

                    is DownloadStatus.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ ${status.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            OutlinedButton(
                                onClick = onDownloadClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry Download (আবার চেষ্টা করুন)")
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "✓ You are using the latest version of Omax Ai.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF81C784)
                )
            }
        }
    }
}

@Composable
private fun AppInfoCard(onOpenGitHub: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LumoTheme.colors.backgroundWeak)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = LumoTheme.colors.primary
                )
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LumoTheme.colors.textNorm
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Developer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LumoTheme.colors.textWeak
                )
                Text(
                    text = "Md Arafath Rahman",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LumoTheme.colors.textNorm
                )
            }

            Text(
                text = "এই অ্যাপটি তৈরি করা হয়েছে open source lumo by proton ব্যবহার করে।",
                style = MaterialTheme.typography.bodySmall,
                color = LumoTheme.colors.textWeak
            )

            OutlinedButton(
                onClick = onOpenGitHub,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("View Source on GitHub")
            }
        }
    }
}

@Composable
private fun ReleaseHistoryItem(release: GitHubRelease) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LumoTheme.colors.backgroundWeak)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LumoTheme.colors.primary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = release.tagName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = LumoTheme.colors.primary
                        )
                    }
                    Text(
                        text = release.name.ifEmpty { "Release" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LumoTheme.colors.textNorm
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LumoTheme.colors.textWeak
                )
            }

            if (release.publishedAt.isNotEmpty()) {
                val cleanDate = release.publishedAt.take(10)
                Text(
                    text = "Released: $cleanDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = LumoTheme.colors.textWeak
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = release.body.ifEmpty { "Bug fixes and stability improvements." },
                    style = MaterialTheme.typography.bodySmall,
                    color = LumoTheme.colors.textWeak,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DonationSupportCard(
    onShowQr: (Int) -> Unit,
    onCopyText: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B22))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF5252)
                )
                Text(
                    text = "☕ Buy Me a Coffee / Support",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Text(
                text = "প্রজেক্টটি আরও বড় করতে এবং চলমান রাখতে নিচের একাউন্টগুলোর মাধ্যমে সরাসরি সহায়তা পাঠাতে পারেন:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            // RedotPay Method
            PaymentMethodCard(
                title = "RedotPay",
                primaryValue = "1965421414",
                primaryLabel = "RedotPay ID",
                subLabel = "Scan with RedotPay App to pay",
                qrDrawableRes = R.drawable.redotpay_qr,
                onShowQr = onShowQr,
                onCopy = onCopyText
            )

            // Payoneer Method
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262630))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Payoneer",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF9800)
                    )
                    PaymentFieldRow(
                        label = "Name",
                        value = "Arafath Rahman",
                        onCopy = { onCopyText("Arafath Rahman", "Name") }
                    )
                    PaymentFieldRow(
                        label = "Email",
                        value = "arafathrahman710@gmail.com",
                        onCopy = { onCopyText("arafathrahman710@gmail.com", "Email") }
                    )
                    PaymentFieldRow(
                        label = "Customer ID",
                        value = "70366820",
                        onCopy = { onCopyText("70366820", "Customer ID") }
                    )
                }
            }

            // nSave Method
            PaymentMethodCard(
                title = "nSave",
                primaryValue = "@arafath_rahman9",
                primaryLabel = "Handle",
                subLabel = "Md Arafath Rahman • Zero fees transfer",
                qrDrawableRes = R.drawable.nsave_qr,
                onShowQr = onShowQr,
                onCopy = onCopyText
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    primaryValue: String,
    primaryLabel: String,
    subLabel: String,
    qrDrawableRes: Int,
    onShowQr: (Int) -> Unit,
    onCopy: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF262630))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF4FC3F7)
                )

                Button(
                    onClick = { onShowQr(qrDrawableRes) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3B48))
                ) {
                    Text("View QR", fontSize = 12.sp, color = Color.White)
                }
            }

            PaymentFieldRow(
                label = primaryLabel,
                value = primaryValue,
                onCopy = { onCopy(primaryValue, primaryLabel) }
            )

            Text(
                text = subLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun PaymentFieldRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Surface(
            onClick = onCopy,
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.1f)
        ) {
            Text(
                text = "Copy",
                fontSize = 11.sp,
                color = Color(0xFFB0BEC5),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard!", Toast.LENGTH_SHORT).show()
}
