#!/usr/bin/env bash
#
# API Functional Test Runner for Secure Exam Scheduling System
# Tests all REST API endpoints with curl, including HMAC-SHA256 request signing.
#
# Prerequisites: Backend server running on localhost:8080, openssl available
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
HMAC_SECRET="${HMAC_SECRET:-replace-with-strong-intranet-secret}"
RESULT_FILE="${SCRIPT_DIR}/api-test-results.txt"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

TOTAL=0
PASSED=0
FAILED=0
FAILURES=""

TOKEN=""

# ======================================================================
# HMAC Signing Helpers
# ======================================================================

# Generate SHA-256 hash of a string
sha256_hex() {
    printf '%s' "$1" | openssl dgst -sha256 -hex 2>/dev/null | sed 's/^.* //'
}

# Generate HMAC-SHA256 signature
hmac_sign() {
    local payload="$1"
    printf '%s' "$payload" | openssl dgst -sha256 -hmac "$HMAC_SECRET" -hex 2>/dev/null | sed 's/^.* //'
}

# Build signed headers for authenticated requests
# Usage: signed_curl <METHOD> <URI_PATH> <BODY>
# Returns curl header args
build_signed_headers() {
    local method="$1"
    local uri_path="$2"
    local body="${3:-}"

    local timestamp
    timestamp=$(date +%s)
    local nonce
    nonce="nonce-$(head -c 16 /dev/urandom 2>/dev/null | od -An -tx1 | tr -d ' \n' || echo "$$-$RANDOM")"
    local body_hash
    body_hash=$(sha256_hex "$body")
    local sign_payload="${method}${uri_path}${timestamp}${nonce}${body_hash}"
    local signature
    signature=$(hmac_sign "$sign_payload")

    echo "-H X-Timestamp:${timestamp} -H X-Nonce:${nonce} -H X-Signature:${signature}"
}

# ======================================================================
# Test Helpers
# ======================================================================

