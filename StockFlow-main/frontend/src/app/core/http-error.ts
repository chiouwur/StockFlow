import { HttpErrorResponse } from '@angular/common/http';

export function errorMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    return e.error?.message ?? `Error ${e.status}: no se pudo completar la solicitud`;
  }
  return 'Ocurrió un error inesperado';
}
