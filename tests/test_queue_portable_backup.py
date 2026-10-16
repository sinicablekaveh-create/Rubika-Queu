"""Offline checks: backup is manual, encrypted, fails closed, and never sends."""
from pathlib import Path
from zipfile import ZipFile
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"source/Rubika-Queu-v1.1-personal-queue-source.zip"
JAVADIR="app/src/main/java/ir/sinicable/rubikaqueue"

class PortableBackupIntegrationTests(unittest.TestCase):
 def setUp(self):
  temp=tempfile.TemporaryDirectory(prefix="queue-backup-")
  self.addCleanup(temp.cleanup)
  self.work=Path(temp.name)
  with ZipFile(SOURCE) as z:
   for item in ("MainActivity.java","QueueService.java"):
    relative=f"{JAVADIR}/{item}"
    out=self.work/relative
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_bytes(z.read(relative))
  manifest=self.work/"app/src/main/AndroidManifest.xml"
  manifest.parent.mkdir(parents=True,exist_ok=True)
  shutil.copy(ROOT/"app/src/main/AndroidManifest.xml",manifest)
  (self.work/"scripts").mkdir()
  for name in ("repair-queue-runtime-launch.py","add-queue-portable-backup.py"):
   shutil.copy(ROOT/"scripts"/name,self.work/"scripts"/name)
  overrides=self.work/"overrides/queue-backup"
  overrides.mkdir(parents=True,exist_ok=True)
  for name in ("QueueBackupCrypto.java","QueueBackupSchema.java","QueueBackupActivity.java"):
   shutil.copy(ROOT/"overrides/queue-backup"/name,overrides/name)
  fix=subprocess.run(["python3","scripts/repair-queue-runtime-launch.py"],cwd=self.work,text=True,capture_output=True)
  self.assertEqual(fix.returncode,0,fix.stderr)
 def apply(self):
  return subprocess.run(["python3","scripts/add-queue-portable-backup.py"],cwd=self.work,text=True,capture_output=True)
 def test_explicit_ui_and_manifest_without_message_sending(self):
  result=self.apply()
  self.assertEqual(result.returncode,0,result.stderr)
  main=(self.work/JAVADIR/"MainActivity.java").read_text("utf-8")
  self.assertIn("پشتیبان رمزگذاری‌شده صف / بازیابی",main)
  self.assertIn('startActivity(new Intent(this,QueueBackupActivity.class))',main)
  manifest=(self.work/"app/src/main/AndroidManifest.xml").read_text("utf-8")
  self.assertIn('android:name=".QueueBackupActivity" android:exported="false"',manifest)
  crypto=(self.work/JAVADIR/"QueueBackupCrypto.java").read_text("utf-8")
  self.assertIn("AES/GCM/NoPadding",crypto)
  self.assertIn("PBKDF2WithHmacSHA256",crypto)
  self.assertIn("210000",crypto)
  self.assertNotIn("RubikaClient",crypto)
  schema=(self.work/JAVADIR/"QueueBackupSchema.java").read_text("utf-8")
  self.assertIn('restored.put("scheduled_start_at",0)',schema)
  self.assertIn('row.put("status","uncertain")',schema)
  activity=(self.work/JAVADIR/"QueueBackupActivity.java").read_text("utf-8")
  self.assertIn("JobStore.load(this)!=null",activity)
  self.assertIn("JobStore.save(this,candidate)",activity)
  self.assertIn("account.guid.equals",activity)
  self.assertNotIn("client.send(",activity)
  self.assertNotIn("startForegroundService(",activity)
  self.assertNotIn("Vault.save(",activity)
 def test_installer_is_idempotent_and_refuses_partial_integrations(self):
  self.assertEqual(self.apply().returncode,0)
  before=(self.work/JAVADIR/"MainActivity.java").read_bytes()
  self.assertEqual(self.apply().returncode,0)
  self.assertEqual((self.work/JAVADIR/"MainActivity.java").read_bytes(),before)
  manifest=self.work/"app/src/main/AndroidManifest.xml"
  manifest.write_text("wrong Android manifest",encoding="utf-8")
  self.assertNotEqual(self.apply().returncode,0)
 def test_secret_field_names_are_not_exported(self):
  schema=(ROOT/"overrides/queue-backup/QueueBackupSchema.java").read_text("utf-8")
  self.assertNotIn("privateKey",schema)
  self.assertNotIn("auth_token",schema)
  self.assertNotIn('"session"',schema)
  self.assertIn("message_id",schema)
  self.assertIn("reviewed_at",schema)
  self.assertIn("rnd",schema)

if __name__=="__main__":unittest.main()
