#!/usr/bin/env bash
#
# Unified Test Runner — Secure Exam Scheduling & Notification Management System
#
# Executes both unit tests and API functional tests, then produces a combined summary.
#
# Usage:
#   ./run_tests.sh              # Run all tests (unit + API)
#   ./run_tests.sh unit         # Run only unit tests
#   ./run_tests.sh api          # Run only API tests
#
# Prerequisites:
#   - Java 17+ and Maven for unit tests
#   - curl and a running backend server for API tests
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODE="${1:-all}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

UNIT_EXIT=0
API_EXIT=0
UNIT_SKIPPED=false
API_SKIPPED=false

echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║   Secure Exam Scheduling System — Test Runner              ║${NC}"
echo -e "${BOLD}║   Date: $(date '+%Y-%m-%d %H:%M:%S')                                ║${NC}"
echo -e "${BOLD}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# ======================================================================
# 1. UNIT TESTS
# ======================================================================
if [ "$MODE" = "all" ] || [ "$MODE" = "unit" ]; then
    echo -e "${CYAN}╭──────────────────────────────────────────────────────────────╮${NC}"
    echo -e "${CYAN}│  PHASE 1: Unit Tests                                        │${NC}"
    echo -e "${CYAN}╰──────────────────────────────────────────────────────────────╯${NC}"
    echo ""

    echo "[INFO] Running unit tests via Maven (test profile with H2 in-memory database)..."
    echo ""

    cd "$SCRIPT_DIR"

    # Run tests via Maven with H2 in-memory database (test profile)
    mvn test -Dspring.profiles.active=test 2>&1
    UNIT_EXIT=$?

    echo ""
    if [ $UNIT_EXIT -eq 0 ]; then
        echo -e "${GREEN}[UNIT TESTS] ✓ All unit tests PASSED${NC}"
    else
        echo -e "${RED}[UNIT TESTS] ✗ Some unit tests FAILED (exit code: $UNIT_EXIT)${NC}"
    fi
    echo ""
else
    UNIT_SKIPPED=true
fi

# ======================================================================
# 2. API FUNCTIONAL TESTS
# ======================================================================
if [ "$MODE" = "all" ] || [ "$MODE" = "api" ]; then
    echo -e "${CYAN}╭──────────────────────────────────────────────────────────────╮${NC}"
    echo -e "${CYAN}│  PHASE 2: API Functional Tests                              │${NC}"
    echo -e "${CYAN}╰──────────────────────────────────────────────────────────────╯${NC}"
    echo ""

    API_SCRIPT="$SCRIPT_DIR/API_tests/run_api_tests.sh"
    if [ -f "$API_SCRIPT" ]; then
        chmod +x "$API_SCRIPT"
        bash "$API_SCRIPT"
        API_EXIT=$?
    else
        echo -e "${RED}[ERROR] API test script not found: $API_SCRIPT${NC}"
        API_EXIT=1
    fi

    echo ""
    if [ $API_EXIT -eq 0 ]; then
        echo -e "${GREEN}[API TESTS] ✓ All API tests PASSED${NC}"
    else
        echo -e "${RED}[API TESTS] ✗ Some API tests FAILED (exit code: $API_EXIT)${NC}"
    fi
    echo ""
else
    API_SKIPPED=true
fi

# ======================================================================
# 3. COMBINED SUMMARY
# ======================================================================
echo ""
echo -e "${BOLD}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║                    COMBINED TEST SUMMARY                    ║${NC}"
echo -e "${BOLD}╠══════════════════════════════════════════════════════════════╣${NC}"

if [ "$UNIT_SKIPPED" = true ]; then
    echo -e "${BOLD}║  Unit Tests:    ${YELLOW}SKIPPED${NC}${BOLD}                                      ║${NC}"
elif [ $UNIT_EXIT -eq 0 ]; then
    echo -e "${BOLD}║  Unit Tests:    ${GREEN}PASSED${NC}${BOLD}                                       ║${NC}"
else
    echo -e "${BOLD}║  Unit Tests:    ${RED}FAILED${NC}${BOLD}                                       ║${NC}"
fi

if [ "$API_SKIPPED" = true ]; then
    echo -e "${BOLD}║  API Tests:     ${YELLOW}SKIPPED${NC}${BOLD}                                      ║${NC}"
elif [ $API_EXIT -eq 0 ]; then
    echo -e "${BOLD}║  API Tests:     ${GREEN}PASSED${NC}${BOLD}                                       ║${NC}"
else
    echo -e "${BOLD}║  API Tests:     ${RED}FAILED${NC}${BOLD}                                       ║${NC}"
fi

echo -e "${BOLD}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Overall exit code
OVERALL_EXIT=0
if [ $UNIT_EXIT -ne 0 ] || [ $API_EXIT -ne 0 ]; then
    OVERALL_EXIT=1
fi

if [ $OVERALL_EXIT -eq 0 ]; then
    echo -e "${GREEN}${BOLD}All tests passed successfully.${NC}"
else
    echo -e "${RED}${BOLD}Some tests failed. Review output above for details.${NC}"
fi

echo ""
echo "Test run completed at: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

exit $OVERALL_EXIT
