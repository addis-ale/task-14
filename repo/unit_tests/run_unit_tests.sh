#!/usr/bin/env bash
#
# Unit Test Runner for Secure Exam Scheduling System
# Compiles and runs all JUnit 5 unit tests under unit_tests/
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_SRC="$SCRIPT_DIR"
BUILD_DIR="$PROJECT_ROOT/target/unit-test-classes"
RESULT_FILE="$PROJECT_ROOT/target/unit-test-results.txt"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "============================================"
echo "  UNIT TEST RUNNER"
echo "  Secure Exam Scheduling System"
echo "============================================"
echo ""

# Ensure Maven dependencies are available
echo "[INFO] Resolving Maven dependencies..."
cd "$PROJECT_ROOT"
if command -v mvn &>/dev/null; then
    mvn dependency:resolve -q 2>/dev/null || true
    mvn compile -q 2>/dev/null || true
fi

# Locate JUnit jars from Maven repository
M2_REPO="${HOME}/.m2/repository"
JUNIT_PLATFORM_LAUNCHER=$(find "$M2_REPO" -name "junit-platform-console-standalone-*.jar" 2>/dev/null | head -1)

# If standalone launcher not available, try to run via Maven
if [ -z "$JUNIT_PLATFORM_LAUNCHER" ]; then
    echo "[INFO] JUnit standalone launcher not found, running tests via Maven surefire..."
    echo ""

    # Copy test sources to the Maven test directory
    MAVEN_TEST_DIR="$PROJECT_ROOT/src/test/java"
    mkdir -p "$MAVEN_TEST_DIR"

    # Copy unit test files
    find "$TEST_SRC" -name "*Test.java" -exec sh -c '
        for f do
            rel="${f#'"$TEST_SRC"'/}"
            target_dir="'"$MAVEN_TEST_DIR"'/$(dirname "$rel")"
            mkdir -p "$target_dir"
            cp "$f" "$target_dir/"
        done
    ' sh {} +

    # Run with Maven
    echo "[INFO] Running unit tests via Maven..."
    echo ""
    cd "$PROJECT_ROOT"
    mvn test -Dspring.profiles.active=test \
        2>&1 | tee "$RESULT_FILE"

    TEST_EXIT=$?

    # Clean up copied test files
    find "$MAVEN_TEST_DIR" -name "*Test.java" -delete 2>/dev/null || true
    find "$MAVEN_TEST_DIR" -type d -empty -delete 2>/dev/null || true

    echo ""
    if [ $TEST_EXIT -eq 0 ]; then
        echo -e "${GREEN}[PASS] All unit tests passed.${NC}"
    else
        echo -e "${RED}[FAIL] Some unit tests failed. Check output above.${NC}"
    fi
    exit $TEST_EXIT
fi

# If standalone launcher is available, use it directly
echo "[INFO] Using JUnit Platform Console Standalone..."

# Collect classpath
MAIN_CLASSES="$PROJECT_ROOT/target/classes"
CLASSPATH="$MAIN_CLASSES"

# Add all Maven dependency jars
for jar in "$M2_REPO"/org/springframework/**/*.jar "$M2_REPO"/jakarta/**/*.jar; do
    [ -f "$jar" ] && CLASSPATH="$CLASSPATH:$jar"
done

mkdir -p "$BUILD_DIR"

# Compile test sources
echo "[INFO] Compiling unit tests..."
find "$TEST_SRC" -name "*.java" > /tmp/unit_test_sources.txt
javac -d "$BUILD_DIR" -cp "$CLASSPATH:$JUNIT_PLATFORM_LAUNCHER" @/tmp/unit_test_sources.txt 2>&1

echo "[INFO] Running unit tests..."
echo ""

java -jar "$JUNIT_PLATFORM_LAUNCHER" \
    --classpath "$BUILD_DIR:$CLASSPATH" \
    --scan-classpath "$BUILD_DIR" \
    2>&1 | tee "$RESULT_FILE"

TEST_EXIT=${PIPESTATUS[0]}
echo ""
if [ $TEST_EXIT -eq 0 ]; then
    echo -e "${GREEN}[PASS] All unit tests passed.${NC}"
else
    echo -e "${RED}[FAIL] Some unit tests failed. Check output above.${NC}"
fi
exit $TEST_EXIT
