import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { mensajeError } from '../core/api.service';

@Component({
  selector: 'app-ingreso',
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <header class="topbar"><div class="brand">Simulador de Psicología Social</div></header>
      <main class="login-wrap">
        <section class="login-card stack">
          <h1>Simulador de Psicología Social</h1>
          <p class="sub">Plataforma de experimentación e investigación social</p>
          <label class="field">
            <span>Correo electrónico</span>
            <input [(ngModel)]="email" placeholder="estudiante@universidad.edu" autocomplete="username" />
          </label>
          @if (!personal()) {
            <label class="field">
              <span>Código de autorización</span>
              <input [(ngModel)]="codigo" placeholder="Ej: SOC-2024-X9" autocomplete="off" />
            </label>
          } @else {
            <label class="field">
              <span>Rol</span>
              <select [(ngModel)]="rolPersonal">
                <option value="PROFESOR">Docente</option>
                <option value="ADMIN">Administración</option>
              </select>
            </label>
            <label class="field">
              <span>Clave</span>
              <input [(ngModel)]="clave" type="password" autocomplete="current-password" />
            </label>
          }
          @if (error()) { <p class="error">{{ error() }}</p> }
          <button class="btn" type="button" (click)="entrar()">Ingresar</button>
          <button class="enlace" type="button" (click)="alternar()">
            {{ personal() ? 'Tengo un código de convocatoria' : '¿No tienes código?' }}
          </button>
        </section>
      </main>
    </div>
  `
})
export class IngresoComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  personal = signal(false);
  rolPersonal = 'PROFESOR';
  email = '';
  clave = '';
  codigo = '';
  error = signal('');

  alternar() {
    this.personal.update((valor) => !valor);
    this.error.set('');
    this.clave = '';
    this.codigo = '';
  }

  entrar() {
    this.error.set('');
    const rol = this.personal() ? this.rolPersonal : 'ESTUDIANTE';
    this.auth.ingresar(this.email.trim(), rol, this.clave, this.codigo.trim()).subscribe({
      next: (sesion) => {
        const destino = sesion.usuario.rol === 'ESTUDIANTE' ? '/estudiante'
          : sesion.usuario.rol === 'PROFESOR' ? '/docente/casos' : '/admin/usuarios';
        this.router.navigateByUrl(destino);
      },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
}
