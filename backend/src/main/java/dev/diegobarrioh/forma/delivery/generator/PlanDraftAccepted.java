package dev.diegobarrioh.forma.delivery.generator;

/**
 * What the funnel is told when it finishes.
 *
 * <p>Deliberately thin. It echoes back the three things the success screen says out loud — where
 * the plan is going, how many calories it is built for and how many meals a day — and nothing else,
 * because nothing else has happened yet.
 *
 * <p>No plan id, and that absence is the honest part: no plan exists. The day one does, this record
 * grows an id and the success screen can link to it.
 *
 * @param email where the plan will be sent, once there is a plan and something to send it with
 * @param planKcal the daily requirement the funnel worked out
 * @param mealsPerDay how the day will be split
 */
public record PlanDraftAccepted(String email, int planKcal, int mealsPerDay) {}
