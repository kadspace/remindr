# Remindr

AI-powered calendar that automatically categorizes and schedules your notes.

## Core Concept

**AI-first input** - Type naturally, AI handles the rest. No date pickers, no category dropdowns, no friction.

The app uses Groq's free LLM API to analyze everything you type and automatically:
- Extract dates and times ("tomorrow at 3pm", "next Monday", "Jan 15")
- Categorize by type (work, personal, health, etc.) with intelligent color coding
- Determine urgency (critical reminder vs casual note)
- Set up recurring schedules ("every Monday", "monthly")
- Configure notification timing

## Visual System

**Lines** (critical) - Bills, appointments, deadlines. Things with consequences if missed.
**Badges** (regular) - Shopping lists, ideas, notes. Quick captures for your brain dump.

Days show up to 3 horizontal lines at the bottom (gray base fills in with color as you add critical tasks) and up to 4 circular badges below that for regular notes. Visual hierarchy at a glance.

## How It Works

1. **Type anything** in the persistent input bar: "pay rent tomorrow", "dentist Friday 2pm", "buy eggs"
2. **AI analyzes** your text and extracts all the details
3. **Confirm or edit** - AI's smart but you have final say
4. **Done** - Your note appears on the calendar, categorized and color-coded

The AI decides whether something is a critical reminder (line) or casual note (badge) based on context. Bills, appointments, and deadlines become lines. Shopping lists and random thoughts become badges.

## Features

- **AI-powered parsing** - Groq API (free tier) handles natural language understanding
- **Zero-friction input** - No forms, no dropdowns, just type
- **Smart categorization** - 5 color-coded categories with AI-suggested assignments (editable)
- **Critical vs casual** - AI determines urgency, you can override
- **Date extraction** - "tomorrow", "next week", "Jan 15" all work
- **Recurring reminders** - Daily, weekly, monthly patterns
- **Multi-offset notifications** - Remind at time, 10m before, 1h before, etc.
- **Nag mode** - Keeps reminding until you mark it done
- **The Dump** - Queue for quick captures that haven't been scheduled yet
- **Swappable palettes** - Rosy Copper (default), Ocean, Sunset, Mono, Original
- **Fully offline** - AI is optional, manual entry always works

## Examples

```
"pay rent tomorrow"          → Critical line, tomorrow's date, red color
"dentist appointment friday 2pm" → Critical line, this Friday at 2pm
"buy eggs milk bread"        → Badge, today's date
"workout every monday 6am"   → Critical line, weekly recurrence
"call mom"                   → Badge, today
"property tax due jan 15"    → Critical line, Jan 15, financial category
```

## Tech Stack

- **Groq API** - llama-3.1-8b-instant model for natural language parsing
- **Kotlin Multiplatform** - Shared codebase (Android, Desktop ready)
- **Compose Multiplatform** - Reactive UI
- **SQLDelight** - Type-safe database with Flow-based queries
- **Ktor** - HTTP client for API calls
- **Kizitonwose Calendar** - Calendar grid rendering library

## Setup

1. Clone the repo
2. Open in Android Studio or IntelliJ IDEA
3. **Get a free Groq API key** from [console.groq.com/keys](https://console.groq.com/keys)
   - Free tier: 14,400 requests/day (more than enough)
   - No credit card required
4. Build and run

```bash
./gradlew :compose-multiplatform:sample:installDebug
```

5. In the app: Go to Settings → Enter your API key → Start using AI features

## AI Categorization Logic

The AI prompt instructs Groq to classify based on these rules:

**HIGH severity (horizontal lines):**
- Bills, payments, rent, financial obligations
- Important appointments (doctor, dentist, meetings)
- Birthdays, anniversaries
- Recurring obligations, deadlines
- Anything with consequences if missed

**MEDIUM severity (circle badges):**
- Shopping lists, groceries
- Quick notes, ideas, random thoughts
- Words or things to remember
- General todos without hard deadlines
- "Dump" items - quick captures

**Color assignment:**
AI suggests colors based on context (work = red/copper, health = teal, personal = sage, etc.). You can customize category names in Settings and override AI's choices when editing.

## Project Structure

```
Calendar/compose-multiplatform/sample/
├── src/
│   ├── androidMain/     # Android-specific (notifications, receivers)
│   ├── commonMain/      # Shared code
│   │   ├── kotlin/
│   │   │   ├── CalendarApp.kt        # Main UI
│   │   │   ├── SettingsScreen.kt     # Settings UI
│   │   │   ├── AIService.kt          # Groq API integration
│   │   │   ├── NoteDbHelper.kt       # Database layer
│   │   │   ├── CalendarNote.kt       # Data models
│   │   │   ├── Colors.kt             # Palette system
│   │   │   └── Theme.kt              # Material theme
│   │   └── sqldelight/
│   │       └── db/
│   │           ├── Note.sq           # Main note schema
│   │           ├── Queue.sq          # Quick notes queue
│   │           └── KeyValueStore.sq  # Settings storage
```

## Color Palette System

Change palettes in `Colors.kt`:

```kotlin
val activePalette = Palettes.rosyCopper  // or .ocean, .sunset, .mono, .original
```

Each palette defines:
- 5 note colors
- Background color
- Surface color
- Accent colors (used for UI elements)

## Database Schema

**Note** - Main reminders table
- `id`, `date`, `time`, `title`, `description`, `end_date`
- `color`, `severity` (HIGH = line, MEDIUM = badge)
- `recurrence_type`, `recurrence_rule`
- `reminder_offsets`, `nag_enabled`, `last_completed_at`, `snoozed_until`

**QueueNote** - Unscheduled notes
- `id`, `text`, `created_at`

**KeyValueStore** - Settings
- `gemini_api_key` (Groq key)
- `event_type_labels` (custom category names, stored as `|||` delimited)

## Version History

- **v1.7.3** (Jan 18, 2025) - Editable event types, new icon
- **v1.7.2** (Jan 18, 2025) - Badge/line system, color palettes, AI categorization
- **v1.7.1** (Jan 18, 2025) - Palette permeation, back button fix
- **v1.7.0** (Jan 17, 2025) - Multiplatform conversion

## Status

Alpha. Things work but expect rough edges.

## License

Check the calendar library license in `Calendar/LICENSE.md` (Apache 2.0).
