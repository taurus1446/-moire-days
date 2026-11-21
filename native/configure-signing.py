
from pathlib import Path

gradle_file = Path("android/app/build.gradle")
if not gradle_file.exists():
    raise SystemExit("android/app/build.gradle not found")

content = gradle_file.read_text()

if "moireRelease" in content:
    print("MOIRE signing already configured")
    raise SystemExit(0)

signing = """
android {
    signingConfigs {
        moireRelease {
            storeFile file("moire-release.jks")
            storePassword System.getenv("STORE_PASS")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASS")
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.moireRelease
        }
    }
}
"""

gradle_file.write_text(content + "\n" + signing)
print("MOIRE release signing configured")
