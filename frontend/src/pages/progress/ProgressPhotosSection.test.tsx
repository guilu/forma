import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProgressPhotosSection } from './ProgressPhotosSection';
import { NotificationProvider } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import {
  deleteProgressPhoto,
  fetchProgressPhotoBlob,
  listProgressPhotos,
  uploadProgressPhoto,
  type ProgressPhoto,
} from '../../api/progress';

/**
 * Progress-photos gallery/upload section tests (FOR-144). `useNotify()`
 * requires a provider (mirrors `IntegrationsSection.test.tsx`'s FOR-123
 * precedent). No real network or binary I/O: `../../api/progress` is mocked
 * wholesale, and `URL.createObjectURL`/`revokeObjectURL` are polyfilled in
 * `src/test/setup.ts` to a deterministic `blob:mock-N` stub.
 */
function renderSection() {
  return render(
    <NotificationProvider>
      <ProgressPhotosSection />
    </NotificationProvider>,
  );
}

vi.mock('../../api/progress', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/progress')>();
  return {
    ...actual,
    listProgressPhotos: vi.fn(),
    uploadProgressPhoto: vi.fn(),
    deleteProgressPhoto: vi.fn(),
    fetchProgressPhotoBlob: vi.fn(),
  };
});

const listMock = vi.mocked(listProgressPhotos);
const uploadMock = vi.mocked(uploadProgressPhoto);
const deleteMock = vi.mocked(deleteProgressPhoto);
const blobMock = vi.mocked(fetchProgressPhotoBlob);

const photo1: ProgressPhoto = {
  id: 'p1',
  contentType: 'image/jpeg',
  sizeBytes: 20480,
  createdAt: '2026-05-08T08:00:00Z',
};
const photo2: ProgressPhoto = {
  id: 'p2',
  contentType: 'image/png',
  sizeBytes: 10240,
  createdAt: '2026-06-08T08:00:00Z',
};

