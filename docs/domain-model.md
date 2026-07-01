# Forma domain model

## Core bounded contexts

Forma is organized around six product areas:

```txt
Body
Training
Nutrition
Shopping
Insights
Integrations
```

## Body

Tracks body composition and measurement trends.

### BodyMeasurement

Represents one measurement event, either manually entered or imported from Withings.

Fields:

```txt
id
measuredAt
source: MANUAL | WITHINGS
weightKg
bodyFatPercentage
leanMassKg
bmi
muscleMassKg optional
waterPercentage optional
notes optional
```

Derived values:

```txt
fatMassKg = weightKg * bodyFatPercentage / 100
leanMassKg = weightKg - fatMassKg
```

## Training

Training includes running and strength.

### RunningSession

Represents an actual run.

Fields:

```txt
id
date
distanceKm
durationSeconds
averagePaceSecondsPerKm
averageHeartRate
calories
rpe
notes
```

### RunningPlanSession

Represents a planned run in a progression block.

Fields:

```txt
id
weekNumber
dayOfWeek
sessionType: EASY | INTERVALS | LONG_RUN | RECOVERY
targetDistanceKm
targetPaceRange
targetRpe
notes
```

### StrengthWorkout

Represents a planned or completed strength session.

Fields:

```txt
id
date
workoutType: PUSH | PULL | LEGS | FULL_BODY
status: PLANNED | COMPLETED
notes
```

### StrengthWorkoutItem

Fields:

```txt
id
workoutId
exerciseId
sets
repsMin
repsMax
weightKg
rir
restSeconds
order
```

### Exercise

Fields:

```txt
id
name
movementPattern: PUSH | PULL | SQUAT | HINGE | CORE | CARRY
primaryMuscles
equipment: DUMBBELL | BENCH | BAND | PULL_UP_BAR | BODYWEIGHT
instructions
```

## Nutrition

Nutrition is based on day templates, not rigid fixed menus.

### NutritionDayTemplate

Fields:

```txt
id
type: RUNNING | STRENGTH | REST
targetCalories
targetProteinG
targetCarbsG
targetFatG
notes
```

### MealTemplate

Fields:

```txt
id
dayTemplateId
mealType: BREAKFAST | MID_MORNING | LUNCH | PRE_WORKOUT | POST_WORKOUT | DINNER
name
preferredTime
notes
```

### MealItem

Fields:

```txt
id
mealTemplateId
foodItemId
quantityG
```

### FoodItem

Fields:

```txt
id
name
kcalPer100g
proteinPer100g
carbsPer100g
fatPer100g
defaultServingG
```

## Shopping

Shopping connects nutrition planning to real-world cost.

### MercadonaProduct

Fields:

```txt
id
name
url
packageSize
priceEur
pricePerUnit
lastCheckedAt
linkedFoodItemId optional
notes
```

### ShoppingItem

Fields:

```txt
id
weekStartDate
productId
quantity
estimatedCostEur
checked
```

## Insights

Insights transform raw measurements into weekly decisions.

### WeeklyCheckIn

Fields:

```txt
id
weekStartDate
weightKg
bodyFatPercentage
leanMassKg
weeklyKm
runningSessions
strengthSessions
averageSleepHours optional
notes
```

### Recommendation

Fields:

```txt
id
createdAt
category: NUTRITION | RUNNING | STRENGTH | RECOVERY | SHOPPING
severity: INFO | WARNING | ACTION
message
reason
```

Example rules:

```txt
If weight is stable and body fat decreases -> keep calories.
If weight drops >0.7% per week and strength worsens -> increase calories by 100-150 kcal.
If body fat increases for 3 weeks and activity is stable -> reduce calories by 100 kcal.
If running pace worsens and heart rate increases -> suggest recovery week.
```

## Integrations

### WithingsConnection

Fields:

```txt
id
userId
status
accessToken encrypted
refreshToken encrypted
tokenExpiresAt
lastSyncAt
```

The app should normalize Withings measures into BodyMeasurement and avoid leaking provider-specific details into Body or Insights.
