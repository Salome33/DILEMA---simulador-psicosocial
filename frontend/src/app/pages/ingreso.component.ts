import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../core/auth.service';
import { mensajeError } from '../core/api.service';

@Component({
  selector: 'app-ingreso',
  imports: [FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="login-page">
      <header class="topbar"><div class="brand">Simulador de Psicología Social</div></header>
      <main class="login-wrap ingreso-dos">
        <mat-card>
          <mat-card-header>
            <mat-card-title>Estudiante</mat-card-title>
            <mat-card-subtitle>Correo institucional y código de la sesión</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content class="stack">
            <mat-form-field appearance="outline">
              <mat-label>Correo institucional</mat-label>
              <input matInput [(ngModel)]="correoEstudiante" autocomplete="username" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Código de la sesión</mat-label>
              <input matInput [(ngModel)]="codigo" autocomplete="off" />
            </mat-form-field>
            @if (errorEstudiante()) { <p class="error">{{ errorEstudiante() }}</p> }
          </mat-card-content>
          <mat-card-actions>
            <button mat-flat-button color="primary" type="button" (click)="entrarEstudiante()">Entrar con código</button>
          </mat-card-actions>
        </mat-card>
        <mat-card>
          <mat-card-header>
            <mat-card-title>Docente o administrador</mat-card-title>
            <mat-card-subtitle>Correo y contraseña</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content class="stack">
            <mat-form-field appearance="outline">
              <mat-label>Correo</mat-label>
              <input matInput [(ngModel)]="correoCuenta" autocomplete="username" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Contraseña</mat-label>
              <input matInput [(ngModel)]="clave" type="password" autocomplete="current-password" />
            </mat-form-field>
            @if (errorCuenta()) { <p class="error">{{ errorCuenta() }}</p> }
          </mat-card-content>
          <mat-card-actions>
            <button mat-flat-button color="primary" type="button" (click)="entrarCuenta()">Entrar con contraseña</button>
          </mat-card-actions>
        </mat-card>
      </main>
    </div>
  `
})
export class IngresoComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  correoEstudiante = '';
  codigo = '';
  correoCuenta = '';
  clave = '';
  errorEstudiante = signal('');
  errorCuenta = signal('');

  entrarEstudiante() {
    this.errorEstudiante.set('');
    this.auth.ingresarEstudiante(this.correoEstudiante.trim(), this.codigo.trim()).subscribe({
      next: (sesion) => this.router.navigateByUrl(this.destino(sesion.usuario.rol)),
      error: (falla) => this.errorEstudiante.set(mensajeError(falla))
    });
  }

  entrarCuenta() {
    this.errorCuenta.set('');
    this.auth.ingresarCuenta(this.correoCuenta.trim(), this.clave).subscribe({
      next: (sesion) => this.router.navigateByUrl(this.destino(sesion.usuario.rol)),
      error: (falla) => this.errorCuenta.set(mensajeError(falla))
    });
  }

  private destino(rol: string) {
    if (rol === 'ESTUDIANTE') return '/estudiante';
    if (rol === 'PROFESOR') return '/docente/casos';
    return '/admin/usuarios';
  }
}
