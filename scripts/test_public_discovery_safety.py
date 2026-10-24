import importlib.util
from pathlib import Path
import unittest
import os

ENGINE=Path(os.environ.get('DISCOVERY_ENGINE_TEST_SOURCE','app/src/main/java/ir/sinicable/rubikaqueue/DiscoveryEngine.java'))
spec=importlib.util.spec_from_file_location('public_safety',Path(__file__).with_name('add-public-discovery-safety.py'))
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class PublicDiscoverySafetyTest(unittest.TestCase):
    def test_transformation_refuses_account_chat_leads_and_unverified_seeds(self):
        src=ENGINE.read_text(encoding='utf-8')
        out=module.transform(src)
        self.assertNotIn('read("getChats"',out)
        self.assertNotIn('Published seed found in this request',out)
        self.assertIn('if(!PublicPages.hasPublicRubikaLanding(html))continue;',out)
        self.assertIn('if(verified)DiscoveryData.merge(state,"groups",item,stamp);',out)
        self.assertIn('DiscoveryLinks.rubika(url)',out)
    def test_transform_is_idempotent_at_checkout_boundary(self):
        src=ENGINE.read_text(encoding='utf-8')
        self.assertEqual(module.transform(src),src)

if __name__=='__main__':unittest.main()
