<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Logo" width="100"/>
  <h1>Termux AI Agent</h1>
  <p>
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Termux-000000?style=for-the-badge&logo=gnu-bash&logoColor=white" alt="Termux" />
    <img src="https://img.shields.io/github/v/release/aciderix/termux-ai-agent?style=for-the-badge" alt="Release" />
  </p>
</div>

## 🇬🇧 English

**Termux AI Agent** is a multimodal AI assistant application powered by the [1min.ai API](https://1min.ai) and closely integrated with [Termux](https://termux.dev). 
It allows you to converse with various advanced AI models (Qwen, Claude, DeepSeek, Gemini, GPT, Mistral, Grok) and grants the AI the ability to autonomously execute shell commands on your Android device via Termux.

### 📑 Table of Contents
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)

### ✨ Features
- **Multimodal AI Models**: Choose from over 30 top-tier AI models directly from the UI.
- **Termux Integration**: The AI can execute bash commands on your device in the background and read the outputs (stdout, stderr).
- **Automated Setup Check**: The app automatically detects if Termux and its API are installed and configured correctly, guiding you if they aren't.

### 🛠 Prerequisites
- An Android device.
- [Termux](https://f-droid.org/packages/com.termux/) and [Termux:API](https://f-droid.org/packages/com.termux.api/) installed from F-Droid.
- A [1min.ai](https://1min.ai) API key.

### 🚀 Installation
1. Go to the [Releases](../../releases) tab.
2. Download the latest `termux-ai-agent-vX.X.X.apk`.
3. Install the APK on your Android device (ensure "Install from unknown sources" is enabled).

### ⚙️ Configuration
For the app to communicate with Termux, you must allow external apps in Termux:
```bash
mkdir -p ~/.termux && echo "allow-external-apps = true" >> ~/.termux/termux.properties && termux-reload-settings
```
*(The app will also guide you through this process upon first launch).*

---

## 🇫🇷 Français

**Termux AI Agent** est une application d'assistant IA multimodale propulsée par l'[API 1min.ai](https://1min.ai) et intimement liée à [Termux](https://termux.dev). 
Elle vous permet de discuter avec de multiples modèles d'IA avancés (Qwen, Claude, DeepSeek, Gemini, GPT, Mistral, Grok) et donne à l'IA la capacité d'exécuter des commandes shell de manière autonome sur votre appareil via Termux.

### 📑 Sommaire
- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Installation](#installation-1)
- [Configuration](#configuration-1)

### ✨ Fonctionnalités
- **Modèles IA Multimodaux** : Choisissez parmi plus de 30 modèles d'IA de pointe.
- **Intégration Termux** : L'IA peut exécuter des commandes bash en arrière-plan et analyser les résultats (stdout, stderr).
- **Vérification Automatique** : L'application détecte la présence de Termux et vous guide pour la configuration système nécessaire.

### 🛠 Prérequis
- Un appareil Android.
- [Termux](https://f-droid.org/packages/com.termux/) et [Termux:API](https://f-droid.org/packages/com.termux.api/) installés depuis F-Droid.
- Une clé API [1min.ai](https://1min.ai).

### 🚀 Installation
1. Allez dans l'onglet [Releases](../../releases).
2. Téléchargez le dernier fichier `termux-ai-agent-vX.X.X.apk`.
3. Installez l'APK sur votre appareil (assurez-vous d'avoir autorisé l'installation de sources inconnues).

### ⚙️ Configuration
Pour que l'application puisse communiquer avec Termux, vous devez autoriser les applications externes dans Termux avec la commande suivante :
```bash
mkdir -p ~/.termux && echo "allow-external-apps = true" >> ~/.termux/termux.properties && termux-reload-settings
```
*(L'application vous guidera dans cette étape lors du premier lancement).*
