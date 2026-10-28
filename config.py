import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
SECRET_KEY = os.environ.get("SECRET_KEY", "psicosim-dev-key-cambiar-en-produccion")
DATABASE_PATH = BASE_DIR / "psicosim.db"

ROLES = ("estudiante", "profesor", "admin")

ROLE_LABELS = {
    "estudiante": "Estudiante",
    "profesor": "Profesor",
    "admin": "Administrador",
}

DEFAULT_USERS = [
    {
        "email": "estudiante@psicosim.com",
        "password": "estudiante123",
        "nombre": "Ana García",
        "role": "estudiante",
    },
    {
        "email": "profesor@psicosim.com",
        "password": "profesor123",
        "nombre": "Dr. Carlos Méndez",
        "role": "profesor",
    },
    {
        "email": "admin@psicosim.com",
        "password": "admin123",
        "nombre": "Admin Sistema",
        "role": "admin",
    },
]
