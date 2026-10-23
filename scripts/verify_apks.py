#!/usr/bin/env python3
"""Check the distributed capability boundary in the actual built APKs."""
import os
from pathlib import Path
import re
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
base_permissions = {'android.permission.SET_WALLPAPER'}
full_permissions = base_permissions | {
    'android.permission.CAMERA',
}
certificates = []
expected_certificate = os.environ.get('LUMA_EXPECTED_DEBUG_CERT')
for edition in ['full', 'studio']:
    apk = root / f'app/build/outputs/apk/{edition}/debug/app-{edition}-debug.apk'
    signed = subprocess.check_output([str(tools/'apksigner'), 'verify', '--print-certs', str(apk)], text=True)
    certificates.append(re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', signed).group(1))
    if expected_certificate:
        assert certificates[-1] == expected_certificate, 'Expected debug signing identity changed'
    badging = subprocess.check_output([str(tools/'aapt'), 'dump', 'badging', str(apk)], text=True)
    assert "package: name='com.luma.lockscreen' versionCode='3'" in badging, edition
    assert "targetSdkVersion:'36'" in badging, edition
    permissions = subprocess.check_output([str(tools/'aapt'), 'dump', 'permissions', str(apk)], text=True)
    actual = set(re.findall(r"uses-permission: name='([^']+)'", permissions))
    expected = full_permissions if edition == 'full' else base_permissions
    assert actual == expected, (edition, actual)
    manifest = subprocess.check_output([str(tools/'aapt'), 'dump', 'xmltree', str(apk), 'AndroidManifest.xml'], text=True)
    assert ('E: service' in manifest) == (edition == 'full'), edition
    assert ('android.permission.BIND_ACCESSIBILITY_SERVICE' in manifest) == (edition == 'full'), edition
    with zipfile.ZipFile(apk) as archive:
        dex = b''.join(archive.read(name) for name in archive.namelist() if re.fullmatch(r'classes\d*\.dex', name))
        for name in ['LockScreenService', 'LumaNotificationListener']:
            descriptor = f'Lcom/luma/lockscreen/{name};'.encode()
            assert (descriptor in dex) == (edition == 'full'), (edition, name)
    print(f'{edition}: signature verified; expected permissions, services and DEX classes verified')
assert certificates[0] == certificates[1], 'Edition signatures differ; in-place updates would fail'
print('Both editions keep the same signing identity; Studio removes sensitive capabilities.')
