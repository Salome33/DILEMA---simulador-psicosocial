import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MarcoComponent } from '../shared/marco.component';
import { EditorTextoComponent } from '../shared/editor-texto.component';
import { Caso, CasoResumen, Fila, Grupo, Participacion, Sesion, mensajeError } from '../core/api.service';

const enlaces = [
  { href: '/docente/casos', texto: 'Mis Casos', exacto: true },
  { href: '/docente/casos/nuevo', texto: 'Crear Caso', exacto: true },
  { href: '/docente/grupos', texto: 'Grupos', exacto: true },
  { href: '/docente/seguimiento', texto: 'Reportes' }
];

function opcionVacia(correcta = false) {
  return { texto: '', correcta, explicacion: '', fundamento: '' };
}
function preguntaVacia() {
  return { enunciado: '', pista: '', criterio: '', peso: 0, opciones: [opcionVacia(true), opcionVacia(false)] };
}
function casoVacio(): Caso {
  return {
    nombre: '', desarrolloSituacional: '', contexto: '', tiempoEstimadoMin: 30, sensible: false, advertencia: '',
    bloques: [{ nombre: 'Escenario 1', contexto: '', preguntas: [preguntaVacia()] }],
    rubrica: [],
    personajes: [1, 2, 3, 4].map((n) => ({ nombre: `Persona ${n}`, descripcion: '', escenarios: [0] }))
  };
}

@Component({
  selector: 'app-panel-docente',
  imports: [MarcoComponent, RouterLink],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="card stack">
        <p class="kicker">Docente</p>
        <h1>Diseño y seguimiento</h1>
        <p class="sub">Arma el caso en borrador, publícalo cuando la rúbrica cierre y prográmalo con un grupo.</p>
        <a class="btn" routerLink="/docente/casos/nuevo">Nuevo caso</a>
        <a class="btn-ghost" routerLink="/docente/programar">Programar sesión</a>
      </section>
    </app-marco>
  `
})
export class PanelDocenteComponent { enlaces = enlaces; }

@Component({
  selector: 'app-casos',
  imports: [MarcoComponent, RouterLink],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="card stack">
        <div class="row"><h1>Mis casos</h1><a class="btn-ghost" routerLink="/docente/casos/nuevo">Crear caso</a></div>
        @for (caso of casos(); track caso.id) {
          <a class="item" [routerLink]="['/docente/casos', caso.id]">
            <small>{{ caso.estado }} · {{ caso.bloques }} escenarios · {{ caso.preguntas }} preguntas</small>
            <h3>{{ caso.nombre }}</h3>
          </a>
        }
      </section>
    </app-marco>
  `
})
export class CasosComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  casos = signal<CasoResumen[]>([]);
  constructor() { this.http.get<CasoResumen[]>('/api/docente/casos').subscribe((dato) => this.casos.set(dato)); }
}

interface PersonaEscena { id: string; nombre: string; color: string; x: number; y: number; }

