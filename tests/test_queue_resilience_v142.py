"""Offline invariants for the user-requested robust personal message queue."""
import unittest
from pathlib import Path

PATH=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def read(f):return (PATH/f).read_text(encoding="utf-8")

class QueueResilienceTests(unittest.TestCase):
    def test_invalid_input_skips_only_bad_recipient(self):
        code=read("QueueService.java")
        a=code.index('if("INVALID_INPUT".equalsIgnoreCase(c))')
        b=code.index('if(c.contains("TOO_REQUESTS")',a)
        branch=code[a:b]
        self.assertIn('current.put("status","skipped")',branch)
        self.assertIn('continue;',branch)
        self.assertNotIn('return;',branch)
        self.assertIn('current=null;',branch)

    def test_transport_errors_dont_duplicate_uncertain_delivery(self):
        code=read("QueueService.java")
        self.assertIn('current.put("status","uncertain")',code)
        self.assertIn('if(current!=null && dispatch)',code)
        self.assertIn('current.put("status","retry_wait")',code)
        self.assertIn('JobStore.uncertain(job)?',code)

    def test_safe_backoff_and_recovery(self):
        code=read("QueueService.java")
        self.assertIn('return START_STICKY;',code)
        self.assertIn('RetryBackoff.delayMs',code)
        self.assertIn('job.put("next_retry_time",',code)
        self.assertNotIn('if(JobStore.uncertain(job))throw',code)

    def test_scheduled_work_can_resume_other_recipients(self):
        self.assertNotIn('||JobStore.uncertain(job)',read('ScheduleManager.java'))
        self.assertNotIn('||JobStore.uncertain(job)',read('ScheduledQueueReceiver.java'))

    def test_group_discovery_still_absent(self):
        self.assertNotIn('searchGroups',read('MainActivity.java'))
        self.assertNotIn('groupPreviewByJoinLink',read('DiscoveryEngine.java'))

    def test_server_rejection_code_tolerates_spaces(self):
        code=read('QueueService.java')
        self.assertIn(".replace(' ','_')",code)
        self.assertIn('if("INVALID_INPUT".equalsIgnoreCase(c))',code)

    def test_success_history_archive_failure_cannot_requeue(self):
        code=read("QueueService.java")
        self.assertIn('catch(Exception archiveError)',code)
        self.assertIn('/* Sent state is already durably saved.',code)
if __name__=="__main__":unittest.main()
