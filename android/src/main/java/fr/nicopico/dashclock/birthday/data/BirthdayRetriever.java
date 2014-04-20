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

package fr.nicopico.dashclock.birthday.data;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import org.joda.time.MonthDay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fr.nicopico.dashclock.birthday.SettingsActivity;

/**
 * User: Nicolas PICON
 * Date: 24/08/13 - 18:58
 */
public class BirthdayRetriever {

    private static final String TAG = BirthdayRetriever.class.getSimpleName();

    private static final Pattern regexDate;
    static {
        try {
            regexDate = Pattern.compile("(\\d{4}|-)-(\\d{2})-(\\d{2})", Pattern.COMMENTS);
        }
        catch (PatternSyntaxException ex) {
            throw new Error(ex);
        }
    }

    private final SharedPreferences sharedPreferences;

    public BirthdayRetriever(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public List<Birthday> getContactWithBirthdays(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        final boolean debugMode = sharedPreferences.getBoolean(SettingsActivity.PREF_DEBUG_MODE, false);

        // Retrieve contacts with birthdays
        @SuppressWarnings("ConstantConditions")
        Cursor c = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                new String[] {
                        ContactsContract.Data.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Event.START_DATE
                },
                String.format(
                        "%s = ? and %s = '%s'",
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Event.TYPE,
                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY
                ),
                new String[] {
                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE
                },
                ContactsContract.Data.CONTACT_ID
        );

        List<Birthday> result = new ArrayList<Birthday>(c != null ? c.getCount() : 0);
        try {
            Birthday birthday;


            // DEBUG MODE
            StringBuilder sb = null;
            int nbColumns = 0;
            if (debugMode) {
                sb = new StringBuilder();
                if (c != null) {
                    nbColumns = c.getColumnCount();
                    for (String s : c.getColumnNames()) {
                        sb.append(s).append(';');
                    }
                    sb.append("is_valid\n");
                }
            }

            while (c != null && c.moveToNext()) {
                birthday = buildBirthday(contentResolver, c);
                if (birthday != null) result.add(birthday);

                // DEBUG MODE
                if (debugMode) {
                    for (int i = 0; i < nbColumns; i++) {
                        sb.append(c.getString(i)).append(';');
                    }
                    sb.append(birthday != null);
                    sb.append('\n');
                }
            }

            // DEBUG MODE
            if (debugMode) {
                // Send email debug content by e-mail
                // FIXME Add as an action to dashclock
                Intent mailIntent = new Intent(Intent.ACTION_SEND);
                mailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mailIntent.setType("message/rfc822");
                mailIntent.putExtra(Intent.EXTRA_EMAIL, "nicopico.dev@gmail.com");
                mailIntent.putExtra(Intent.EXTRA_SUBJECT, "[DashClock Birthday] DEBUG");
                mailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

                context.startActivity(mailIntent);

                // Disable debug mode to prevent spamming the user
                sharedPreferences.edit()
                        .putBoolean(SettingsActivity.PREF_DEBUG_MODE, false)
                        .apply();
            }
        }
        finally {
            if (c != null) c.close();
        }

        Collections.sort(result);
        return result;
    }

    private Birthday buildBirthday(ContentResolver contentResolver, Cursor c) {
        String birthDate = c.getString(1);
        if (birthDate == null) return null;

        // Analyze birthday string
        try {
            Matcher regexMatcher = regexDate.matcher(birthDate);

            if (regexMatcher.find()) {
                Birthday birthday = new Birthday(contentResolver, c.getLong(0));

                // Birthday *must* have a display name
                if (birthday.displayName == null) return null;

                birthday.birthdayDate = new MonthDay(
                        Integer.parseInt(regexMatcher.group(2)),
                        Integer.parseInt(regexMatcher.group(3))
                );

                if (!"-".equals(regexMatcher.group(1))) {
                    birthday.year = Integer.parseInt(regexMatcher.group(1));
                    birthday.unknownYear = false;
                }

                return birthday;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "Error while analyzing birthday", e);
            return null;
        }

        return null;
    }

}
