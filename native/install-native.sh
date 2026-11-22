#!/usr/bin/env bash
set -euo pipefail
SRC=android/app/src/main
PKG="$SRC/java/app/moire/anniversary"
mkdir -p "$PKG" "$SRC/res/layout" "$SRC/res/xml"
cp native/MainActivity.java native/MoireWidget.java native/MoireAlarm.java "$PKG/"
cp native/moire_widget.xml "$SRC/res/layout/"
cp native/moire_widget_info.xml "$SRC/res/xml/"
python3 - <<'PYXML'
from pathlib import Path
p=Path('android/app/src/main/AndroidManifest.xml');s=p.read_text();s=s.replace('<application', '<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/><uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/><uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/><application',1)
receiver="""
        <receiver android:name=".MoireAlarm" android:exported="false"/>
        <receiver android:name=".MoireWidget" android:exported="true" android:label="MOIRE">
          <intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/><action android:name="android.intent.action.BOOT_COMPLETED"/><action android:name="android.intent.action.DATE_CHANGED"/><action android:name="android.intent.action.TIMEZONE_CHANGED"/><action android:name="android.intent.action.TIME_SET"/></intent-filter>
          <meta-data android:name="android.appwidget.provider" android:resource="@xml/moire_widget_info"/>
        </receiver>
"""
s=s.replace('</application>',receiver+'</application>');p.write_text(s)
PYXML
python3 - <<'PYNAME'
from pathlib import Path
p=Path('android/app/src/main/res/values/strings.xml');s=p.read_text();import re
s=re.sub(r'(<string name="app_name">).*?(</string>)',r'\1MOIRE\2',s)
s=re.sub(r'(<string name="title_activity_main">).*?(</string>)',r'\1MOIRE\2',s)
p.write_text(s)
PYNAME
