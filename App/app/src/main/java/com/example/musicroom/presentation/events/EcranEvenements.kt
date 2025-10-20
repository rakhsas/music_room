package com.example.musicroom.presentation.events

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.musicroom.data.models.Event
import com.example.musicroom.presentation.theme.*
import kotlinx.coroutines.delay

/**
 * ========================================================================
 * ÉCRAN DES ÉVÉNEMENTS - Design Moderne
 * ========================================================================
 * Découverte et gestion des événements musicaux
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranEvenements(
    controleurNavigation: NavController,
    modeleVue: EventsViewModel = hiltViewModel()
) {
    val etatEvenementsPublics by modeleVue.publicEventsState.collectAsState()
    val etatMesEvenements by modeleVue.myEventsState.collectAsState()
    val ongletSelectionne by modeleVue.selectedTab.collectAsState()
    val enCreation by modeleVue.isCreating.collectAsState()
    val resultatCreation by modeleVue.createResult.collectAsState()
    
    var afficherDialogueCreation by remember { mutableStateOf(false) }
    
    // Gérer le résultat de création
    LaunchedEffect(resultatCreation) {
        resultatCreation?.let { resultat ->
            when (resultat) {
                is CreateEventResult.Success -> {
                    delay(2000)
                    modeleVue.clearCreateResult()
                }
                is CreateEventResult.Error -> {
                    delay(3000)
                    modeleVue.clearCreateResult()
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondSombre)
    ) {
        // En-tête
        EnTeteEvenements(
            surRafraichir = { modeleVue.refreshCurrentTab() },
            surCreer = { afficherDialogueCreation = true }
        )
        
        // Onglets
        OngletsEvenements(
            ongletSelectionne = ongletSelectionne,
            surChangementOnglet = { modeleVue.switchTab(it) }
        )
        
        // Notification de résultat
        resultatCreation?.let { resultat ->
            NotificationResultat(resultat)
        }
        
        // Contenu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val etatActuel = when (ongletSelectionne) {
                EventTab.PUBLIC -> etatEvenementsPublics
                EventTab.MY_EVENTS -> etatMesEvenements
            }
            
            ContenuOngletsEvenements(
                etatUI = etatActuel,
                ongletSelectionne = ongletSelectionne,
                surRafraichir = { modeleVue.refreshCurrentTab() },
                surClicEvenement = { evenement ->
                    try {
                        controleurNavigation.navigate("event_details/${evenement.id}")
                    } catch (e: Exception) {
                        Log.e("EcranEvenements", "Erreur de navigation", e)
                    }
                }
            )
        }
    }
    
    // Dialogue de création
    if (afficherDialogueCreation) {
        CreateEventDialog(
            onDismiss = { afficherDialogueCreation = false },
            onCreate = { titre, description, date, heure, lieu ->
                modeleVue.createEvent(titre, description, date, heure, lieu)
                afficherDialogueCreation = false
            },
            isCreating = enCreation
        )
    }
}

@Composable
private fun EnTeteEvenements(
    surRafraichir: () -> Unit,
    surCreer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CouleurPrincipale.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = CouleurPrincipale,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = "Événements",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextePrimaire
                    )
                    Text(
                        text = "Découvrez et gérez vos événements",
                        fontSize = 14.sp,
                        color = TexteSecondaire
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = CouleurPrincipale.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = surRafraichir) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Rafraîchir",
                            tint = CouleurPrincipale
                        )
                    }
                }
                
                FloatingActionButton(
                    onClick = surCreer,
                    containerColor = CouleurPrincipale,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Créer un événement",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OngletsEvenements(
    ongletSelectionne: EventTab,
    surChangementOnglet: (EventTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(SurfaceSombre, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EventTab.values().forEach { onglet ->
            val estSelectionne = ongletSelectionne == onglet
            
            Button(
                onClick = { surChangementOnglet(onglet) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (estSelectionne) CouleurPrincipale else Color.Transparent,
                    contentColor = if (estSelectionne) Color.White else TexteSecondaire
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (estSelectionne) 2.dp else 0.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = when (onglet) {
                            EventTab.PUBLIC -> Icons.Default.Public
                            EventTab.MY_EVENTS -> Icons.Default.Person
                        },
                        contentDescription = onglet.displayName,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (onglet) {
                            EventTab.PUBLIC -> "Publics"
                            EventTab.MY_EVENTS -> "Mes Événements"
                        },
                        fontSize = 14.sp,
                        fontWeight = if (estSelectionne) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationResultat(resultat: CreateEventResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (resultat) {
                is CreateEventResult.Success -> CouleurSucces.copy(alpha = 0.2f)
                is CreateEventResult.Error -> ErreurSombre.copy(alpha = 0.2f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            when (resultat) {
                is CreateEventResult.Success -> CouleurSucces
                is CreateEventResult.Error -> ErreurSombre
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (resultat) {
                    is CreateEventResult.Success -> Icons.Default.CheckCircle
                    is CreateEventResult.Error -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (resultat) {
                    is CreateEventResult.Success -> CouleurSucces
                    is CreateEventResult.Error -> ErreurSombre
                },
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = when (resultat) {
                    is CreateEventResult.Success -> "Événement '${resultat.title}' créé avec succès !"
                    is CreateEventResult.Error -> "Erreur : ${resultat.message}"
                },
                color = when (resultat) {
                    is CreateEventResult.Success -> CouleurSucces
                    is CreateEventResult.Error -> ErreurSombre
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ContenuOngletsEvenements(
    etatUI: EventsUiState,
    ongletSelectionne: EventTab,
    surRafraichir: () -> Unit,
    surClicEvenement: (Event) -> Unit
) {
    when (etatUI) {
        is EventsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = CouleurPrincipale,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        "Chargement des événements...",
                        color = TexteSecondaire,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        is EventsUiState.Success -> {
            if (etatUI.events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EtatVideEvenements(ongletSelectionne)
                }
            } else {
                ListeEvenements(
                    evenements = etatUI.events,
                    surClicEvenement = surClicEvenement
                )
            }
        }
        
        is EventsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EtatErreurEvenements(
                    message = etatUI.message,
                    surReessayer = surRafraichir
                )
            }
        }
    }
}

@Composable
private fun ListeEvenements(
    evenements: List<Event>,
    surClicEvenement: (Event) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(evenements) { evenement ->
            CarteEvenement(
                evenement = evenement,
                surClic = { surClicEvenement(evenement) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun CarteEvenement(
    evenement: Event,
    surClic: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { surClic() },
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // En-tête avec gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                CouleurAccent.copy(alpha = 0.4f),
                                CouleurPrincipale.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = TextePrimaire,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Titre
            Text(
                text = evenement.title,
                color = TextePrimaire,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Organisateur
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = CouleurPrincipale,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = evenement.organizer.name,
                    color = TexteSecondaire,
                    fontSize = 14.sp
                )
            }
            
            // Participants
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = CouleurAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${evenement.attendee_count} participants",
                    color = CouleurAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EtatVideEvenements(onglet: EventTab) {
    Card(
        modifier = Modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = CouleurPrincipale.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        tint = CouleurPrincipale,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Text(
                text = when (onglet) {
                    EventTab.PUBLIC -> "Aucun événement public"
                    EventTab.MY_EVENTS -> "Aucun événement créé"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextePrimaire,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = when (onglet) {
                    EventTab.PUBLIC -> "Les événements publics apparaîtront ici"
                    EventTab.MY_EVENTS -> "Créez votre premier événement musical"
                },
                fontSize = 14.sp,
                color = TexteSecondaire,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EtatErreurEvenements(
    message: String,
    surReessayer: () -> Unit
) {
    Card(
        modifier = Modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSombre),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = ErreurSombre,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Erreur de chargement",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextePrimaire
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = TexteSecondaire,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = surReessayer,
                colors = ButtonDefaults.buttonColors(containerColor = CouleurPrincipale),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Réessayer")
            }
        }
    }
}

