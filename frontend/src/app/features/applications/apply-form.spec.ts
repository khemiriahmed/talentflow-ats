import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ApplyForm } from './apply-form';

const pdf = (bytes = 2000, name = 'cv.pdf') =>
  new File(['%PDF-1.4\n', new Uint8Array(bytes)], name, { type: 'application/pdf' });

describe('ApplyForm', () => {
  let fixture: ComponentFixture<ApplyForm>;
  let http: HttpTestingController;
  let root: HTMLElement;

  // accept() est protégée : on la déclenche comme le ferait le navigateur après un choix de fichier.
  const choose = async (file: File) => {
    await (fixture.componentInstance as unknown as { accept(f: File): Promise<void> }).accept(file);
    await fixture.whenStable();
  };
  const submit = async () => {
    root.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ApplyForm);
    fixture.componentRef.setInput('jobId', 5);
    await fixture.whenStable();
    root = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  it('asks for a CV before sending anything', async () => {
    await submit();
    http.expectNone('/api/v1/jobs/5/applications');
    expect(root.querySelector('.error')?.textContent).toContain('Ajoutez votre CV');
  });

  it('refuses a file that is not a PDF, whatever its name says', async () => {
    await choose(new File(['just some text'], 'cv.pdf', { type: 'application/pdf' }));
    expect(root.querySelector('.error')?.textContent).toContain('n’est pas un PDF');
    expect(root.querySelector('.file-name')).toBeNull();
  });

  it('refuses a file over 5 MB', async () => {
    await choose(pdf(5 * 1024 * 1024 + 1, 'gros.pdf'));
    expect(root.querySelector('.error')?.textContent).toContain('dépasse 5 Mo');
  });

  it('sends the CV, the cover letter and the source as multipart form data', async () => {
    await choose(pdf());
    expect(root.querySelector('.file-name')?.textContent).toContain('cv.pdf');

    const letter = root.querySelector<HTMLTextAreaElement>('#cover-letter')!;
    letter.value = '  Bonjour, ce poste m’intéresse.  ';
    letter.dispatchEvent(new Event('input'));
    const source = root.querySelector<HTMLSelectElement>('#source')!;
    source.value = 'linkedin';
    source.dispatchEvent(new Event('change'));
    await submit();

    const request = http.expectOne('/api/v1/jobs/5/applications');
    const body = request.request.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect((body.get('cv') as File).name).toBe('cv.pdf');
    expect(body.get('coverLetter')).toBe('Bonjour, ce poste m’intéresse.');
    expect(body.get('source')).toBe('linkedin');
    request.flush({ id: 1 }, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(root.querySelector('.success')?.textContent).toContain('Candidature envoyée');
  });

  it('omits the optional fields when they are empty', async () => {
    await choose(pdf());
    await submit();
    const request = http.expectOne('/api/v1/jobs/5/applications');
    const body = request.request.body as FormData;
    expect(body.has('coverLetter')).toBe(false);
    expect(body.has('source')).toBe(false);
    request.flush({}, { status: 201, statusText: 'Created' });
  });

  it('explains what to do when the candidate already applied', async () => {
    await choose(pdf());
    await submit();
    http.expectOne('/api/v1/jobs/5/applications').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(root.querySelector('.alert')?.textContent).toContain('déjà postulé');
    expect(root.querySelector('button[type=submit]')?.hasAttribute('disabled')).toBe(false);
  });
});
