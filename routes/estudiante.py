from flask import Blueprint, flash, redirect, render_template, request, url_for

from auth_utils import current_user, role_required
from database import (
    accept_warning,
    finish_participation,
    get_assigned_cases_for_student,
    get_case_by_id,
    get_case_scenarios,
    get_case_warning,
    get_observations_for_student,
    get_or_create_participation,
    get_participation,
    get_participation_detail,
    get_reflective_logs,
    get_results_by_user,
    has_accepted_warning,
    log_audit,
    save_reflective_log,
    save_simulation_result,
)
from security import sanitize_int, sanitize_text

estudiante_bp = Blueprint("estudiante", __name__, url_prefix="/estudiante")


def _require_case_access(case_id, user_id):
    cases = get_assigned_cases_for_student(user_id)
    return next((c for c in cases if c["id"] == case_id), None)


@estudiante_bp.route("/")
@role_required("estudiante")
def dashboard():
    user = current_user()
    cases = get_assigned_cases_for_student(user["id"])
    results = get_results_by_user(user["id"], 5)
    best = max((r["porcentaje"] for r in results), default=0)
    return render_template(
        "estudiante/dashboard.html",
        active="dashboard",
        cases=cases,
        results=results,
        total_intentos=len(results),
        mejor_puntaje=best,
    )


@estudiante_bp.route("/casos")
@role_required("estudiante")
def casos():
    user = current_user()
    return render_template(
        "estudiante/casos.html",
        active="casos",
        cases=get_assigned_cases_for_student(user["id"]),
    )


