# Birthday DashClock Extension

Source of the Birthday DashClock Extension

See https://play.google.com/store/apps/details?id=fr.nicopico.dashclock.birthday

## Build

The keystore and information to sign the application are not included. 
To build the project, create a ```gradle.properties``` in the [android](android) sub-project:

```
keystorePath=/PATH/TO/KEYSTORE
keystorePassword=KEYSTORE_PASSWORD
keystoreKeyAlias=KEY_ALIAS
keystoreKeyPassword=KEY_PASSWORD
```

This allows automating the signing of the release version without putting the sensible information in source control.