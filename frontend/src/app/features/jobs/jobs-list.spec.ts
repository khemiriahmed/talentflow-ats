import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Page, PublicJob } from '../../core/models';
import { JobsList } from './jobs-list';

const job = (id: number, overrides: Partial<PublicJob> = {}): PublicJob => ({
  id,
  title: `Offre ${id}`,
  description: 'Description',
  department: 'Ingénierie',
  location: 'Tunis',
  contractType: 'CDI',
  salaryMin: 3500,
  salaryMax: 5200,
  deadline: null,
  createdAt: '2026-11-01T10:00:00Z',
  ...overrides,
});

const page = (content: PublicJob[], totalPages = 1): Page<PublicJob> => ({
  content,
  page: { size: 10, number: 0, totalElements: content.length, totalPages },
});

describe('JobsList', () => {
  let fixture: ComponentFixture<JobsList>;
  let http: HttpTestingController;

  async function open(inputs: Record<string, string> = {}): Promise<HTMLElement> {
    fixture = TestBed.createComponent(JobsList);
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    fixture.detectChanges();
    TestBed.tick();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists the open jobs with their details', async () => {
    const root = await open();
    http.expectOne((r) => r.url === '/api/v1/jobs' && r.params.get('size') === '10').flush(page([job(1), job(2, { location: null })]));
    await fixture.whenStable();

    const titles = Array.from(root.querySelectorAll('.job h2')).map((h) => h.textContent?.trim());
    expect(titles).toEqual(['Offre 1', 'Offre 2']);
    expect(root.querySelector('.count')?.textContent).toContain('2 offres ouvertes');
    expect(root.querySelector('.job .salary')?.textContent).toMatch(/3\s?500 à 5\s?200 €/);
    expect(root.querySelector<HTMLAnchorElement>('a.job')?.getAttribute('href')).toBe('/offres/1');
  });

  it('sends the filters found in the URL to the API', async () => {
    await open({ keyword: 'java', location: 'Tunis', contractType: 'CDI', page: '2' });
    const request = http.expectOne((r) => r.url === '/api/v1/jobs');
    expect(request.request.params.get('keyword')).toBe('java');
    expect(request.request.params.get('location')).toBe('Tunis');
    expect(request.request.params.get('contractType')).toBe('CDI');
    expect(request.request.params.get('page')).toBe('2');
    request.flush(page([]));
  });

  it('offers to clear the filters when nothing matches', async () => {
    const root = await open({ keyword: 'cobol' });
    http.expectOne((r) => r.url === '/api/v1/jobs').flush(page([]));
    await fixture.whenStable();

    expect(root.querySelector('.empty')?.textContent).toContain('Aucune offre ne correspond');
    expect(root.querySelector('.empty button')?.textContent).toContain('Effacer les filtres');
  });

  it('shows a different message when no job is open at all', async () => {
    const root = await open();
    http.expectOne((r) => r.url === '/api/v1/jobs').flush(page([]));
    await fixture.whenStable();

    expect(root.querySelector('.empty')?.textContent).toContain('Aucune offre n’est ouverte');
    expect(root.querySelector('.empty button')).toBeNull();
  });

  it('shows pagination only when there is more than one page', async () => {
    const root = await open();
    http.expectOne((r) => r.url === '/api/v1/jobs').flush(page([job(1)], 3));
    await fixture.whenStable();

    expect(root.querySelector('.pager')?.textContent).toContain('Page 1 sur 3');
    expect(root.querySelector<HTMLButtonElement>('.pager button')?.disabled).toBe(true);
  });

  it('lets the visitor retry when the API is unreachable', async () => {
    const root = await open();
    http.expectOne((r) => r.url === '/api/v1/jobs').error(new ProgressEvent('error'));
    await fixture.whenStable();

    expect(root.querySelector('.alert')?.textContent).toContain('Impossible de charger les offres');
    root.querySelector<HTMLButtonElement>('.alert button')!.click();
    TestBed.tick();
    http.expectOne((r) => r.url === '/api/v1/jobs').flush(page([job(1)]));
  });
});
