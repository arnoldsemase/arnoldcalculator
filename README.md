# Arnold Calculator

Premium native Android calculator from the Arnold OS Apps ecosystem.
Kotlin · Jetpack Compose · Material 3 · MVVM · Room · DataStore · Navigation Compose. Offline, no backend.

## Get the APK
1. Push this folder to a GitHub repo (branch `main`).
2. Open the **Actions** tab → *Build Arnold Calculator APK* → download the `ArnoldCalculator-apk` artifact.
3. Install `ArnoldCalculator-release.apk` on your phone (allow installs from unknown sources).
   Tag a commit `v1.0.0` to also publish the APKs as a GitHub Release.

Or open the folder in Android Studio (Ladybug or newer) and press Run. Studio creates the Gradle wrapper for you.

## Structure
```
engine/      ExpressionEvaluator (parser, precedence, DEG/RAD, errors), NumberFormatter
data/        Room history, DataStore settings, repositories
ui/theme     Arnold brand tokens (dark + light)
ui/calculator  keypad, display, ViewModel
ui/history   grouped history with copy / use / edit / delete + undo
ui/settings  theme, haptics, number format, about
ui/navigation  NavHost + transitions
```

## Notes
- `app/src/test` has engine unit tests (`gradle :app:testDebugUnitTest`).
- The release build is signed with the debug key so it installs for sideloading. Add your own keystore before Google Play.
- Fonts: the site uses Poppins and Space Grotesk. Drop the .ttf files in `res/font` and point `ui/theme/Type.kt` at them.
