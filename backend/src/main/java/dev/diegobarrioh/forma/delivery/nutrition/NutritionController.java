package dev.diegobarrioh.forma.delivery.nutrition;

import dev.diegobarrioh.forma.application.HydrationService;
import dev.diegobarrioh.forma.application.MealLogService;
import dev.diegobarrioh.forma.application.NotFoundException;
import dev.diegobarrioh.forma.application.NutritionCalculationService;
import dev.diegobarrioh.forma.application.NutritionDayCatalogService;
import dev.diegobarrioh.forma.application.UserProfileService;
import dev.diegobarrioh.forma.delivery.ApiPaths;
import dev.diegobarrioh.forma.domain.NutritionDayType;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nutrition REST endpoint: {@code GET /api/v1/nutrition/days/{type}} (FOR-34, enriched FOR-105)
 * returns a seeded nutrition day (targets + ordered meals) for the given day type, with per-meal
 * and per-day macro totals and the target comparison. {@code POST /api/v1/nutrition/log} and {@code
 * GET /api/v1/nutrition/consumption} (FOR-127) log a consumed meal entry and read the day's
 * consumption vs plan target, reusing the same FOR-32 calculators — macros only.
 *
 * <p>Thin controller (ADR-001, ADR-005): maps service results to delivery read models, delegating
 * macro totals to {@link NutritionCalculationService}, meal-log use cases to {@link
 * MealLogService}, and hydration use cases to {@link HydrationService} (no business logic here).
 * {@code POST /nutrition/hydration} and {@code GET /nutrition/hydration} (FOR-130) log water intake
 * and read the hydration progress read model (total vs daily goal). An unknown day type yields
 * {@code NOT_FOUND} (404); an unknown meal type or invalid logging input yields {@code
 * VALIDATION_ERROR} (400), both via the FOR-27 {@code GlobalExceptionHandler}. Mounted under {@link
 * ApiPaths#V1}.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/nutrition")
public class NutritionController {

  private final NutritionDayCatalogService service;
  private final NutritionCalculationService calculationService;
  private final MealLogService mealLogService;
  private final HydrationService hydrationService;
  private final UserProfileService profileService;

  public NutritionController(
      NutritionDayCatalogService service,
      NutritionCalculationService calculationService,
      MealLogService mealLogService,
      HydrationService hydrationService,
      UserProfileService profileService) {
    this.service = service;
    this.calculationService = calculationService;
    this.mealLogService = mealLogService;
    this.hydrationService = hydrationService;
    this.profileService = profileService;
  }

  /**
   * Returns the seeded nutrition day for the given type (e.g. {@code running}). First-run gate
   * (FOR-169): before onboarding, the in-code day catalog is not exposed as the user's active plan
   * — the endpoint returns an empty day so the UI shows its "configure your plan" empty state.
   */
  @GetMapping("/days/{type}")
  public NutritionDayResponse day(@PathVariable String type) {
    NutritionDayType dayType = parseType(type);
    if (!profileService.firstRunCompleted()) {
      return NutritionDayResponse.empty(dayType);
    }
    return service
        .findByType(dayType)
        .map(day -> NutritionDayResponse.from(day, calculationService))
        .orElseThrow(() -> new NotFoundException("No existe el día de nutrición: " + type));
  }

  /** Logs a consumed meal entry (catalog food + portions, or free/ad-hoc macros) (FOR-127). */
  @PostMapping("/log")
  @ResponseStatus(HttpStatus.CREATED)
  public MealLogResponse log(@Valid @RequestBody LogMealRequest request) {
    return MealLogResponse.from(mealLogService.log(request.toCommand()));
  }

  /**
   * Day consumption read model: consumed macros vs plan target (FOR-127). Never 404s — an empty day
   * returns zeroed consumption.
   */
  @GetMapping("/consumption")
  public DayConsumptionResponse consumption(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return DayConsumptionResponse.from(mealLogService.consumption(date));
  }

  /** Logs a water-intake volume for a day (FOR-130). */
  @PostMapping("/hydration")
  @ResponseStatus(HttpStatus.CREATED)
  public WaterIntakeResponse logHydration(@Valid @RequestBody LogWaterIntakeRequest request) {
    return WaterIntakeResponse.from(hydrationService.log(request.toCommand()));
  }

  /**
   * Hydration progress read model: total logged volume vs daily goal (FOR-130). Never 404s — an
   * empty day returns a zeroed total.
   */
  @GetMapping("/hydration")
  public HydrationProgressResponse hydration(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return HydrationProgressResponse.from(hydrationService.hydrationProgress(date));
  }

  private static NutritionDayType parseType(String type) {
    try {
      return NutritionDayType.valueOf(type.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new NotFoundException("Tipo de día de nutrición desconocido: " + type);
    }
  }
}