@Component({
  selector: 'app-editor-caso',
  imports: [MarcoComponent, FormsModule, RouterLink, EditorTextoComponent],
  template: `
    <app-marco [enlaces]="enlaces" ancho="ancho">
      <section class="stack">
        <div class="row">
          <div>
            <p class="kicker">Creación / edición de caso</p>
            <h1>{{ caso.nombre || 'Crear caso de estudio clínico' }}</h1>
          </div>
          <a class="btn-ghost" routerLink="/docente/casos">Volver</a>
        </div>
        <div class="pasos">
          @for (paso of pasos; track paso.id) {
            <button type="button" [class.activa]="activo() === paso.id" (click)="activo.set(paso.id)">{{ paso.texto }}</button>
          }
        </div>
        @if (error()) { <p class="error">{{ error() }}</p> }
        @if (ok()) { <p class="ok">{{ ok() }}</p> }

        @if (activo() === 'estructura') {
          <section class="card stack">
            <label class="field"><span>NOMBRE</span><input [(ngModel)]="caso.nombre" [disabled]="bloqueado()" /></label>
            <label class="field"><span>DESARROLLO SITUACIONAL</span>
              <app-editor [(valor)]="caso.desarrolloSituacional" [bloqueado]="bloqueado()" />
            </label>
            <label class="field"><span>CONTEXTO</span>
              <app-editor [(valor)]="caso.contexto" [bloqueado]="bloqueado()" />
            </label>
            <label class="field"><span>TIEMPO ESTIMADO (MIN)</span><input type="number" [(ngModel)]="caso.tiempoEstimadoMin" [disabled]="bloqueado()" /></label>
          </section>
        }

        @if (activo() === 'escenas') {
          <section class="card stack">
            <h2>Escenas y contexto</h2>
            <p class="sub">Cada escena lleva su contexto. Las decisiones se arman en el siguiente paso.</p>
            @for (bloque of caso.bloques; track $index) {
              <article class="item stack">
                <label class="field"><span>NOMBRE DEL ESCENARIO</span><input [(ngModel)]="bloque.nombre" [disabled]="bloqueado()" /></label>
                <label class="field"><span>CONTEXTO SITUACIONAL</span><textarea [(ngModel)]="bloque.contexto" [disabled]="bloqueado()"></textarea></label>
                @if (!bloqueado()) { <button class="btn-amber" type="button" (click)="caso.bloques.splice($index, 1)">Quitar escenario</button> }
              </article>
            }
            @if (!bloqueado()) { <button class="btn-ghost" type="button" (click)="agregarBloque()">Agregar escenario académico</button> }
          </section>
        }

        @if (activo() === 'preguntas') {
          <section class="card stack">
            @for (bloque of caso.bloques; track $index) {
              <article class="item stack">
                <h3>{{ bloque.nombre || 'Escenario' }}</h3>
                <p class="sub">{{ bloque.contexto }}</p>
                @for (pregunta of bloque.preguntas; track $index) {
                  <div class="stack">
                    <label class="field"><span>ENUNCIADO</span>
                      <app-editor [(valor)]="pregunta.enunciado" [bloqueado]="bloqueado()" />
                    </label>
                    <label class="field"><span>HERRAMIENTA (PISTA, OPCIONAL)</span><input [(ngModel)]="pregunta.pista" [disabled]="bloqueado()" /></label>
                    <div class="grid-2">
                      <label class="field"><span>CRITERIO</span><input [(ngModel)]="pregunta.criterio" [disabled]="bloqueado()" /></label>
                      <label class="field"><span>PESO</span><input type="number" step="0.1" [(ngModel)]="pregunta.peso" [disabled]="bloqueado()" /></label>
                    </div>
                    @for (opcion of pregunta.opciones; track $index) {
                      <div class="item stack">
                        <label class="field"><span>OPCIÓN</span><input [(ngModel)]="opcion.texto" [disabled]="bloqueado()" /></label>
                        <label class="check"><input type="checkbox" [checked]="opcion.correcta" [disabled]="bloqueado()" (change)="marcar(pregunta, $index)" /> <span>Correcta</span></label>
                        <label class="field"><span>POR QUÉ</span><input [(ngModel)]="opcion.explicacion" [disabled]="bloqueado()" /></label>
                      </div>
                    }
                    @if (!bloqueado()) {
                      <button class="btn-ghost" type="button" (click)="pregunta.opciones.push(opcionVacia())">Agregar opción</button>
                      <button class="btn-amber" type="button" (click)="bloque.preguntas.splice($index, 1)">Quitar pregunta</button>
                    }
                  </div>
                }
                @if (!bloqueado()) { <button class="btn" type="button" (click)="bloque.preguntas.push(preguntaVacia())">Agregar pregunta</button> }
              </article>
            }
          </section>
        }

        @if (activo() === 'rubrica') {
          <section class="card stack">
            <p class="sub">Cada pregunta tiene su criterio. Los pesos van de 0,0 a 5,0 y deben sumar 5,0.</p>
            @for (bloque of caso.bloques; track $index) {
              @for (pregunta of bloque.preguntas; track $index) {
                <div class="row"><span>{{ bloque.nombre }} · {{ pregunta.criterio || 'Sin criterio' }}</span><strong>{{ pregunta.peso || 0 }}</strong></div>
              }
            }
            <p>Suma actual: {{ suma() }}</p>
          </section>
        }

        @if (activo() === 'escena') {
          <section class="card stack">
            <h2>Diseño de escena</h2>
            <p class="sub">Los personajes quedan guardados en el caso. Elige en qué escenarios aparecen y arrástralos. Las cuatro usan la misma pose. La posición es solo el boceto visual.</p>
            @for (personaje of caso.personajes; track $index) {
              <div class="grid-2">
                <label class="field"><span>NOMBRE</span><input [(ngModel)]="personaje.nombre" [disabled]="bloqueado()" (ngModelChange)="sincronizarPersona($index)" /></label>
                <label class="field"><span>DESCRIPCIÓN</span><input [(ngModel)]="personaje.descripcion" [disabled]="bloqueado()" /></label>
              </div>
            }
            <div class="fondos">
              @for (fondo of fondos; track fondo.id) {
                <button type="button" [class.activa]="fondoActivo() === fondo.id" (click)="elegirFondo(fondo.id)">{{ fondo.nombre }}</button>
              }
            </div>
            <div class="escena" [class]="'escena fondo-' + fondoActivo()">
              @for (persona of personas(); track persona.id) {
                <div class="persona" [style.left.%]="persona.x" [style.top.%]="persona.y" (pointerdown)="arrastrar($event, persona)">
                  <svg viewBox="0 0 80 160" aria-hidden="true">
                    <circle cx="40" cy="28" r="16" fill="#f3d3bf"/>
                    <rect x="22" y="48" width="36" height="48" rx="10" [attr.fill]="persona.color"/>
                    <rect x="8" y="52" width="12" height="40" rx="6" [attr.fill]="persona.color"/>
                    <rect x="60" y="52" width="12" height="40" rx="6" [attr.fill]="persona.color"/>
                    <rect x="26" y="98" width="10" height="42" rx="5" fill="#243044"/>
                    <rect x="44" y="98" width="10" height="42" rx="5" fill="#243044"/>
                  </svg>
                  <em>{{ persona.nombre }}</em>
                </div>
              }
            </div>
          </section>
        }

        @if (activo() === 'estado') {
          <section class="card stack">
            <p class="kicker">{{ caso.estado || 'SIN GUARDAR' }}</p>
            <p class="sub">Publicar exige nombre, desarrollo, contexto, al menos un escenario con preguntas, una sola opción correcta, su explicación y pesos que sumen 5,0.</p>
            @if (!bloqueado()) { <button class="btn" type="button" (click)="guardar()">Guardar borrador</button> }
            @if (caso.id && caso.estado === 'BORRADOR') { <button class="btn" type="button" (click)="publicar()">Publicar</button> }
            @if (caso.estado === 'PUBLICADO') { <button class="btn-amber" type="button" (click)="archivar()">Archivar</button> }
            @if (caso.estado === 'PUBLICADO') { <button class="btn-ghost" type="button" (click)="despublicar()">Volver a borrador</button> }
            @if (caso.estado === 'ARCHIVADO') { <button class="btn" type="button" (click)="publicar()">Volver a publicar</button> }
            @if (caso.id) { <button class="btn-ghost" type="button" (click)="duplicar()">Duplicar</button> }
          </section>
        }
        @if (vistaPrevia()) {
          <section class="card stack">
            <p class="kicker">Previsualización</p>
            <h2>{{ caso.nombre || 'Caso sin nombre' }}</h2>
            <div [innerHTML]="caso.desarrolloSituacional"></div>
            <div class="sub" [innerHTML]="caso.contexto"></div>
          </section>
        }
        <div class="row">
          @if (!bloqueado()) { <button class="btn-ghost" type="button" (click)="guardar()">Guardar borrador</button> }
          <button class="btn-ghost" type="button" (click)="vistaPrevia.set(!vistaPrevia())">Previsualizar</button>
        </div>
      </section>
    </app-marco>
  `
})
export class EditorCasoComponent {
  enlaces = enlaces;
  pasos = [
    { id: 'estructura', texto: 'Información general' },
    { id: 'escenas', texto: 'Escenas y contexto' },
    { id: 'preguntas', texto: 'Decisiones y opciones' },
    { id: 'rubrica', texto: 'Rúbrica' },
    { id: 'escena', texto: 'Diseño de escena' },
    { id: 'estado', texto: 'Estado' }
  ];
  fondos = [
    { id: 'hospital', nombre: 'Hospital' },
    { id: 'comisaria', nombre: 'Comisaría' },
    { id: 'aula', nombre: 'Aula' },
    { id: 'comunidad', nombre: 'Comunidad' },
    { id: 'consultorio', nombre: 'Consultorio' }
  ];
  opcionVacia = opcionVacia;
  preguntaVacia = preguntaVacia;
  activo = signal('estructura');
  vistaPrevia = signal(false);
  caso: Caso = casoVacio();
  error = signal('');
  ok = signal('');
  fondoActivo = signal('hospital');
  personas = signal<PersonaEscena[]>([
    { id: 'p1', nombre: 'Persona 1', color: '#3ee0ff', x: 8, y: 58 },
    { id: 'p2', nombre: 'Persona 2', color: '#ffba20', x: 30, y: 58 },
    { id: 'p3', nombre: 'Persona 3', color: '#00ff94', x: 52, y: 58 },
    { id: 'p4', nombre: 'Persona 4', color: '#c4b5fd', x: 74, y: 58 }
  ]);
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private claveBoceto = 'dilema-boceto-nuevo';

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    this.claveBoceto = `dilema-boceto-${id ?? 'nuevo'}`;
    const guardado = localStorage.getItem(this.claveBoceto);
    if (guardado) {
      const boceto = JSON.parse(guardado) as { fondo: string; personas: PersonaEscena[] };
      this.fondoActivo.set(boceto.fondo);
      this.personas.set(boceto.personas);
    }
    if (id) {
      this.http.get<Caso>(`/api/docente/casos/${id}`).subscribe({
        next: (dato) => {
          this.caso = dato;
          this.caso.personajes ??= [];
          this.personas.update((lista) => lista.map((persona, indice) => ({
            ...persona,
            nombre: this.caso.personajes[indice]?.nombre || persona.nombre
          })));
        },
        error: (falla) => this.error.set(mensajeError(falla))
      });
    }
  }
  bloqueado() { return this.caso.estado === 'PUBLICADO' || this.caso.estado === 'ARCHIVADO'; }
  sincronizarPersona(indice: number) {
    const nombre = this.caso.personajes[indice]?.nombre;
    if (!nombre) return;
    this.personas.update((lista) => lista.map((persona, i) => i === indice ? { ...persona, nombre } : persona));
    this.guardarBoceto();
  }
  suma() {
    return this.caso.bloques.reduce((total, bloque) => total + bloque.preguntas.reduce((peso, pregunta) => peso + Number(pregunta.peso || 0), 0), 0);
  }
  agregarBloque() {
    this.caso.bloques.push({ nombre: `Escenario académico ${this.caso.bloques.length + 1}`, contexto: '', preguntas: [preguntaVacia()] });
  }
  marcar(pregunta: { opciones: { correcta: boolean }[] }, indice: number) {
    pregunta.opciones.forEach((opcion, i) => opcion.correcta = i === indice);
  }
  elegirFondo(id: string) { this.fondoActivo.set(id); this.guardarBoceto(); }
  arrastrar(evento: PointerEvent, persona: PersonaEscena) {
    const stage = (evento.currentTarget as HTMLElement).parentElement;
    if (!stage) return;
    const mover = (e: PointerEvent) => {
      const caja = stage.getBoundingClientRect();
      persona.x = Math.min(78, Math.max(0, ((e.clientX - caja.left) / caja.width) * 100 - 8));
      persona.y = Math.min(70, Math.max(0, ((e.clientY - caja.top) / caja.height) * 100 - 10));
      this.personas.set([...this.personas()]);
      this.guardarBoceto();
    };
    const soltar = () => {
      window.removeEventListener('pointermove', mover);
      window.removeEventListener('pointerup', soltar);
    };
    window.addEventListener('pointermove', mover);
    window.addEventListener('pointerup', soltar);
  }
  guardar() {
    this.error.set('');
    const pedido = this.caso.id
      ? this.http.put<Caso>(`/api/docente/casos/${this.caso.id}`, this.caso)
      : this.http.post<Caso>('/api/docente/casos', this.caso);
    pedido.subscribe({
      next: (dato) => {
        this.caso = dato;
        this.ok.set('Borrador guardado.');
        if (!this.route.snapshot.paramMap.get('id')) this.router.navigate(['/docente/casos', dato.id]);
      },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  publicar() {
    this.guardarYluego(() => {
      this.http.post<Caso>(`/api/docente/casos/${this.caso.id}/publicar`, {}).subscribe({
        next: (dato) => { this.caso = dato; this.ok.set('Caso publicado. Ya se puede programar.'); },
        error: (falla) => this.error.set(mensajeError(falla))
      });
    });
  }
  archivar() {
    this.http.post<Caso>(`/api/docente/casos/${this.caso.id}/archivar`, {}).subscribe({
      next: (dato) => { this.caso = dato; this.ok.set('Caso archivado.'); },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  despublicar() {
    this.http.post<Caso>(`/api/docente/casos/${this.caso.id}/despublicar`, {}).subscribe({
      next: (dato) => { this.caso = dato; this.ok.set('El caso volvió a borrador.'); },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  duplicar() {
    this.http.post<Caso>(`/api/docente/casos/${this.caso.id}/duplicar`, {}).subscribe({
      next: (dato) => this.router.navigate(['/docente/casos', dato.id]),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  private guardarYluego(luego: () => void) {
    if (this.caso.estado && this.caso.estado !== 'BORRADOR') { luego(); return; }
    const pedido = this.caso.id
      ? this.http.put<Caso>(`/api/docente/casos/${this.caso.id}`, this.caso)
      : this.http.post<Caso>('/api/docente/casos', this.caso);
    pedido.subscribe({
      next: (dato) => { this.caso = dato; luego(); },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  private guardarBoceto() {
    localStorage.setItem(this.claveBoceto, JSON.stringify({ fondo: this.fondoActivo(), personas: this.personas() }));
  }
}

@Component({
  selector: 'app-grupos',
  imports: [MarcoComponent, FormsModule, RouterLink],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="stack">
        <div>
          <h1>Gestión de grupos</h1>
          <p class="sub">Cohortes, matrícula y asignación de casos.</p>
        </div>
        @if (error()) { <p class="error">{{ error() }}</p> }
        <div class="dos">
          <section class="card stack">
            <div class="row"><h2>Mis grupos</h2><button class="btn-ghost" type="button" (click)="nuevo.set(!nuevo())">Nuevo grupo</button></div>
            @if (nuevo()) {
              <label class="field"><span>Nombre</span><input [(ngModel)]="nombre" /></label>
              <label class="field"><span>Semestre</span><input [(ngModel)]="espacio" placeholder="2026-I" /></label>
              <button class="btn" type="button" (click)="crear()">Crear grupo</button>
            }
            @for (grupo of grupos(); track grupo.id) {
              <button class="item grupo-item" type="button" [class.activa]="seleccionado() === grupo.id" (click)="elegir(grupo.id)">
                <strong>{{ grupo.nombre }}</strong>
                <p class="sub">{{ grupo.espacioAcademico }} · {{ grupo.estudiantes.length }} estudiantes</p>
              </button>
            }
          </section>
          @if (actual(); as grupo) {
            <section class="card stack">
              <div class="row">
                <div>
                  <h2>{{ grupo.nombre }}</h2>
                  <p class="sub">{{ grupo.espacioAcademico }}</p>
                </div>
                <a class="btn" [routerLink]="['/docente/programar']" [queryParams]="{ grupo: grupo.id }">Asignar caso a grupo</a>
              </div>
              <label class="field"><span>Añadir estudiante por correo</span>
                <div class="row">
                  <input [(ngModel)]="correos[grupo.id]" placeholder="estudiante@universidad.edu" />
                  <button class="btn-ghost" type="button" (click)="agregar(grupo.id)">Agregar</button>
                </div>
              </label>
              <table>
                <thead><tr><th>Nombre</th><th>Correo</th><th></th></tr></thead>
                <tbody>
                  @for (estudiante of grupo.estudiantes; track estudiante.id) {
                    <tr>
                      <td>{{ estudiante.nombre }}</td>
                      <td>{{ estudiante.email }}</td>
                      <td><button class="btn-ghost" type="button" (click)="retirar(grupo.id, estudiante.id)">Eliminar</button></td>
                    </tr>
                  } @empty { <tr><td colspan="3">Este grupo no tiene estudiantes.</td></tr> }
                </tbody>
              </table>
            </section>
          }
        </div>
      </section>
    </app-marco>
  `
})
export class GruposComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  grupos = signal<Grupo[]>([]);
  seleccionado = signal<number | null>(null);
  nuevo = signal(false);
  actual = computed(() => this.grupos().find((g) => g.id === this.seleccionado()) ?? null);
  nombre = '';
  espacio = '';
  correos: Record<number, string> = {};
  error = signal('');
  constructor() { this.cargar(); }
  crear() {
    this.http.post<Grupo>('/api/docente/grupos', { nombre: this.nombre, espacioAcademico: this.espacio }).subscribe({
      next: () => { this.nombre = ''; this.espacio = ''; this.cargar(); },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  agregar(id: number) {
    this.http.post(`/api/docente/grupos/${id}/estudiantes`, { email: this.correos[id] ?? '' }).subscribe({
      next: () => this.cargar(),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  retirar(grupoId: number, estudianteId: number) {
    this.http.delete(`/api/docente/grupos/${grupoId}/estudiantes/${estudianteId}`).subscribe({ next: () => this.cargar() });
  }
  elegir(id: number) { this.seleccionado.set(id); }
  private cargar() {
    this.http.get<Grupo[]>('/api/docente/grupos').subscribe((dato) => {
      this.grupos.set(dato);
      if (!dato.some((g) => g.id === this.seleccionado())) this.seleccionado.set(dato[0]?.id ?? null);
    });
  }
}

@Component({
  selector: 'app-programar',
  imports: [MarcoComponent, FormsModule, RouterLink],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="stack">
        <div>
          <p class="kicker">Módulo de planificación</p>
          <h1>Agendar caso de estudio a grupo</h1>
        </div>
        <div class="dos-ancha">
          <section class="card stack">
            <h2>Caso y grupo</h2>
            <label class="field"><span>Caso publicado</span>
              <select [(ngModel)]="casoId">
                @for (caso of casos(); track caso.id) { @if (caso.estado === 'PUBLICADO') { <option [ngValue]="caso.id">{{ caso.nombre }}</option> } }
              </select>
            </label>
            <label class="field"><span>Grupo</span>
              <select [(ngModel)]="grupoId">
                @for (grupo of grupos(); track grupo.id) { <option [ngValue]="grupo.id">{{ grupo.nombre }} · {{ grupo.espacioAcademico }}</option> }
              </select>
            </label>
            <h2>Programación</h2>
            <div class="grid-2">
              <label class="field"><span>Inicio</span><input type="datetime-local" [(ngModel)]="inicio" /></label>
              <label class="field"><span>Fin</span><input type="datetime-local" [(ngModel)]="fin" /></label>
            </div>
            <div class="grid-2">
              <label class="field"><span>Tiempo máximo (min)</span><input type="number" [(ngModel)]="tiempoMax" /></label>
              <label class="field"><span>Tolerancia de ingreso (min)</span><input type="number" [(ngModel)]="tolerancia" /></label>
            </div>
            <label class="field"><span>Lugar</span><input [(ngModel)]="lugar" /></label>
            <label class="field"><span>Mensaje para los estudiantes</span><textarea [(ngModel)]="mensaje"></textarea></label>
            @if (error()) { <p class="error">{{ error() }}</p> }
            @if (ok()) { <p class="ok">{{ ok() }}</p> }
            <div class="row">
              <a class="btn-ghost" routerLink="/docente/grupos">Cancelar</a>
              <button class="btn-ghost" type="button" (click)="borrador()">Guardar borrador</button>
              <button class="btn" type="button" (click)="agendar()">Guardar y notificar</button>
            </div>
          </section>
          <aside class="card stack">
            <h2>Resumen</h2>
            <p><strong>Caso.</strong> {{ nombreCaso() }}</p>
            <p><strong>Grupo.</strong> {{ nombreGrupo() }}</p>
            <p><strong>Ventana.</strong> {{ inicio || '—' }} → {{ fin || '—' }}</p>
            <p><strong>Tiempo.</strong> {{ tiempoMax }} minutos · tolerancia {{ tolerancia }}</p>
            <p><strong>Lugar.</strong> {{ lugar || 'Sin lugar físico' }}</p>
            <p class="sub">Al notificar, cada estudiante autorizado recibe el aviso. El código de acceso no se muestra en pantalla.</p>
          </aside>
        </div>
        <h2>Convocatorias</h2>
        @for (sesion of sesiones(); track sesion.id) {
          <article class="item stack">
            <small>{{ sesion.estado }}</small>
            <h3>{{ sesion.caso }}</h3>
            <p class="sub">{{ sesion.grupo }} · {{ sesion.inicio }} → {{ sesion.fin }}</p>
            <div class="row">
              @if (sesion.estado === 'SIN_INICIAR') { <button class="btn" type="button" (click)="accion(sesion.id, 'iniciar')">Iniciar</button> }
              @if (sesion.estado === 'EN_CURSO') { <button class="btn-amber" type="button" (click)="accion(sesion.id, 'finalizar')">Finalizar</button> }
            </div>
          </article>
        }
      </section>
    </app-marco>
  `
})
export class ProgramarComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  casos = signal<CasoResumen[]>([]);
  grupos = signal<Grupo[]>([]);
  sesiones = signal<Sesion[]>([]);
  casoId = 0;
  grupoId = 0;
  inicio = '';
  fin = '';
  tiempoMax = 30;
  tolerancia = 15;
  lugar = '';
  mensaje = '';
  error = signal('');
  ok = signal('');
  constructor() {
    const guardado = localStorage.getItem('dilema-agenda');
    if (guardado) {
      try {
        const dato = JSON.parse(guardado);
        this.casoId = dato.casoId ?? 0;
        this.grupoId = dato.grupoId ?? 0;
        this.inicio = dato.inicio ?? '';
        this.fin = dato.fin ?? '';
        this.tiempoMax = dato.tiempoMax ?? 30;
        this.tolerancia = dato.tolerancia ?? 15;
        this.lugar = dato.lugar ?? '';
        this.mensaje = dato.mensaje ?? '';
      } catch { localStorage.removeItem('dilema-agenda'); }
    }
    const grupo = Number(this.route.snapshot.queryParamMap.get('grupo') || 0);
    if (grupo) this.grupoId = grupo;
    this.cargar();
  }
  nombreCaso() { return this.casos().find((c) => c.id === Number(this.casoId))?.nombre ?? 'Sin caso'; }
  nombreGrupo() { return this.grupos().find((g) => g.id === Number(this.grupoId))?.nombre ?? 'Sin grupo'; }
  borrador() {
    localStorage.setItem('dilema-agenda', JSON.stringify({
      casoId: this.casoId, grupoId: this.grupoId, inicio: this.inicio, fin: this.fin,
      tiempoMax: this.tiempoMax, tolerancia: this.tolerancia, lugar: this.lugar, mensaje: this.mensaje
    }));
    this.ok.set('Borrador guardado en este navegador.');
    this.error.set('');
  }
  agendar() {
    this.http.post<Sesion>('/api/docente/sesiones', {
      casoId: Number(this.casoId), grupoId: Number(this.grupoId), inicio: this.inicio, fin: this.fin,
      tiempoMaxMin: Number(this.tiempoMax), toleranciaMin: Number(this.tolerancia), lugar: this.lugar, mensaje: this.mensaje
    }).subscribe({
      next: () => {
        this.ok.set('Convocatoria registrada. Los estudiantes reciben el aviso sin que el código quede visible.');
        localStorage.removeItem('dilema-agenda');
        this.cargar();
      },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  accion(id: number, verbo: string) {
    this.http.post(`/api/docente/sesiones/${id}/${verbo}`, {}).subscribe({ next: () => this.cargar(), error: (falla) => this.error.set(mensajeError(falla)) });
  }
  private cargar() {
    this.http.get<CasoResumen[]>('/api/docente/casos').subscribe((dato) => {
      this.casos.set(dato);
      if (!dato.some((c) => c.id === Number(this.casoId))) this.casoId = dato.find((c) => c.estado === 'PUBLICADO')?.id ?? 0;
    });
    this.http.get<Grupo[]>('/api/docente/grupos').subscribe((dato) => {
      this.grupos.set(dato);
      if (!dato.some((g) => g.id === Number(this.grupoId))) this.grupoId = dato[0]?.id ?? 0;
    });
    this.http.get<Sesion[]>('/api/docente/sesiones').subscribe((dato) => this.sesiones.set(dato));
  }
}

@Component({
  selector: 'app-seguimiento',
  imports: [MarcoComponent, RouterLink, FormsModule],
  template: `
    <app-marco [enlaces]="enlaces">
      <section class="stack">
        <div>
          <h1>Reportes de participación</h1>
          <p class="sub">Desempeño, estado de la bitácora y descarga del grupo.</p>
        </div>
        <div class="filtros">
          <label class="field"><span>Caso</span>
            <select [(ngModel)]="sesionId" (ngModelChange)="ver($event)">
              @for (sesion of sesiones(); track sesion.id) {
                <option [ngValue]="sesion.id">{{ sesion.caso }} · {{ sesion.grupo }}</option>
              }
            </select>
          </label>
          <label class="field"><span>Estudiante</span>
            <input [(ngModel)]="busqueda" placeholder="Filtrar por estudiante" />
          </label>
        </div>
        <div class="dos-ancha">
          <table>
            <thead><tr><th>Estudiante</th><th>Estado</th><th>Puntaje</th><th>Nivel</th><th></th></tr></thead>
            <tbody>
              @for (fila of visibles(); track fila.email) {
                <tr>
                  <td>{{ fila.estudiante }}<br><small>{{ fila.email }}</small></td>
                  <td>{{ fila.estado }}</td>
                  <td>{{ fila.nota ?? '—' }} / 5,0</td>
                  <td>{{ nivel(fila.nota) }}</td>
                  <td>
                    @if (fila.participacionId) { <a class="btn-ghost" [routerLink]="['/docente/seguimiento', fila.participacionId]">Ver respuestas</a> }
                  </td>
                </tr>
              } @empty { <tr><td colspan="5">No hay estudiantes en esta convocatoria.</td></tr> }
            </tbody>
          </table>
          <aside class="card stack">
            <h2>Ficha de cohorte</h2>
            <p class="nota">{{ promedio() }}</p>
            <p class="sub">Promedio sobre 5,0</p>
            <p>Finalizados: {{ finalizados() }} / {{ filas().length }}</p>
            <p>Pendientes: {{ filas().length - finalizados() }}</p>
            <p>Superior {{ conteo('Superior') }} · Alto {{ conteo('Alto') }} · Básico {{ conteo('Básico') }} · Bajo {{ conteo('Bajo') }}</p>
            <button class="btn" type="button" (click)="descargar()">Descargar CSV</button>
          </aside>
        </div>
      </section>
    </app-marco>
  `
})
export class SeguimientoComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  sesiones = signal<Sesion[]>([]);
  filas = signal<Fila[]>([]);
  sesionId = 0;
  busqueda = '';
  constructor() {
    this.http.get<Sesion[]>('/api/docente/sesiones').subscribe((dato) => {
      this.sesiones.set(dato);
      if (dato[0]) this.ver(dato[0].id);
    });
  }
  ver(id: number) {
    this.sesionId = Number(id);
    this.http.get<Fila[]>(`/api/docente/sesiones/${this.sesionId}/seguimiento`).subscribe((dato) => this.filas.set(dato));
  }
  visibles() {
    const q = this.busqueda.trim().toLowerCase();
    return this.filas().filter((f) => !q || f.estudiante.toLowerCase().includes(q) || f.email.toLowerCase().includes(q));
  }
  nivel(nota: number | null) {
    if (nota == null) return '—';
    if (nota >= 4.6) return 'Superior';
    if (nota >= 4) return 'Alto';
    if (nota >= 3) return 'Básico';
    return 'Bajo';
  }
  finalizados() { return this.filas().filter((f) => f.estado === 'FINALIZADA' || f.estado === 'AUSENTE').length; }
  promedio() {
    const notas = this.filas().map((f) => f.nota).filter((n): n is number => n != null);
    if (!notas.length) return '—';
    return (notas.reduce((a, b) => a + b, 0) / notas.length).toFixed(1);
  }
  conteo(nivel: string) { return this.filas().filter((f) => this.nivel(f.nota) === nivel).length; }
  descargar() {
    this.http.get(`/api/docente/sesiones/${this.sesionId}/reporte.csv`, { responseType: 'text' }).subscribe((texto) => {
      const url = URL.createObjectURL(new Blob([texto], { type: 'text/csv' }));
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = `reporte-${this.sesionId}.csv`;
      enlace.click();
      URL.revokeObjectURL(url);
    });
  }
}

@Component({
  selector: 'app-detalle-participacion',
  imports: [MarcoComponent, FormsModule],
  template: `
    <app-marco [enlaces]="enlaces">
      @if (dato(); as actual) {
        <section class="card stack">
          <p class="kicker">{{ actual.estudiante }} · {{ actual.estado }}</p>
          <h1>{{ actual.caso }}</h1>
          <p class="nota">{{ actual.nota ?? '—' }}</p>
          @for (pregunta of actual.preguntas; track pregunta.id) {
            <article class="item">
              <small>{{ pregunta.bloque }} · {{ pregunta.criterio }}</small>
              <h3>{{ pregunta.enunciado }}</h3>
              @for (opcion of pregunta.opciones; track opcion.id) {
                <p [class.ok]="opcion.correcta" [class.error]="pregunta.seleccionId === opcion.id && !opcion.correcta">
                  {{ pregunta.seleccionId === opcion.id ? 'Elegida: ' : '' }}{{ opcion.texto }}
                </p>
              }
            </article>
          }
          <label class="field"><span>VALORACIÓN GENERAL</span><textarea [(ngModel)]="texto" [disabled]="actual.estado !== 'FINALIZADA'"></textarea></label>
          @if (error()) { <p class="error">{{ error() }}</p> }
          @if (ok()) { <p class="ok">{{ ok() }}</p> }
          <button class="btn" type="button" [disabled]="actual.estado !== 'FINALIZADA'" (click)="valorar(actual.id)">Guardar valoración</button>
          <button class="btn-amber" type="button" (click)="repetir(actual.id)">Autorizar repetición</button>
        </section>
      }
    </app-marco>
  `
})
export class DetalleParticipacionComponent {
  enlaces = enlaces;
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  dato = signal<Participacion | null>(null);
  texto = '';
  error = signal('');
  ok = signal('');
  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<Participacion>(`/api/participaciones/${id}`).subscribe({
      next: (dato) => { this.dato.set(dato); this.texto = dato.retroalimentacion ?? ''; },
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  valorar(id: number) {
    this.http.post(`/api/docente/participaciones/${id}/retroalimentacion`, { texto: this.texto }).subscribe({
      next: () => this.ok.set('Valoración registrada. El estudiante recibe el aviso.'),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
  repetir(id: number) {
    this.http.post(`/api/docente/participaciones/${id}/repeticion`, {}).subscribe({
      next: () => this.ok.set('Repetición autorizada para un intento adicional.'),
      error: (falla) => this.error.set(mensajeError(falla))
    });
  }
}
