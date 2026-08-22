# Aria — Stage 1 MVP

Mic button → Speech Recognition → AI reply → Text-to-Speech. That's it — this
is deliberately the smallest possible working slice, so we can verify the
pipeline works on your actual phone before adding personality, context,
Android actions, screen understanding, memory, task planning, and safety
on top of it.

## What's in this stage

- `MainActivity.kt` — requests mic permission, hosts the Compose screen
- `ui/AssistantScreen.kt` — dark UI with an animated orb (idle/listening/thinking/success/error)
- `ui/AssistantViewModel.kt` — owns state, wires mic → placeholder brain → TTS
- `voice/SpeechRecognizerManager.kt` — wraps Android's built-in `SpeechRecognizer`
- `voice/TextToSpeechManager.kt` — wraps Android's built-in `TextToSpeech`
- `core/AssistantState.kt` — the 6-state state machine (Idle/Listening/Thinking/Executing/Success/Error)

The "brain" in the ViewModel (`respondTo()`) is a placeholder — a few
if/else rules just so you can hear Aria talk back. Stage 2 replaces it with
real intent parsing and personality.

## How to build and run (from your phone or a computer)

1. **Get the project onto a machine with Android Studio.** The easiest way
   from a phone: install **Android Studio** on a laptop/desktop (it's not
   buildable purely on-phone), then transfer this folder there — e.g. upload
   the zip to Google Drive, then download it on the Android Studio machine.
   (If you only have a phone available, tools like an Android IDE app can
   sometimes open Gradle projects, but Android Studio is the reliable path.)

2. **Open the project.** Android Studio → Open → select the `AriaAssistant`
   folder. It will detect there's no Gradle wrapper jar and offer to
   generate one automatically — accept that prompt (or run
   `gradle wrapper` once if you have a system Gradle installed).

3. **Let Gradle sync.** First sync will download the Android Gradle Plugin,
   Kotlin, and Compose dependencies — needs internet access once.

4. **Connect your phone.**
   - Enable Developer Options: Settings → About phone → tap "Build number" 7 times.
   - Enable USB debugging: Settings → Developer options → USB debugging.
   - Plug in via USB (or set up wireless debugging).

5. **Run.** Click the green ▶ Run button in Android Studio, select your
   device. The app installs and launches.

6. **Grant mic permission** when prompted, then tap the orb and speak.

## Verified compatibility (as of this stage)

- Android Gradle Plugin 8.5.2 + Kotlin 1.9.24 + Compose compiler 1.5.14 —
  this is a known-good combination per the official Compose-Kotlin
  compatibility map.
- `compileSdk`/`targetSdk` 34, `minSdk` 26 (covers ~95%+ of active devices).

## Roadmap (next stages — say "next stage" when ready)

- **Stage 2:** Personality + emotion — replace the placeholder brain with
  a real response engine that has Aria's warm/expressive voice, in both
  English and Hinglish.
- **Stage 3:** Conversation context — follow-up commands ("open YouTube" →
  "search GTA videos").
- **Stage 4:** Real Android actions — calls, apps, brightness, alarms, etc.,
  each gated by the actual Android permission it needs.
- **Stage 5:** Accessibility/screen understanding — reading on-screen
  elements to act on "the second option," never blind coordinate-tapping.
- **Stage 6:** Local memory system (preferences, contacts, routines) with
  view/delete/clear-all controls.
- **Stage 7:** Task planner for multi-step commands.
- **Stage 8:** Risk classification (LOW/MEDIUM/HIGH) + confirmation flow
  for sensitive actions.
- **Stage 9:** Polished futuristic UI (particles/waveform).

Each stage will land as specific files to add/edit, with full code — not
a dump of everything at once.
