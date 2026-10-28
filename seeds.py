"""Datos semilla — Caso 1, grupo académico y asignaciones."""
from datetime import datetime, timezone


def _now():
    return datetime.now(timezone.utc).isoformat()


def seed_demo_data(conn):
    if conn.execute("SELECT COUNT(*) c FROM academic_groups").fetchone()["c"]:
        return

    prof = conn.execute(
        "SELECT id FROM users WHERE email = ?", ("profesor@psicosim.com",)
    ).fetchone()
    prof_id = prof["id"] if prof else None

    conn.execute(
        """
        INSERT INTO academic_groups (nombre, asignatura, periodo, docente_id, estado, created_at)
        VALUES (?, ?, ?, ?, 'activo', ?)
        """,
        ("Grupo Psicología Social 2026-I", "Psicología Social", "2026-I", prof_id, _now()),
    )
    group_id = conn.execute("SELECT last_insert_rowid() id").fetchone()["id"]

    for email, vinculo in [
        ("estudiante@psicosim.com", "estudiante"),
        ("profesor@psicosim.com", "docente"),
    ]:
        uid = conn.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone()
        if uid:
            conn.execute(
                """
                INSERT INTO user_groups (user_id, group_id, vinculo, estado, created_at)
                VALUES (?, ?, ?, 'activo', ?)
                """,
                (uid["id"], group_id, vinculo, _now()),
            )

    conn.execute(
        """
        INSERT INTO simulation_cases (
            titulo, descripcion, proposito, tematica, estado, docente_id,
            contenido_sensible, created_at
        ) VALUES (?, ?, ?, ?, 'publicado', ?, 1, ?)
        """,
        (
            "Violencia familiar y tentativa de feminicidio",
            "Caso clínico en barrio de alta vulnerabilidad. Agresión con arma blanca, "
            "fallecimiento de menor y sobreviviente en urgencias.",
            "Desafiar el juicio clínico y ético integrando normativa colombiana.",
            "Violencia de género intrafamiliar",
            prof_id,
            _now(),
        ),
    )
    case_id = conn.execute("SELECT last_insert_rowid() id").fetchone()["id"]

    conn.execute(
        """
        INSERT INTO sensitive_warnings (case_id, scenario_id, mensaje, requiere_aceptacion, version, estado, created_at)
        VALUES (?, NULL, ?, 1, 1, 'activo', ?)
        """,
        (
            case_id,
            "Este caso contiene descripciones de violencia familiar, feminicidio, "
            "muerte de un menor y trauma psicológico. Uso exclusivamente académico.",
            _now(),
        ),
    )

    for orden, nombre, desc, icono in [
        (1, "Atención en Hospital", "Urgencia vital y crisis familiar.", "local_hospital"),
        (2, "Comisaría de Familia", "Medidas de protección y apoyo psicosocial.", "balance"),
    ]:
        conn.execute(
            """
            INSERT INTO scenarios (case_id, nombre, descripcion, orden, contenido_sensible, estado, icono, created_at)
            VALUES (?, ?, ?, ?, 1, 'activo', ?, ?)
            """,
            (case_id, nombre, desc, orden, icono, _now()),
        )

    conn.execute(
        """
        INSERT INTO case_assignments (case_id, group_id, fecha_inicio, fecha_cierre, estado, created_at)
        VALUES (?, ?, date('now'), date('now', '+90 days'), 'activo', ?)
        """,
        (case_id, group_id, _now()),
    )
