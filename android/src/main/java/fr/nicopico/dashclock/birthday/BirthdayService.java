/*
 * Copyright 2015 Nicolas Picon <nicopico.dev@gmail.com>
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

import fr.nicopico.dashclock.birthday.data.Birthday;
import fr.nicopico.dashclock.birthday.data.BirthdayRetriever;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import org.joda.time.*;

import java.util.List;
import java.util.Locale;

/**
 * User: Nicolas PICON
 * Date: 24/08/13 - 17:22
 */
public class BirthdayService extends DashClockExtension {

    private static final String TAG = BirthdayService.class.getSimpleName();
    private static final String DEFAULT_LANG = "en";

    private BirthdayRetriever birthdayRetriever;
    private SharedPreferences sharedPreferences;

    private int daysLimit;
    private boolean showQuickContact;
    private boolean disableLocalization;
    private String contactGroupId;

    private boolean needToRefreshLocalization;

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        final Context applicationContext = getApplicationContext();
        assert applicationContext != null;
        birthdayRetriever = new BirthdayRetriever(applicationContext);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

        updatePreferences();

        // Listen for contact update
        //noinspection ConstantConditions
        addWatchContentUris(new String[] {
                ContactsContract.Contacts.CONTENT_URI.toString()
        });
    }

    private void updatePreferences() {
        daysLimit = Integer.valueOf(sharedPreferences.getString(
                SettingsActivity.PREF_DAYS_LIMIT_KEY, "7"
        ));
        showQuickContact = sharedPreferences.getBoolean(
                SettingsActivity.PREF_SHOW_QUICK_CONTACT, true
        );

        boolean previousDisableLocalizationValue = disableLocalization;
        disableLocalization = sharedPreferences.getBoolean(
                SettingsActivity.PREF_DISABLE_LOCALIZATION, false
        );
        needToRefreshLocalization = previousDisableLocalizationValue != disableLocalization;

        contactGroupId = sharedPreferences.getString(
                SettingsActivity.PREF_CONTACT_GROUP, SettingsActivity.NO_CONTACT_GROUP_SELECTED
        );

    }

    @Override
    protected void onUpdateData(int reason) {
        if (reason == UPDATE_REASON_SETTINGS_CHANGED) {
            updatePreferences();
        }

        final Resources res = getResources();
        final List<Birthday> birthdays =
                birthdayRetriever.getContactWithBirthdays(getApplicationContext(), contactGroupId);

        Configuration config = new Configuration();
        config.setToDefaults();

        // Disable/enable Android localization
        if (needToRefreshLocalization ||
                (disableLocalization && !DEFAULT_LANG.equals(Locale.getDefault().getLanguage()))) {
            if (disableLocalization) {
                config.locale = new Locale(DEFAULT_LANG);
            }
            else {
                // Restore Android localization
                //noinspection ConstantConditions
                config.locale = Resources.getSystem().getConfiguration().locale;
            }

            Locale.setDefault(config.locale);
            getBaseContext().getResources()
                    .updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }

        DateTime today = new DateTime();

        int upcomingBirthdays = 0;
        String collapsedTitle = null;
        String expandedTitle = null;
        StringBuilder body = new StringBuilder();

        for (Birthday birthday : birthdays) {
            DateTime birthdayEvent;
            MonthDay birthdayDate = birthday.birthdayDate;
            try {
                birthdayEvent = birthdayDate.toDateTime(today);
            }
            catch (IllegalFieldValueException e) {
                if (birthdayDate.getDayOfMonth() == 29 && birthdayDate.getMonthOfYear() == 2) {
                    // Birthday on February 29th (leap year) -> March 1st
                    birthdayEvent = birthdayDate.dayOfMonth().addToCopy(1).toDateTime(today);
                }
                else {
                    Log.e(TAG, "Invalid date", e);
                    continue;
                }
            }

            // How many days before the birthday ?
            int days;
            if (birthdayEvent.isAfter(today) || birthdayEvent.isEqual(today)) {
                days = Days.daysBetween(today, birthdayEvent).getDays();
            }
            else {
                // Next birthday event is next year
                days = Days.daysBetween(today, birthdayEvent.plusYears(1)).getDays();
            }

            // Should the birthday be displayed ?
            if (days <= daysLimit) {
                upcomingBirthdays++;

                if (upcomingBirthdays == 1) {
                    // A single birthday will be displayed
                    collapsedTitle = birthday.displayName;
                    expandedTitle = res.getString(R.string.single_birthday_title_format, birthday.displayName);
                }

                // More than 1 upcoming birthday: display contact name
                if (upcomingBirthdays > 1) {
                    body.append("\n").append(birthday.displayName).append(", ");
                }

                // Age
                if (!birthday.unknownYear) {
                    int age = today.get(DateTimeFieldType.year()) - birthday.year;
                    body.append(res.getQuantityString(R.plurals.age_format, age, age));
                    body.append(' ');
                }

                // When
                int daysFormatResId;
                switch (days) {
                    case 0:
                        daysFormatResId = R.string.when_today_format;
                        break;
                    case 1:
                        daysFormatResId = R.string.when_tomorrow_format;
                        break;
                    default:
                        daysFormatResId = R.string.when_days_format;
                }

                body.append(res.getString(daysFormatResId, days));
            }
            else {
                // All visible birthdays have been processed
                break;
            }
        }

        if (upcomingBirthdays > 0) {
            Intent clickIntent = buildClickIntent(birthdays.subList(0, upcomingBirthdays));

            if (upcomingBirthdays > 1) {
                collapsedTitle += " + " + (upcomingBirthdays - 1);
            }

            // Display message
            publishUpdate(
                    new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_extension_white)
                            .status(collapsedTitle)
                            .expandedTitle(expandedTitle)
                            .expandedBody(body.toString())
                            .clickIntent(clickIntent)
            );
        }
        else {
            // Nothing to show
            publishUpdate(new ExtensionData().visible(false));
        }
    }

    private Intent buildClickIntent(List<Birthday> birthdays) {
        Intent clickIntent;
        if (showQuickContact) {
            // Open QuickContact dialog on click
            clickIntent = QuickContactProxy.buildIntent(getApplicationContext(), birthdays.get(0).lookupKey);
        }
        else {
            clickIntent = new Intent(Intent.ACTION_VIEW);
            //noinspection ConstantConditions
            clickIntent.setData(
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(birthdays.get(0).contactId))
            );
        }

        return clickIntent;
    }
}
