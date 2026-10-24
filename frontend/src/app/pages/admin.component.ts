import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MarcoComponent } from '../shared/marco.component';
import { Usuario, mensajeError } from '../core/api.service';

const enlaces = [
  { href: '/admin/usuarios', texto: 'Usuarios', exacto: true },
  { href: '/admin/usuarios/nuevo', texto: 'Crear usuario', exacto: true },
  { href: '/admin/configuracion', texto: 'Configuración global', exacto: true }
];

@Component({
  selector: 'app-usuarios',
  imports: [MarcoComponent],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="card stack">
        <h1>Usuarios</h1>
        <p class="sub">Cuentas del simulador. El acceso de cada persona lo define su rol.</p>
        @for (usuario of usuarios(); track usuario.id) {
          <article class="item">
            <h3>{{ usuario.nombre }}</h3>
            <p class="sub">{{ usuario.email }} · {{ usuario.rol }}</p>
          </article>
        }
      </section>
    </app-marco>
  `
})
export class UsuariosComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  usuarios = signal<Usuario[]>([]);
  constructor() {
    this.http.get<Usuario[]>('/api/admin/usuarios').subscribe((dato) => this.usuarios.set(dato));
  }
}

@Component({
  selector: 'app-crear-usuario',
  imports: [MarcoComponent, FormsModule],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="card stack">
        <h1>Registrar nuevo usuario</h1>
        <label class="field"><span>Nombre completo</span><input [(ngModel)]="nombre" /></label>
        <label class="field"><span>Correo electrónico</span><input [(ngModel)]="email" /></label>
        <label class="field"><span>Rol</span>
          <select [(ngModel)]="rol">
            <option value="PROFESOR">Docente</option>
            <option value="ESTUDIANTE">Estudiante</option>
            <option value="ADMIN">Administración</option>
          </select>
        </label>
        <label class="field"><span>Contraseña temporal</span><input [(ngModel)]="clave" type="password" autocomplete="new-password" /></label>
        <p class="sub">La contraseña se entrega por un canal seguro. No queda visible en el listado.</p>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <button class="btn" type="button" (click)="crear()">Guardar usuario</button>
      </section>
    </app-marco>
  `
})
export class CrearUsuarioComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  private router = inject(Router);
  nombre = '';
  email = '';
  clave = '';
  rol = 'PROFESOR';
  error = signal('');
  crear() {
    this.http.post('/api/admin/usuarios', { nombre: this.nombre, email: this.email, clave: this.clave, rol: this.rol }).subscribe({
      next: () => this.router.navigateByUrl('/admin/usuarios'),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
}

@Component({
  selector: 'app-configuracion',
  imports: [MarcoComponent, FormsModule],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="card stack">
        <p class="kicker">Configuración global del sistema</p>
        <h1>Parámetros generales</h1>
        <label class="field"><span>Nombre del sistema</span><input [(ngModel)]="config.nombre" /></label>
        <label class="field"><span>Correo de soporte</span><input [(ngModel)]="config.soporte" /></label>
        <label class="field"><span>Zona horaria</span><input [(ngModel)]="config.zona" /></label>
        <label class="field"><span>Inactividad máxima (minutos)</span><input type="number" [(ngModel)]="config.inactividad" /></label>
        <label class="check"><input type="checkbox" [(ngModel)]="config.notificar" /> Habilitar notificaciones por correo</label>
        <label class="check"><input type="checkbox" [(ngModel)]="config.docentePublica" /> Permitir que el docente publique sin aprobación previa</label>
        <div class="grid-2">
          <label class="field"><span>Escala mínima</span><input type="number" step="0.1" [(ngModel)]="config.escalaMin" /></label>
          <label class="field"><span>Escala máxima</span><input type="number" step="0.1" [(ngModel)]="config.escalaMax" /></label>
        </div>
        @if (ok()) { <p class="ok">{{ ok() }}</p> }
        <button class="btn" type="button" (click)="guardar()">Guardar configuración</button>
      </section>
    </app-marco>
  `
})
export class ConfiguracionComponent {
  enlaces = enlaces;
  ok = signal('');
  config = this.leer();
  guardar() {
    localStorage.setItem('dilema-config', JSON.stringify(this.config));
    this.ok.set('Configuración guardada en este navegador.');
  }
  private leer() {
    const base = { nombre: 'Simulador de Psicología Social', soporte: 'soporte.academico@universidad.edu', zona: 'America/Bogota', inactividad: 30, notificar: true, docentePublica: true, escalaMin: 0, escalaMax: 5 };
    try { return { ...base, ...JSON.parse(localStorage.getItem('dilema-config') || '{}') }; }
    catch { return base; }
  }
}
