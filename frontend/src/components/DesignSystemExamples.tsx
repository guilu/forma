import { Badge } from './Badge';
import { Button, type ButtonVariant } from './Button';
import { Card } from './Card';
import { ChartContainer } from './ChartContainer';
import { Chip } from './Chip';
import { Icon } from './Icon';
import { IconButton } from './IconButton';
import { LineChart, type ChartPoint } from './LineChart';
import { MacroRing } from './MacroRing';
import { MetricCard } from './MetricCard';
import { SelectField, TextField } from './FormField';
import { StatusPill } from './StatusPill';

/**
 * Living usage/examples surface for the FOR-50 design system (open question:
 * "how much to formalize as a style guide"). A heavyweight tool like Storybook
 * would be overkill for the current MVP surface area (a handful of primitives),
 * so this renders every variant as plain React — cheap to keep in sync with the
 * components because it imports them directly, and easy to smoke-test.
 *
 * <p>Not wired into app routing/navigation on purpose: it is not a product
 * screen, and adding a route would touch the app shell (FOR-49/FOR-81), which
 * is out of scope for this story. Mount it ad hoc during development, or wire
 * a route to it later if the team wants a persistent internal page.
 */
/*
 * Ordered as the emphasis ladder, loudest first, because that ordering is the
 * documentation: the row itself shows which variant outranks which.
 */
const BUTTON_VARIANTS: ButtonVariant[] = [
  'primary',
  'accent',
  'soft',
  'secondary',
  'ghost',
  'destructive',
];

const SAMPLE_POINTS: ChartPoint[] = [
  { t: Date.parse('2026-07-01T08:00:00Z'), y: 75.1, dateLabel: '1 jul' },
  { t: Date.parse('2026-07-05T08:00:00Z'), y: 73.6, dateLabel: '5 jul' },
];

export function DesignSystemExamples() {
  return (
    <div>
      <h1>Design system</h1>

      <Card title="Buttons — acciones" headingLevel={2}>
        {BUTTON_VARIANTS.map((variant) => (
          <Button key={variant} variant={variant}>
            {variant}
          </Button>
        ))}
        <Button loading>Guardando…</Button>
        <Button disabled>Deshabilitado</Button>
        {/* The danger tone rides on the quiet variants; `destructive` above is
            the loud one, for a dialog's confirm. */}
        <Button variant="soft" tone="danger">
          soft · danger
        </Button>
        <Button variant="ghost" tone="danger">
          ghost · danger
        </Button>
      </Card>

      {/* The three families are shown as three cards rather than one row of
          controls, because their separation is the thing worth documenting: a
          selected Chip and an accent Button are near-identical to the eye and
          are chosen for entirely different reasons. */}
      <Card title="IconButton — acciones sin etiqueta" headingLevel={2}>
        <IconButton label="Notificaciones" variant="surface">
          <Icon name="bell" />
        </IconButton>
        <IconButton label="Editar" variant="soft">
          <Icon name="edit" />
        </IconButton>
        <IconButton label="Cerrar" variant="ghost">
          <Icon name="close" />
        </IconButton>
        <IconButton label="Eliminar" variant="ghost" tone="danger">
          <Icon name="trash" />
        </IconButton>
        <IconButton label="Anterior" size="sm" variant="ghost">
          <Icon name="chevron" size={16} />
        </IconButton>
        <IconButton label="Refrescar" disabled>
          <Icon name="refresh" />
        </IconButton>
      </Card>

      <Card title="Chip — selección, no acción" headingLevel={2}>
        <Chip selected>Seleccionado</Chip>
        <Chip selected={false}>Sin seleccionar</Chip>
        <Chip selected={false} disabled>
          Deshabilitado
        </Chip>
        {/* `sm` for a group living inside a card, next to the data it filters. */}
        <Chip size="sm" selected>
          30 días
        </Chip>
        <Chip size="sm" selected={false}>
          90 días
        </Chip>
      </Card>

      <Card title="Badges y estados" headingLevel={2}>
        <Badge tone="accent">Saludable</Badge>
        <StatusPill kind="severity" value="INFO" />
        <StatusPill kind="severity" value="WARNING" />
        <StatusPill kind="severity" value="ACTION" />
        <StatusPill kind="connection" value="Conectado" />
        <StatusPill kind="connection" value="No conectado" />
        <StatusPill kind="plazo" value="Corto plazo" />
        <StatusPill kind="plazo" value="Medio plazo" />
        <StatusPill kind="plazo" value="Largo plazo" />
      </Card>

      <Card title="Campos de formulario" headingLevel={2}>
        <TextField id="example-weight" label="Peso (kg)" value="" onChange={() => {}} />
        <TextField
          id="example-weight-error"
          label="Peso (kg)"
          value=""
          onChange={() => {}}
          error="Introduce un número válido."
        />
        <SelectField id="example-unit" label="Unidad" value="kg" onChange={() => {}}>
          <option value="kg">kg</option>
          <option value="lb">lb</option>
        </SelectField>
      </Card>

      <ChartContainer title="Evolución de peso" headingLevel={2}>
        <LineChart points={SAMPLE_POINTS} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>
      <ChartContainer title="Cargando" headingLevel={2} state="loading">
        <LineChart points={SAMPLE_POINTS} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>
      <ChartContainer title="Sin datos" headingLevel={2} state="empty">
        <LineChart points={SAMPLE_POINTS} formatValue={(v) => v.toFixed(1)} ariaLabel="Peso" />
      </ChartContainer>

      <Card title="Métricas" headingLevel={2}>
        <MetricCard
          label="Peso"
          value="82.4"
          unit="kg"
          trend={
            <LineChart
              points={SAMPLE_POINTS}
              formatValue={(v) => v.toFixed(1)}
              ariaLabel="Tendencia de peso"
            />
          }
        />
        <MetricCard label="Racha" value="12" unit="días" />
      </Card>

      <Card title="Distribución de macros" headingLevel={2}>
        <MacroRing proteinG={162} carbsG={236} fatG={68} />
      </Card>
    </div>
  );
}
