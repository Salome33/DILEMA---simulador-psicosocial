import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MarcoComponent } from '../shared/marco.component';
import { Aviso, PanelEstudiante, Participacion, Sesion, mensajeError } from '../core/api.service';

const enlaces = [
  { href: '/estudiante', texto: 'Casos asignados', exacto: true },
  { href: '/estudiante/avisos', texto: 'Notificaciones', exacto: true }
];

@Component({
  selector: 'app-panel-estudiante',
  imports: [MarcoComponent, RouterLink, FormsModule],
  template: `
    <app-marco [enlaces]="enlaces" modo="superior">
      <section class="stack">
        <h1>Casos asignados</h1>
        <p class="sub">Selecciona un caso para iniciar la participación o consultar los resultados.</p>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <div class="metricas">
          <article class="mini"><span>Pendientes</span><strong>{{ pendientes() }}</strong></article>
          <article class="mini"><span>En curso</span><strong>{{ enCurso() }}</strong></article>
          <article class="mini"><span>Finalizados</span><strong>{{ finalizados() }}</strong></article>
          <article class="mini"><span>Promedio</span><strong>{{ promedio() }}</strong></article>
        </div>
        <input class="busqueda" [(ngModel)]="busqueda" placeholder="Buscar por caso o grupo" />
        <div class="lista">
          @for (sesion of visibles(); track sesion.id) {
            <article class="item caso-fila">
              <div class="stack">
                <small>CS-{{ codigoCaso(sesion.casoId) }} · {{ sesion.grupo }}</small>
                <h3>{{ sesion.caso }}</h3>
                <p class="sub">{{ resumen(sesion.contexto) }}</p>
                <div class="progreso"><div [style.width.%]="barra(sesion)"></div></div>
                <small>{{ barra(sesion) }}% de avance</small>
              </div>
              @if (sesion.participacionEstado === 'FINALIZADA' && sesion.participacionId) {
                <a class="btn-ghost" [routerLink]="['/estudiante/participacion', sesion.participacionId]">Ver resultados</a>
              } @else {
                <a class="btn" [routerLink]="['/estudiante/sesion', sesion.id]">Ingresar al caso</a>
              }
            </article>
          } @empty { <p class="sub">No tienes casos asignados.</p> }
        </div>
      </section>
    </app-marco>
  `
})
export class PanelEstudianteComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  panel = signal<PanelEstudiante | null>(null);
  error = signal('');
  busqueda = '';
  constructor() {
    this.http.get<PanelEstudiante>('/api/estudiante/panel').subscribe({
      next: (dato) => this.panel.set(dato),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  pendientes() { return (this.panel()?.sesiones ?? []).filter((s) => !s.participacionEstado).length; }
  enCurso() { return (this.panel()?.sesiones ?? []).filter((s) => s.participacionEstado === 'EN_CURSO').length; }
  finalizados() { return (this.panel()?.sesiones ?? []).filter((s) => s.participacionEstado === 'FINALIZADA' || s.participacionEstado === 'AUSENTE').length; }
  promedio() {
    const notas = (this.panel()?.sesiones ?? []).map((s) => s.nota).filter((n): n is number => n !== null);
    if (!notas.length) return '—';
    return (notas.reduce((a, b) => a + b, 0) / notas.length).toFixed(1);
  }
  visibles() {
    const q = this.busqueda.trim().toLowerCase();
    return (this.panel()?.sesiones ?? []).filter((s) => !q || s.caso.toLowerCase().includes(q) || s.grupo.toLowerCase().includes(q));
  }
  barra(sesion: Sesion) {
    if (sesion.participacionEstado === 'FINALIZADA' || sesion.participacionEstado === 'AUSENTE') return 100;
    return sesion.progreso ?? 0;
  }
  codigoCaso(id: number) { return String(id).padStart(3, '0'); }
  resumen(html: string) {
    const texto = (html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    return texto.length > 140 ? texto.slice(0, 140) + '…' : texto;
  }
}

@Component({
  selector: 'app-briefing',
  imports: [MarcoComponent, RouterLink, FormsModule],
  template: `
    <app-marco [enlaces]="enlaces" modo="superior">
      @if (sesion(); as actual) {
        <section class="stack">
          <p class="kicker">{{ actual.codigo }}</p>
          <h1>Bienvenido al caso: {{ actual.caso }}</h1>
          <article class="card stack">
            <h2>Mensaje del docente</h2>
            <p>{{ actual.mensaje || 'Sigue las instrucciones del caso y responde cada escena.' }}</p>
          </article>
          <div class="grid-2">
            <article class="card stack">
              <h2>Reglas de participación</h2>
              <p>1. Tiempo máximo: {{ actual.tiempoMaxMin }} minutos.</p>
              <p>2. No podrás ingresar si ya pasó el tiempo de tolerancia.</p>
              <p>3. La retroalimentación se muestra solo al finalizar.</p>
            </article>
            <article class="card stack">
              <h2>Ficha</h2>
              <p>{{ actual.grupo }} · {{ actual.espacio }}</p>
              <p>{{ actual.lugar || 'Sin lugar físico' }}</p>
            </article>
          </div>
          <div [innerHTML]="actual.desarrollo"></div>
          <label class="check"><input type="checkbox" [(ngModel)]="acepto" /> He leído y acepto las reglas de participación.</label>
          @if (error()) { <p class="error">{{ error() }}</p> }
          <div class="row">
            <a class="btn-ghost" routerLink="/estudiante">Volver al panel</a>
            @if (actual.participacionEstado === 'FINALIZADA' && actual.participacionId) {
              <button class="btn" type="button" (click)="ver(actual.participacionId)">Ver resultados</button>
            } @else {
              <button class="btn" type="button" [disabled]="!acepto" (click)="iniciar(actual)">Iniciar simulación</button>
            }
          </div>
        </section>
      }
    </app-marco>
  `
})
export class BriefingComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  sesion = signal<Sesion | null>(null);
  acepto = false;
  error = signal('');
  constructor() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.http.get<PanelEstudiante>('/api/estudiante/panel').subscribe({
      next: (panel) => this.sesion.set(panel.sesiones.find((item) => item.id === id) ?? null),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  iniciar(sesion: Sesion) {
    this.http.post<Participacion>(`/api/estudiante/sesiones/${sesion.id}/participaciones`, {}).subscribe({
      next: (participacion) => this.router.navigate(['/estudiante/participacion', participacion.id]),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  ver(id: number) { this.router.navigate(['/estudiante/participacion', id]); }
}

@Component({
  imports: [MarcoComponent, RouterLink],
  template: `
    <app-marco [enlaces]="enlaces" modo="superior">
      @if (juego(); as actual) {
        @if (actual.estado === 'FINALIZADA' || actual.estado === 'AUSENTE') {
          <section class="stack">
            <p class="kicker">Informe del caso</p>
            <h1>Resultados del caso</h1>
            <p class="sub">{{ actual.caso }}</p>
            <p class="nota">Calificación final: {{ actual.nota ?? '—' }} / 5,0</p>
            <p class="sub">Nivel: {{ nivel(actual.nota) }}</p>
            <h2>Resumen de respuestas</h2>
            @for (pregunta of actual.preguntas; track pregunta.id) {
              <article class="item">
                <small>{{ pregunta.bloque }}</small>
                <h3>{{ pregunta.enunciado }}</h3>
                @for (opcion of pregunta.opciones; track opcion.id) {
                  <div class="opcion" [class.bien]="opcion.correcta" [class.mal]="pregunta.seleccionId === opcion.id && opcion.correcta === false" [class.elegida]="pregunta.seleccionId === opcion.id">
                    {{ pregunta.seleccionId === opcion.id ? 'Tu elección: ' : '' }}{{ opcion.texto }}
                    @if (pregunta.seleccionId === opcion.id || opcion.correcta) {
                      <p class="sub">{{ opcion.explicacion }}</p>
                    }
                  </div>
                }
              </article>
            }
            <a class="btn-ghost" routerLink="/estudiante">Volver al panel</a>
          </section>
        } @else {
          @if (pregunta(); as item) {
            <section class="stack">
              <div class="row">
                <h2>Escena {{ indice() + 1 }}: {{ item.bloque }}</h2>
                <strong>Tiempo restante: {{ reloj() }}</strong>
              </div>
              <div class="progreso"><div [style.width.%]="actual.progreso"></div></div>
              <div class="escena-vacia">Imagen de la escena</div>
              <article class="card">
                <p class="kicker">Contexto de la situación</p>
                <p>{{ item.contexto }}</p>
              </article>
              <article class="card stack">
                <p class="kicker">Interacción evaluativa</p>
                <h2>{{ item.enunciado }}</h2>
                @if (item.pista) { <p class="aviso"><strong>Herramienta.</strong> {{ item.pista }}</p> }
                @for (opcion of item.opciones; track opcion.id; let i = $index) {
                  <button class="opcion radio" [class.elegida]="item.seleccionId === opcion.id" type="button" (click)="elegir(item.id, opcion.id)">
                    <span>{{ item.seleccionId === opcion.id ? '●' : '○' }}</span>
                    <span>Opción {{ letras[i] }}: {{ opcion.texto }}</span>
                  </button>
                }
              </article>
              @if (error()) { <p class="error">{{ error() }}</p> }
              <div class="row">
                <button class="btn-ghost" type="button" (click)="deshacer()">Deshacer</button>
                @if (indice() < actual.preguntas.length - 1) {
                  <button class="btn" type="button" (click)="mover(1)">Siguiente escena</button>
                } @else {
                  <button class="btn" type="button" (click)="finalizar()">Finalizar</button>
                }
              </div>
            </section>
          }
        }
      }
    </app-marco>
  `
})
export class SimulacionComponent {
  enlaces = enlaces;
  letras = ['A', 'B', 'C', 'D', 'E', 'F'];
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  juego = signal<Participacion | null>(null);
  indice = signal(0);
  error = signal('');
  restante = signal(0);
  private timer = 0;
  constructor() {
    this.cargar();
    this.timer = window.setInterval(() => this.restante.update((valor) => Math.max(0, valor - 1)), 1000);
  }
  pregunta() { return this.juego()?.preguntas[this.indice()] ?? null; }
  nivel(nota: number | null) {
    if (nota == null) return '—';
    if (nota >= 4.6) return 'Superior';
    if (nota >= 4) return 'Alto';
    if (nota >= 3) return 'Básico';
    return 'Bajo';
  }
  reloj() {
    const segundos = this.restante();
    return `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, '0')}`;
  }
  elegir(preguntaId: number, opcionId: number) {
    const id = this.juego()?.id;
    this.http.post<Participacion>(`/api/participaciones/${id}/responder`, { preguntaId, opcionId }).subscribe({
      next: (dato) => this.juego.set(dato),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  deshacer() {
    this.http.post<Participacion>(`/api/participaciones/${this.juego()?.id}/deshacer`, {}).subscribe({
      next: (dato) => this.juego.set(dato),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  finalizar() {
    this.http.post<Participacion>(`/api/participaciones/${this.juego()?.id}/finalizar`, {}).subscribe({
      next: (dato) => this.juego.set(dato),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  mover(delta: number) { this.indice.update((valor) => valor + delta); }
  private cargar() {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<Participacion>(`/api/participaciones/${id}`).subscribe({
      next: (dato) => {
        this.juego.set(dato);
        this.restante.set(Math.max(0, dato.segundosMax - dato.segundosUsados));
      },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
}

@Component({
  selector: 'app-avisos',
  imports: [MarcoComponent],
  template: `
    <app-marco [enlaces]="enlaces" modo="superior">
      <section class="card stack">
        <div class="row">
          <div>
            <p class="kicker">Notificaciones</p>
            <h1>Centro de avisos</h1>
          </div>
          <button class="btn" type="button" (click)="marcar()">Marcar todo como leído</button>
        </div>
        @for (aviso of visibles(); track aviso.id) {
          <article class="item">
            <h3>{{ aviso.titulo }}</h3>
            <p class="sub">{{ aviso.cuerpo }}</p>
          </article>
        } @empty { <p class="sub">No hay avisos pendientes.</p> }
      </section>
    </app-marco>
  `
})
export class AvisosComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  avisos = signal<Aviso[]>([]);
  leidos = signal<number[]>(JSON.parse(localStorage.getItem('dilema-leidos') || '[]'));
  constructor() { this.refrescar(); }
  visibles() { return this.avisos().filter((aviso) => !this.leidos().includes(aviso.id)); }
  marcar() {
    const ids = this.avisos().map((aviso) => aviso.id);
    localStorage.setItem('dilema-leidos', JSON.stringify(ids));
    this.leidos.set(ids);
  }
  private refrescar() {
    this.http.get<PanelEstudiante>('/api/estudiante/panel').subscribe((panel) => this.avisos.set(panel.avisos));
  }
}
