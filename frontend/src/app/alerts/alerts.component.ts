import { Component } from '@angular/core';

// Pantalla reservada para notification-service (EA2).
// Cuando exista GET /api/alerts, crea un AlertService igual que ProductService y lista aquí.
@Component({
  selector: 'app-alerts',
  standalone: true,
  template: `<div class="card"><h2>Alertas de stock bajo</h2>
    <p class="mut">Esta pantalla se conectará a notification-service en EA2.</p></div>`
})
export class AlertsComponent {}
