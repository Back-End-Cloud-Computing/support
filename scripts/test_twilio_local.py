import importlib.util
from pathlib import Path
import tempfile
import unittest
import json
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("twilio_local", Path(__file__).with_name("twilio-local.py"))
local = importlib.util.module_from_spec(spec)
spec.loader.exec_module(local)


class LocalConfigurationTest(unittest.TestCase):
    def setUp(self):
        self.data = dict(TWILIO_ACCOUNT_SID="AC" + "a" * 32, TWILIO_AUTH_TOKEN="b" * 32,
                         TWILIO_SMS_FROM="+15550000001", TWILIO_SMS_TRIAL_TEMPLATE="sms_order_confirmation",
                         TWILIO_WHATSAPP_FROM="whatsapp:+15550000001", TWILIO_WHATSAPP_TRIAL_MODE=True,
                         TWILIO_WHATSAPP_CONTENT_SID="")

    def test_sms_can_run_without_whatsapp_template_and_does_not_activate_other_channels(self):
        env = local.configuration(self.data, "sms")
        self.assertEqual(env["SMS_PROVIDER"], "twilio")
        self.assertEqual(env["WHATSAPP_PROVIDER"], "fake")
        self.assertEqual(env["EMAIL_PROVIDER"], "fake")
        self.assertEqual(env["AMQP_URL"], "")
        self.assertEqual(env["SPRING_CONFIG_IMPORT"], "")

    def test_whatsapp_and_both_refuse_missing_or_wrong_template(self):
        for channel in ("whatsapp", "both"):
            for sid in ("", self.data["TWILIO_ACCOUNT_SID"]):
                self.data["TWILIO_WHATSAPP_CONTENT_SID"] = sid
                with self.assertRaisesRegex(local.ConfigError, "TWILIO_WHATSAPP_CONTENT_SID"):
                    local.configuration(self.data, channel)

    def test_valid_template_selects_only_whatsapp(self):
        self.data["TWILIO_WHATSAPP_CONTENT_SID"] = "HX" + "c" * 32
        env = local.configuration(self.data, "whatsapp")
        self.assertEqual(env["SMS_PROVIDER"], "fake")
        self.assertEqual(env["WHATSAPP_PROVIDER"], "twilio")
        self.assertEqual(env["TWILIO_WHATSAPP_TRIAL_MODE"], "true")

    def test_invalid_token_is_not_in_error(self):
        self.data["TWILIO_AUTH_TOKEN"] = "private-invalid-token"
        with self.assertRaises(local.ConfigError) as caught:
            local.configuration(self.data, "sms")
        self.assertNotIn(self.data["TWILIO_AUTH_TOKEN"], str(caught.exception))

    def test_private_file_required_and_invalid_json_not_echoed(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "config.json"
            p.write_text(json.dumps(self.data))
            p.chmod(0o644)
            with self.assertRaisesRegex(local.ConfigError, "0600"):
                local.load_config(p)
            p.chmod(0o600)
            self.assertEqual(local.load_config(p), self.data)
            p.write_text('"private-invalid-json')
            with self.assertRaises(local.ConfigError) as caught:
                local.load_config(p)
            self.assertNotIn("private-invalid-json", str(caught.exception))

    def test_run_uses_private_database_loopback_and_no_broker(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp) / "config.json"
            p.write_text(json.dumps(self.data))
            p.chmod(0o600)
            args = ["twilio-local.py", "--config", str(p), "--channel", "sms", "--run"]
            with patch.object(local.sys, "argv", args), patch.object(local.Path, "is_file", return_value=True), \
                    patch.object(local.shutil, "which", return_value="/usr/bin/java"), \
                    patch.object(local.os, "chdir"), patch.object(local.os, "umask"), \
                    patch.object(local.os, "execve", side_effect=RuntimeError("captured")) as execute:
                with self.assertRaisesRegex(RuntimeError, "captured"):
                    local.main()
            _, arguments, env = execute.call_args.args
            self.assertIn("--server.address=127.0.0.1", arguments)
            self.assertEqual(env["DB_PATH"], str(Path(tmp).resolve() / "support-twilio-local/support.db"))
            self.assertEqual(env["AMQP_URL"], "")
            self.assertEqual(env["WHATSAPP_PROVIDER"], "fake")
            secret = Path(tmp) / "support-twilio-local/internal-secret"
            self.assertEqual(secret.stat().st_mode & 0o777, 0o600)
            self.assertEqual(env["INTERNAL_SECRET"], secret.read_text())


if __name__ == "__main__":
    unittest.main()
