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
    <div class="page-head">
      <div>
        <h1>Movimientos</h1>
        <p class="lede">Registra las entradas y salidas de stock. Cada cambio queda en el historial.</p>
      </div>
    </div>

    @if (error) { <div class="notice err" role="alert">{{ error }}</div> }

    <div class="movements-grid">
      <section class="card">
        <h2 style="margin-bottom:16px">Registrar movimiento</h2>

        <div class="field">
          <label for="m-prod">Producto</label>
          <select id="m-prod" [(ngModel)]="productId">
            @for (p of products; track p.id) { <option [value]="p.id">{{ p.name }} ({{ p.code }})</option> }
          </select>
        </div>

        <div class="field" style="margin-top:16px">
          <label id="m-type">Tipo de movimiento</label>
          <div class="seg" role="group" aria-labelledby="m-type">
            <button type="button" class="in" [attr.aria-pressed]="type === 'ENTRY'" (click)="type = 'ENTRY'">Entrada</button>
            <button type="button" class="out" [attr.aria-pressed]="type === 'EXIT'" (click)="type = 'EXIT'">Salida</button>
          </div>
        </div>

        <div class="field" style="margin-top:16px">
          <label for="m-qty">Cantidad</label>
          <input id="m-qty" type="number" min="1" step="1" [(ngModel)]="quantity">
        </div>

        @if (productId) {
          <p class="preview" [class.bad]="insufficient" aria-live="polite">
            @if (insufficient) {
              Stock insuficiente: hay <b>{{ current }}</b> y quieres sacar <b>{{ qty }}</b>.
            } @else {
              Stock actual <b>{{ current }}</b>. Quedará en <b>{{ after }}</b>.
              @if (willAlert) { Queda en el mínimo o por debajo, así que se generará una alerta. }
            }
          </p>
        }

        <div class="form-actions">
          <button class="btn" (click)="register()" [disabled]="!productId || qty < 1 || insufficient">Registrar movimiento</button>
        </div>
      </section>

      <section class="card flush">
        <div class="card-head"><h2>Historial</h2></div>
        <ul class="feed">
          @for (m of movements; track m.id) {
            <li>
              <div class="what">
                <b>{{ nameOf(m.productId) }}</b>
                <span class="when">{{ m.type === 'ENTRY' ? 'Entrada' : 'Salida' }}, {{ m.date | date:'dd/MM/yyyy HH:mm' }}</span>
              </div>
              <span class="delta" [class.pos]="m.type === 'ENTRY'" [class.neg]="m.type === 'EXIT'">{{ m.type === 'ENTRY' ? '+' : '−' }}{{ m.quantity }}</span>
            </li>
          } @empty {
            <li><div class="empty" style="width:100%"><strong>Aún no hay movimientos</strong>Registra una entrada para empezar.</div></li>
          }
        </ul>
      </section>
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

  // Vista previa del movimiento antes de registrarlo
  get current() { return this.stockOf(this.productId); }
  get qty() { return Number(this.quantity) || 0; }
  get after() { return this.type === 'ENTRY' ? this.current + this.qty : this.current - this.qty; }
  get insufficient() { return this.type === 'EXIT' && this.qty > this.current; }
  get willAlert() {
    const min = this.allProducts.find(p => p.id === this.productId)?.minimumStock ?? 0;
    return this.qty > 0 && this.after <= min;
  }

  register() {
    this.inventory.register({ productId: this.productId, type: this.type, quantity: this.quantity }).subscribe({
      next: () => { this.error = ''; this.load(); },
      error: e => this.error = errorMessage(e)
    });
  }
}