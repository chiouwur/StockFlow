
import { Component, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { errorMessage } from '../core/http-error';

interface AlertView {
  id: string;
  product: string;
  message: string;
  stock: string;
  minimumStock: string;
  date: string;
}

@Component({
  selector: 'app-alerts',
  standalone: true,
  template: `
    <div class="bar">
      <h2>Alertas de stock bajo</h2>
      <button class="btn" (click)="load()">Actualizar</button>
    </div>

    @if (error) {
      <div class="card">
        <p class="err">{{ error }}</p>
        <button class="btn" (click)="load()">Reintentar</button>
      </div>
    }

    @if (loading) {
      <div class="card"><p class="mut">Cargando alertas...</p></div>
    }

    @if (!loading && !error && alerts.length === 0) {
      <div class="card">
        <p class="mut">No hay alertas registradas.</p>
      </div>
    }

    @if (!loading && alerts.length > 0) {
      <div class="card scroll">
        <table>
          <thead>
            <tr>
              <th>Producto</th>
              <th>Detalle</th>
              <th>Stock actual</th>
              <th>Stock mínimo</th>
              <th>Fecha</th>
            </tr>
          </thead>
          <tbody>
            @for (a of alerts; track a.id) {
              <tr class="low">
                <td>{{ a.product }}</td>
                <td>{{ a.message }}</td>
                <td>{{ a.stock }}</td>
                <td>{{ a.minimumStock }}</td>
                <td>{{ a.date }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class AlertsComponent implements OnInit {
  private http = inject(HttpClient);

  alerts: AlertView[] = [];
  loading = false;
  error = '';

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.error = '';

    this.http.get<unknown>(`${environment.apiBase}/api/alerts`)
      .subscribe({
        next: response => {
          const records = this.extractRecords(response);

          this.alerts = records.map((item, index) => ({
            id: this.text(item['id'] ?? item['alertId'] ?? index),
            product: this.text(
              item['productName'] ?? item['productCode'] ??
              item['productId'] ?? item['product'] ?? 'Producto'
            ),
            message: this.text(
              item['message'] ?? item['description'] ??
              item['text'] ?? 'Stock bajo'
            ),
            stock: this.text(
              item['currentStock'] ?? item['stockAfter'] ??
              item['stock'] ?? item['quantity']
            ),
            minimumStock: this.text(
              item['minimumStock'] ?? item['minStock']
            ),
            date: this.text(
              item['createdAt'] ?? item['timestamp'] ??
              item['date'] ?? item['occurredAt']
            )
          }));

          this.loading = false;
        },
        error: e => {
          this.error = errorMessage(e);
          this.loading = false;
        }
      });
  }

  private extractRecords(response: unknown): Record<string, unknown>[] {
    let value = response;

    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const obj = value as Record<string, unknown>;
      value = obj['alerts'] ?? obj['content'] ??
              obj['data'] ?? obj['items'] ?? value;
    }

    if (!Array.isArray(value)) return [];

    return value.filter(
      (item): item is Record<string, unknown> =>
        item !== null && typeof item === 'object' && !Array.isArray(item)
    );
  }

  private text(value: unknown): string {
    if (value === null || value === undefined || value === '') return '—';
    return String(value);
  }
}
