from flask import Blueprint, flash, redirect, render_template, request, url_for

from auth_utils import current_user, dashboard_route_for_role, login_user, logout_user
from database import get_user_by_email, log_audit, verify_password
from security import sanitize_email, sanitize_text

auth_bp = Blueprint("auth", __name__)


@auth_bp.route("/login", methods=["GET", "POST"])
def login():
    if current_user():
        return redirect(url_for(dashboard_route_for_role(current_user()["role"])))

    if request.method == "POST":
        try:
            email = sanitize_email(request.form.get("email", ""))
            password = request.form.get("password", "")
            user = get_user_by_email(email)
            if user and verify_password(user, password):
                login_user(user)
                log_audit(user["id"], "login", sanitize_text(email, 120))
                flash(f"Bienvenido/a, {user['nombre']}.", "success")
                return redirect(url_for(dashboard_route_for_role(user["role"])))
            log_audit(None, "login_fallido", sanitize_text(email, 120))
            flash("Correo o contraseña incorrectos.", "danger")
        except ValueError as e:
            flash(str(e), "danger")

    return render_template("auth/login.html")


@auth_bp.route("/logout")
def logout():
    user = current_user()
    if user:
        log_audit(user["id"], "logout")
    logout_user()
    flash("Sesión cerrada.", "info")
    return redirect(url_for("auth.login"))
