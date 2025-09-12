Key Features:

1. Real Network Calls - Uses httpbin.org for actual HTTP testing
2. Proper Package Structure - Located in integration sub-package
3. @Tag("integration") - Allows selective test execution
4. Comprehensive Coverage:
   - Basic GET requests with headers validation
   - POST requests with JSON body and custom configuration
   - Asynchronous requests with timing validation
   - Error handling (404, 500, connection errors, timeouts)
   - HTTPS/SSL verification
   - Large response handling
   - Async error scenarios

Benefits over Mock Tests:

- Real HTTP Stack Testing - Validates actual network communication
- SSL/TLS Validation - Tests HTTPS connections
- Performance Measurement - Verifies async operations are truly parallel
- Real Error Conditions - Tests genuine network failures and timeouts
- End-to-End Validation - Proves the HTTP client works with real servers

Usage:

# Run only integration tests
mvn test -Dgroups=integration

# Exclude integration tests from regular builds
mvn test -DexcludedGroups=integration

This gives you both testing approaches: fast, reliable unit tests with mocks AND comprehensive integration tests with real network calls.