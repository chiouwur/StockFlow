import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { ProductService } from '../core/product.service';
import { InventoryService } from '../core/inventory.service';
import { Product, ProductRequest, ProductView } from '../core/models';
import { errorMessage } from '../core/http-error';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="bar">
      <input [(ngModel)]="q" (change)="load()" placeholder="Buscar por nombre o código">
      <button class="btn" (click)="startNew()">Nuevo producto</button>
    </div>
    @if (error) { <p class="err">{{ error }}</p> }
    @if (stockDown) { <p class="mut">No se pudo consultar el stock. Se muestra “—” hasta que inventory-service responda.</p> }
    @if (form) {
      <div class="card">
        <h2>{{ editingId ? 'Editar producto' : 'Nuevo producto' }}</h2>
        <label>Código</label><input [(ngModel)]="form.code">
        <label>Nombre</label><input [(ngModel)]="form.name">
        @if (!editingId) { <label>Stock inicial (se registra como una entrada)</label><input type="number" min="0" [(ngModel)]="initialStock"> }
        <label>Stock mínimo</label><input type="number" min="0" [(ngModel)]="form.minimumStock">
        <p><button class="btn" (click)="save()">Guardar producto</button>
           <button class="btn g" (click)="form = null">Cancelar</button></p>
      </div>
    }
    <div class="card scroll"><table>
      <thead><tr><th>Código</th><th>Nombre</th><th>Stock</th><th>Mínimo</th><th>Estado</th><th></th></tr></thead>
      <tbody>
        @for (p of rows; track p.id) {
          <tr [class.low]="p.active && p.currentStock !== null && p.currentStock <= p.minimumStock">
            <td>{{ p.code }}</td><td>{{ p.name }}</td><td>{{ p.currentStock ?? '—' }}</td><td>{{ p.minimumStock }}</td>
            <td><span class="tag" [class.ok]="p.active">{{ p.active ? 'Activo' : 'Desactivado' }}</span></td>
            <td><button class="btn g s" (click)="edit(p)">Editar</button>
              @if (p.active) { <button class="btn g s" (click)="deactivate(p)">Desactivar</button> }</td>
          </tr>
        } @empty {
          <tr><td colspan="6" class="mut">No hay productos. Crea el primero con “Nuevo producto”.</td></tr>
        }
      </tbody>
    </table></div>`
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
        this.error = '';
      },
      error: e => this.error = errorMessage(e)
    });
  }

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
