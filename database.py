import sqlite3
from contextlib import contextmanager
from datetime import datetime, timezone

from werkzeug.security import check_password_hash, generate_password_hash

from config import DATABASE_PATH, DEFAULT_USERS
from seeds import seed_demo_data


def _now():
    return datetime.now(timezone.utc).isoformat()


@contextmanager
def get_db():
    conn = sqlite3.connect(DATABASE_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


SCHEMA = """
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    nombre TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('estudiante', 'profesor', 'admin')),
    activo INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS academic_groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    asignatura TEXT NOT NULL,
    periodo TEXT NOT NULL,
    docente_id INTEGER,
    estado TEXT NOT NULL DEFAULT 'activo',
    created_at TEXT NOT NULL,
    FOREIGN KEY (docente_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_groups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    vinculo TEXT NOT NULL,
    estado TEXT NOT NULL DEFAULT 'activo',
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (group_id) REFERENCES academic_groups(id)
);

CREATE TABLE IF NOT EXISTS simulation_cases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    titulo TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    proposito TEXT NOT NULL,
    tematica TEXT NOT NULL,
    estado TEXT NOT NULL DEFAULT 'borrador',
    docente_id INTEGER,
    contenido_sensible INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    FOREIGN KEY (docente_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS scenarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id INTEGER NOT NULL,
    nombre TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    orden INTEGER NOT NULL,
    contenido_sensible INTEGER NOT NULL DEFAULT 0,
    estado TEXT NOT NULL DEFAULT 'activo',
    icono TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (case_id) REFERENCES simulation_cases(id)
);

CREATE TABLE IF NOT EXISTS case_assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id INTEGER NOT NULL,
    group_id INTEGER NOT NULL,
    fecha_inicio TEXT,
    fecha_cierre TEXT,
    estado TEXT NOT NULL DEFAULT 'activo',
    created_at TEXT NOT NULL,
    FOREIGN KEY (case_id) REFERENCES simulation_cases(id),
    FOREIGN KEY (group_id) REFERENCES academic_groups(id)
);

CREATE TABLE IF NOT EXISTS sensitive_warnings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    case_id INTEGER NOT NULL,
    scenario_id INTEGER,
    mensaje TEXT NOT NULL,
    requiere_aceptacion INTEGER NOT NULL DEFAULT 1,
    version INTEGER NOT NULL DEFAULT 1,
    estado TEXT NOT NULL DEFAULT 'activo',
    created_at TEXT NOT NULL,
    FOREIGN KEY (case_id) REFERENCES simulation_cases(id),
    FOREIGN KEY (scenario_id) REFERENCES scenarios(id)
);

CREATE TABLE IF NOT EXISTS warning_acceptances (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    case_id INTEGER NOT NULL,
    scenario_id INTEGER,
    version INTEGER NOT NULL,
    accepted_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (case_id) REFERENCES simulation_cases(id)
);

CREATE TABLE IF NOT EXISTS participations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    case_id INTEGER NOT NULL,
    group_id INTEGER,
    estado TEXT NOT NULL DEFAULT 'en_progreso',
    scene_index INTEGER NOT NULL DEFAULT 0,
    question_index INTEGER NOT NULL DEFAULT 0,
    score INTEGER NOT NULL DEFAULT 0,
    phase TEXT NOT NULL DEFAULT 'welcome',
    started_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    finished_at TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (case_id) REFERENCES simulation_cases(id),
    FOREIGN KEY (group_id) REFERENCES academic_groups(id)
);

CREATE TABLE IF NOT EXISTS student_responses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participation_id INTEGER NOT NULL,
    scene_index INTEGER NOT NULL,
    question_index INTEGER NOT NULL,
    option_id TEXT NOT NULL,
    puntos INTEGER NOT NULL DEFAULT 0,
    correcta INTEGER NOT NULL DEFAULT 0,
    feedback_tipo TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (participation_id) REFERENCES participations(id)
);

CREATE TABLE IF NOT EXISTS simulation_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    participation_id INTEGER,
    caso TEXT NOT NULL,
    puntaje INTEGER NOT NULL,
    total INTEGER NOT NULL,
    porcentaje INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (participation_id) REFERENCES participations(id)
);

CREATE TABLE IF NOT EXISTS reflective_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participation_id INTEGER NOT NULL,
    scenario_index INTEGER NOT NULL,
    contenido TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (participation_id) REFERENCES participations(id)
);

CREATE TABLE IF NOT EXISTS teacher_observations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    teacher_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    case_id INTEGER,
    participation_id INTEGER,
    observacion TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (student_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    accion TEXT NOT NULL,
    detalle TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS system_parameters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL,
    nombre TEXT NOT NULL,
    valor TEXT NOT NULL,
    estado TEXT NOT NULL DEFAULT 'activo',
    created_at TEXT NOT NULL
);
"""


def init_db():
    with get_db() as conn:
        conn.executescript(SCHEMA)
        _migrate_legacy(conn)
        if conn.execute("SELECT COUNT(*) c FROM users").fetchone()["c"] == 0:
            for user in DEFAULT_USERS:
                conn.execute(
                    """
                    INSERT INTO users (email, password_hash, nombre, role, activo, created_at)
                    VALUES (?, ?, ?, ?, 1, ?)
                    """,
                    (
                        user["email"].lower(),
                        generate_password_hash(user["password"]),
                        user["nombre"],
                        user["role"],
                        _now(),
                    ),
                )
        seed_demo_data(conn)


def _migrate_legacy(conn):
    cols = {r[1] for r in conn.execute("PRAGMA table_info(simulation_results)").fetchall()}
    if cols and "participation_id" not in cols:
        conn.execute("ALTER TABLE simulation_results ADD COLUMN participation_id INTEGER")


def log_audit(user_id, accion, detalle=""):
    with get_db() as conn:
        conn.execute(
            "INSERT INTO audit_logs (user_id, accion, detalle, created_at) VALUES (?, ?, ?, ?)",
            (user_id, accion, detalle, _now()),
        )


# ── Usuarios ──

def get_user_by_email(email):
    with get_db() as conn:
        row = conn.execute(
            "SELECT * FROM users WHERE email = ? AND activo = 1", (email.lower().strip(),)
        ).fetchone()
        return dict(row) if row else None


def get_user_by_id(user_id):
    with get_db() as conn:
        row = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
        return dict(row) if row else None


def verify_password(user, password):
    return check_password_hash(user["password_hash"], password)


def list_users(role=None):
    with get_db() as conn:
        q = "SELECT id, email, nombre, role, activo, created_at FROM users"
        if role:
            rows = conn.execute(q + " WHERE role = ? ORDER BY nombre", (role,)).fetchall()
        else:
            rows = conn.execute(q + " ORDER BY role, nombre").fetchall()
        return [dict(r) for r in rows]


def create_user(email, password, nombre, role):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO users (email, password_hash, nombre, role, activo, created_at)
            VALUES (?, ?, ?, ?, 1, ?)
            """,
            (email.lower(), generate_password_hash(password), nombre.strip(), role, _now()),
        )


def update_user(user_id, nombre, role, activo, password=None):
    with get_db() as conn:
        if password:
            conn.execute(
                "UPDATE users SET nombre=?, role=?, activo=?, password_hash=? WHERE id=?",
                (nombre.strip(), role, 1 if activo else 0, generate_password_hash(password), user_id),
            )
        else:
            conn.execute(
                "UPDATE users SET nombre=?, role=?, activo=? WHERE id=?",
                (nombre.strip(), role, 1 if activo else 0, user_id),
            )


def delete_user(user_id):
    with get_db() as conn:
        conn.execute("DELETE FROM users WHERE id = ?", (user_id,))


# ── Grupos ──

def list_groups(docente_id=None):
    with get_db() as conn:
        if docente_id:
            rows = conn.execute(
                """
                SELECT g.*, u.nombre AS docente_nombre,
                    (SELECT COUNT(*) FROM user_groups ug WHERE ug.group_id = g.id AND ug.vinculo='estudiante') AS num_estudiantes
                FROM academic_groups g
                LEFT JOIN users u ON u.id = g.docente_id
                WHERE g.docente_id = ? OR ? IS NULL
                ORDER BY g.nombre
                """,
                (docente_id, docente_id),
            ).fetchall()
        else:
            rows = conn.execute(
                """
                SELECT g.*, u.nombre AS docente_nombre,
                    (SELECT COUNT(*) FROM user_groups ug WHERE ug.group_id = g.id AND ug.vinculo='estudiante') AS num_estudiantes
                FROM academic_groups g
                LEFT JOIN users u ON u.id = g.docente_id
                ORDER BY g.nombre
                """
            ).fetchall()
        return [dict(r) for r in rows]


def create_group(nombre, asignatura, periodo, docente_id):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO academic_groups (nombre, asignatura, periodo, docente_id, estado, created_at)
            VALUES (?, ?, ?, ?, 'activo', ?)
            """,
            (nombre, asignatura, periodo, docente_id, _now()),
        )
        return conn.execute("SELECT last_insert_rowid() id").fetchone()["id"]


