"""Crea (o promueve) un administrador. Uso:
    python -m scripts.create_admin "Nombre" correo@mail.com Clave12345
"""
import sys

from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.security import hash_password
from app.models import Member
from app.models.enums import Role


def main() -> None:
    if len(sys.argv) != 4:
        print('Uso: python -m scripts.create_admin "Nombre" correo clave')
        sys.exit(1)
    name, email, password = sys.argv[1], sys.argv[2].lower(), sys.argv[3]
    if len(password) < 8:
        print("La clave debe tener al menos 8 caracteres")
        sys.exit(1)
    with SessionLocal() as db:
        member = db.scalar(select(Member).where(Member.email == email))
        if member:
            member.role = Role.ADMINISTRATOR
            print(f"El usuario {email} ya existía: ahora es ADMINISTRATOR")
        else:
            db.add(Member(name=name, email=email, password_hash=hash_password(password), role=Role.ADMINISTRATOR))
            print(f"Administrador {email} creado")
        db.commit()


if __name__ == "__main__":
    main()
