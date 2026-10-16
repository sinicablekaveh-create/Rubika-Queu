"""Dependency-free regression checks for data-preserving APK updates."""
import importlib.util
import pathlib
import unittest

MODULE_PATH = pathlib.Path(__file__).resolve().parents[1] / 'scripts' / 'apk_update_policy.py'
spec = importlib.util.spec_from_file_location('apk_update_policy', MODULE_PATH)
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)

INSTALLED = {
    'package': 'ir.sinicable.rubikaqueue',
    'version_code': 15,
    'certificate_sha256': 'fdb827fa66b05309a2e7a759bc03d9f22355b54cd74c63e74a5d889823087026',
}


class TestApkUpdatePolicy(unittest.TestCase):
    def make_candidate(self, **changes):
        result = dict(INSTALLED, version_code=16)
        result.update(changes)
        return result

    def test_matching_identity_and_increased_version_is_only_a_candidate(self):
        status, checks = policy.classify(self.make_candidate(), INSTALLED)
        self.assertEqual(status, 'UPDATE_CANDIDATE_UNTESTED')
        self.assertTrue(all(checks.values()))

    def test_same_version_is_not_an_upgrade(self):
        self.assertEqual(policy.classify(self.make_candidate(version_code=15), INSTALLED)[0], 'NOT_UPDATABLE')

    def test_previous_version_is_rejected(self):
        self.assertEqual(policy.classify(self.make_candidate(version_code=14), INSTALLED)[0], 'NOT_UPDATABLE')

    def test_different_signer_is_rejected_even_at_higher_version(self):
        candidate = self.make_candidate(certificate_sha256='8747838d913e2f0f58dc66e432eb3753bc826e6a3f7e23eaf6bb799cd63af28b')
        self.assertEqual(policy.classify(candidate, INSTALLED)[0], 'NOT_UPDATABLE')

    def test_different_package_is_rejected(self):
        self.assertEqual(policy.classify(self.make_candidate(package='app.rbmain.a'), INSTALLED)[0], 'NOT_UPDATABLE')

    def test_colon_separated_certificate_accepted(self):
        formatted = ':'.join(INSTALLED['certificate_sha256'][i:i + 2] for i in range(0, 64, 2)).upper()
        self.assertEqual(policy.normalize_certificate(formatted), INSTALLED['certificate_sha256'])

    def test_bad_fingerprint_rejected(self):
        with self.assertRaises(ValueError):
            policy.normalize_certificate('not-a-certificate')

    def test_aapt_package_parser(self):
        record = policy.parse_badging("package: name='ir.sinicable.rubikaqueue' versionCode='16' versionName='1.3.2-messaging-safety'\n")
        self.assertEqual(record, {'package': 'ir.sinicable.rubikaqueue', 'version_code': 16, 'version_name': '1.3.2-messaging-safety'})

    def test_apksigner_parser_rejects_multisigner_without_policy(self):
        cert = INSTALLED['certificate_sha256']
        with self.assertRaises(ValueError):
            policy.parse_signer(f'Signer #1 certificate SHA-256 digest: {cert}\nSigner #2 certificate SHA-256 digest: {cert}\n')

    def test_apksigner_parser(self):
        cert = INSTALLED['certificate_sha256']
        self.assertEqual(policy.parse_signer(f'Signer #1 certificate SHA-256 digest: {cert}\n'), cert)


if __name__ == '__main__':
    unittest.main()
