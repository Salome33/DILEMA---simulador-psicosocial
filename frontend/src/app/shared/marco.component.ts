import { Component, inject, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-marco',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <div class="app-frame" [class.superior]="modo() === 'superior'">
      <header class="topbar">
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
          <button class="salir-top" type="button" (click)="auth.salir()">Cerrar sesión</button>
        </div>
      </header>
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
