import pathlib
import shutil
import subprocess
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ZIP = ROOT / "source/Rubika-Queu-v1.1-personal-queue-source.zip"
NAMES = ("AllowedDestinationPolicy.java", "AllowedDestinationStore.java",
         "AllowedDestinationsActivity.java")


class AllowedDestinationInstallerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="rubika-approved-destinations-")
        self.addCleanup(self.temp.cleanup)
        self.work = pathlib.Path(self.temp.name)
        with zipfile.ZipFile(ZIP) as source:
            source.extractall(self.work)
        (self.work / "overrides/allowed-destinations").mkdir(parents=True)
        for name in NAMES:
            shutil.copy(ROOT / "overrides/allowed-destinations" / name,
                        self.work / "overrides/allowed-destinations" / name)
        shutil.copy(ROOT / "scripts/add-allowed-destinations.py", self.work / "install.py")
        self.java = self.work / "app/src/main/java/ir/sinicable/rubikaqueue"

    def install(self):
        return subprocess.run(["python3", "install.py"], cwd=self.work,
                              text=True, capture_output=True)

    def test_both_checks_and_ui_installed_without_network(self):
        result = self.install()
        self.assertEqual(result.returncode, 0, result.stderr)
        service = (self.java / "QueueService.java").read_text(encoding="utf-8")
        self.assertEqual(service.count("AllowedDestinationStore.requireApproved("), 2)
        self.assertLess(service.index("AllowedDestinationStore.requireApproved("),
                        service.index("client.resolve("))
        self.assertLess(service.rindex("AllowedDestinationStore.requireApproved("),
                        service.index("client.send("))
        self.assertTrue(all((self.java / name).exists() for name in NAMES))
        main = (self.java / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("AllowedDestinationsActivity.class", main)
        manifest = (self.work / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn('android:name=".AllowedDestinationsActivity" android:exported="false"', manifest)

    def test_idempotent(self):
        self.assertEqual(self.install().returncode, 0)
        before = (self.java / "QueueService.java").read_bytes()
        second = self.install()
        self.assertEqual(second.returncode, 0, second.stderr)
        self.assertEqual((self.java / "QueueService.java").read_bytes(), before)

    def test_fail_closed_when_service_source_changes(self):
        service = self.java / "QueueService.java"
        service.write_text("unexpected service", encoding="utf-8")
        main = self.java / "MainActivity.java"
        before = main.read_bytes()
        result = self.install()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(main.read_bytes(), before)
        self.assertEqual(service.read_text(encoding="utf-8"), "unexpected service")


if __name__ == "__main__":
    unittest.main()
