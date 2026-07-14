# Security Audit

## Summary

The app is primarily a local Android application, and the current implementation does not expose a public backend API. This reduces some traditional server-side attack surfaces, but there are still important mobile-security considerations.

## Permission Review

The manifest requests several capabilities that should be intentionally justified:

- POST_NOTIFICATIONS for reminder alerts
- SCHEDULE_EXACT_ALARM and USE_EXACT_ALARM for precise reminders
- RECEIVE_BOOT_COMPLETED to reschedule reminders after reboot
- SYSTEM_ALERT_WINDOW for overlay reminders
- FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE for background reliability
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS for reminder reliability

### Notes

Overlay reminders and exact alarms are powerful features. They should remain opt-in and clearly explained to users.

## Data Handling

- Habit data is stored locally through Room.
- User preferences are stored with DataStore.
- The app supports profile photo selection and persistence of the selected URI.

### Observations

- The app uses Android backup and data extraction rules, which can increase data mobility.
- Because the app stores personal habit data locally, backup and restore behavior should be reviewed for privacy expectations.

## Network and Secrets

- The source tree includes Retrofit, OkHttp, Moshi, and Firebase dependencies.
- No verified remote API endpoints or backend client usage were found in the inspected source.
- The presence of these dependencies suggests either planned expansion or legacy scaffolding.

## Recommendations

1. Keep sensitive permissions behind clear user consent and explain their purpose in UI copy.
2. Review backup/restore behavior and avoid exporting sensitive data unless necessary.
3. If Firebase or remote services are introduced later, move all secrets and API configuration to secure Gradle or build-time handling.
4. Add regression tests for permission-sensitive flows and reminder behavior.
