"""Offline regressions for the app's no-group-search, invalid-input patch."""
import unittest
from pathlib import Path

ROOT = Path("app/src/main/java/ir/sinicable/rubikaqueue")

def source(name):
    return (ROOT / name).read_text(encoding="utf-8")

class NoGroupInvalidInputTests(unittest.TestCase):
    def test_group_search_controls_are_gone(self):
        s = source("MainActivity.java")
        for removed in ("searchGroups", "groupSend", "chooseGroups(", "confirmGroups(",
                        "new Intent(this,AiDiscoveryActivity.class)"):
            self.assertNotIn(removed, s)
        self.assertIn('putBoolean("groups",false)', s)

    def test_group_network_methods_are_disabled(self):
        s = source("DiscoveryEngine.java")
        for removed in ('"groupPreviewByJoinLink"', '"getChats"', 'if(wantGroups)for',
                        'LeadExtractor.groupLinks(text)', 'DiscoveryData.merge(state,"groups"'):
            self.assertNotIn(removed, s)
        self.assertIn('wantGroups=false', s)
        self.assertIn('state.put("groups",new JSONArray())', s)
        self.assertIn('"searchGlobalObjects"', s)  # Contact research stays available.

    def test_legacy_group_queue_does_not_send(self):
        s = source("QueueService.java")
        self.assertIn('if("group".equals(current.optString("kind")))', s)
        self.assertIn('other explicit server rejections', s)
        self.assertIn('current.put("status","skipped")', s)
        self.assertIn('server_code', s)
        self.assertIn('صف به مورد بعدی ادامه می', s)

    def test_invalid_input_context_no_secrets(self):
        s = source("RubikaClient.java")
        self.assertIn('public final String code,method;', s)
        self.assertIn('),method)', s)
        self.assertIn('number.matches("989[0-9]{9}")', s)
        self.assertIn('clean.length()>60', s)
        self.assertIn('cleanText.length()>4200', s)
        self.assertIn('"INVALID_INPUT"', source("MainActivity.java"))

    def test_no_group_results_are_exported(self):
        self.assertNotIn('data.getJSONArray("groups")', source("ResearchXlsx.java"))
        self.assertIn('boolean groups = false;', source("QueueStartDiscovery.java"))
        self.assertIn('engine.run(data,terms,false,',
                      source("DiscoveryService.java"))
        manifest=Path("app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertNotIn('android:name=".AiDiscoveryActivity"',manifest)

if __name__ == "__main__":
    unittest.main()
