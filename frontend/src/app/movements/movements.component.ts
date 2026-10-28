import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ProductService } from '../core/product.service';
import { InventoryService } from '../core/inventory.service';
import { Movement, MovementType, Product } from '../core/models';
import { errorMessage } from '../core/http-error';

@Component({
  selector: 'app-movements',
  standalone: true,
  imports: [FormsModule, DatePipe],
  template: `
    @if (error) { <p class="err">{{ error }}</p> }
    <div class="grid">
      <div class="card">
        <h2>Registrar movimiento</h2>
        <label>Producto</label>
        <select [(ngModel)]="productId">
          @for (p of products; track p.id) { <option [value]="p.id">{{ p.code }} · {{ p.name }} (stock {{ stockOf(p.id) }})</option> }
        </select>
        <label>Tipo</label>
        <select [(ngModel)]="type"><option value="ENTRY">Entrada</option><option value="EXIT">Salida</option></select>
        <label>Cantidad</label><input type="number" min="1" step="1" [(ngModel)]="quantity">
        <p><button class="btn" (click)="register()">Registrar movimiento</button></p>
      </div>
      <div class="card scroll"><table>
        <thead><tr><th>Historial</th><th>Cant.</th></tr></thead>
        <tbody>
          @for (m of movements; track m.id) {
            <tr><td>{{ nameOf(m.productId) }}<br><span class="mut">{{ m.date | date:'short' }}</span></td>
              <td [class.pos]="m.type === 'ENTRY'" [class.neg]="m.type === 'EXIT'">{{ m.type === 'ENTRY' ? '+' : '−' }}{{ m.quantity }}</td></tr>
          } @empty { <tr><td colspan="2" class="mut">Aún no hay movimientos.</td></tr> }
        </tbody>
      </table></div>
    </div>`
})
export class MovementsComponent implements OnInit {
  private catalog = inject(ProductService);
  private inventory = inject(InventoryService);
  allProducts: Product[] = [];
  products: Product[] = [];
  movements: Movement[] = [];
  stock = new Map<string, number>();
  productId = '';
  type: MovementType = 'ENTRY';
  quantity = 1;
  error = '';

  ngOnInit() { this.load(); }

  load() {
    forkJoin({ p: this.catalog.list(), m: this.inventory.list(), s: this.inventory.stock() }).subscribe({
      next: ({ p, m, s }) => {
        this.allProducts = p;
        this.products = p.filter(x => x.active);
        this.movements = m;
        this.stock = new Map(s.map(x => [x.productId, x.currentStock]));
        if (!this.productId && this.products.length) this.productId = this.products[0].id;
      },
      error: e => this.error = errorMessage(e)
    });
  }
  stockOf(id: string) { return this.stock.get(id) ?? 0; }
  nameOf(id: string) { return this.allProducts.find(p => p.id === id)?.name ?? id; }
  register() {
    this.inventory.register({ productId: this.productId, type: this.type, quantity: this.quantity }).subscribe({
      next: () => { this.error = ''; this.load(); },
      error: e => this.error = errorMessage(e)
    });
  }
}
