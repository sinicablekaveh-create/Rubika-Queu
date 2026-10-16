"""Offline regression tests for trusted-device stable signing bootstrap.

Never runs keytool generation, GitHub CLI authentication, or secret uploads.
"""
import importlib.util
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "provision_stable_signing", ROOT / "scripts" / "provision_stable_signing.py"
)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class SigningProvisionTests(unittest.TestCase):
    def test_parses_one_public_colon_fingerprint(self):
        formatted = ":".join(["AB"] * 32)
        self.assertEqual(module.extract_fingerprint(" SHA256: " + formatted + "\n"), "ab" * 32)

    def test_refuses_multiple_public_fingerprints(self):
        text = "SHA256: " + ("ab:" * 31) + "ab\n"
        with self.assertRaisesRegex(ValueError, "uniquely"):
            module.extract_fingerprint(text + text)

    def test_rejects_short_or_malformed_pin(self):
        for sample in ("", "sha-1", "abcd" * 8, "XX" * 32):
            with self.subTest(sample=sample):
                with self.assertRaises(ValueError):
                    module.normalize_fingerprint(sample)

    def test_never_store_private_key_inside_source_tree(self):
        with self.assertRaisesRegex(ValueError, "NEVER"):
            module.validate_key_location(ROOT / "signing" / "stable-release.p12")

    def test_rejects_wrong_keystore_suffix(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaisesRegex(ValueError, ".p12"):
                module.validate_key_location(Path(root) / "secret.txt")

    def test_keeps_existing_local_key_and_never_invokes_keytool(self):
        with tempfile.TemporaryDirectory() as root:
            path = Path(root) / "rubika-stable-v2.p12"
            path.write_bytes(b"sentinel-existing-key")
            with patch.object(module, "command") as invoke:
                self.assertFalse(module.generate_key(path, {}))
                invoke.assert_not_called()
            self.assertEqual(path.read_bytes(), b"sentinel-existing-key")

    def test_secret_is_supplied_by_stdin_not_argv(self):
        fake = SimpleNamespace(returncode=0, stdout="", stderr="")
        argv = ["gh", "secret", "set", "RUBIKA_STABLE_KEY_PASSWORD", "-R", module.REPOSITORY]
        with patch.object(module.subprocess, "run", return_value=fake) as call:
            module.command(argv, input_text="super-secret-value")
        self.assertEqual(call.call_args.args[0], argv)
        self.assertEqual(call.call_args.kwargs["input"], "super-secret-value")
        self.assertNotIn("super-secret-value", " ".join(call.call_args.args[0]))

    def test_gh_secret_names_are_loaded_without_values(self):
        with patch.object(module, "command", return_value='[{"name":"RUBIKA_STABLE_KEY_PASSWORD"}]') as cmd:
            names = module.list_gh_values("secret", module.REPOSITORY)
        self.assertIn("RUBIKA_STABLE_KEY_PASSWORD", names)
        self.assertIsNone(names["RUBIKA_STABLE_KEY_PASSWORD"])
        self.assertEqual(cmd.call_args.args[0][-1], "name")


if __name__ == "__main__":
    unittest.main()
