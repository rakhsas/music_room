# MusicRoom

MusicRoom is a mobile application designed for collaborative playlist creation and music sharing. The app enables users to create playlists, vote on songs, and share music with friends in a seamless and intuitive way.

---

## 📁 Project Structure

The project follows a modular and layered architecture to ensure scalability, maintainability, and ease of understanding.

### Root Directory
- **`build.gradle`**: Project-level Gradle configuration.
- **`settings.gradle`**: Project settings.
- **`gradle.properties`**: Gradle properties for the project.

---

### 📁 `app/` - Main Application Module

#### 📁 `src/` - Source Code
- **📁 `main/`** - Main source set.
  - **📁 `java/com/example/musicroom/`** - Application code.
    - **📁 `data/`** - Data Layer.
      - **📁 `api/`** - Network services and API calls.
      - **📁 `models/`** - Data models.
      - **📁 `repositories/`** - Repository layer for data handling.
      - **📁 `local/`** - Local database and storage.
    - **📁 `di/`** - Dependency injection setup.
    - **📁 `domain/`** - Domain Layer.
      - **📁 `models/`** - Domain models.
      - **📁 `usecases/`** - Business logic and use cases.
    - **📁 `presentation/`** - UI and Presentation Layer.
      - **📁 `auth/`** - Authentication screens and flows.
      - **📁 `components/`** - Reusable UI components.
      - **📁 `home/`** - Home screen and voting functionality.
      - **📁 `playlist/`** - Playlist editor screens.
      - **📁 `profile/`** - User profile screens.
      - **📁 `theme/`** - App theme and styling resources.
    - **📁 `utils/`** - Utility classes and helpers.
  - **📁 `res/`** - Application resources (e.g., layouts, strings, and drawables).
  - **📄 `AndroidManifest.xml`** - Application manifest file.
- **📁 `test/`** - Unit tests for the application.

#### Other Files
- **📄 `build.gradle`** - Gradle configuration for the app module.
- **📄 `proguard-rules.pro`** - ProGuard rules for code obfuscation.

---

## 🛠️ Features

- **Collaborative Playlist Creation**: Create and manage playlists with friends.
- **Voting System**: Vote on songs to decide what plays next.
- **Authentication**: User authentication for personalized experiences.
- **User Profiles**: Manage user profiles and settings.
- **Reusable UI Components**: Modular and reusable components for consistent UI/UX.
- **Offline Support**: Local database for offline functionality.

---


## 📝 Contribution Guidelines

1. Fork the repository.
2. Create a new branch for your feature:
   ```bash
   git checkout -b feature-name
   ```
3. Commit your changes:
   ```bash
   git commit -m "Add feature-name"
   ```
4. Push to your forked repository:
   ```bash
   git push origin feature-name
   ```
5. Open a pull request.

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).


