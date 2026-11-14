#include <jni.h>
#include <stddef.h>
// Native validation only. TLS, credentials, and actual Rubika dispatch remain in Java.
extern "C" JNIEXPORT jint JNICALL
Java_ir_sinicable_rubikaqueue_NativeSendCore_nativeValidate(
    JNIEnv* env, jclass, jstring recipient, jstring text, jlong rnd) {
  if (recipient == nullptr || text == nullptr) return 1;
  if (rnd < 1 || rnd > 1000000) return 4;
  const jsize size = env->GetStringUTFLength(recipient);
  if (size < 12 || size > 65) return 2;
  const char* guid = env->GetStringUTFChars(recipient, nullptr);
  if (guid == nullptr) return 2;
  bool valid = guid[0] == 'u';
  for (jsize i = 1; i < size; ++i) {
    const char c = guid[i];
    if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
          (c >= '0' && c <= '9') || c == '_')) { valid=false; break; }
  }
  env->ReleaseStringUTFChars(recipient, guid);
  if (!valid) return 2;
  const jsize length = env->GetStringLength(text);
  if (length < 1 || length > 4200) return 3;
  return 0;
}
