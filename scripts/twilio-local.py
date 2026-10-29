#!/usr/bin/env python3
"""Carrega o JSON privado para um laboratório isolado; nunca envia mensagens."""
import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import sys


class ConfigError(Exception):
    pass


def load_config(path):
    if path.stat().st_mode & 0o077:
        raise ConfigError("O JSON deve ter permissão 0600 (somente seu usuário).")
    try:
        data = json.loads(path.read_text())
    except (json.JSONDecodeError, UnicodeError):
        raise ConfigError("JSON privado inválido; conteúdo omitido.") from None
    if not isinstance(data, dict):
        raise ConfigError("O JSON privado deve conter um objeto.")
    return data


def configuration(data, channel):
    def field(key, pattern=None):
        value = data.get(key, "")
        if not isinstance(value, str) or not value or (pattern and not re.fullmatch(pattern, value)):
            raise ConfigError(key + " ausente ou inválido; valor omitido.")
        return value

    result = {
        "TWILIO_ACCOUNT_SID": field("TWILIO_ACCOUNT_SID", r"AC[0-9a-fA-F]{32}"),
        "TWILIO_AUTH_TOKEN": field("TWILIO_AUTH_TOKEN", r"[0-9a-fA-F]{32}"),
        "SMS_PROVIDER": "twilio" if channel in ("sms", "both") else "fake",
        "WHATSAPP_PROVIDER": "twilio" if channel in ("whatsapp", "both") else "fake",
        "EMAIL_PROVIDER": "fake",
        "AMQP_URL": "",
        "SPRING_CONFIG_IMPORT": "",  # não importar outro .env do checkout
        "TWILIO_WHATSAPP_CONTENT_SID": "",
        "TWILIO_WHATSAPP_TRIAL_MODE": "false",
        "TWILIO_SMS_TRIAL_TEMPLATE": "",
        "TWILIO_SMS_FROM": "",
        "TWILIO_WHATSAPP_FROM": "",
    }
    if channel in ("sms", "both"):
        result["TWILIO_SMS_FROM"] = field("TWILIO_SMS_FROM", r"\+[1-9][0-9]{7,14}")
        preset = data.get("TWILIO_SMS_TRIAL_TEMPLATE", "")
        if preset not in ("", "sms_order_confirmation"):
            raise ConfigError("TWILIO_SMS_TRIAL_TEMPLATE inválido; valor omitido.")
        result["TWILIO_SMS_TRIAL_TEMPLATE"] = preset
    if channel in ("whatsapp", "both"):
        result["TWILIO_WHATSAPP_FROM"] = field("TWILIO_WHATSAPP_FROM", r"whatsapp:\+[1-9][0-9]{7,14}")
        trial = data.get("TWILIO_WHATSAPP_TRIAL_MODE", True)
        if not isinstance(trial, bool):
            raise ConfigError("TWILIO_WHATSAPP_TRIAL_MODE deve ser booleano.")
        sid = data.get("TWILIO_WHATSAPP_CONTENT_SID", "")
        if trial or sid:
            sid = field("TWILIO_WHATSAPP_CONTENT_SID", r"HX[0-9a-fA-F]{32}")
        result["TWILIO_WHATSAPP_CONTENT_SID"] = sid
        result["TWILIO_WHATSAPP_TRIAL_MODE"] = str(trial).lower()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", required=True, type=Path)
    parser.add_argument("--channel", choices=("sms", "whatsapp", "both"), required=True)
    parser.add_argument("--run", action="store_true", help="Inicia o Java; sem esta opção apenas valida, sem rede.")
    parser.add_argument("--port", type=int, default=8095)
    args = parser.parse_args()
    try:
        env_values = configuration(load_config(args.config), args.channel)
        if not 1024 <= args.port <= 65535:
            raise ConfigError("Porta deve estar entre 1024 e 65535.")
        if not args.run:
            print("Configuração local válida para " + args.channel + ". API não consultada; nenhuma mensagem enviada.")
            return 0
        jar = Path(__file__).resolve().parents[1] / "target/support-1.0.3.jar"
        java_home = os.environ.get("JAVA_HOME")
        java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
        if not java or not Path(java).is_file() or not jar.is_file():
            raise ConfigError("Java 21+ e target/support-1.0.3.jar são necessários.")
        workspace = args.config.resolve().parent / "support-twilio-local"
        workspace.mkdir(mode=0o700, exist_ok=True)
        if workspace.stat().st_mode & 0o077:
            raise ConfigError("Diretório de laboratório deve ser privado (0700).")
        secret_file = workspace / "internal-secret"
        if not secret_file.exists():
            with open(secret_file, "x", opener=lambda p, flags: os.open(p, flags, 0o600)) as f:
                f.write(secrets.token_urlsafe(32))
        if secret_file.stat().st_mode & 0o077:
            raise ConfigError("Arquivo internal-secret deve ser privado (0600).")
        env = os.environ.copy()
        env.update(env_values)
        env.update(INTERNAL_SECRET=secret_file.read_text().strip(),
                   DB_PATH=str(workspace / "support.db"), PORT=str(args.port), LOG_LEVEL="info")
        os.umask(0o077)
        print("Iniciando laboratório local na porta " + str(args.port) + ". Nenhum evento será criado por este comando.", flush=True)
        os.chdir(workspace)
        os.execve(java, [java, "-jar", str(jar), "--server.address=127.0.0.1"], env)
    except (ConfigError, OSError):
        error = sys.exc_info()[1]
        print(str(error) if isinstance(error, ConfigError) else "Não foi possível acessar arquivo ou executar Java; dados privados omitidos.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
