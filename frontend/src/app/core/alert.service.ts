import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Alert } from './models';

// Cliente de notification-service.
@Injectable({ providedIn: 'root' })
export class AlertService {
  private http = inject(HttpClient);

  list(): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${environment.apiBase}/api/alerts`);
  }
}
