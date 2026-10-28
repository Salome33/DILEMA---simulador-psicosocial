# NEURAL_SIM — Simulador Interactivo de Psicología Social

Plataforma **Python (Flask)** alineada con los requisitos del documento Notion y el diseño **NeuroCommand** del ZIP de Stitch.

## Ejecutar

```powershell
cd "C:\Users\JOHN JAIRO\Projects\simulador-psicologia-social-python"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python app.py
```

http://localhost:5000

## Cuentas demo

| Rol | Correo | Contraseña |
|-----|--------|------------|
| Estudiante | estudiante@psicosim.com | estudiante123 |
| Profesor | profesor@psicosim.com | profesor123 |
| Admin | admin@psicosim.com | admin123 |

## Flujo del estudiante (MVP)

1. Login → Panel de actividades
2. Caso asignado → Briefing → **Advertencia de contenido sensible**
3. **Tutorial** (3 pasos) → Introducción narrativa
4. **Simulación** (Hospital + Comisaría) con guardado de progreso
5. **Bitácora reflexiva** tras escenario hospitalario
6. **Resultados** + **Informe final**
7. **Caja de herramientas** (PAP, EPICEE/SPIKES, rutas, conceptos)

## Módulo docente

- Grupos académicos y asignación de casos
- Seguimiento de estudiantes y resultados
- Revisión de bitácoras
- Creación/publicación de casos (borrador → publicado)
- **Observaciones docentes** (RF-55)

## Módulo admin

- Usuarios (CRUD)
- Grupos académicos
- **Auditoría** de accesos y acciones

## Requisitos implementados

| Área | Implementación |
|------|----------------|
| RI-01 Usuario | SQLite + roles |
| RI-03 Grupo académico | `academic_groups` |
| RI-05 Caso | `simulation_cases` + semilla Caso 1 |
| RI-06 Asignación | `case_assignments` |
| RI-07 Escenarios | Hospital + Comisaría |
| RI-16 Participación | Progreso guardado / reanudar |
| RI-17 Respuestas | Registro por decisión |
| RI-21/22 Advertencia | Aceptación obligatoria |
| RNF Seguridad | Sanitización de entradas |
| RNF Recuperación | Progreso en BD + API `/api/progreso` |

## Diseño (ZIP Stitch)

Referencia en `design-reference/stitch_gamified_simulation_web_interface/`:
- Tema **Cyber-Surgical Glassmorphism** (cyan/amber/emerald)
- Tipografía Inter + JetBrains Mono
- Login estilo NEURAL_SIM

## Estructura

```
app.py, database.py, seeds.py, security.py
routes/     auth, estudiante, profesor, admin, api
templates/  por módulo + layouts
static/     css (NeuroCommand), js (simulación + animaciones)
```
