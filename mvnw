#!/bin/sh
# Maven Wrapper startup script

WRAPPER_DIR="$(dirname "$0")/.mvn/wrapper"
PROPERTIES_FILE="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Cannot find $PROPERTIES_FILE" >&2
  exit 1
fi

distributionUrl=$(grep -E '^distributionUrl=' "$PROPERTIES_FILE" | cut -d'=' -f2-)
wrapperUrl=$(grep -E '^wrapperUrl=' "$PROPERTIES_FILE" | cut -d'=' -f2-)

MAVEN_DIR="$WRAPPER_DIR/.mvn-cache"
mkdir -p "$MAVEN_DIR"

download() {
  url="$1"; dest="$2"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$url" -o "$dest"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$url" -O "$dest"
  else
    echo "Neither curl nor wget found" >&2
    exit 1
  fi
}

JAR="$WRAPPER_DIR/maven-wrapper.jar"
if [ ! -f "$JAR" ]; then
  download "$wrapperUrl" "$JAR"
fi

DIST_ZIP="$MAVEN_DIR/maven-dist.zip"
if [ ! -f "$DIST_ZIP" ]; then
  download "$distributionUrl" "$DIST_ZIP"
fi

M2_HOME="$MAVEN_DIR/apache-maven"
if [ ! -d "$M2_HOME" ]; then
  unzip -q "$DIST_ZIP" -d "$MAVEN_DIR"
  mv "$MAVEN_DIR"/apache-maven-* "$M2_HOME"
fi

JAVA_CMD="java"
exec "$JAVA_CMD" -Dmaven.multiModuleProjectDirectory="$PWD" -cp "$JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"


