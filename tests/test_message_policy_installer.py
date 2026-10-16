import pathlib
import subprocess
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "source/Rubika-Queu-v1.1-personal-queue-source.zip"


class MessagePolicyInstallerTests(unittest.TestCase):
    def test_installs_idempotently_into_audited_source(self):
        with tempfile.TemporaryDirectory(prefix="rubika-message-policy-") as directory:
            work = pathlib.Path(directory)
            with zipfile.ZipFile(SOURCE) as archive:
                archive.extractall(work)
            (work / "overrides/message-policy").mkdir(parents=True)
            (work / "overrides/message-policy/MessageQueuePolicy.java").write_bytes(
                (ROOT / "overrides/message-policy/MessageQueuePolicy.java").read_bytes()
            )
            discovery = work / "overrides/ai-discovery"
            discovery.mkdir(parents=True)
            for name in ("QueueStartDiscovery.java", "QueueStartDiscoveryPolicy.java"):
                (discovery / name).write_bytes((ROOT / "overrides/ai-discovery" / name).read_bytes())
            queue_script = work / "queue-start.py"
            queue_script.write_bytes((ROOT / "scripts/add-queue-start-discovery.py").read_bytes())
            script = work / "install.py"
            script.write_bytes((ROOT / "scripts/add-message-queue-policy.py").read_bytes())
            queue_result = subprocess.run(["python3", str(queue_script)], cwd=work, capture_output=True, text=True)
            first = subprocess.run(["python3", str(script)], cwd=work, capture_output=True, text=True)
            second = subprocess.run(["python3", str(script)], cwd=work, capture_output=True, text=True)
            self.assertEqual(queue_result.returncode, 0, queue_result.stderr)
            self.assertEqual(first.returncode, 0, first.stderr)
            self.assertEqual(second.returncode, 0, second.stderr)
            source_root = work / "app/src/main/java/ir/sinicable/rubikaqueue"
            job_store = (source_root / "JobStore.java").read_text(encoding="utf-8")
            main = (source_root / "MainActivity.java").read_text(encoding="utf-8")
            self.assertIn("MessageQueuePolicy.mayDispatch", job_store)
            self.assertIn("MessageQueuePolicy.requiresReview", job_store)
            self.assertIn("MessageQueuePolicy.requireDraft", main)
            self.assertIn("MessageQueuePolicy.requireRecipients", main)
            self.assertTrue((source_root / "MessageQueuePolicy.java").is_file())

    def test_refuses_unexpected_source_without_partial_writes(self):
        with tempfile.TemporaryDirectory(prefix="rubika-message-policy-guard-") as directory:
            work = pathlib.Path(directory)
            source_root = work / "app/src/main/java/ir/sinicable/rubikaqueue"
            source_root.mkdir(parents=True)
            (source_root / "JobStore.java").write_text("unexpected", encoding="utf-8")
            (source_root / "MainActivity.java").write_text("untouched", encoding="utf-8")
            override = work / "overrides/message-policy"
            override.mkdir(parents=True)
            (override / "MessageQueuePolicy.java").write_bytes(
                (ROOT / "overrides/message-policy/MessageQueuePolicy.java").read_bytes()
            )
            script = work / "install.py"
            script.write_bytes((ROOT / "scripts/add-message-queue-policy.py").read_bytes())
            result = subprocess.run(["python3", str(script)], cwd=work, capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual((source_root / "JobStore.java").read_text(encoding="utf-8"), "unexpected")
            self.assertEqual((source_root / "MainActivity.java").read_text(encoding="utf-8"), "untouched")


if __name__ == "__main__":
    unittest.main()
