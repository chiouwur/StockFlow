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
    <div class="page-head">
      <div>
        <h1>Alertas</h1>
        <p class="lede">Avisos que se generan cuando un movimiento deja un producto en el mínimo o por debajo.</p>
      </div>
      <button class="btn g" (click)="load()">Actualizar</button>
    </div>

    @if (error) { <div class="notice err" role="alert">{{ error }}</div> }

    <div class="alert-list">
      @for (a of alerts; track a.id) {
        <article class="alert">
          <div>
            <h3>{{ nameOf(a.productId) }}</h3>
            <p>{{ a.message }}</p>
          </div>
          <div class="meta">
            <span class="pill" [class.low]="a.status === 'OPEN'">{{ a.status === 'OPEN' ? 'Abierta' : a.status }}</span>
            <span>{{ a.date | date:'dd/MM/yyyy HH:mm' }}</span>
          </div>
        </article>
      } @empty {
        <div class="card"><div class="empty"><strong>Todo en orden</strong>No hay alertas por ahora. Aparecen cuando un movimiento deja el stock en el mínimo o por debajo.</div></div>
      }
    </div>`
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