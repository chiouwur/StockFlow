import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { loginRequest } from './core/msal.config';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="topbar-in">
        <a class="brand" routerLink="/">
          <svg viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="7" fill="var(--brand)"/>
            <rect x="7" y="8" width="18" height="3.5" rx="1.75" fill="var(--brand-on)"/>
            <rect x="7" y="14.25" width="12" height="3.5" rx="1.75" fill="var(--brand-on)"/>
            <rect x="7" y="20.5" width="7" height="3.5" rx="1.75" fill="#f2a900"/>
          </svg>
          StockFlow
        </a>
        @if (account) {
          <nav class="tabs" aria-label="Secciones">
            <a routerLink="/products" routerLinkActive="on">Productos</a>
            <a routerLink="/movements" routerLinkActive="on">Movimientos</a>
            <a routerLink="/alerts" routerLinkActive="on">Alertas</a>
          </nav>
          <div class="who">
            <span class="name">{{ account.name || account.username }}</span>
            <button class="btn g s" (click)="logout()">Cerrar sesión</button>
          </div>
        } @else {
          <div class="who"><button class="btn" (click)="login()">Iniciar sesión</button></div>
        }
      </div>
    </header>

    <main class="page">
      @if (account) {
        <router-outlet />
      } @else {
        <section class="signin">
          <div>
            <h1>Tu inventario, siempre al día.</h1>
            <p>Revisa cuánto stock queda de cada producto, registra entradas y salidas, y recibe un aviso cuando algo llega al mínimo.</p>
            <button class="btn lg" (click)="login()">Iniciar sesión</button>
          </div>
          <!-- Ejemplo ilustrativo de la vista de productos -->
          <div aria-hidden="true">
            <div class="card flush sample">
              <div class="row">
                <span><span class="pname">Teclado mecánico</span><span class="pcode">TEC-001</span></span>
                <span class="qty">24</span>
                <div class="gauge"><span class="fill" style="width:100%"></span><span class="tick" style="left:33.3%"></span></div>
              </div>
              <div class="row">
                <span><span class="pname">Mouse inalámbrico</span><span class="pcode">MOU-014</span></span>
                <span class="qty">6</span>
                <div class="gauge low"><span class="fill" style="width:25%"></span><span class="tick" style="left:33.3%"></span></div>
              </div>
              <div class="row">
                <span><span class="pname">Monitor 24 pulgadas</span><span class="pcode">MON-024</span></span>
                <span class="qty">0</span>
                <div class="gauge out"><span class="fill" style="width:0"></span><span class="tick" style="left:33.3%"></span></div>
              </div>
            </div>
            <p class="hint" style="margin-top:10px">Ejemplo de la vista de productos. La marca vertical indica el stock mínimo.</p>
          </div>
        </section>
      }
    </main>`
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