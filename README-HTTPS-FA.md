# Rubika 1.8.1 C++/JNI HTTPS-only
This update uses Android network_security_config with cleartextTrafficPermitted=false and system trust anchors.
Rubika personal-account HTTPS transport requires approved Rubika / Iranlms hosts; redirects are disabled.
Official Bot API transport now has a concrete HttpsURLConnection type.
No login or message sending tests performed per user request.
Personal-account Rubika protocol remains unofficial.
