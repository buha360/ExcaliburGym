import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStore } from './auth-store';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  if (auth.setupRequired()) {
    return router.createUrlTree(['/setup']);
  }
  return auth.currentEmployee() ? true : router.createUrlTree(['/login']);
};

export const setupGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.setupRequired() ? true : router.createUrlTree([auth.currentEmployee() ? '/' : '/login']);
};

export const loginGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  if (auth.setupRequired()) {
    return router.createUrlTree(['/setup']);
  }
  return auth.currentEmployee() ? router.createUrlTree(['/']) : true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);
  return auth.isAdmin() ? true : router.createUrlTree(['/']);
};
