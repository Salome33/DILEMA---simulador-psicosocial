from flask import Blueprint, flash, redirect, render_template, request, url_for

from auth_utils import current_user, role_required
from config import ROLES
from database import (
    create_group,
    create_user,
    delete_user,
    get_admin_stats,
    get_audit_logs,
    get_user_by_id,
    list_groups,
    list_users,
    log_audit,
    update_user,
)
from security import sanitize_email, sanitize_text

admin_bp = Blueprint("admin", __name__, url_prefix="/admin")


@admin_bp.route("/")
@role_required("admin")
def dashboard():
    return render_template(
        "admin/dashboard.html",
        active="dashboard",
        stats=get_admin_stats(),
        users=list_users()[:8],
        roles=ROLES,
    )


@admin_bp.route("/usuarios")
@role_required("admin")
def usuarios():
    role_filter = request.args.get("role")
    return render_template(
        "admin/usuarios.html",
        active="usuarios",
        users=list_users(role_filter if role_filter in ROLES else None),
        roles=ROLES,
        role_filter=role_filter,
    )


@admin_bp.route("/usuarios/nuevo", methods=["GET", "POST"])
@role_required("admin")
def usuario_nuevo():
    if request.method == "POST":
        try:
            email = sanitize_email(request.form.get("email", ""))
            nombre = sanitize_text(request.form.get("nombre", ""), 120)
            password = request.form.get("password", "")
            role = request.form.get("role", "estudiante")
            if not all([nombre, password]) or role not in ROLES:
                raise ValueError("Datos incompletos.")
            create_user(email, password, nombre, role)
            log_audit(current_user()["id"], "usuario_creado", email)
            flash("Usuario creado.", "success")
            return redirect(url_for("admin.usuarios"))
        except ValueError as e:
            flash(str(e), "danger")
        except Exception:
            flash("El correo ya está registrado.", "danger")
    return render_template("admin/usuario_form.html", active="usuarios", user=None, roles=ROLES)


@admin_bp.route("/usuarios/<int:user_id>/editar", methods=["GET", "POST"])
@role_required("admin")
def usuario_editar(user_id):
    user = get_user_by_id(user_id)
    me = current_user()
    if not user:
        flash("Usuario no encontrado.", "danger")
        return redirect(url_for("admin.usuarios"))
    if request.method == "POST":
        nombre = sanitize_text(request.form.get("nombre", ""), 120)
        role = request.form.get("role", user["role"])
        activo = request.form.get("activo") == "on"
        password = request.form.get("password", "").strip()
        if user_id == me["id"] and role != "admin":
            flash("No puedes quitarte el rol admin.", "danger")
        elif role not in ROLES:
            flash("Rol inválido.", "danger")
        else:
            update_user(user_id, nombre, role, activo, password or None)
            log_audit(me["id"], "usuario_actualizado", str(user_id))
            flash("Usuario actualizado.", "success")
            return redirect(url_for("admin.usuarios"))
    return render_template("admin/usuario_form.html", active="usuarios", user=user, roles=ROLES)


@admin_bp.route("/usuarios/<int:user_id>/eliminar", methods=["POST"])
@role_required("admin")
def usuario_eliminar(user_id):
    me = current_user()
    if user_id == me["id"]:
        flash("No puedes eliminar tu cuenta.", "danger")
    else:
        delete_user(user_id)
        log_audit(me["id"], "usuario_eliminado", str(user_id))
        flash("Usuario eliminado.", "info")
    return redirect(url_for("admin.usuarios"))


@admin_bp.route("/grupos", methods=["GET", "POST"])
@role_required("admin")
def grupos():
    if request.method == "POST":
        try:
            nombre = sanitize_text(request.form.get("nombre", ""), 120)
            asignatura = sanitize_text(request.form.get("asignatura", ""), 120)
            periodo = sanitize_text(request.form.get("periodo", ""), 40)
            docente_id = request.form.get("docente_id") or None
            if not all([nombre, asignatura, periodo]):
                raise ValueError("Completa los campos del grupo.")
            gid = create_group(nombre, asignatura, periodo, int(docente_id) if docente_id else None)
            log_audit(current_user()["id"], "grupo_creado", str(gid))
            flash("Grupo académico creado.", "success")
        except ValueError as e:
            flash(str(e), "danger")
    return render_template(
        "admin/grupos.html",
        active="grupos",
        groups=list_groups(),
        profesores=list_users("profesor"),
    )


@admin_bp.route("/auditoria")
@role_required("admin")
def auditoria():
    return render_template(
        "admin/auditoria.html",
        active="auditoria",
        logs=get_audit_logs(100),
    )
