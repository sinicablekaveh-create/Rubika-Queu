"""Offline backup validation; no adb device, Rubika account or messages."""
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
from unittest import TestCase, main
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("backup", ROOT/"scripts/backup_sidecar_queue.py")
backup=importlib.util.module_from_spec(spec)
spec.loader.exec_module(backup)

def sample():
    return json.dumps({"owner":"mock-user","message":"TEST ONLY",
        "recipients":[{"status":"sent"},{"status":"pending"},{"status":"uncertain"}]}).encode()

class TestSafeBackup(TestCase):
    def test_authenticated_roundtrip_and_tamper_rejection(self):
        encoded=backup.encrypt(sample(), "testing-only-long-password")
        self.assertTrue(encoded.startswith(backup.MAGIC))
        self.assertNotIn(b"TEST ONLY",encoded)
        self.assertEqual(backup.decrypt(encoded,"testing-only-long-password"),sample())
        with self.assertRaises(Exception):
            backup.decrypt(encoded,"wrong-password")
        with self.assertRaises(Exception):
            backup.decrypt(encoded[:-1]+bytes([encoded[-1]^1]),"testing-only-long-password")

    def test_secret_state_and_bad_password_rejected(self):
        data=json.loads(sample())
        data["privateKey"]="MOCK_NOT_A_KEY"
        with self.assertRaises(ValueError):
            backup.validate(json.dumps(data).encode())
        del data["privateKey"]
        data["recipients"][0]["status"]="mystery"
        with self.assertRaises(ValueError):
            backup.validate(json.dumps(data).encode())
        with self.assertRaises(ValueError):
            backup.encrypt(sample(),"short")
        stats=backup.validate(sample())
        self.assertEqual(stats["rows"],3)
        self.assertTrue(stats["manual_review"])

    def test_strict_read_only_device_access(self):
        cp=subprocess.CompletedProcess([],0,stdout=sample(),stderr=b"")
        with patch.object(backup.shutil,"which",return_value="/mock/adb"), \
             patch.object(backup.subprocess,"run",return_value=cp) as runner:
            self.assertEqual(backup.read_debug_queue("mockserial"),sample())
        args=runner.call_args.args[0]
        self.assertEqual(args,["adb","-s","mockserial","exec-out","run-as",
                              "ir.sinicable.rubikaqueue.sidecar","cat","files/job.json"])
        for bad in ("install","rm","push","Vault","shared_prefs"):
            self.assertNotIn(bad,args)

    def test_refuses_git_and_overwrite(self):
        with tempfile.TemporaryDirectory() as folder:
            parent=Path(folder)
            with self.assertRaises(ValueError):
                with patch.object(backup,"__file__",str(parent/"script.py")):
                    backup.protected_path(parent/"foo.rqbk")
            repo=parent/"git"; repo.mkdir(); (repo/".git").mkdir()
            with self.assertRaises(ValueError):
                backup.protected_path(repo/"private.rqbk")
            target=parent/"private"/"queue.rqbk"
            self.assertEqual(backup.protected_path(target),target)
            target.write_bytes(b"older encrypted file")
            with self.assertRaises(FileExistsError):
                backup.protected_path(target)
            self.assertEqual(target.read_bytes(),b"older encrypted file")

    def test_pure_backup_without_login_state(self):
        text=(ROOT/"scripts/backup_sidecar_queue.py").read_text()
        self.assertIn('"cat", "files/job.json"',text)
        self.assertNotIn('cat", "files/vault',text)
        self.assertNotIn("getPrivateKey(",text)

if __name__=="__main__":
    main()
