# KuroMusic

<div align="center">
  <img src="https://raw.githubusercontent.com/KuroMusic/KuroMusic/main/assets/BannerKuro.png" alt="KuroMusic Banner" width="100%"/>
  
### Cliente Avanzado Music Pro con Material Design 3 para Android (Negro/Morado)

  [![Android Version](https://img.shields.io/badge/Android-6.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![License](https://img.shields.io/badge/License-GPLv3-000000?style=for-the-badge&logo=gnu-bash&logoColor=white)](LICENSE)
  [![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=github-actions&logoColor=white)](https://github.com/KuroMusic/KuroMusic/actions)
  [![Downloads](https://img.shields.io/github/downloads/KuroMusic/KuroMusic/total?style=for-the-badge&logo=github&color=purple)](https://github.com/KuroMusic/KuroMusic/releases)
  
  [![Telegram](https://img.shields.io/badge/Telegram-Join%20Chat-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/KiritoAPT2)

</div>

---

## 📖 About

**KuroMusic** es un cliente open-source de nueva generación para Music Pro en Android. Diseñado meticulosamente con **Material Design 3**, ofrece una interfaz inmersiva en tonos **Negro Puro (#000000)** y **Morado Vibrante (#7B1FA2)**.

Nuestro objetivo es proporcionar una experiencia musical premium, sin anuncios, con privacidad total y funcionalidades avanzadas que no encontrarás en clientes estándar. KuroMusic no está afiliado a Google LLC.

---

## 🏗️ Architecture

KuroMusic sigue los principios de **Clean Architecture** y **MVVM (Model-View-ViewModel)**, asegurando un código modular, escalable y fácil de mantener.

* **UI Layer**: Jetpack Compose con Material 3.
* **Domain Layer**: Casos de uso y lógica de negocio pura.
* **Data Layer**: Repositorio único, fuentes de datos locales (Room) y remotas (InnerTune).

---

## 🛠️ Tech Stack & Dependencies

El proyecto utiliza las últimas tecnologías de desarrollo Android moderno.

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Language** | ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) | Lenguaje principal del proyecto. |
| **UI** | ![Compose](https://img.shields.io/badge/-Jetpack%20Compose-4285F4?style=flat-square&logo=android&logoColor=white) | Toolkit moderno para UI nativa. |
| **Multimedia** | ![Media3](https://img.shields.io/badge/-Media3%20ExoPlayer-000000?style=flat-square) | Reproducción de audio robusta y eficiente. |
| **Database** | ![Room](https://img.shields.io/badge/-Room%20Database-42A5F5?style=flat-square&logo=sqlite&logoColor=white) | Persistencia de datos local robusta. |
| **Injection** | ![Hilt](https://img.shields.io/badge/-Dagger%20Hilt-795548?style=flat-square&logo=google&logoColor=white) | Inyección de dependencias estándar. |
| **Image Loading** | ![Coil](https://img.shields.io/badge/-Coil-000000?style=flat-square) | Carga de imágenes rápida y ligera. |

---

## ✨ Key Features

### 🎵 Core Experience

* **Ad-Free**: Disfruta de tu música sin interrupciones ni anuncios.
* **Background Playback**: Reproducción continua en segundo plano y pantalla bloqueada.
* **Offline Mode**: Descarga tus canciones favoritas para escuchar sin conexión.
* **High Quality Audio**: Soporte para streaming de alta fidelidad.

### 🎨 Visual & Customization

* **Dynamic Theming**: La interfaz se adapta a los colores de la carátula del álbum.
* **Pure Black Mode**: Tema optimizado para pantallas OLED.
* **Lyrics en Tiempo Real**: Sigue la letra de tus canciones sincronizadas perfectamente.

### 🚀 Advanced Tools

* **Silence Skip**: Salta automáticamente los silencios en las canciones.
* **Volume Normalization**: Audio consistente en todas las pistas.
* **Android Auto**: Compatible para una experiencia segura mientras conduces.

## 📱 Galería Visual

<p align="center">
  <img src="screenshots/home.png" width="30%" alt="Pantalla de Inicio" />
  <img src="screenshots/player.png" width="30%" alt="Reproductor" />
  <img src="screenshots/settings.png" width="30%" alt="Ajustes" />
</p>

---

## 📥 Installation

| Requirement | Details |
| :--- | :--- |
| **OS** | Android 6.0 (Marshmallow) o superior |
| **Architecture** | Universal (ARMv7, ARM64, x86) |
| **Space** | ~20 MB de espacio libre |

### 🚀 Quick Start

1. Descarga el último **APK** desde la sección de [Releases](https://github.com/KuroMusic/KuroMusic/releases).
2. Habilita "Instalar de orígenes desconocidos" en tu dispositivo Android si es necesario.
3. Instala el APK y disfruta de la música.

```bash
# Para desarrolladores: Clonar y compilar
git clone https://github.com/KuroMusic/KuroMusic.git
cd KuroMusic
./gradlew assembleRelease
```

---

## 🛡️ Security & Privacy

Nos tomamos la seguridad en serio. Consulta nuestra [Política de Seguridad](SECURITY.md) para más detalles.

> [!NOTE]
> Al ser una aplicación de código abierto no firmada por una gran entidad, Play Protect puede mostrar una advertencia. El código es 100% auditable y seguro.

---

## 🤝 Community & Support

Únete a nuestra creciente comunidad.

* 💬 **Telegram**: [t.me/KiritoAPT2](https://t.me/KiritoAPT2) - Soporte directo y chat.
* 🐛 **Issues**: Reporta bugs o sugiere mejoras en [GitHub Issues](https://github.com/KuroMusic/KuroMusic/issues).
* 📜 **Code of Conduct**: Revisa nuestro [Código de Conducta](CODE_OF_CONDUCT.md).

---

## 👥 Credits

| User | Role |
| :--- | :--- |
| **KiritoAPT2** | Lead Developer 👨‍💻 |
| **TeamAnimax** | Agradecimiento de apoyo � |

---

## 📜 License

Este proyecto está licenciado bajo la **GNU General Public License v3.0**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

Copyright © 2026 KuroMusic
