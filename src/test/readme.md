Key Features:

1. Real Network Calls - Uses httpbin.org for actual HTTP testing
2. Proper Package Structure - Located in integration sub-package
3. @Tag("integration") - Allows selective test execution
4. Comprehensive Coverage:
   - Basic GET requests with headers validation
   - POST requests with JSON body and custom configuration
   - Error handling (404 status, timeouts)

Benefits over Mock Tests:

- Real HTTP Stack Testing - Validates actual network communication
- SSL/TLS Validation - Tests HTTPS connections
- Real Error Conditions - Tests genuine network failures and timeouts
- End-to-End Validation - Proves the HTTP client works with real servers

Usage:

# Regular builds (mvn test) exclude them. See excludedGroups in pom.xml.
# Run only integration tests
mvn test -Dgroups=integration -DexcludedGroups=

This gives you both testing approaches: fast, reliable unit tests with mocks AND comprehensive integration tests with real network calls.