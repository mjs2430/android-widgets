# Sasso Widgets

An Android app (minSdk 31) that hosts a collection of customizable home screen widgets.

## Widgets

**Today**: a customizable take on Pixel's At a Glance.
- **Clock**: 12/24h, leading zero, AM/PM, date format, size, font. Tap opens alarms or any app you choose.
- **Next event**: reads the calendars you select, with a look-ahead window and an all-day toggle. Tap opens the event, your calendar, or any app.
- **Weather**: current temp, condition, high/low, hourly and daily strips, °F/°C. Uses your current location or a city you pick. Tap opens any weather app.
- **Style**: liquid glass, soft glass, transparent, solid, gradient or Material You, plus opacity, tint, corner radius, padding, text color and font.
- **Layout**: stacked, side by side or compact; left or centered; sections can be reordered and hidden.

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
  widgets/today/        the Today widget: config model, Glance content, sections,
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
