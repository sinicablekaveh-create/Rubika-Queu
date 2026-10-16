#!/usr/bin/env python3
"""Install an isolated native RTL dashboard after the audited source is extracted.

Never change QueueService, MainActivity, original signing files, or app data.
"""
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

ANDROID = 'http://schemas.android.com/apk/res/android'
ET.register_namespace('android', ANDROID)
A = lambda name: '{%s}%s' % (ANDROID, name)
MANIFEST = Path('app/src/main/AndroidManifest.xml')
DEST = Path('app/src/main/java/ir/sinicable/rubikaqueue')
SRC = Path('overrides/native-ui/NativeDashboardActivity.java')
TARGET = DEST / 'NativeDashboardActivity.java'
PREVIEW_SRC = Path('overrides/native-ui/NativeDashboardPreviewPolicy.java')
PREVIEW_TARGET = DEST / 'NativeDashboardPreviewPolicy.java'


def install():
    if not (DEST / 'MainActivity.java').is_file():
        raise SystemExit('MainActivity missing: run this after source extraction')
    if not SRC.is_file() or not PREVIEW_SRC.is_file() or not MANIFEST.is_file():
        raise SystemExit('Native dashboard source or Android manifest missing')

    tree = ET.parse(MANIFEST)
    manifest = tree.getroot()
    app = manifest.find('application')
    if app is None:
        raise SystemExit('Manifest has no application element')
    main = next((x for x in app.findall('activity') if x.get(A('name')) == '.MainActivity'), None)
    if main is None:
        raise SystemExit('MainActivity declaration missing')
    dashboards = [x for x in app.findall('activity') if x.get(A('name')) == '.NativeDashboardActivity']
    if len(dashboards) > 1:
        raise SystemExit('Ambiguous dashboard declarations')
    filters = []
    for activity in app.findall('activity'):
        for intent in activity.findall('intent-filter'):
            main_action = any(x.get(A('name')) == 'android.intent.action.MAIN' for x in intent.findall('action'))
            launcher = any(x.get(A('name')) == 'android.intent.category.LAUNCHER' for x in intent.findall('category'))
            if main_action and launcher:
                filters.append((activity, intent))
    if len(filters) != 1:
        raise SystemExit('Expected exactly one launcher entry')

    activity, filt = filters[0]
    if not dashboards:
        dashboard = ET.Element('activity', {A('name'): '.NativeDashboardActivity', A('exported'): 'true'})
        app.insert(list(app).index(main), dashboard)
    else:
        dashboard = dashboards[0]
        if dashboard.get(A('exported')) != 'true':
            raise SystemExit('Unexpected dashboard exported flag')
    if activity is not dashboard:
        activity.remove(filt)
        dashboard.append(filt)

    # Keep the original app entry activity private, but navigable from the dashboard.
    main.set(A('exported'), 'false')
    if TARGET.exists() and TARGET.read_bytes() != SRC.read_bytes():
        raise SystemExit('Unexpected existing dashboard; refusing overwrite')
    if PREVIEW_TARGET.exists() and PREVIEW_TARGET.read_bytes() != PREVIEW_SRC.read_bytes():
        raise SystemExit('Unexpected existing preview policy; refusing overwrite')
    # Write only after all validations have passed.
    tree.write(MANIFEST, encoding='unicode', xml_declaration=True)
    shutil.copyfile(SRC, TARGET)
    shutil.copyfile(PREVIEW_SRC, PREVIEW_TARGET)
    print('Installed isolated RTL dashboard launcher and preserved legacy MainActivity')


if __name__ == '__main__':
    install()
