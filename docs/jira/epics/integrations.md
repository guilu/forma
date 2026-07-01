# FOR-EPIC Integrations

## Goal

Connect Forma to external health and automation systems without leaking provider-specific details into the core domain.

## Business value

Automatic imports reduce manual entry and make weekly insights more reliable.

## Scope

- Withings OAuth
- Withings measurement import
- Future Strava/Garmin support
- Future Home Assistant support
- Integration status dashboard

## Initial stories

- FOR-060 Withings OAuth preparation
- FOR-061 Withings token storage
- FOR-062 Withings measurement sync
- FOR-063 Integration status page
- FOR-064 Future integration extension point

## Acceptance criteria

- Core Body model does not depend on Withings payloads.
- Tokens are stored securely.
- Imported measures are normalized into BodyMeasurement.
- Future integrations can follow the same pattern.
