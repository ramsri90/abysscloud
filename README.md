# InfiniDrive 🌌

A highly secure, offline-first local file vault with limitless safe backup and restore capabilities via your own private Telegram Bot.

## Overview
InfiniDrive leverages the Telegram Bot API to give you virtually unlimited cloud storage. It acts as a local media and file vault with the ability to automatically back up your photos, videos, and files to a private Telegram chat. Keep your device's storage free and your data securely backed up in the cloud.

## Features
- **🚀 Unlimited Cloud Sync:** Uses Telegram's powerful file-sharing API to securely store large media files online.
- **📸 Auto-Backup Service:** Background synchronization using Android WorkManager to automatically back up new media from your Camera, and other folders.
- **📱 Restore Hub:** Fully featured Restore operations! Deleted a file locally? Download the original back from Telegram securely to your device's `Downloads` folder.
- **📁 Vault Organization:** Organize your documents, photos, and videos gracefully in custom folders.
- **🎨 Modern UI/UX:** Built entirely with Android Jetpack Compose featuring a beautiful, fluid "Cosmic Slate" dark neon theme.
- **🛡 Offline First:** Browse your cached local vault items, manage state, and queue uploads even without an internet connection.

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Local Database:** Room Database (Offline-first architecture)
- **Background Tasks:** Android WorkManager
- **Networking:** Retrofit & OkHttp (Multipart file uploads/streaming downloads)
- **Asynchronous Flow:** Kotlin Coroutines & Flow

## Setup Instructions

To sync your files on the cloud, you need a Telegram Bot and a private destination (channel/chat).

1. **Create a Telegram Bot**:
   - Search for and message **`@BotFather`** on Telegram.
   - Send the `/newbot` command and follow the instructions to create a new bot.
   - Copy the provided **Bot Token**.

2. **Get your Chat ID**:
   - Create a Private Telegram Channel or Group to act as your secure vault.
   - Add your newly created Bot to the channel/group and make it an admin (so it can post files).
   - Forward a message from that channel to `@RawDataBot` (or use Telegram Web API) to find your **Chat ID** (usually starts with `-100`).

3. **Configure in App**:
   - Open **InfiniDrive** on your Android device.
   - Tap the Settings (key/gear icon) in the top bar.
   - Enter your `Bot Token` and `Chat ID`.
   - Your unlimited cloud storage is ready to use!

## License

This project is licensed under the MIT License.
