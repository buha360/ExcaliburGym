import { Routes } from '@angular/router';
import { adminGuard, authGuard, loginGuard, setupGuard } from './core/auth/auth-guards';

export const routes: Routes = [
  {
    path: 'setup',
    canActivate: [setupGuard],
    loadComponent: () => import('./features/auth/setup-page').then((module) => module.SetupPage),
  },
  {
    path: 'login',
    canActivate: [loginGuard],
    loadComponent: () => import('./features/auth/login-page').then((module) => module.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/app-shell').then((module) => module.AppShell),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/dashboard/dashboard-page').then((module) => module.DashboardPage),
      },
      {
        path: 'statistics',
        loadComponent: () => import('./features/statistics/statistics-page').then((module) => module.StatisticsPage),
      },      {
        path: 'retail',
        loadComponent: () => import('./features/retail/retail-page').then((module) => module.RetailPage),
      },
      {
        path: 'solarium',
        loadComponent: () => import('./features/solarium/solarium-page').then((module) => module.SolariumPage),
      },
      {
        path: 'sauna',
        loadComponent: () => import('./features/sauna/sauna-page').then((module) => module.SaunaPage),
      },
      {
        path: 'audit',
        loadComponent: () => import('./features/audit/audit-log-page').then((module) => module.AuditLogPage),
      },
      {
        path: 'guests',
        loadComponent: () => import('./features/guests/guests-page').then((module) => module.GuestsPage),
      },
      {
        path: 'guests/:guestId',
        loadComponent: () => import('./features/guests/guest-details-page').then((module) => module.GuestDetailsPage),
      },
      {
        path: 'admin/products',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/products/product-admin-page').then((module) => module.ProductAdminPage),
      },
      {
        path: 'admin/retail-products',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/retail/retail-product-admin-page').then((module) => module.RetailProductAdminPage),
      },
      {
        path: 'system-health',
        loadComponent: () => import('./features/system-health/system-health-page').then((module) => module.SystemHealthPage),
      },
      {
        path: 'admin/employees',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/employees/employees-page').then((module) => module.EmployeesPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
