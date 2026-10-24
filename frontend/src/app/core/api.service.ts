import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('dilema-token');
  const pedido = (!token || req.url.includes('/api/auth/ingresar'))
    ? next(req)
    : next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  return pedido.pipe(catchError((error: unknown) => {
    if (error instanceof HttpErrorResponse && error.status === 401 && !req.url.includes('/api/auth/')) {
      localStorage.removeItem('dilema-token');
      localStorage.removeItem('dilema-usuario');
      localStorage.removeItem('dilema-sesion');
      if (!location.pathname.endsWith('/ingreso')) location.assign('/ingreso');
    }
    return throwError(() => error);
  }));
};

export function mensajeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const cuerpo = error.error as { mensaje?: string } | null;
    return cuerpo?.mensaje ?? 'No se pudo completar la operación.';
  }
  return 'No se pudo completar la operación.';
}

export interface Usuario { id: number; nombre: string; email: string; rol: string; permisos: string[]; }
export interface SesionAuth { token: string; usuario: Usuario; sesionId: number | null; }
export interface Opcion { id?: number; texto: string; correcta: boolean; explicacion: string; fundamento: string; }
export interface Pregunta { id?: number; enunciado: string; pista: string; criterio: string; peso: number; opciones: Opcion[]; }
export interface Bloque { id?: number; nombre: string; contexto: string; preguntas: Pregunta[]; }
export interface PersonajeCaso { id?: number; nombre: string; descripcion: string; escenarios: number[]; }
export interface Criterio { id?: number; nombre: string; peso: number; descripcion: string; }
export interface Caso {
  id?: number; nombre: string; desarrolloSituacional: string; contexto: string; tiempoEstimadoMin: number;
  sensible: boolean; advertencia: string; estado?: string; bloques: Bloque[]; rubrica: Criterio[];
  personajes: PersonajeCaso[];
}
export interface CasoResumen { id: number; nombre: string; estado: string; tiempoEstimadoMin: number; bloques: number; preguntas: number; }
export interface Persona { id: number; nombre: string; email: string; }
export interface Grupo { id: number; nombre: string; espacioAcademico: string; docente: Persona; estudiantes: Persona[]; }
export interface Sesion {
  id: number; codigo: string; estado: string; inicio: string; fin: string; tiempoMaxMin: number; toleranciaMin: number;
  lugar: string; mensaje: string; casoId: number; caso: string; grupoId: number; grupo: string; espacio: string;
  desarrollo: string; contexto: string; sensible: boolean; advertencia: string;
  participacionId: number | null; participacionEstado: string | null; nota: number | null; progreso: number | null;
}
export interface Aviso { id: number; canal: string; titulo: string; cuerpo: string; leida: boolean; fecha: string; }
export interface PanelEstudiante { sesiones: Sesion[]; avisos: Aviso[]; sinLeer: number; }
export interface OpcionJuego { id: number; texto: string; correcta: boolean | null; explicacion: string | null; fundamento: string | null; }
export interface PreguntaJuego {
  id: number; bloque: string; contexto: string; enunciado: string; pista: string | null; criterio: string;
  seleccionId: number | null; opciones: OpcionJuego[];
}
export interface Participacion {
  id: number; sesionId: number; estado: string; caso: string; progreso: number; total: number;
  segundosMax: number; segundosUsados: number; nota: number | null; retroalimentacion: string | null;
  estudiante: string; preguntas: PreguntaJuego[];
}
export interface Fila { participacionId: number | null; estudianteId: number; estudiante: string; email: string; estado: string; nota: number | null; retroalimentacion: string | null; }
export interface Auditoria { id: number; fecha: string; accion: string; detalle: string; usuario: string; }
