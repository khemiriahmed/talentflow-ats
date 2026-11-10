import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Stage } from '../core/models';
import { StageTrack } from './stage-track';

describe('StageTrack', () => {
  let fixture: ComponentFixture<StageTrack>;

  async function render(stage: Stage): Promise<HTMLElement> {
    fixture = TestBed.createComponent(StageTrack);
    fixture.componentRef.setInput('stage', stage);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('marks the steps already completed, the current one and the ones to come', async () => {
    const root = await render('TECH_INTERVIEW');
    const steps = Array.from(root.querySelectorAll('li.step'));

    expect(steps).toHaveLength(6);
    expect(steps.map((s) => (s.classList.contains('done') ? 'done' : s.classList.contains('current') ? 'current' : 'todo'))).toEqual(
      ['done', 'done', 'done', 'current', 'todo', 'todo'],
    );
    expect(steps[3].getAttribute('aria-current')).toBe('step');
    expect(root.querySelectorAll('[aria-current]')).toHaveLength(1);
    expect(root.querySelector('ol')?.getAttribute('aria-label')).toBe('Étape 4 sur 6 : Entretien technique');
    expect(root.querySelector('.message')?.textContent).toContain('entretien technique');
  });

  it('starts at the first step for a new application', async () => {
    const root = await render('RECEIVED');
    expect(root.querySelector('li.step.current')?.textContent).toContain('Reçue');
    expect(root.querySelectorAll('li.step.done')).toHaveLength(0);
  });

  it('completes every step when the candidate is hired', async () => {
    const root = await render('HIRED');
    expect(root.querySelectorAll('li.step.done')).toHaveLength(5);
    expect(root.querySelector('li.step.current')?.textContent).toContain('Embauché(e)');
    expect(root.querySelector('.message')?.textContent).toContain('Félicitations');
  });

  it('shows a plain message instead of a track when the application was rejected', async () => {
    const root = await render('REJECTED');
    expect(root.querySelector('ol')).toBeNull();
    expect(root.querySelector('.ended')?.textContent).toContain('n’a pas été retenue');
  });
});