def get_group_students(group_id):
    with get_db() as conn:
        rows = conn.execute(
            """
            SELECT u.id, u.nombre, u.email
            FROM user_groups ug
            JOIN users u ON u.id = ug.user_id
            WHERE ug.group_id = ? AND ug.vinculo = 'estudiante' AND ug.estado = 'activo'
            ORDER BY u.nombre
            """,
            (group_id,),
        ).fetchall()
        return [dict(r) for r in rows]


# ── Casos ──

def get_assigned_cases_for_student(user_id):
    with get_db() as conn:
        rows = conn.execute(
            """
            SELECT DISTINCT c.*, ca.fecha_inicio, ca.fecha_cierre,
                p.id AS participation_id, p.estado AS participation_estado,
                p.score, p.scene_index, p.question_index
            FROM simulation_cases c
            JOIN case_assignments ca ON ca.case_id = c.id AND ca.estado = 'activo'
            JOIN user_groups ug ON ug.group_id = ca.group_id AND ug.user_id = ?
            LEFT JOIN participations p ON p.case_id = c.id AND p.user_id = ?
            WHERE c.estado = 'publicado' AND ug.estado = 'activo'
            ORDER BY c.titulo
            """,
            (user_id, user_id),
        ).fetchall()
        return [dict(r) for r in rows]