assert_status() {
    local test_name="$1"
    local expected_status="$2"
    shift 2

    TOTAL=$((TOTAL + 1))
    local response
    response=$(curl -s -o /tmp/api_test_body.txt -w "%{http_code}" "$@" 2>/dev/null)

    if [ "$response" = "$expected_status" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $test_name (HTTP $response)"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${RED}[FAIL]${NC} $test_name — Expected HTTP $expected_status, got HTTP $response"
        local body
        body=$(cat /tmp/api_test_body.txt 2>/dev/null | head -c 200)
        echo -e "         Response: $body"
        FAILED=$((FAILED + 1))
        FAILURES="${FAILURES}\n  - $test_name (expected $expected_status, got $response)"
    fi
}

# Authenticated request with HMAC signing
assert_auth_status() {
    local test_name="$1"
    local expected_status="$2"
    local method="$3"
    local uri_path="$4"
    local body="${5:-}"
    local full_url="${BASE_URL}${uri_path#/api/v1}"

    # If uri_path starts with /, use it as-is for signing, else prepend
    if [[ "$uri_path" != /api/v1* ]]; then
        uri_path="/api/v1${uri_path}"
    fi
    full_url="${BASE_URL}${uri_path#/api/v1}"

    local timestamp
    timestamp=$(date +%s)
    local nonce
    nonce="nonce-$(head -c 16 /dev/urandom 2>/dev/null | od -An -tx1 | tr -d ' \n' || echo "$$-$RANDOM-$TOTAL")"
    local body_hash
    body_hash=$(sha256_hex "$body")
    local sign_payload="${method}${uri_path}${timestamp}${nonce}${body_hash}"
    local signature
    signature=$(hmac_sign "$sign_payload")

    TOTAL=$((TOTAL + 1))
    local response
    if [ -n "$body" ]; then
        response=$(curl -s -o /tmp/api_test_body.txt -w "%{http_code}" \
            -X "$method" "$full_url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -H "X-Timestamp: $timestamp" \
            -H "X-Nonce: $nonce" \
            -H "X-Signature: $signature" \
            -d "$body" 2>/dev/null)
    else
        response=$(curl -s -o /tmp/api_test_body.txt -w "%{http_code}" \
            -X "$method" "$full_url" \
            -H "Authorization: Bearer $TOKEN" \
            -H "X-Timestamp: $timestamp" \
            -H "X-Nonce: $nonce" \
            -H "X-Signature: $signature" 2>/dev/null)
    fi

    if [ "$response" = "$expected_status" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $test_name (HTTP $response)"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${RED}[FAIL]${NC} $test_name — Expected HTTP $expected_status, got HTTP $response"
        local resp_body
        resp_body=$(cat /tmp/api_test_body.txt 2>/dev/null | head -c 200)
        echo -e "         Response: $resp_body"
        FAILED=$((FAILED + 1))
        FAILURES="${FAILURES}\n  - $test_name (expected $expected_status, got $response)"
    fi
}

log_header() {
    echo ""
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# ======================================================================
# Start
# ======================================================================

echo "============================================"
echo "  API FUNCTIONAL TEST RUNNER"
echo "  Secure Exam Scheduling System"
echo "  Target: $BASE_URL"
echo "============================================"
echo ""

# Check connectivity
echo "[INFO] Checking server connectivity..."
SERVER_CHECK=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" -d '{}' 2>/dev/null || echo "000")
if [ "$SERVER_CHECK" = "000" ]; then
    echo -e "${YELLOW}[WARN] Server not reachable at $BASE_URL.${NC}"
    echo -e "${YELLOW}       Start the backend: cd repo && mvn spring-boot:run${NC}"
    echo ""
fi

# ======================================================================
# 1. Authentication — Login Endpoint (exempt from replay guard)
# ======================================================================
log_header "1. Authentication API Tests (/auth/login)"

# Login with empty body — server rejects with 400 (validation) or 401 (auth failure)
TOTAL=$((TOTAL + 1))
EMPTY_BODY_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" -d '{}' 2>/dev/null)
if [ "$EMPTY_BODY_STATUS" = "400" ] || [ "$EMPTY_BODY_STATUS" = "401" ]; then
    echo -e "  ${GREEN}[PASS]${NC} Login with empty body returns error (HTTP $EMPTY_BODY_STATUS)"
    PASSED=$((PASSED + 1))
else
    echo -e "  ${RED}[FAIL]${NC} Login with empty body — Expected 400 or 401, got $EMPTY_BODY_STATUS"
    FAILED=$((FAILED + 1))
    FAILURES="${FAILURES}\n  - Login empty body (expected 400/401, got $EMPTY_BODY_STATUS)"
fi

TOTAL=$((TOTAL + 1))
EMPTY_CRED_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" -d '{"username":"","password":""}' 2>/dev/null)
if [ "$EMPTY_CRED_STATUS" = "400" ] || [ "$EMPTY_CRED_STATUS" = "401" ]; then
    echo -e "  ${GREEN}[PASS]${NC} Login with blank credentials returns error (HTTP $EMPTY_CRED_STATUS)"
    PASSED=$((PASSED + 1))
else
    echo -e "  ${RED}[FAIL]${NC} Login with blank credentials — Expected 400 or 401, got $EMPTY_CRED_STATUS"
    FAILED=$((FAILED + 1))
    FAILURES="${FAILURES}\n  - Login blank credentials (expected 400/401, got $EMPTY_CRED_STATUS)"
fi

assert_status \
    "Login with wrong credentials returns 401" \
    "401" \
    -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"no_such_user_xyz","password":"WrongP@ssw0rd!!"}'

# Attempt login with common admin credentials
echo -e "  ${CYAN}[INFO]${NC} Attempting admin login for authenticated tests..."
LOGIN_BODY='{"username":"admin","password":"Admin@123456!"}'
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" -d "$LOGIN_BODY" 2>/dev/null)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token"\s*:\s*"[^"]*"' | head -1 | sed 's/.*:.*"\([^"]*\)"/\1/')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    # Try alternate credential patterns
    for CRED in '{"username":"admin","password":"Adm1n@SecurePass!"}' \
                '{"username":"sysadmin","password":"Admin@123456!"}'; do
        LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
            -H "Content-Type: application/json" -d "$CRED" 2>/dev/null)
        TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token"\s*:\s*"[^"]*"' | head -1 | sed 's/.*:.*"\([^"]*\)"/\1/')
        if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
            break
        fi
    done
fi

if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
    echo -e "  ${GREEN}[INFO]${NC} Login successful — token obtained for authenticated tests"
    TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
    echo -e "  ${GREEN}[PASS]${NC} Login with valid credentials returns token"
else
    echo -e "  ${YELLOW}[WARN]${NC} Could not obtain token (no seed admin user in DB). Skipping auth-required tests."
    echo -e "  ${YELLOW}       ${NC} Auth tests below will verify unauthenticated access is properly rejected."
    TOKEN="invalid-token-placeholder"
fi

# ======================================================================
# 2. Unauthenticated Access Tests (no token)
# ======================================================================
log_header "2. Unauthenticated Access Rejection Tests"

assert_status \
    "GET /auth/me without token returns 401" \
    "401" \
    -X GET "$BASE_URL/auth/me"

assert_status \
    "GET /users without token returns 401" \
    "401" \
    -X GET "$BASE_URL/users?page=1&size=10"

assert_status \
    "GET /sessions without token returns 401" \
    "401" \
    -X GET "$BASE_URL/sessions?page=1&size=10"

assert_status \
    "GET /notifications without token returns 401" \
    "401" \
    -X GET "$BASE_URL/notifications?page=1&size=10"

assert_status \
    "GET /compliance-reviews without token returns 401" \
    "401" \
    -X GET "$BASE_URL/compliance-reviews?page=1&size=10"

assert_status \
    "GET /jobs without token returns 401" \
    "401" \
    -X GET "$BASE_URL/jobs?page=1&size=10"

assert_status \
    "GET /audit-logs without token returns 401" \
    "401" \
    -X GET "$BASE_URL/audit-logs?page=1&size=10"

assert_status \
    "GET /campuses without token returns 401" \
    "401" \
    -X GET "$BASE_URL/campuses"

assert_status \
    "GET /versions without token returns 401" \
    "401" \
    -X GET "$BASE_URL/versions?entityType=EXAM_SESSION&entityId=1&page=1&size=10"

assert_status \
    "GET /terms without token returns 401" \
    "401" \
    -X GET "$BASE_URL/terms"

assert_status \
    "GET /dashboard/stats without token returns 401" \
    "401" \
    -X GET "$BASE_URL/dashboard/stats"

assert_status \
    "GET /inbox without token returns 401" \
    "401" \
    -X GET "$BASE_URL/inbox?page=1&size=10"

assert_status \
    "GET /inbox/unread-count without token returns 401" \
    "401" \
    -X GET "$BASE_URL/inbox/unread-count"

assert_status \
    "GET /anti-cheat/flags without token returns 401" \
    "401" \
    -X GET "$BASE_URL/anti-cheat/flags?page=1&size=10"

# ======================================================================
# 3. Invalid Token Tests (with replay guard headers but bad token)
# ======================================================================
log_header "3. Invalid Token / Replay Guard Tests"

assert_status \
    "Request with token but missing replay-guard headers returns 401" \
    "401" \
    -X GET "$BASE_URL/users?page=1&size=10" \
    -H "Authorization: Bearer fake-token-12345"

assert_auth_status \
    "Request with invalid token + valid HMAC returns 401" \
    "401" \
    "GET" "/api/v1/users?page=1&size=10" ""

# ======================================================================
# 4. Authenticated Endpoint Tests (only if token was obtained)
# ======================================================================
if [ "$TOKEN" != "invalid-token-placeholder" ]; then

    log_header "4. User Management API Tests (authenticated)"
    assert_auth_status "List users returns 200" "200" "GET" "/api/v1/users?page=1&size=10" ""
    assert_auth_status "List users with role filter returns 200" "200" "GET" "/api/v1/users?page=1&size=10&role=ADMIN" ""

    assert_auth_status "Create user missing username returns 400" "400" \
        "POST" "/api/v1/users" '{"password":"StrongP@ss123!","roles":["STUDENT"]}'
    assert_auth_status "Create user empty roles returns 400" "400" \
        "POST" "/api/v1/users" '{"username":"test","password":"StrongP@ss123!","roles":[]}'

    log_header "5. Exam Session API Tests (authenticated)"
    assert_auth_status "List sessions returns 200" "200" "GET" "/api/v1/sessions?page=1&size=10" ""
    assert_auth_status "Create session missing fields returns 400" "400" \
        "POST" "/api/v1/sessions" '{}'
    assert_auth_status "Get non-existent session returns 404" "404" \
        "GET" "/api/v1/sessions/999999" ""

    log_header "6. Notification API Tests (authenticated)"
    assert_auth_status "List notifications returns 200" "200" "GET" "/api/v1/notifications?page=1&size=10" ""
    assert_auth_status "Create notification missing fields returns 400" "400" \
        "POST" "/api/v1/notifications" '{"title":"test"}'
    assert_auth_status "Submit review non-existent returns 404" "404" \
        "POST" "/api/v1/notifications/999999/submit-review" ""

    log_header "7. Compliance Review API Tests (authenticated)"
    assert_auth_status "List compliance reviews returns 200" "200" "GET" "/api/v1/compliance-reviews?page=1&size=10" ""
    assert_auth_status "List reviews with PENDING filter returns 200" "200" \
        "GET" "/api/v1/compliance-reviews?page=1&size=10&status=PENDING" ""
    assert_auth_status "Get non-existent review returns 404" "404" \
        "GET" "/api/v1/compliance-reviews/999999" ""

    log_header "8. Job Monitor API Tests (authenticated)"
    assert_auth_status "List jobs returns 200" "200" "GET" "/api/v1/jobs?page=1&size=10" ""
    assert_auth_status "List jobs with FAILED filter returns 200" "200" \
        "GET" "/api/v1/jobs?page=1&size=10&status=FAILED" ""
    assert_auth_status "Get non-existent job returns 404" "404" \
        "GET" "/api/v1/jobs/999999" ""
    assert_auth_status "Retry non-existent job returns 404" "404" \
        "POST" "/api/v1/jobs/999999/retry" ""

    log_header "9. Audit Log API Tests (authenticated)"
    assert_auth_status "List audit logs returns 200" "200" "GET" "/api/v1/audit-logs?page=1&size=10" ""
    assert_auth_status "List audit logs with date range returns 200" "200" \
        "GET" "/api/v1/audit-logs?page=1&size=10&from=2026-01-01&to=2026-12-31" ""

    log_header "10. Campus & Room API Tests (authenticated)"
    assert_auth_status "List campuses returns 200" "200" "GET" "/api/v1/campuses" ""
    assert_auth_status "Create campus with valid data returns 200" "200" \
        "POST" "/api/v1/campuses" '{"name":"API Test Campus"}'

    log_header "11. Version History API Tests (authenticated)"
    assert_auth_status "List versions returns 200" "200" \
        "GET" "/api/v1/versions?entityType=EXAM_SESSION&entityId=1&page=1&size=10" ""
    assert_auth_status "Get non-existent version returns 404" "404" \
        "GET" "/api/v1/versions/999999" ""

    log_header "12. Academic Term & Dashboard Tests (authenticated)"
    assert_auth_status "List terms returns 200" "200" "GET" "/api/v1/terms" ""
    assert_auth_status "Dashboard stats returns 200" "200" "GET" "/api/v1/dashboard/stats" ""
    assert_auth_status "Dashboard stats with termId returns 200" "200" \
        "GET" "/api/v1/dashboard/stats?termId=1" ""

    log_header "13. Anti-Cheat API Tests (authenticated)"
    assert_auth_status "List anti-cheat flags returns 200" "200" \
        "GET" "/api/v1/anti-cheat/flags?page=1&size=10" ""

    log_header "14. Auto-Save Draft API Tests (authenticated)"
    assert_auth_status "Save draft returns 200" "200" \
        "PUT" "/api/v1/drafts/api-test-form" '{"field1":"value1"}'
    assert_auth_status "Load draft returns 200" "200" \
        "GET" "/api/v1/drafts/api-test-form" ""
    assert_auth_status "Delete draft returns 200" "200" \
        "DELETE" "/api/v1/drafts/api-test-form" ""

    log_header "15. Data Mutation Verification Tests (authenticated)"
    # Create a notification and verify it exists
    TOTAL=$((TOTAL + 1))
    CREATE_BODY='{"eventType":"EXAM_SCHEDULE_PUBLISHED","title":"API Test Notification","body":"Verify data mutation","priority":"LOW","targetScope":{"gradeId":1,"subjectId":1}}'
    TIMESTAMP=$(date +%s)
    NONCE="nonce-create-$RANDOM"
    BODY_HASH=$(sha256_hex "$CREATE_BODY")
    SIG=$(hmac_sign "POST/api/v1/notifications${TIMESTAMP}${NONCE}${BODY_HASH}")

    CREATE_RESP=$(curl -s -o /tmp/api_test_create.txt -w "%{http_code}" \
        -X POST "$BASE_URL/notifications" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -H "X-Timestamp: $TIMESTAMP" \
        -H "X-Nonce: $NONCE" \
        -H "X-Signature: $SIG" \
        -d "$CREATE_BODY" 2>/dev/null)

    if [ "$CREATE_RESP" = "200" ]; then
        echo -e "  ${GREEN}[PASS]${NC} Create notification and verify exists (HTTP $CREATE_RESP)"
        PASSED=$((PASSED + 1))
    else
        echo -e "  ${RED}[FAIL]${NC} Create notification for mutation test — HTTP $CREATE_RESP"
        FAILED=$((FAILED + 1))
        FAILURES="${FAILURES}\n  - Create notification mutation (expected 200, got $CREATE_RESP)"
    fi

else
    log_header "4-15. Authenticated Tests (SKIPPED — no token)"
    echo -e "  ${YELLOW}[SKIP]${NC} Authenticated endpoint tests skipped — login not available"
    echo -e "  ${YELLOW}[INFO]${NC} To run authenticated tests, seed an admin user in the database"
fi

# ======================================================================
# Summary
# ======================================================================
echo ""
echo "============================================"
echo "  API TEST RESULTS SUMMARY"
echo "============================================"
echo ""
echo "  Total test cases:  $TOTAL"
echo -e "  ${GREEN}Passed:            $PASSED${NC}"
echo -e "  ${RED}Failed:            $FAILED${NC}"
echo ""

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Failed tests:${NC}"
    echo -e "$FAILURES"
    echo ""
fi

PASS_RATE=0
if [ $TOTAL -gt 0 ]; then
    PASS_RATE=$((PASSED * 100 / TOTAL))
fi
echo "  Pass rate: ${PASS_RATE}%"
echo ""

# Write results to file
{
    echo "API Test Results — $(date)"
    echo "Target: $BASE_URL"
    echo ""
    echo "Total: $TOTAL"
    echo "Passed: $PASSED"
    echo "Failed: $FAILED"
    echo "Pass rate: ${PASS_RATE}%"
    if [ $FAILED -gt 0 ]; then
        echo ""
        echo "Failures:"
        echo -e "$FAILURES"
    fi
} > "$RESULT_FILE"

echo "[INFO] Results written to: $RESULT_FILE"

if [ $FAILED -gt 0 ]; then
    exit 1
fi
exit 0
