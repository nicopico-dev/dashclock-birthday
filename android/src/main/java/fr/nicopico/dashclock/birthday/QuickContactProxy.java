/*
 * Copyright 2013 Nicolas Picon <nicopico.dev@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private static final String EXTRA_CONTACT_ID = "EXTRA_CONTACT_ID";

    /**
     * Build an intent to display a QuickContact dialog
     *
     * @param context Context used to create the intent
     * @param lookupKey LOOKUP_KEY of the contact to display
     * @return Intent for the QuickContact
     */
    public static Intent buildIntent(Context context, String lookupKey) {
        return new Intent(context, QuickContactProxy.class)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                .putExtra(EXTRA_CONTACT_ID, lookupKey);
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