def get_case_by_id(case_id):
    with get_db() as conn:
        row = conn.execute("SELECT * FROM simulation_cases WHERE id = ?", (case_id,)).fetchone()
        return dict(row) if row else None


def get_case_scenarios(case_id):
    with get_db() as conn:
        rows = conn.execute(
            "SELECT * FROM scenarios WHERE case_id = ? ORDER BY orden", (case_id,)
        ).fetchall()
        return [dict(r) for r in rows]


def list_cases(estado=None):
    with get_db() as conn:
        if estado:
            rows = conn.execute(
                "SELECT c.*, u.nombre AS docente_nombre FROM simulation_cases c "
                "LEFT JOIN users u ON u.id = c.docente_id WHERE c.estado = ? ORDER BY c.created_at DESC",
                (estado,),
            ).fetchall()
        else:
            rows = conn.execute(
                "SELECT c.*, u.nombre AS docente_nombre FROM simulation_cases c "
                "LEFT JOIN users u ON u.id = c.docente_id ORDER BY c.created_at DESC"
            ).fetchall()
        return [dict(r) for r in rows]


def create_case(titulo, descripcion, proposito, tematica, docente_id, sensible=False):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO simulation_cases (titulo, descripcion, proposito, tematica, estado, docente_id, contenido_sensible, created_at)
            VALUES (?, ?, ?, ?, 'borrador', ?, ?, ?)
            """,
            (titulo, descripcion, proposito, tematica, docente_id, 1 if sensible else 0, _now()),
        )
        return conn.execute("SELECT last_insert_rowid() id").fetchone()["id"]


def publish_case(case_id):
    with get_db() as conn:
        n = conn.execute("SELECT COUNT(*) c FROM scenarios WHERE case_id = ?", (case_id,)).fetchone()["c"]
        if n < 1:
            return False, "El caso necesita al menos un escenario."
        conn.execute("UPDATE simulation_cases SET estado = 'publicado' WHERE id = ?", (case_id,))
        return True, "Caso publicado."


def assign_case_to_group(case_id, group_id, fecha_inicio, fecha_cierre):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO case_assignments (case_id, group_id, fecha_inicio, fecha_cierre, estado, created_at)
            VALUES (?, ?, ?, ?, 'activo', ?)
            """,
            (case_id, group_id, fecha_inicio, fecha_cierre, _now()),
        )


