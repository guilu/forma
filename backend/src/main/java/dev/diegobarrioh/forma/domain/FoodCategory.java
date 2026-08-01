package dev.diegobarrioh.forma.domain;

/**
 * What kind of food a catalog entry is (FOR-190), from the Macros sheet's own "Categoría" column
 * (docs/fitness_os.xlsm).
 *
 * <p>The sheet's Spanish terms, uppercased and accent-stripped so the stored value is a stable
 * identifier rather than display copy — the UI renders its own labels. Closed on purpose: it is a
 * shelf, not a tag cloud, and the meal planner will reason over these buckets.
 *
 * <p>A food's category is optional at the persistence layer: one nobody has classified yet is a
 * real state, and defaulting it into some bucket would be inventing a fact (FOR-134).
 */
public enum FoodCategory {
  CARBOHIDRATO,
  PROTEINA,
  FRUTA,
  VERDURA,
  GRASA,
  LACTEO
}
