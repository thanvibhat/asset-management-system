import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if ([401, 403].includes(error.status)) {
        // Auto logout if 401 or 403 response returned from api
        auth.logout();
      }
      
      const errorMessage = error.error?.message || error.statusText;
      return throwError(() => error);
    })
  );
};
