package fr.nicopico.dashclock.birthday;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Display;

/**
 * User: Nicolas PICON
 * Date: 25/08/13 - 13:09
 */
public class QuickContactProxy extends Activity {

    private static final String TAG = QuickContactProxy.class.getSimpleName();

    public static final String EXTRA_CONTACT_ID = "EXTRA_CONTACT_ID";

    /**
     * Build an intent to display a QuickContact dialog
     *
     * @param context Context used to create the intent
     * @param lookupKey LOOKUP_KEY of the contact to display
     * @return Intent for the QuickContact
     */
    public static Intent buildIntent(Context context, String lookupKey) {
        return new Intent(context, QuickContactProxy.class).putExtra(EXTRA_CONTACT_ID, lookupKey);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(EXTRA_CONTACT_ID)) {
            Log.w(TAG, "QuickContact action received without EXTRA_CONTACT_ID extra");
            return;
        }

        // Build lookup uri for contact
        Uri uriContact = Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                getIntent().getStringExtra(EXTRA_CONTACT_ID)
        );

        // Get screen dimensions
        Rect screenRect = new Rect();
        final DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        displayManager.getDisplay(Display.DEFAULT_DISPLAY).getRectSize(screenRect);

        // Show quick contact dialog
        ContactsContract.QuickContact.showQuickContact(
                getBaseContext(), screenRect, uriContact, ContactsContract.QuickContact.MODE_MEDIUM, null
        );

        finish();
    }
}
