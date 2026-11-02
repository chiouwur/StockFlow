import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Product, ProductRequest } from './models';

// Cliente de product-service. El token lo agrega MsalInterceptor.
@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private url = `${environment.apiBase}/api/products`;

  list(search = ''): Observable<Product[]> {
    const params = search ? new HttpParams().set('search', search) : undefined;
    return this.http.get<Product[]>(this.url, { params });
  }
  create(d: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.url, d);
  }
  update(id: string, d: ProductRequest): Observable<Product> {
    return this.http.put<Product>(`${this.url}/${id}`, d);
  }
  deactivate(id: string): Observable<Product> {
    return this.http.patch<Product>(`${this.url}/${id}/deactivate`, {});
  }
}
