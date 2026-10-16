import shutil
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class QueueStartDiscoveryInstallerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.project = Path(self.temp.name)
        with zipfile.ZipFile(ROOT / "source/Rubika-Queu-v1.1-personal-queue-source.zip") as archive:
            archive.extractall(self.project)
        (self.project / "scripts").mkdir(parents=True, exist_ok=True)
        for directory in ("overrides/ai-assistant", "overrides/ai-discovery"):
            (self.project / directory).mkdir(parents=True, exist_ok=True)
        for script in ("add-ai-assistant.py", "add-ai-discovery.py", "add-queue-start-discovery.py"):
            shutil.copy(ROOT / "scripts" / script, self.project / "scripts" / script)
        for directory in ("ai-assistant", "ai-discovery"):
            for source in (ROOT / "overrides" / directory).glob("*.java"):
                shutil.copy(source, self.project / "overrides" / directory / source.name)
        for script in ("add-ai-assistant.py", "add-ai-discovery.py", "add-queue-start-discovery.py"):
            subprocess.run(["python3", f"scripts/{script}"], cwd=self.project, check=True, capture_output=True, text=True)

    def tearDown(self):
        self.temp.cleanup()

    def test_adds_saved_default_on_setting_and_queue_hook_before_delivery(self):
        main = (self.project / "app/src/main/java/ir/sinicable/rubikaqueue/MainActivity.java").read_text()
        queue = (self.project / "app/src/main/java/ir/sinicable/rubikaqueue/QueueService.java").read_text()
        self.assertIn('getBoolean(QueueStartDiscoveryPolicy.PREFERENCE,true)', main)
        self.assertIn('putBoolean(QueueStartDiscoveryPolicy.PREFERENCE,checked)', main)
        self.assertLess(queue.index("QueueStartDiscovery.run(this,client"), queue.index("while(!cancel)"))
        self.assertIn("catch(RubikaClient.ApiError api){if(DiscoveryEngine.rate(api))throw api;", queue)

    def test_installer_is_idempotent(self):
        before = (self.project / "app/src/main/java/ir/sinicable/rubikaqueue/QueueService.java").read_text()
        subprocess.run(["python3", "scripts/add-queue-start-discovery.py"], cwd=self.project, check=True, capture_output=True, text=True)
        after = (self.project / "app/src/main/java/ir/sinicable/rubikaqueue/QueueService.java").read_text()
        self.assertEqual(before, after)
        self.assertEqual(1, after.count("QueueStartDiscovery.run(this,client"))


if __name__ == "__main__":
    unittest.main()
