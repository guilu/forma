package dev.diegobarrioh.forma.domain;

/**
 * Equipment a plan request says is available for training (ADR-015 decisions 6 and 11).
 *
 * <p>Not {@link Equipment}: that vocabulary is deliberately restricted to home-friendly gear for
 * {@link Exercise} ("machine/gym-only equipment is deliberately absent so the catalog matches what
 * is available at home") and has no value for a gym machine or cardio machine. This one carries
 * exactly the wizard's six checkboxes ({@code frontend/src/pages/onboarding/steps/
 * EquipmentStep.tsx}) mapped 1:1 to a machine vocabulary — the Spanish sentence never crosses the
 * boundary (ADR-015 decision 6):
 *
 * <pre>
 *   'Sin equipamiento (peso corporal)'  -&gt; BODYWEIGHT
 *   'Mancuernas'                        -&gt; DUMBBELLS
 *   'Barra y discos'                    -&gt; BARBELL
 *   'Bandas elásticas'                  -&gt; BANDS
 *   'Máquinas de gimnasio'              -&gt; MACHINES
 *   'Cinta o bicicleta estática'        -&gt; CARDIO_MACHINE
 * </pre>
 *
 * <p>Carried on {@code plan_request} from day one but unused by this slice: it is a training input,
 * and there is no training generator yet (ADR-015 decision 11).
 */
public enum TrainingEquipment {
  BODYWEIGHT,
  DUMBBELLS,
  BARBELL,
  BANDS,
  MACHINES,
  CARDIO_MACHINE
}
