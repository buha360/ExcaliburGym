import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { routes } from './app.routes';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should expose the sauna page inside the authenticated shell', () => {
    const shellRoute = routes.find((route) => route.path === '');

    expect(shellRoute?.children?.some((route) => route.path === 'sauna')).toBe(true);
  });

  it('should expose the system health page inside the authenticated shell', () => {
    const shellRoute = routes.find((route) => route.path === '');
    const healthRoute = shellRoute?.children?.find((route) => route.path === 'system-health');

    expect(healthRoute).toBeDefined();
    expect(healthRoute?.canActivate).toBeUndefined();
  });
});