describe('ProgressPhotosSection', () => {
  beforeEach(() => {
    listMock.mockReset();
    uploadMock.mockReset();
    deleteMock.mockReset();
    blobMock.mockReset();
    blobMock.mockResolvedValue(new Blob(['fake-bytes'], { type: 'image/jpeg' }));
  });

  it('shows a loading skeleton while the list request resolves', () => {
    listMock.mockReturnValue(new Promise(() => {}));

    renderSection();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando fotos de progreso');
  });

  it('shows a calm empty state when the owner has no photos yet (normal, not an error)', async () => {
    listMock.mockResolvedValue([]);

    renderSection();

    expect(await screen.findByText('Aún no tienes fotos de progreso.')).toBeInTheDocument();
  });

  it('shows an error state with retry when the list request fails', async () => {
    listMock.mockRejectedValue(new Error('network'));

    renderSection();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus fotos de progreso',
    );

    listMock.mockResolvedValue([]);
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Aún no tienes fotos de progreso.')).toBeInTheDocument();
    expect(listMock).toHaveBeenCalledTimes(2);
  });

  it('renders a thumbnail per photo, fetched via the authenticated binary endpoint as an object URL', async () => {
    listMock.mockResolvedValue([photo1, photo2]);

    renderSection();

    const images = await screen.findAllByRole('img');
    expect(images).toHaveLength(2);
    expect(blobMock).toHaveBeenCalledWith('p1');
    expect(blobMock).toHaveBeenCalledWith('p2');
    // Never a public/static URL — always the mocked object URL.
    for (const img of images) {
      expect(img.getAttribute('src')).toMatch(/^blob:mock-/);
    }
  });

  it('shows a per-thumbnail error when a photo binary fails to load, without failing the whole gallery', async () => {
    listMock.mockResolvedValue([photo1]);
    blobMock.mockRejectedValue(new Error('boom'));

    renderSection();

    expect(await screen.findByText('No se pudo cargar esta foto.')).toBeInTheDocument();
  });

  it('uploads the selected file and prepends it to the gallery', async () => {
    listMock.mockResolvedValue([photo1]);
    const uploaded: ProgressPhoto = {
      id: 'p3',
      contentType: 'image/png',
      sizeBytes: 500,
      createdAt: '2026-07-18T09:00:00Z',
    };
    uploadMock.mockResolvedValue(uploaded);

    renderSection();
    await screen.findAllByRole('img');

    const file = new File(['bytes'], 'photo.png', { type: 'image/png' });
    const input = screen.getByLabelText('Subir foto de progreso') as HTMLInputElement;
    await userEvent.upload(input, file);

    expect(uploadMock).toHaveBeenCalledWith(file);
    await waitFor(() => expect(screen.getAllByRole('img')).toHaveLength(2));
  });

  it('restricts the file picker to jpeg/png via the accept attribute', async () => {
    listMock.mockResolvedValue([]);

    renderSection();
    await screen.findByText('Aún no tienes fotos de progreso.');

    expect(screen.getByLabelText('Subir foto de progreso')).toHaveAttribute(
      'accept',
      'image/jpeg,image/png',
    );
  });

  it('surfaces the backend 400 VALIDATION_ERROR message on an oversized/wrong-type upload', async () => {
    listMock.mockResolvedValue([]);
    uploadMock.mockRejectedValue(
      new ApiRequestError(400, 'El archivo supera el tamaño máximo de 5 MB.', 'VALIDATION_ERROR'),
    );

    renderSection();
    await screen.findByText('Aún no tienes fotos de progreso.');

    const file = new File(['bytes'], 'big.jpg', { type: 'image/jpeg' });
    const input = screen.getByLabelText('Subir foto de progreso') as HTMLInputElement;
    await userEvent.upload(input, file);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El archivo supera el tamaño máximo de 5 MB.',
    );
  });

  it('deletes a photo after explicit confirmation and removes it from the gallery', async () => {
    listMock.mockResolvedValue([photo1, photo2]);
    deleteMock.mockResolvedValue(undefined);

    renderSection();
    await screen.findAllByRole('img');

    const dateLabel = new Date(photo1.createdAt).toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
    await userEvent.click(screen.getByRole('button', { name: `Eliminar foto del ${dateLabel}` }));

    expect(
      await screen.findByRole('heading', { name: 'Eliminar foto de progreso' }),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('p1'));
    await waitFor(() => expect(screen.getAllByRole('img')).toHaveLength(1));
  });

  it('cancelling the delete confirmation never calls deleteProgressPhoto', async () => {
    listMock.mockResolvedValue([photo1]);

    renderSection();
    await screen.findAllByRole('img');

    const dateLabel = new Date(photo1.createdAt).toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
    await userEvent.click(screen.getByRole('button', { name: `Eliminar foto del ${dateLabel}` }));
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(deleteMock).not.toHaveBeenCalled();
  });

  it('surfaces an error message when delete fails', async () => {
    listMock.mockResolvedValue([photo1]);
    deleteMock.mockRejectedValue(new Error('boom'));

    renderSection();
    await screen.findAllByRole('img');

    const dateLabel = new Date(photo1.createdAt).toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
    await userEvent.click(screen.getByRole('button', { name: `Eliminar foto del ${dateLabel}` }));
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo eliminar la foto.');
  });

  it('revokes the object URL when a thumbnail unmounts (e.g. after delete), avoiding leaks', async () => {
    listMock.mockResolvedValue([photo1]);
    deleteMock.mockResolvedValue(undefined);
    const revokeSpy = vi.spyOn(URL, 'revokeObjectURL');

    renderSection();
    await screen.findAllByRole('img');

    const dateLabel = new Date(photo1.createdAt).toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
    await userEvent.click(screen.getByRole('button', { name: `Eliminar foto del ${dateLabel}` }));
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }));

    await waitFor(() => expect(screen.queryAllByRole('img')).toHaveLength(0));
    expect(revokeSpy).toHaveBeenCalled();
  });
});
