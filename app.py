from flask import Flask, redirect, url_for

from auth_utils import current_user, dashboard_route_for_role, role_label
from config import SECRET_KEY
from database import init_db
from routes.admin import admin_bp
from routes.api import api_bp
from routes.auth import auth_bp
from routes.estudiante import estudiante_bp
from routes.profesor import profesor_bp


def create_app():
    app = Flask(__name__)
    app.config["SECRET_KEY"] = SECRET_KEY
    app.config["SESSION_COOKIE_HTTPONLY"] = True
    app.config["SESSION_COOKIE_SAMESITE"] = "Lax"

    init_db()

    app.register_blueprint(auth_bp)
    app.register_blueprint(estudiante_bp)
    app.register_blueprint(profesor_bp)
    app.register_blueprint(admin_bp)
    app.register_blueprint(api_bp)

    @app.route("/")
    def index():
        user = current_user()
        if user:
            return redirect(url_for(dashboard_route_for_role(user["role"])))
        return redirect(url_for("auth.login"))

    @app.context_processor
    def inject_globals():
        return {"current_user": current_user(), "role_label": role_label}

    return app


app = create_app()

if __name__ == "__main__":
    app.run(debug=True, port=5000)
