"""Ensure the removed destination gate is not reintroduced into the queue build."""
from pathlib import Path
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
SOURCE = ROOT / "source/Rubika-Queu-v1.1-personal-queue-source.zip"


class QueueFeatureCleanupTests(unittest.TestCase):
    def test_no_legacy_destination_gate_files(self):
        for path in (
            "overrides/allowed-destinations/AllowedDestinationPolicy.java",
            "overrides/allowed-destinations/AllowedDestinationStore.java",
            "overrides/allowed-destinations/AllowedDestinationsActivity.java",
            "scripts/add-allowed-destinations.py",
            "tests/AllowedDestinationPolicyTests.java",
            "tests/test_allowed_destinations_installer.py",
            "docs/ALLOWED_DESTINATIONS.md",
        ):
            with self.subTest(path=path):
                self.assertFalse((ROOT / path).exists(), path)

    def test_workflow_keeps_queue_and_scheduling_without_gate(self):
        for removed in ("scripts/add-allowed-destinations.py",
                        "AllowedDestinationPolicyTests",
                        "approved-destinations-review"):
            self.assertNotIn(removed, WORKFLOW)
        for kept in ("scripts/add-message-queue-policy.py",
                     "scripts/add-queue-start-discovery.py",
                     "lintDebug assembleDebug",
                     "SendInterval.java",
                     "QueueService.java"):
            self.assertIn(kept, WORKFLOW)

    def test_existing_queue_and_scheduler_source_remains_unmodified(self):
        with zipfile.ZipFile(SOURCE) as archive:
            files = set(archive.namelist())
            base = "app/src/main/java/ir/sinicable/rubikaqueue/"
            for java in ("MainActivity.java", "QueueService.java",
                         "SendInterval.java", "JobStore.java",
                         "ScheduledQueueReceiver.java"):
                with self.subTest(java=java):
                    self.assertIn(base + java, files)
            for java in ("QueueService.java", "MainActivity.java"):
                source = archive.read(base + java).decode("utf-8")
                self.assertNotIn("AllowedDestinationStore", source)
                self.assertNotIn("AllowedDestinationsActivity", source)


if __name__ == "__main__":
    unittest.main()
