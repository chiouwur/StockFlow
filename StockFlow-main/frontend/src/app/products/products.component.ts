import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { ProductService } from '../core/product.service';
import { InventoryService } from '../core/inventory.service';
import { Product, ProductRequest, ProductView } from '../core/models';
import { errorMessage } from '../core/http-error';

type StockStatus = 'off' | 'unknown' | 'out' | 'low' | 'ok';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="page-head">
      <div>
        <h1>Productos</h1>
        <p class="lede">
          @if (rows.length === 0) {
            Crea tus productos para empezar a controlar su stock.
          } @else if (stockDown) {
            Revisa el catálogo mientras se recupera la consulta de stock.
          } @else if (attention > 0) {
            <b>{{ attention }} de {{ activeCount }}</b> productos activos están en el mínimo o por debajo.
          } @else {
            Todo el stock activo está sobre el mínimo.
          }
        </p>
      </div>
      <button class="btn" (click)="startNew()">Nuevo producto</button>
    </div>

    @if (error) { <div class="notice err" role="alert">{{ error }}</div> }
    @if (stockDown) { <div class="notice">No se pudo consultar el stock. Verás “—” hasta que el servicio de inventario responda.</div> }

    @if (form) {
      <div class="card">
        <h2 style="margin-bottom:16px">{{ editingId ? 'Editar producto' : 'Nuevo producto' }}</h2>
        <div class="form-grid">
          <div class="field"><label for="p-code">Código</label><input id="p-code" [(ngModel)]="form.code"></div>
          <div class="field"><label for="p-name">Nombre</label><input id="p-name" [(ngModel)]="form.name"></div>
          @if (!editingId) {
            <div class="field">
              <label for="p-init">Stock inicial</label>
              <input id="p-init" type="number" min="0" [(ngModel)]="initialStock">
              <p class="hint">Se registra como una entrada.</p>
            </div>
          }
          <div class="field">
            <label for="p-min">Stock mínimo</label>
            <input id="p-min" type="number" min="0" [(ngModel)]="form.minimumStock">
            <p class="hint">Cuando el stock llegue a este número, se genera una alerta.</p>
          </div>
        </div>
        <div class="form-actions">
          <button class="btn" (click)="save()">Guardar producto</button>
          <button class="btn g" (click)="form = null">Cancelar</button>
        </div>
      </div>
    }

    <div class="toolbar">
      <div class="search">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
        <input [(ngModel)]="q" (change)="load()" aria-label="Buscar productos" placeholder="Buscar por nombre o código">
      </div>
    </div>

    <div class="card flush scroll">
      <table>
        <thead>
          <tr><th>Producto</th><th>Stock</th><th>Mínimo</th><th>Estado</th><th></th></tr>
        </thead>
        <tbody>
          @for (p of rows; track p.id) {
            <tr [class.low]="status(p) === 'low'" [class.out]="status(p) === 'out'" [class.off]="!p.active">
              <td><span class="pname">{{ p.name }}</span><span class="pcode">{{ p.code }}</span></td>
              <td>
                <div class="stock-cell">
                  <span class="qty">{{ p.currentStock ?? '—' }}</span>
                  @if (p.active && p.currentStock !== null) {
                    <div class="gauge" [class.low]="status(p) === 'low'" [class.out]="status(p) === 'out'"
                         [attr.title]="'Stock mínimo: ' + p.minimumStock">
                      <span class="fill" [style.width.%]="fillPct(p)"></span>
                      @if (p.minimumStock > 0) { <span class="tick" [style.left.%]="tickPct(p)"></span> }
                    </div>
                  }
                </div>
              </td>
              <td class="num">{{ p.minimumStock }}</td>
              <td><span class="pill" [class.ok]="status(p) === 'ok'" [class.low]="status(p) === 'low'" [class.bad]="status(p) === 'out'">{{ label(p) }}</span></td>
              <td>
                <div class="actions">
                  <button class="btn t s" (click)="edit(p)">Editar</button>
                  @if (p.active) { <button class="btn t s" (click)="deactivate(p)">Desactivar</button> }
                </div>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5"><div class="empty"><strong>Aún no hay productos</strong>Crea el primero con “Nuevo producto”.</div></td></tr>
          }
        </tbody>
      </table>
    </div>`
})
export class ProductsComponent implements OnInit {
  private products = inject(ProductService);
  private inventory = inject(InventoryService);
  rows: ProductView[] = [];
  q = '';
  error = '';
  stockDown = false;
  form: ProductRequest | null = null;
  initialStock = 0;
  editingId: string | null = null;
  activeCount = 0;
  attention = 0; // productos activos en el mínimo o por debajo

  ngOnInit() { this.load(); }

  // Une el catálogo (product-service) con las existencias (inventory-service) por productId.
  load() {
    forkJoin({
      p: this.products.list(this.q),
      s: this.inventory.stock().pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ p, s }) => {
        this.stockDown = s === null;
        const map = new Map((s ?? []).map(x => [x.productId, x.currentStock]));
        this.rows = p.map(x => ({ ...x, currentStock: s === null ? null : (map.get(x.id) ?? 0) }));
        this.activeCount = this.rows.filter(r => r.active).length;
        this.attention = this.rows.filter(r => ['low', 'out'].includes(this.status(r))).length;
        this.error = '';
      },
      error: e => this.error = errorMessage(e)
    });
  }

  // --- Presentación del stock ---
  status(p: ProductView): StockStatus {
    if (!p.active) return 'off';
    if (p.currentStock === null) return 'unknown';
    if (p.currentStock <= 0) return 'out';
    return p.currentStock <= p.minimumStock ? 'low' : 'ok';
  }
  label(p: ProductView): string {
    switch (this.status(p)) {
      case 'off': return 'Desactivado';
      case 'out': return 'Sin stock';
      case 'low': return 'Stock bajo';
      case 'ok': return 'Disponible';
      default: return 'Activo';
    }
  }
  // El medidor se llena hasta 3 veces el mínimo; la marca indica dónde está el mínimo.
  private scale(p: ProductView): number {
    return Math.max(p.minimumStock * 3, p.currentStock ?? 0, 1);
  }
  fillPct(p: ProductView): number {
    return Math.min(100, ((p.currentStock ?? 0) / this.scale(p)) * 100);
  }
  tickPct(p: ProductView): number {
    return (p.minimumStock / this.scale(p)) * 100;
  }

  // --- Alta, edición y baja ---
  startNew() {
    this.editingId = null;
    this.initialStock = 0;
    this.form = { code: '', name: '', minimumStock: 5 };
  }
  edit(p: Product) {
    this.editingId = p.id;
    this.form = { code: p.code, name: p.name, minimumStock: p.minimumStock };
  }

  save() {
    if (!this.form) return;
    const f = this.form;
    if (this.editingId) {
      this.products.update(this.editingId, f).subscribe({
        next: () => { this.form = null; this.error = ''; this.load(); },
        error: e => this.error = errorMessage(e)
      });
      return;
    }
    const qty = Number(this.initialStock) || 0;
    this.products.create(f).pipe(
      switchMap(p => qty > 0
        ? this.inventory.register({ productId: p.id, type: 'ENTRY', quantity: qty }).pipe(map(() => p))
        : of(p))
    ).subscribe({
      next: () => { this.form = null; this.error = ''; this.load(); },
      error: e => { this.error = 'El producto puede haberse creado, pero hubo un problema: ' + errorMessage(e); this.load(); }
    });
  }

  deactivate(p: Product) {
    this.products.deactivate(p.id).subscribe({ next: () => this.load(), error: e => this.error = errorMessage(e) });
  }
}