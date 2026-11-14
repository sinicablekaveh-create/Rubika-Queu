package ir.sinicable.rubikaqueue;

/** Fail-closed C++/JNI policy before a Java personal-account HTTP request. */
public final class NativeSendCore {
    static { System.loadLibrary("rubikasendcore"); }
    private NativeSendCore() {}
    private static native int nativeValidate(String recipient, String text, long rnd);
    public static boolean available() {
        try { return nativeValidate("u___________","test",1L) == 0; }
        catch (UnsatisfiedLinkError e) { return false; }
    }
    public static void requireValid(String recipient, String text, long rnd) {
        final int error=nativeValidate(recipient,text,rnd);
        if(error==0)return;
        switch(error){
            case 2: throw new IllegalArgumentException("شناسه گیرنده در هسته C++ معتبر نیست");
            case 3: throw new IllegalArgumentException("طول پیام در هسته C++ معتبر نیست");
            case 4: throw new IllegalArgumentException("شناسه یکتای پیام در هسته C++ نامعتبر است");
            default: throw new IllegalArgumentException("پارامترهای ارسال معتبر نیستند");
        }
    }
}
