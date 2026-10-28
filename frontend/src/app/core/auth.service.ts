import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { SesionAuth, Usuario } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  usuario = signal<Usuario | null>(this.leer());
  token = signal<string | null>(localStorage.getItem('dilema-token'));

  rol() { return this.usuario()?.rol ?? null; }

  puede(permiso: string) { return this.usuario()?.permisos?.includes(permiso) ?? false; }

  ingresarEstudiante(email: string, codigo: string) {
    return this.http.post<SesionAuth>('/api/auth/ingresar', {
      email, rol: 'ESTUDIANTE', clave: null, codigoAutorizacion: codigo
    }).pipe(tap((sesion) => this.guardar(sesion)));
  }

  ingresarCuenta(email: string, clave: string) {
    return this.http.post<SesionAuth>('/api/auth/ingresar', {
      email, rol: null, clave, codigoAutorizacion: null
    }).pipe(tap((sesion) => this.guardar(sesion)));
  }

  salir() {
    this.http.delete('/api/auth/sesion').subscribe({ complete: () => this.limpiar(), error: () => this.limpiar() });
  }

  private guardar(sesion: SesionAuth) {
    localStorage.setItem('dilema-token', sesion.token);
    localStorage.setItem('dilema-usuario', JSON.stringify(sesion.usuario));
    if (sesion.sesionId) localStorage.setItem('dilema-sesion', String(sesion.sesionId));
    this.token.set(sesion.token);
    this.usuario.set(sesion.usuario);
  }

  private limpiar() {
    localStorage.removeItem('dilema-token');
    localStorage.removeItem('dilema-usuario');
    localStorage.removeItem('dilema-sesion');
    this.token.set(null);
    this.usuario.set(null);
    this.router.navigateByUrl('/ingreso');
  }

  private leer(): Usuario | null {
    const texto = localStorage.getItem('dilema-usuario');
    return texto ? JSON.parse(texto) as Usuario : null;
  }
}
