package ir.sinicable.rubikaqueue;

import android.content.Context;
import android.content.Intent;

/**
 * User-controlled handoff to the separately installed official Rubika client.
 *
 * The official APK is a different signed application. This bridge deliberately
 * does not inspect its files, session, database, or private APIs.
 */
public final class OfficialRubikaBridge {
    public static final String PACKAGE_NAME = "app.rbmain.a";
    public static final int EXPECTED_VERSION_CODE = 411;
    public static final String EXPECTED_VERSION_NAME = "4.1.1";

    private OfficialRubikaBridge() {}

    /** Return whether the official Rubika package exposes an activity on this device. */
    public static boolean isInstalled(Context context) {
        return context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME) != null;
    }

    /** Launch the separately installed official client after an explicit user action. */
    public static boolean open(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
        if (launch == null) return false;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return true;
    }

    /**
     * Open a user-approved draft in the official client. This does not send it;
     * the user must review and confirm the destination and action there.
     */
    public static boolean shareText(Context context, String text) {
        if (text == null || text.trim().isEmpty()) return false;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.setPackage(PACKAGE_NAME);
        send.putExtra(Intent.EXTRA_TEXT, text.trim());
        if (send.resolveActivity(context.getPackageManager()) == null) return false;
        context.startActivity(send);
        return true;
    }
}
