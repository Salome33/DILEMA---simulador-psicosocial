import { Component, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-marco',
  imports: [RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule],
  template: `
    <div class="app-frame" [class.superior]="modo() === 'superior'">
      <mat-toolbar class="topbar">
        <div class="brand">Simulador de Psicología Social</div>
        @if (modo() === 'superior') {
          <nav class="topnav">
            @for (enlace of enlaces(); track enlace.href) {
              <a [routerLink]="enlace.href" routerLinkActive="activa" [routerLinkActiveOptions]="{ exact: !!enlace.exacto }">{{ enlace.texto }}</a>
            }
          </nav>
        }
        <div class="top-acciones">
          <div class="quien">{{ rotulo() }}</div>
          <button mat-stroked-button type="button" (click)="auth.salir()">Cerrar sesión</button>
        </div>
      </mat-toolbar>
      <div class="work">
        @if (modo() !== 'superior') {
          <aside class="side">
            <p class="lado">Módulos de control</p>
            @for (enlace of enlaces(); track enlace.href) {
              <a [routerLink]="enlace.href" routerLinkActive="activa" [routerLinkActiveOptions]="{ exact: !!enlace.exacto }">{{ enlace.texto }}</a>
            }
          </aside>
        }
        <section class="main">
          <ng-content />
        </section>
      </div>
    </div>
  `
})
export class MarcoComponent {
  auth = inject(AuthService);
  modo = input('lateral');
  enlaces = input<{ href: string; texto: string; exacto?: boolean }[]>([]);

  rotulo() {
    const usuario = this.auth.usuario();
    if (!usuario) return '';
    const rol = usuario.rol === 'PROFESOR' ? 'Prof.' : usuario.rol === 'ADMIN' ? 'Admin' : 'Estudiante';
    return rol + ' ' + usuario.nombre;
  }
}
