# 📝 File Naming Guide - Music Room App

## Overview
This guide provides the naming conventions and patterns used in the Music Room app refactoring.

---

## 🎯 File Naming Conventions

### **General Principles**
1. **Descriptive Names**: Use clear, meaningful names that describe the file's purpose
2. **PascalCase**: All file names use PascalCase (e.g., `LoginView.kt`, `MainDashboard.kt`)
3. **Suffix Patterns**: Follow consistent patterns for different file types
4. **No Abbreviations**: Avoid abbreviations unless universally understood (e.g., UI, API)

---

## 📂 Category-Based Naming

### **1. Screen/View Files**

#### **Pattern**: `[Feature][Type].kt`
- **View**: UI screens (e.g., `LoginView.kt`, `MediaPlayerView.kt`)
- **Screen**: Complete screen implementations (e.g., `DashboardScreen.kt`, `OnboardingScreen.kt`)

#### **Examples**:
```
Old Name              →  New Name
────────────────────────────────────────
loginScreen.kt       →  LoginView.kt
SignUpScreen.kt      →  RegistrationView.kt
NowPlayingScreen.kt  →  MediaPlayerView.kt
home.kt              →  DashboardScreen.kt
SimpleHomeScreen.kt  →  MainDashboard.kt
```

### **2. ViewModel Files**

#### **Pattern**: `[Feature]ViewModel.kt`

#### **Examples**:
```
Old Name                →  New Name
──────────────────────────────────────────────
NowPlayingViewModel.kt →  MediaPlayerViewModel.kt
HomeViewModel.kt       →  DashboardViewModel.kt
```

### **3. Component Files**

#### **Pattern**: `[Component][Type].kt`
- Use descriptive names for reusable components
- Include the component type (Button, Card, Dialog, etc.)

#### **Examples**:
```
Component Files:
─────────────────
GoogleButton.kt
NotificationCard.kt
AudioVisualizer.kt
AddToPlaylistDialog.kt
```

### **4. Theme Files**

#### **Pattern**: `App[Type].kt`
- Prefix with "App" to indicate app-wide resources

#### **Examples**:
```
Old Name    →  New Name
──────────────────────────
Color.kt   →  AppColors.kt
Theme.kt   →  AppTheme.kt
Type.kt    →  AppTypography.kt
```

### **5. Data Model Files**

#### **Pattern**: `[Model][Type].kt`
- Use singular for single models
- Use plural or descriptive suffix for collections

#### **Examples**:
```
Model Files:
─────────────
User.kt
Track.kt
Playlist.kt
HomeModels.kt
PlaylistModels.kt
```

### **6. Repository Files**

#### **Pattern**: `[Feature]Repository.kt`

#### **Examples**:
```
Repository Files:
──────────────────────
MusicRepository.kt
ArtistRepository.kt
UserProfileRepository.kt
```

### **7. Service Files**

#### **Pattern**: `[Feature]Service.kt` or `[Feature]ApiService.kt`

#### **Examples**:
```
Service Files:
────────────────────────
MusicPlayerService.kt
AuthApiService.kt
HomeApiService.kt
PlaylistApiService.kt
```

---

## 🎨 Screen Naming Matrix

### **Authentication Screens**
```
Purpose                  →  File Name
────────────────────────────────────────
Login                   →  LoginView.kt
Sign Up/Registration    →  RegistrationView.kt
Forgot Password         →  ForgotPasswordScreen.kt
Email Confirmation      →  EmailConfirmationScreen.kt
Reset Password          →  ResetPasswordScreen.kt
```

### **Main App Screens**
```
Purpose                  →  File Name
────────────────────────────────────────
Main Dashboard          →  MainDashboard.kt
Home Feed               →  DashboardScreen.kt
Music Player            →  MediaPlayerView.kt
Music Search            →  MusicSearchScreen.kt
Artist Details          →  ArtistDetailsScreen.kt
Event Details           →  EventDetailsScreen.kt
```

### **Feature Screens**
```
Purpose                  →  File Name
────────────────────────────────────────
Playlists List          →  PublicPlaylistsScreen.kt
Playlist Tracks         →  PlaylistTracksScreen.kt
Events List             →  EventsScreen.kt
Profile                 →  ProfileScreen.kt
Onboarding              →  OnboardingScreen.kt
Splash                  →  SplashScreen.kt
```

