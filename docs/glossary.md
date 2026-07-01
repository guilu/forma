# FORMA Glossary

## Account

The authenticated identity that owns personal data in FORMA.

## Body Composition

The set of measurements and derived metrics used to evaluate physical progress beyond body weight alone.

## Measurement

A timestamped body composition record. It may be manual or imported from an external provider.

## Fat Mass

Estimated kilograms of body fat. Usually derived from weight and body fat percentage.

## Lean Mass

Estimated non-fat body mass. Used to understand whether changes are mostly fat, muscle, water or noise.

## Training Session

A planned or completed workout. It can be running, strength or rest/recovery.

## Training Plan

A structured set of sessions over time.

## Nutrition Plan

A set of meals and macro targets adapted to day type: running, strength or rest.

## Meal Template

Reusable meal definition with ingredients, approximate macros and usage context.

## Product

A purchasable food item used by the shopping assistant. Product prices and URLs are editable estimates in the MVP.

## Shopping List

Generated weekly list of products and quantities required by a nutrition plan.

## Insight

An explainable recommendation derived from body, training, nutrition and integration data.

## External Provider

A third-party system such as Withings or Garmin that can provide measurements or activities.

## Synchronization

The process of importing external provider data into FORMA's normalized model.

## Source

Origin of a record, such as manual entry, Withings import or future provider import.

## Adapter

Infrastructure code that connects FORMA to external systems, databases, HTTP APIs or platform services.

## Port

An interface owned by the application/domain side and implemented by an adapter.

## Read Model

Data prepared for UI consumption. It should be simple to render and should not require duplicating business logic in the frontend.

## Command

A user or system action that changes state.

## Query

A read-only request for current or historical data.

## AI Context

Story-specific context that helps coding agents implement a ticket safely and consistently.