@estudiante_bp.route("/caso/<int:case_id>")
@role_required("estudiante")
def caso_detalle(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case:
        flash("No tienes acceso a este caso.", "danger")
        return redirect(url_for("estudiante.casos"))
    return render_template(
        "estudiante/caso_detalle.html",
        active="casos",
        case=case,
        scenarios=get_case_scenarios(case_id),
    )


@estudiante_bp.route("/caso/<int:case_id>/advertencia", methods=["GET", "POST"])
@role_required("estudiante")
def advertencia(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case:
        flash("Caso no disponible.", "danger")
        return redirect(url_for("estudiante.casos"))
    warning = get_case_warning(case_id)
    if request.method == "POST":
        if request.form.get("acepto") != "on":
            flash("Debes aceptar las condiciones para continuar.", "warning")
        else:
            accept_warning(user["id"], case_id)
            log_audit(user["id"], "advertencia_aceptada", f"case={case_id}")
            return redirect(url_for("estudiante.tutorial", case_id=case_id))
    if has_accepted_warning(user["id"], case_id):
        return redirect(url_for("estudiante.tutorial", case_id=case_id))
    return render_template(
        "estudiante/advertencia.html",
        active="casos",
        case=case,
        warning=warning,
    )


@estudiante_bp.route("/caso/<int:case_id>/tutorial")
@role_required("estudiante")
def tutorial(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case or not has_accepted_warning(user["id"], case_id):
        return redirect(url_for("estudiante.advertencia", case_id=case_id))
    return render_template("estudiante/tutorial.html", active="casos", case=case)


@estudiante_bp.route("/caso/<int:case_id>/introduccion")
@role_required("estudiante")
def introduccion_caso(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case or not has_accepted_warning(user["id"], case_id):
        return redirect(url_for("estudiante.advertencia", case_id=case_id))
    return render_template(
        "introduccion_caso.html",
        active="casos",
        case=case,
        scenarios=get_case_scenarios(case_id),
    )


@estudiante_bp.route("/caso/<int:case_id>/iniciar")
@role_required("estudiante")
def iniciar_caso(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case or not has_accepted_warning(user["id"], case_id):
        return redirect(url_for("estudiante.advertencia", case_id=case_id))
    p = get_or_create_participation(user["id"], case_id)
    log_audit(user["id"], "caso_iniciado", f"case={case_id}")
    return redirect(url_for("estudiante.simulacion", case_id=case_id))


@estudiante_bp.route("/caso/<int:case_id>/simulacion")
@role_required("estudiante")
def simulacion(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case or not has_accepted_warning(user["id"], case_id):
        return redirect(url_for("estudiante.advertencia", case_id=case_id))
    p = get_or_create_participation(user["id"], case_id)
    return render_template(
        "simulacion.html",
        active="sim",
        case=case,
        participation=p,
    )


@estudiante_bp.route("/caso/<int:case_id>/bitacora/<int:scenario_index>", methods=["GET", "POST"])
@role_required("estudiante")
def bitacora(case_id, scenario_index):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case:
        return redirect(url_for("estudiante.casos"))
    p = get_or_create_participation(user["id"], case_id)
    if request.method == "POST":
        try:
            contenido = sanitize_text(request.form.get("contenido", ""), 4000)
            if len(contenido) < 20:
                raise ValueError("Escribe al menos 20 caracteres.")
            save_reflective_log(p["id"], scenario_index, contenido)
            log_audit(user["id"], "bitacora_guardada", f"scenario={scenario_index}")
            if scenario_index == 0:
                return redirect(url_for("estudiante.simulacion", case_id=case_id) + "?resume=1")
            return redirect(url_for("estudiante.informe", case_id=case_id))
        except ValueError as e:
            flash(str(e), "danger")
    esc = get_case_scenarios(case_id)
    nombre = esc[scenario_index]["nombre"] if scenario_index < len(esc) else f"Escenario {scenario_index + 1}"
    return render_template(
        "estudiante/bitacora.html",
        active="sim",
        case=case,
        scenario_index=scenario_index,
        scenario_nombre=nombre,
        participation=p,
    )


@estudiante_bp.route("/caso/<int:case_id>/informe")
@role_required("estudiante")
def informe(case_id):
    user = current_user()
    case = _require_case_access(case_id, user["id"])
    if not case:
        return redirect(url_for("estudiante.casos"))
    p = get_or_create_participation(user["id"], case_id)
    detail = get_participation_detail(p["id"])
    observations = get_observations_for_student(user["id"])
    return render_template(
        "estudiante/informe.html",
        active="historial",
        case=case,
        detail=detail,
        observations=observations,
    )


@estudiante_bp.route("/herramientas")
@role_required("estudiante")
def herramientas():
    return render_template("estudiante/herramientas.html", active="herramientas")


@estudiante_bp.route("/resultados")
@role_required("estudiante")
def resultados():
    user = current_user()
    puntaje = sanitize_int(request.args.get("puntaje"), 0, 0, 100)
    total = sanitize_int(request.args.get("total"), 100, 1, 100)
    porcentaje = round((puntaje * 100) / total) if total > 0 else 0
    caso = sanitize_text(request.args.get("caso", "Caso 1"), 200)
    pid = sanitize_int(request.args.get("participation_id"), 0)

    if puntaje > 0 and pid:
        finish_participation(pid, puntaje)
        save_simulation_result(user["id"], caso, puntaje, total, porcentaje, pid)
        log_audit(user["id"], "simulacion_finalizada", f"score={puntaje}")

    return render_template(
        "resultados.html",
        active="sim",
        puntaje=puntaje,
        total=total,
        porcentaje=porcentaje,
        case_id=sanitize_int(request.args.get("case_id"), 0),
        participation_id=sanitize_int(request.args.get("participation_id"), 0),
    )


@estudiante_bp.route("/historial")
@role_required("estudiante")
def historial():
    user = current_user()
    return render_template(
        "estudiante/historial.html",
        active="historial",
        results=get_results_by_user(user["id"], 50),
    )


@estudiante_bp.route("/seguimiento/<int:participation_id>")
@role_required("estudiante")
def mi_seguimiento(participation_id):
    user = current_user()
    detail = get_participation_detail(participation_id)
    if not detail or detail["user_id"] != user["id"]:
        flash("Registro no encontrado.", "danger")
        return redirect(url_for("estudiante.historial"))
    return render_template(
        "estudiante/seguimiento.html",
        active="historial",
        detail=detail,
    )
