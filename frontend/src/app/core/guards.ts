import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export function rolGuard(rol: string): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (auth.rol() === rol) return true;
    return inject(Router).createUrlTree(['/ingreso']);
  };
}
