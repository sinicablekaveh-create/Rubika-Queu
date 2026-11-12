import unittest
from pathlib import Path

ROOT=Path('app/src/main/java/ir/sinicable/rubikaqueue')
class RecoverySafety(unittest.TestCase):
    def test_identity_and_icon_preserved(self):
        gradle=Path('app/build.gradle').read_text()
        manifest=Path('app/src/main/AndroidManifest.xml').read_text()
        self.assertIn("applicationId 'ir.sinicable.rubikaqueue'",gradle)
        self.assertNotIn("applicationId 'ir.sinicable.rubikaqueue.triple'",gradle)
        self.assertIn('android:icon="@drawable/icon"',manifest)
        self.assertIn('#086C66',Path('app/src/main/res/drawable/icon.xml').read_text())
    def test_photo_upload_is_fail_closed(self):
        ui=(ROOT/'MultiAccountActivity.java').read_text()
        self.assertIn('void pickPhoto(int slot)',ui)
        self.assertIn('void clearPhoto(int slot)',ui)
        self.assertIn('void showPhoto(int slot)',ui)
        self.assertIn('if(a.selectedPhoto!=null)',ui)
        self.assertIn('صف تصویری شروع نمی‌شود',ui)
    def test_consent_and_pause_block_resume(self):
        ui=(ROOT/'MultiAccountActivity.java').read_text()
        self.assertIn('if(!a.consent.isChecked())',ui)
        self.assertIn('job.put("consented",true)',ui)
        self.assertIn('saved.put("consented",true)',ui)
        receiver=(ROOT/'ScheduledQueueReceiver.java').read_text()
        self.assertIn('job.optBoolean("consented",false)',receiver)
        self.assertIn('.getBoolean("paused",false)',receiver)
        for n in ('QueueService.java','QueueService2.java','QueueService3.java'):
            s=(ROOT/n).read_text()
            self.assertIn('"PAUSE".equals(i.getAction())',s)
            self.assertIn('!job.optBoolean("consented",false)',s)
            self.assertIn('job.optString("media_type","")',s)
    def test_csv_support(self):
        excel=(ROOT/'ExcelReader.java').read_text()
        self.assertIn('readCsv(File file)',excel)
        self.assertIn('CodingErrorAction.REPORT',excel)
        self.assertIn('out.entries.size()>1000',excel)

if __name__=='__main__':unittest.main()
