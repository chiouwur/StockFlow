import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

// Cada ruta exige sesión iniciada. Para sumar un microservicio, agrega aquí su pantalla.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'products' },
  { path: 'products', canActivate: [MsalGuard], loadComponent: () => import('./products/products.component').then(m => m.ProductsComponent) },
  { path: 'movements', canActivate: [MsalGuard], loadComponent: () => import('./movements/movements.component').then(m => m.MovementsComponent) },
  { path: 'alerts', canActivate: [MsalGuard], loadComponent: () => import('./alerts/alerts.component').then(m => m.AlertsComponent) },
  { path: '**', redirectTo: 'products' }
];
