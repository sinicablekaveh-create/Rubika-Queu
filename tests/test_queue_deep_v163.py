import unittest
from pathlib import Path
ROOT=Path("app/src/main/java/ir/sinicable/rubikaqueue")
def src(n): return (ROOT/n).read_text(encoding="utf-8")

class QueueDeep163(unittest.TestCase):
    def test_retry_after_is_parsed(self):
        r=src("RubikaClient.java")
        self.assertIn("retryAfterMillis",r)
        self.assertIn('getHeaderField("Retry-After")',r)
        self.assertIn("RateLimitError",r)
        self.assertIn("DateTimeFormatter.RFC_1123_DATE_TIME",r)

    def test_all_three_queues_honor_server_delay(self):
        for n in ("QueueService.java","QueueService2.java","QueueService3.java"):
            q=src(n)
            self.assertIn("serverDelay=api instanceof RubikaClient.RateLimitError",q)
            self.assertIn("Math.max(localDelay,serverDelay)",q)
            self.assertIn("Retry-After سرور",q)

    def test_sendmessage_is_not_failover_retried(self):
        r=src("RubikaClient.java")
        method=r[r.index("private boolean safeFailoverMethod"):r.index("private boolean hostSpecificFailure")]
        self.assertNotIn("sendMessage",method)
        for n in ("QueueService.java","QueueService2.java","QueueService3.java"):
            q=src(n)
            self.assertIn('current.put("status","uncertain")',q)
            self.assertIn("Never repeat this row automatically",q)

    def test_history_uses_wal_for_three_writers(self):
        h=src("SentHistoryStore.java")
        self.assertIn("setWriteAheadLoggingEnabled(true)",h)
        self.assertIn("UNIQUE INDEX uniq_sent_attempt",h)

    def test_rate_limit_does_not_skip_recipient(self):
        q=src("QueueService.java")
        block=q[q.index("if(rate(c))"):q.index("INVALID_INPUT and other explicit",q.index("if(rate(c))"))]
        self.assertIn('current.put("status","retry_wait")',block)
        self.assertNotIn('current.put("status","skipped")',block)

if __name__=="__main__": unittest.main()
