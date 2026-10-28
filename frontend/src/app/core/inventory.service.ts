import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Movement, MovementRequest, StockLevel } from './models';

// Cliente de inventory-service.
@Injectable({ providedIn: 'root' })
export class InventoryService {
  private http = inject(HttpClient);
  private base = environment.apiBase;

  register(d: MovementRequest): Observable<Movement> {
    return this.http.post<Movement>(`${this.base}/api/movements`, d);
  }
  list(): Observable<Movement[]> {
    return this.http.get<Movement[]>(`${this.base}/api/movements`);
  }
  stock(): Observable<StockLevel[]> {
    return this.http.get<StockLevel[]>(`${this.base}/api/stock`);
  }
}
