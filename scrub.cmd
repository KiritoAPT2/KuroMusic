@echo off
if exist app\build.gradle.kts (
    sed -i "s/storePassword = \"110474\"/storePassword = \"\"/g" app/build.gradle.kts
    sed -i "s/keyPassword = \"110474\"/keyPassword = \"\"/g" app/build.gradle.kts
    sed -i "s|storeFile = file(\"/home/kiritoapt2/KuroMusic\")|storeFile = file(\"\")|g" app/build.gradle.kts
)
exit /b 0
