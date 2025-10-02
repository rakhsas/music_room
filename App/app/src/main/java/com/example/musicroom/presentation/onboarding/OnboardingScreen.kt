package com.example.musicroom.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicroom.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Preview()
@Composable
fun OnboardingScreen(onFinish: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val pages = listOf(
        OnboardingPage(
            title = "Bienvenue sur MusicRoom",
            description = "Votre expérience ultime de partage de musique",
            icon = Icons.Default.Headphones
        ),
        OnboardingPage(
            title = "Créer des salles de musique",
            description = "Organisez des sessions de musique live avec des amis",
            icon = Icons.Default.Group
        ),
        OnboardingPage(
            title = "Partager et découvrir",
            description = "Partagez vos morceaux préférés et découvrez-en de nouveaux",
            icon = Icons.Default.MusicNote
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingGradient)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(pages[page])
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            // Page Indicator
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) PrimaryPurple else TextSecondary
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Skip button
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(
                    onClick = { onFinish() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Passer", color = TextSecondary)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Next/Get Started Button
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)                ,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    if (pagerState.currentPage == pages.size - 1) "Commencer" else "Suivant",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = PrimaryPurple
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)