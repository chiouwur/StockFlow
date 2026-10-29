import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { AlertService } from '../core/alert.service';
import { ProductService } from '../core/product.service';
import { Alert } from '../core/models';
import { errorMessage } from '../core/http-error';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="bar"><button class="btn g" (click)="load()">Actualizar</button></div>
    @if (error) { <p class="err">{{ error }}</p> }
    <div class="card scroll"><table>
      <thead><tr><th>Producto</th><th>Alerta</th><th>Fecha</th><th>Estado</th></tr></thead>
      <tbody>
        @for (a of alerts; track a.id) {
          <tr><td>{{ nameOf(a.productId) }}</td><td>{{ a.message }}</td>
            <td>{{ a.date | date:'short' }}</td><td><span class="tag">{{ a.status === 'OPEN' ? 'Abierta' : a.status }}</span></td></tr>
        } @empty {
          <tr><td colspan="4" class="mut">Sin alertas. Aparecen cuando un movimiento deja el stock en el mínimo o por debajo.</td></tr>
        }
      </tbody>
    </table></div>`
})
export class AlertsComponent implements OnInit {
  private alertApi = inject(AlertService);
  private productApi = inject(ProductService);
  alerts: Alert[] = [];
  names = new Map<string, string>();
  error = '';

  ngOnInit() { this.load(); }

  load() {
    forkJoin({ a: this.alertApi.list(), p: this.productApi.list() }).subscribe({
      next: ({ a, p }) => {
        this.alerts = a;
        this.names = new Map(p.map(x => [x.id, x.name]));
        this.error = '';
      },
      error: e => this.error = errorMessage(e)
    });
  }
  nameOf(id: string) { return this.names.get(id) ?? id; }
}
