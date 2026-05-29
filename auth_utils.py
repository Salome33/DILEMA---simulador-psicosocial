from functools import wraps

from flask import flash, redirect, session, url_for

from config import ROLE_LABELS


def login_user(user):
    session.clear()
    session["user_id"] = user["id"]
    session["user_email"] = user["email"]
    session["user_nombre"] = user["nombre"]
    session["user_role"] = user["role"]


def logout_user():
    session.clear()


def current_user():
    if "user_id" not in session:
        return None
    return {
        "id": session["user_id"],
        "email": session.get("user_email"),
        "nombre": session.get("user_nombre"),
        "role": session.get("user_role"),
    }


def role_label(role):
    return ROLE_LABELS.get(role, role)


def dashboard_route_for_role(role):
    return {
        "estudiante": "estudiante.dashboard",
        "profesor": "profesor.dashboard",
        "admin": "admin.dashboard",
    }.get(role, "auth.login")


def login_required(view):
    @wraps(view)
    def wrapped(*args, **kwargs):
        if not current_user():
            flash("Debes iniciar sesión para continuar.", "warning")
            return redirect(url_for("auth.login"))
        return view(*args, **kwargs)

    return wrapped


def role_required(*roles):
    def decorator(view):
        @wraps(view)
        def wrapped(*args, **kwargs):
            user = current_user()
            if not user:
                flash("Debes iniciar sesión para continuar.", "warning")
                return redirect(url_for("auth.login"))
            if user["role"] not in roles:
                flash("No tienes permiso para acceder a este módulo.", "danger")
                return redirect(url_for(dashboard_route_for_role(user["role"])))
            return view(*args, **kwargs)

        return wrapped

    return decorator
