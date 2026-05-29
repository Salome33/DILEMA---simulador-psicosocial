from flask import Blueprint, flash, redirect, render_template, request, url_for

from auth_utils import current_user, role_required
from database import (
    assign_case_to_group,
    create_case,
    get_all_results,
    get_group_students,
    get_observations_for_student,
    get_participation_detail,
    get_reflective_logs,
    get_student_stats,
    list_cases,
    list_groups,
    log_audit,
    publish_case,
    save_teacher_observation,
)
from security import sanitize_int, sanitize_text

profesor_bp = Blueprint("profesor", __name__, url_prefix="/profesor")


@profesor_bp.route("/")
@role_required("profesor")
def dashboard():
    user = current_user()
    groups = list_groups(user["id"])
    students = get_student_stats()
    results = get_all_results()[:10]
    completed = sum(1 for s in students if s["intentos"] > 0)
    avg = round(sum(s["promedio"] for s in students) / len(students)) if students else 0
    return render_template(
        "profesor/dashboard.html",
        active="dashboard",
        groups=groups,
        students=students[:8],
        recent_results=results,
        total_estudiantes=len(students),
        estudiantes_activos=completed,
        promedio_clase=avg,
    )


@profesor_bp.route("/grupos")
@role_required("profesor")
def grupos():
    user = current_user()
    return render_template(
        "profesor/grupos.html",
        active="grupos",
        groups=list_groups(user["id"]),
    )


@profesor_bp.route("/grupos/<int:group_id>")
@role_required("profesor")
def grupo_detalle(group_id):
    students = get_group_students(group_id)
    stats = get_student_stats(group_id)
    return render_template(
        "profesor/grupo_detalle.html",
        active="grupos",
        group_id=group_id,
        students=students,
        stats=stats,
    )


@profesor_bp.route("/estudiantes")
@role_required("profesor")
def estudiantes():
    return render_template(
        "profesor/estudiantes.html",
        active="estudiantes",
        students=get_student_stats(),
    )


@profesor_bp.route("/estudiantes/<int:student_id>/observacion", methods=["GET", "POST"])
@role_required("profesor")
def observacion(student_id):
    user = current_user()
    if request.method == "POST":
        try:
            obs = sanitize_text(request.form.get("observacion", ""), 2000)
            if len(obs) < 10:
                raise ValueError("La observación debe tener al menos 10 caracteres.")
            save_teacher_observation(
                user["id"],
                student_id,
                obs,
                sanitize_int(request.form.get("case_id"), 0) or None,
                sanitize_int(request.form.get("participation_id"), 0) or None,
            )
            log_audit(user["id"], "observacion_registrada", f"student={student_id}")
            flash("Observación registrada.", "success")
            return redirect(url_for("profesor.estudiantes"))
        except ValueError as e:
            flash(str(e), "danger")
    return render_template(
        "profesor/observacion.html",
        active="estudiantes",
        student_id=student_id,
        observations=get_observations_for_student(student_id),
    )


@profesor_bp.route("/seguimiento/<int:participation_id>")
@role_required("profesor")
def seguimiento(participation_id):
    detail = get_participation_detail(participation_id)
    if not detail:
        flash("Participación no encontrada.", "danger")
        return redirect(url_for("profesor.resultados"))
    return render_template(
        "profesor/seguimiento.html",
        active="resultados",
        detail=detail,
    )


@profesor_bp.route("/resultados")
@role_required("profesor")
def resultados():
    return render_template(
        "profesor/resultados.html",
        active="resultados",
        results=get_all_results(),
    )


@profesor_bp.route("/casos")
@role_required("profesor")
def casos():
    user = current_user()
    return render_template(
        "profesor/casos.html",
        active="casos",
        cases=list_cases(),
    )


@profesor_bp.route("/casos/nuevo", methods=["GET", "POST"])
@role_required("profesor")
def caso_nuevo():
    user = current_user()
    if request.method == "POST":
        try:
            titulo = sanitize_text(request.form.get("titulo", ""), 200)
            desc = sanitize_text(request.form.get("descripcion", ""), 2000)
            prop = sanitize_text(request.form.get("proposito", ""), 1000)
            tem = sanitize_text(request.form.get("tematica", ""), 200)
            if not all([titulo, desc, prop, tem]):
                raise ValueError("Completa todos los campos obligatorios.")
            cid = create_case(titulo, desc, prop, tem, user["id"], request.form.get("sensible") == "on")
            log_audit(user["id"], "caso_creado", f"case={cid}")
            flash("Caso creado en borrador.", "success")
            return redirect(url_for("profesor.casos"))
        except ValueError as e:
            flash(str(e), "danger")
    return render_template("profesor/caso_form.html", active="casos", case=None)


@profesor_bp.route("/casos/<int:case_id>/publicar", methods=["POST"])
@role_required("profesor")
def publicar_caso(case_id):
    ok, msg = publish_case(case_id)
    flash(msg, "success" if ok else "warning")
    return redirect(url_for("profesor.casos"))


@profesor_bp.route("/asignar-caso", methods=["GET", "POST"])
@role_required("profesor")
def asignar_caso():
    user = current_user()
    if request.method == "POST":
        case_id = sanitize_int(request.form.get("case_id"))
        group_id = sanitize_int(request.form.get("group_id"))
        if case_id and group_id:
            assign_case_to_group(
                case_id,
                group_id,
                request.form.get("fecha_inicio"),
                request.form.get("fecha_cierre"),
            )
            log_audit(user["id"], "caso_asignado", f"case={case_id} group={group_id}")
            flash("Caso asignado al grupo.", "success")
            return redirect(url_for("profesor.grupos"))
        flash("Selecciona caso y grupo.", "danger")
    return render_template(
        "profesor/asignar_caso.html",
        active="grupos",
        cases=[c for c in list_cases() if c["estado"] == "publicado"],
        groups=list_groups(user["id"]),
    )


@profesor_bp.route("/bitacoras")
@role_required("profesor")
def bitacoras():
    from database import get_db

    with get_db() as conn:
        rows = conn.execute(
            """
            SELECT rl.*, u.nombre AS estudiante, c.titulo AS caso
            FROM reflective_logs rl
            JOIN participations p ON p.id = rl.participation_id
            JOIN users u ON u.id = p.user_id
            JOIN simulation_cases c ON c.id = p.case_id
            ORDER BY rl.created_at DESC LIMIT 50
            """
        ).fetchall()
    return render_template(
        "profesor/bitacoras.html",
        active="bitacoras",
        logs=[dict(r) for r in rows],
    )
