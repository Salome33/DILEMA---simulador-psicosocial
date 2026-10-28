from flask import Blueprint, jsonify, request

from auth_utils import current_user, role_required
from database import get_participation, log_audit, save_progress, save_response
from security import sanitize_int, sanitize_text

api_bp = Blueprint("api", __name__, url_prefix="/api")


@api_bp.route("/progreso", methods=["POST"])
@role_required("estudiante")
def guardar_progreso():
    data = request.get_json(silent=True) or {}
    user = current_user()
    pid = sanitize_int(data.get("participation_id"))
    p = get_participation(pid)
    if not p or p["user_id"] != user["id"]:
        return jsonify({"ok": False, "error": "Participación inválida"}), 403

    scene = sanitize_int(data.get("scene_index"), 0, 0, 10)
    question = sanitize_int(data.get("question_index"), 0, 0, 20)
    score = sanitize_int(data.get("score"), 0, 0, 100)
    phase = sanitize_text(data.get("phase", "question"), 30)

    save_progress(pid, scene, question, score, phase)

    if data.get("option_id"):
        save_response(
            pid,
            scene,
            question,
            sanitize_text(data.get("option_id"), 5),
            sanitize_int(data.get("puntos"), 0, 0, 100),
            1 if data.get("correcta") else 0,
            sanitize_text(data.get("feedback_tipo", "inmediata"), 30),
        )

    log_audit(user["id"], "progreso_guardado", f"participation={pid}")
    return jsonify({"ok": True})


@api_bp.route("/participacion/<int:participation_id>")
@role_required("estudiante")
def obtener_participacion(participation_id):
    user = current_user()
    p = get_participation(participation_id)
    if not p or p["user_id"] != user["id"]:
        return jsonify({"ok": False}), 403
    return jsonify({"ok": True, "participation": p})
