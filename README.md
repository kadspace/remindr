<p align="center">
  <img src="assets/wordmark-lockup.svg" alt="Remindr wordmark" width="460"/>
</p>

A fast reminder and note app where you type naturally and the app does the schedule parsing.

<p align="center">
  <img src="screenshots/main.png" alt="Remindr main view" width="300"/>
</p>

## Current Scope

- One-tap quick add for reminders and notes
- Expandable compact rows (active, completed, archived)
- Recurring schedules (daily/weekly/monthly/yearly)
- Calendar month view with day markers and date detail
- Local-first data with optional Groq parsing
- Notifications are intentionally out of scope for now

## Setup

1. Clone and open in Android Studio
2. Grab a free API key from [console.groq.com](https://console.groq.com/keys)
3. Build and run on Android

<p align="center">
  <img src="screenshots/settings.png" alt="Settings screen" width="300"/>
</p>

```bash
./gradlew :compose-multiplatform:sample:installDebug
```

Enter your API key in Settings and you're good to go.

## Status
Alpha, actively changing.