---

## 🔧 Special Cases

### **Entry Points**
```
Old Name         →  New Name
────────────────────────────────
MainActivity.kt →  AppEntryPoint.kt
```

### **Container/Wrapper Screens**
```
AuthContainer.kt    (keeps same name - it's a logical container)
```

### **Navigation and Routing**
```
AuthScreen.kt       (keeps same name - defines navigation routes)
```

---

## 📋 Checklist for Renaming Files

When renaming a file, follow these steps:

### **1. Create New File**
- [ ] Create new file with updated name
- [ ] Copy and update header documentation
- [ ] Update package declaration (if needed)
- [ ] Update class/function names if appropriate
- [ ] Apply new design system (colors, spacing, etc.)

### **2. Update References**
- [ ] Update imports in all files using the old file
- [ ] Update navigation references
- [ ] Update manifest (for Activity files)
- [ ] Update dependency injection modules (if applicable)

### **3. Backward Compatibility** (if needed)
- [ ] Keep old file with deprecation warnings
- [ ] Add forwarding/delegation to new implementation
- [ ] Document migration path

### **4. Documentation**
- [ ] Update REFACTORING_SUMMARY.md
- [ ] Update this FILE_NAMING_GUIDE.md
- [ ] Add inline comments explaining changes

---

## 🎯 Best Practices

### **DO's**
✅ Use descriptive, self-documenting names
✅ Follow established patterns consistently
✅ Include purpose in the name (View, Screen, Service, etc.)
✅ Use PascalCase for all file names
✅ Group related files by feature/module

### **DON'Ts**
❌ Use abbreviations (except standard ones like UI, API)
❌ Use generic names like "Utils", "Helper" without context
❌ Mix naming conventions within the same category
❌ Use camelCase or snake_case for file names
❌ Create overly long names (keep under 30 characters if possible)

---

## 📊 Quick Reference Table

| File Type | Pattern | Example | Notes |
|-----------|---------|---------|-------|
| Screen | `[Feature]Screen.kt` | `DashboardScreen.kt` | Complete UI screen |
| View | `[Feature]View.kt` | `LoginView.kt` | UI view component |
| ViewModel | `[Feature]ViewModel.kt` | `MediaPlayerViewModel.kt` | State management |
| Repository | `[Feature]Repository.kt` | `MusicRepository.kt` | Data access layer |
| Service | `[Feature]Service.kt` | `MusicPlayerService.kt` | Business logic |
| API Service | `[Feature]ApiService.kt` | `AuthApiService.kt` | Network calls |
| Model | `[Entity].kt` | `Track.kt` | Data model |
| Models (Collection) | `[Feature]Models.kt` | `HomeModels.kt` | Multiple models |
| Dialog | `[Feature]Dialog.kt` | `AddToPlaylistDialog.kt` | Dialog component |
| Card | `[Feature]Card.kt` | `NotificationCard.kt` | Card component |
| Theme | `App[Type].kt` | `AppColors.kt` | App-wide resources |

---

## 🔄 Migration Examples

### **Example 1: Screen Rename**
```kotlin
// Old: loginScreen.kt
package com.example.musicroom.presentation.auth

@Composable
fun LoginScreen(...) { }

// New: LoginView.kt
package com.example.musicroom.presentation.auth

@Composable
fun LoginView(...) { }
```

### **Example 2: Update Imports**
```kotlin
// Old import
import com.example.musicroom.presentation.auth.LoginScreen

// New import
import com.example.musicroom.presentation.auth.LoginView

// Old usage
LoginScreen(...)

// New usage
LoginView(...)
```

### **Example 3: Navigation Update**
```kotlin
// Old
composable("login") {
    LoginScreen(...)
}

// New
composable("login") {
    LoginView(...)
}
```

---

## 📚 Related Documents
- `REFACTORING_SUMMARY.md` - Complete list of all changes made
- `AUTHENTICATION_README.md` - Authentication system documentation
- `README.md` - Project overview

---

**Note**: This guide evolves as new patterns emerge. When adding new files, refer to this guide to maintain consistency across the codebase.

**Last Updated**: October 21, 2025