# ── Advertencias ──

def get_case_warning(case_id):
    with get_db() as conn:
        row = conn.execute(
            "SELECT * FROM sensitive_warnings WHERE case_id = ? AND estado = 'activo' ORDER BY id DESC LIMIT 1",
            (case_id,),
        ).fetchone()
        return dict(row) if row else None


def has_accepted_warning(user_id, case_id):
    with get_db() as conn:
        w = conn.execute(
            "SELECT * FROM sensitive_warnings WHERE case_id = ? AND estado = 'activo' ORDER BY id DESC LIMIT 1",
            (case_id,),
        ).fetchone()
        if not w or not w["requiere_aceptacion"]:
            return True
        row = conn.execute(
            "SELECT 1 FROM warning_acceptances WHERE user_id = ? AND case_id = ? AND version = ?",
            (user_id, case_id, w["version"]),
        ).fetchone()
        return row is not None


def accept_warning(user_id, case_id):
    w = get_case_warning(case_id)
    if not w:
        return
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO warning_acceptances (user_id, case_id, scenario_id, version, accepted_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            (user_id, case_id, w.get("scenario_id"), w["version"], _now()),
        )


# ── Participación y progreso ──

def get_or_create_participation(user_id, case_id, group_id=None):
    with get_db() as conn:
        row = conn.execute(
            """
            SELECT * FROM participations
            WHERE user_id = ? AND case_id = ? AND estado != 'finalizado'
            ORDER BY id DESC LIMIT 1
            """,
            (user_id, case_id),
        ).fetchone()
        if row:
            return dict(row)
        conn.execute(
            """
            INSERT INTO participations (user_id, case_id, group_id, estado, started_at, updated_at)
            VALUES (?, ?, ?, 'en_progreso', ?, ?)
            """,
            (user_id, case_id, group_id, _now(), _now()),
        )
        pid = conn.execute("SELECT last_insert_rowid() id").fetchone()["id"]
        row = conn.execute("SELECT * FROM participations WHERE id = ?", (pid,)).fetchone()
        return dict(row)


def get_participation(participation_id):
    with get_db() as conn:
        row = conn.execute("SELECT * FROM participations WHERE id = ?", (participation_id,)).fetchone()
        return dict(row) if row else None


def save_progress(participation_id, scene_index, question_index, score, phase):
    with get_db() as conn:
        conn.execute(
            """
            UPDATE participations
            SET scene_index=?, question_index=?, score=?, phase=?, updated_at=?
            WHERE id=?
            """,
            (scene_index, question_index, score, phase, _now(), participation_id),
        )


def save_response(participation_id, scene_index, question_index, option_id, puntos, correcta, feedback_tipo):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO student_responses
            (participation_id, scene_index, question_index, option_id, puntos, correcta, feedback_tipo, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (participation_id, scene_index, question_index, option_id, puntos, correcta, feedback_tipo, _now()),
        )


def finish_participation(participation_id, score):
    with get_db() as conn:
        conn.execute(
            """
            UPDATE participations SET estado='finalizado', score=?, finished_at=?, updated_at=?
            WHERE id=?
            """,
            (score, _now(), _now(), participation_id),
        )


