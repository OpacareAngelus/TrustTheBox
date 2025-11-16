package com.pTech.trustTheBox.ui.theme.screens.mainScreen

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pTech.trustTheBox.R
import com.pTech.trustTheBox.ui.theme.component.SetBackground
import com.pTech.trustTheBox.util.BillingManager
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    selectFileLauncher: ActivityResultLauncher<Intent>,
    selectZipLauncher: ActivityResultLauncher<Intent>,
    hasPassphrase: Boolean
) {
    val alpha = remember { mutableFloatStateOf(0.75f) }
    val context = LocalContext.current
    val toastAlertText = stringResource(R.string.enter_key_first)
    val isPremium by BillingManager.isPremium.collectAsState()

    LaunchedEffect(Unit) {
        var dir = -0.003f
        while (true) {
            alpha.floatValue += dir
            if (alpha.floatValue <= 0.1f) {
                alpha.floatValue = 0.1f; dir = 0.003f
            } else if (alpha.floatValue >= 0.75f) {
                alpha.floatValue = 0.75f; dir = -0.003f
            }
            delay(24)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SetBackground()
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Image(
                painter = painterResource(R.drawable.ic),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .alpha(alpha.floatValue),
                contentScale = ContentScale.FillBounds
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(64.dp)
                            .shadow(
                                elevation = 24.dp,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            .blur(16.dp)
                            .alpha(0.6f)
                    )
                    Button(
                        onClick = {
                            if (hasPassphrase) {
                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }
                                selectFileLauncher.launch(intent)
                            } else {
                                Toast.makeText(context, toastAlertText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .width(300.dp)
                            .height(64.dp)
                            .alpha(if (hasPassphrase) 1f else 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.background
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.select_files_for_password),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(Modifier.height(36.dp))

                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(64.dp)
                            .shadow(
                                elevation = 24.dp,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            .blur(16.dp)
                            .alpha(0.6f)
                    )
                    Button(
                        onClick = {
                            if (hasPassphrase) {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/zip"
                                }
                                selectZipLauncher.launch(intent)
                            } else {
                                Toast.makeText(context, toastAlertText, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .width(300.dp)
                            .height(64.dp)
                            .alpha(if (hasPassphrase) 1f else 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.background
                                        )
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.select_archive_for_viewing),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (!isPremium) {
                    Spacer(Modifier.height(36.dp))

                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(64.dp)
                                .shadow(
                                    elevation = 24.dp,
                                    ambientColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f),
                                    spotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                                .blur(16.dp)
                                .alpha(0.6f)
                        )
                        Button(
                            onClick = {
                                BillingManager.launchPurchase(context as androidx.activity.ComponentActivity)
                            },
                            modifier = Modifier
                                .width(300.dp)
                                .height(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.secondary,
                                                MaterialTheme.colorScheme.background
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Прибрати рекламу назавжди",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}