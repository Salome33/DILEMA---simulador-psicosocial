"""Utilidades de seguridad — RNF: entradas maliciosas."""
import html
import re

EMAIL_RE = re.compile(r"^[\w.\-+]+@[\w.\-]+\.[A-Za-z]{2,}$")
SCRIPT_RE = re.compile(r"<\s*script|javascript:|on\w+\s*=", re.I)


def sanitize_text(value, max_len=5000):
    if value is None:
        return ""
    text = str(value).strip()[:max_len]
    if SCRIPT_RE.search(text):
        raise ValueError("Entrada no permitida.")
    return html.escape(text)


def sanitize_email(value):
    email = str(value or "").strip().lower()[:120]
    if not EMAIL_RE.match(email):
        raise ValueError("Correo inválido.")
    return email


def sanitize_int(value, default=0, min_val=None, max_val=None):
    try:
        num = int(value)
    except (TypeError, ValueError):
        num = default
    if min_val is not None:
        num = max(min_val, num)
    if max_val is not None:
        num = min(max_val, num)
    return num
