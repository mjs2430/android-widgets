# Sasso Widgets

An Android app (minSdk 31) that hosts a collection of customizable home screen widgets.

## Widgets

**Today**: a customizable take on Pixel's At a Glance.
- **Clock**: 12/24h, leading zero, AM/PM, date format, font; sized automatically to fill its space (or a fixed size). Tap opens alarms or any app you choose.
- **Events**: upcoming events from the calendars you select, as a list one event tall: swipe it up and down to see the next ones, and tap an event to open it.
- **Weather**: place, condition, temperature, high/low, and an hourly (every 2 hours, from the next full hour) or daily forecast (tap the forecast to switch). Uses your current location or a city you pick.
- **Style**: liquid glass, soft glass, transparent, solid, gradient or Material You, plus opacity, tint, corner radius, padding, text color and font.
- **Layouts**: each one fixes placement and alignment.
  - *Split*: clock and today's weather share a row; events and the forecast sit together in a row below (best at 4×2 or taller).
  - *Stacked*: large centered clock over the weather, events on their own card (best at 4×3).
  - *Compact*: clock and today's weather (right-aligned) in one row, events underneath, no forecast.

  Content scales and drops to fit the widget's size rather than being clipped.

## Project layout

```
app/src/main/java/com/sassoapps/widgets/
  WidgetsApp.kt         AppContainer: app-wide singletons (no DI framework)
  WidgetCatalog.kt      list of widgets shown in the app; add new widgets here
  MainActivity.kt       catalog, permissions, about
  core/                 shared by every widget
    config/             per-widget settings stored as JSON in DataStore
    style/              WidgetStyle, theme palettes, glass/gradient background renderer
    apps/               "what opens on tap" (LaunchTarget) and the installed-apps list
    calendar/           CalendarContract queries, next-event selection and formatting
    weather/            WeatherProvider interface, MET Norway implementation, cache
    location/           device location (foreground only) and city search
    ui/                 shared Compose settings rows, app picker, live Glance preview
  widgets/today/        the Today widget: config model, layouts (TodayContent), sizing
                        (TodayMetrics), on-widget taps (TodayUiState), sections,
                        settings screen, refresh scheduling
```

To add a widget, create `widgets/<name>/` with a `GlanceAppWidget`, a receiver, a config model and a settings activity. Then register it in the manifest and in `WidgetCatalog`.

## How data stays fresh

- **Clock**: Android `TextClock`, which ticks by itself and never wakes the app.
- **Calendar**: a WorkManager content-URI trigger redraws when calendars change, and an inexact alarm redraws when the next event starts or ends, or at midnight.
- **Weather**: hourly WorkManager refresh. The cache honors MET Norway's `Expires` header, and failed fetches back off for 15 minutes.

## Weather data

Forecasts come from [MET Norway](https://api.met.no/), which is free and allows commercial use with attribution (CC BY 4.0). Their terms require an identifying User-Agent. Before release, add a contact to `~/.gradle/gradle.properties`:

```
weatherContact=you@example.com
```

To switch providers, implement `WeatherProvider` and swap it in `AppContainer`.

## Build

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

## Before publishing

- Replace the placeholder launcher icon.
- Write a privacy policy. Calendar data stays on the device; rounded coordinates go to MET Norway.
- Set `weatherContact` and configure release signing.
