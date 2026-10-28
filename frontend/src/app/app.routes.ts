import { Routes } from '@angular/router';
import { rolGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'ingreso' },
  { path: 'ingreso', loadComponent: () => import('./pages/ingreso.component').then(m => m.IngresoComponent) },
  { path: 'estudiante', canActivate: [rolGuard('ESTUDIANTE')], loadComponent: () => import('./pages/estudiante.component').then(m => m.PanelEstudianteComponent) },
  { path: 'estudiante/sesion/:id', canActivate: [rolGuard('ESTUDIANTE')], loadComponent: () => import('./pages/estudiante.component').then(m => m.BriefingComponent) },
  { path: 'estudiante/participacion/:id', canActivate: [rolGuard('ESTUDIANTE')], loadComponent: () => import('./pages/estudiante.component').then(m => m.SimulacionComponent) },
  { path: 'estudiante/avisos', canActivate: [rolGuard('ESTUDIANTE')], loadComponent: () => import('./pages/estudiante.component').then(m => m.AvisosComponent) },
  { path: 'docente', pathMatch: 'full', redirectTo: 'docente/casos' },
  { path: 'docente/casos', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.CasosComponent) },
  { path: 'docente/casos/nuevo', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.EditorCasoComponent) },
  { path: 'docente/casos/:id', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.EditorCasoComponent) },
  { path: 'docente/grupos', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.GruposComponent) },
  { path: 'docente/programar', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.ProgramarComponent) },
  { path: 'docente/seguimiento', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.SeguimientoComponent) },
  { path: 'docente/seguimiento/:id', canActivate: [rolGuard('PROFESOR')], loadComponent: () => import('./pages/docente.component').then(m => m.DetalleParticipacionComponent) },
  { path: 'admin', pathMatch: 'full', redirectTo: 'admin/usuarios' },
  { path: 'admin/usuarios', canActivate: [rolGuard('ADMIN')], loadComponent: () => import('./pages/admin.component').then(m => m.UsuariosComponent) },
  { path: 'admin/usuarios/nuevo', canActivate: [rolGuard('ADMIN')], loadComponent: () => import('./pages/admin.component').then(m => m.CrearUsuarioComponent) },
  { path: 'admin/usuarios/:id', canActivate: [rolGuard('ADMIN')], loadComponent: () => import('./pages/admin.component').then(m => m.PermisosDocenteComponent) },
  { path: 'admin/configuracion', canActivate: [rolGuard('ADMIN')], loadComponent: () => import('./pages/admin.component').then(m => m.ConfiguracionComponent) },
  { path: '**', redirectTo: 'ingreso' }
];
