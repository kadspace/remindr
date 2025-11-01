# Remindr

Bare-bones Expo project scaffold for the Remindr app.

## Current prototype

- Capture lightweight reminders by entering a title and free-form "when" note.
- Jot standalone notes with optional titles for things that don't need a nudge.
- Entries are stored in-memory so you can focus on shaping the UX before wiring up persistence.

> **Note:** To keep this repository text-only for easier code review, no image
> assets are checked in. Expo will fall back to its defaults for icons and the
> splash screen. When you are ready to brand the app, add your own images
> locally and point to them from `app.json`.

## Getting started

1. Install dependencies:
   ```bash
   npm install
   ```
2. Run the development server:
   ```bash
   npm run start
   ```
3. Open the Expo Go app on your Android device and scan the QR code to preview the project.

> **Tip:** Expo Go reloads quickly. Make a change in `App.js`, save, and your device will refresh to show the updated reminder/note flow.

## Scripts

- `npm run start` – start the Expo dev server.
- `npm run android` – launch the project on an Android emulator/device.
- `npm run ios` – launch the project on an iOS simulator/device.
- `npm run web` – open the project in a web browser.
