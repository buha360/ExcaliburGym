import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideApi } from './api/generated/provide-api';
import { AuthStore } from './core/auth/auth-store';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes),
    provideApi({ basePath: '', withCredentials: true }),
    provideAppInitializer(() => inject(AuthStore).initialize())
  ]
};
