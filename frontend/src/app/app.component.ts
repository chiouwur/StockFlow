import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { loginRequest } from './core/msal.config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="wrap">
      <header>
        <h1>StockFlow</h1>
        @if (account) {
          <div><span class="mut">{{ account.name || account.username }}</span>
            <button class="btn g s" (click)="logout()">Cerrar sesión</button></div>
        } @else {
          <button class="btn" (click)="login()">Iniciar sesión</button>
        }
      </header>
      @if (account) {
        <nav>
          <a routerLink="/products" routerLinkActive="on">Productos</a>
          <a routerLink="/movements" routerLinkActive="on">Movimientos</a>
          <a routerLink="/alerts" routerLinkActive="on">Alertas</a>
        </nav>
        <router-outlet />
      } @else {
        <div class="card center"><h2>Inicia sesión para gestionar tu inventario</h2></div>
      }
    </div>`
})
export class AppComponent {
  private msal = inject(MsalService);

  get account() {
    return this.msal.instance.getActiveAccount();
  }
  login() {
    this.msal.loginPopup(loginRequest).subscribe(r => this.msal.instance.setActiveAccount(r.account));
  }
  logout() {
    this.msal.logoutPopup().subscribe();
  }
}
