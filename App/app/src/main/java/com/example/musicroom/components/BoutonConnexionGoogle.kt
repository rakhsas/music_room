package com.example.musicroom.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicroom.R
import com.example.musicroom.presentation.theme.*

/**
 * ========================================================================
 * BOUTON DE CONNEXION GOOGLE - Design Moderne
 * ========================================================================
 * Composant réutilisable pour l'authentification Google
 */
@Composable
fun BoutonGoogle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    texte: String = "Continuer avec Google"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceSombre,
            contentColor = TextePrimaire
        ),
        border = BorderStroke(1.dp, CouleurBordure),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Logo Google",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = texte,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextePrimaire
            )
        }
    }
}

// Alias pour compatibilité avec l'ancien code
@Composable
fun GoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoutonGoogle(onClick = onClick, modifier = modifier)
}