def save_reflective_log(participation_id, scenario_index, contenido):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO reflective_logs (participation_id, scenario_index, contenido, created_at)
            VALUES (?, ?, ?, ?)
            """,
            (participation_id, scenario_index, contenido, _now()),
        )


def get_reflective_logs(participation_id):
    with get_db() as conn:
        rows = conn.execute(
            "SELECT * FROM reflective_logs WHERE participation_id = ? ORDER BY scenario_index",
            (participation_id,),
        ).fetchall()
        return [dict(r) for r in rows]


def save_simulation_result(user_id, caso, puntaje, total, porcentaje, participation_id=None):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO simulation_results (user_id, participation_id, caso, puntaje, total, porcentaje, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (user_id, participation_id, caso, puntaje, total, porcentaje, _now()),
        )


def get_results_by_user(user_id, limit=20):
    with get_db() as conn:
        rows = conn.execute(
            "SELECT * FROM simulation_results WHERE user_id = ? ORDER BY created_at DESC LIMIT ?",
            (user_id, limit),
        ).fetchall()
        return [dict(r) for r in rows]


def get_all_results():
    with get_db() as conn:
        rows = conn.execute(
            """
            SELECT r.*, u.nombre, u.email
            FROM simulation_results r JOIN users u ON u.id = r.user_id
            ORDER BY r.created_at DESC
            """
        ).fetchall()
        return [dict(r) for r in rows]


def get_student_stats(group_id=None):
    with get_db() as conn:
        q = """
            SELECT u.id, u.nombre, u.email,
                COUNT(DISTINCT p.id) AS intentos,
                COALESCE(MAX(r.porcentaje), 0) AS mejor_puntaje,
                COALESCE(ROUND(AVG(r.porcentaje)), 0) AS promedio,
                MAX(p.estado) AS ultimo_estado
            FROM users u
            LEFT JOIN participations p ON p.user_id = u.id
            LEFT JOIN simulation_results r ON r.user_id = u.id
            WHERE u.role = 'estudiante' AND u.activo = 1
        """
        if group_id:
            q += " AND u.id IN (SELECT user_id FROM user_groups WHERE group_id = ? AND vinculo='estudiante')"
            rows = conn.execute(q + " GROUP BY u.id ORDER BY u.nombre", (group_id,)).fetchall()
        else:
            rows = conn.execute(q + " GROUP BY u.id ORDER BY u.nombre").fetchall()
        return [dict(r) for r in rows]


def save_teacher_observation(teacher_id, student_id, observacion, case_id=None, participation_id=None):
    with get_db() as conn:
        conn.execute(
            """
            INSERT INTO teacher_observations (teacher_id, student_id, case_id, participation_id, observacion, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (teacher_id, student_id, case_id, participation_id, observacion, _now()),
        )


def get_observations_for_student(student_id):
    with get_db() as conn:
        rows = conn.execute(
            """
            SELECT o.*, t.nombre AS docente_nombre
            FROM teacher_observations o
            JOIN users t ON t.id = o.teacher_id
            WHERE o.student_id = ?
            ORDER BY o.created_at DESC
            """,
            (student_id,),
        ).fetchall()
        return [dict(r) for r in rows]


def get_participation_detail(participation_id):
    with get_db() as conn:
        p = conn.execute(
            """
            SELECT p.*, u.nombre AS estudiante, c.titulo AS caso_titulo
            FROM participations p
            JOIN users u ON u.id = p.user_id
            JOIN simulation_cases c ON c.id = p.case_id
            WHERE p.id = ?
            """,
            (participation_id,),
        ).fetchone()
        if not p:
            return None
        data = dict(p)
        data["responses"] = [
            dict(r)
            for r in conn.execute(
                "SELECT * FROM student_responses WHERE participation_id = ? ORDER BY id",
                (participation_id,),
            ).fetchall()
        ]
        data["logs"] = [
            dict(r)
            for r in conn.execute(
                "SELECT * FROM reflective_logs WHERE participation_id = ? ORDER BY scenario_index",
                (participation_id,),
            ).fetchall()
        ]
        return data


def get_admin_stats():
    with get_db() as conn:
        users = conn.execute(
            "SELECT role, COUNT(*) total FROM users WHERE activo=1 GROUP BY role"
        ).fetchall()
        return {
            "by_role": {r["role"]: r["total"] for r in users},
            "total_results": conn.execute("SELECT COUNT(*) c FROM simulation_results").fetchone()["c"],
            "total_groups": conn.execute("SELECT COUNT(*) c FROM academic_groups").fetchone()["c"],
            "total_cases": conn.execute("SELECT COUNT(*) c FROM simulation_cases").fetchone()["c"],
            "avg_score": conn.execute(
                "SELECT COALESCE(ROUND(AVG(porcentaje)),0) avg FROM simulation_results"
            ).fetchone()["avg"],
        }


def get_audit_logs(limit=50):
    with get_db() as conn:
        rows = conn.execute(
            """
            SELECT a.*, u.nombre, u.email
            FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id
            ORDER BY a.created_at DESC LIMIT ?
            """,
            (limit,),
        ).fetchall()
        return [dict(r) for r in rows]
