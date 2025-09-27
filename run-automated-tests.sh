#!/bin/bash

# Automated Testing Suite Runner for Kotlin-to-C++/C# Validation
# 
# This script runs the comprehensive automated testing system that validates
# Kotlin implementations against original C++ reference behavior using:
# - Behavioral compatibility testing
# - Google Gemini AI-powered code analysis  
# - Protocol validation against C++ references
# - Recursive debugging until bugs are resolved

set -e

echo "🚀 Linkpoint-kotlin Automated Testing Suite"
echo "==========================================="
echo

# Check for Google Gemini API key
if [ -z "$GEMINI_API_KEY" ]; then
    echo "⚠️  WARNING: GEMINI_API_KEY environment variable not set"
    echo "   AI-powered features will run in fallback mode"
    echo "   For full AI capabilities, set: export GEMINI_API_KEY=your_api_key"
    echo
else
    echo "✅ Google Gemini API key detected - AI features enabled"
    echo
fi

# Create temp directory for test reports
mkdir -p /tmp/linkpoint-test-reports
echo "📁 Test reports will be saved to: /tmp/linkpoint-test-reports/"
echo

# Check if gradle wrapper exists
if [ ! -f "./gradlew" ]; then
    echo "❌ Gradle wrapper not found. Using kotlinc for compilation..."
    
    # Compile test files with kotlinc (fallback)
    echo "🔨 Compiling test files..."
    find src/test -name "*.kt" -type f > /tmp/test_files.txt
    
    if [ -s /tmp/test_files.txt ]; then
        echo "📝 Found $(wc -l < /tmp/test_files.txt) test files"
        
        # Basic compilation check
        kotlinc -cp ".:$(find . -name "*.jar" | tr '\n' ':')" -d /tmp/test-classes $(cat /tmp/test_files.txt) 2>&1 | head -20
        
        if [ $? -eq 0 ]; then
            echo "✅ Test compilation successful"
        else
            echo "⚠️  Some compilation warnings/errors found"
        fi
    else
        echo "❌ No test files found"
        exit 1
    fi
    
else
    echo "🔨 Running automated tests with Gradle..."
    echo
    
    # Run the comprehensive test suite
    ./gradlew test --tests "com.linkpoint.testing.AutomatedTestRunner" \
        --info \
        --stacktrace \
        2>&1 | tee /tmp/linkpoint-test-reports/test-execution.log
    
    TEST_RESULT=$?
    
    echo
    echo "📊 Test Execution Summary"
    echo "========================"
    
    if [ $TEST_RESULT -eq 0 ]; then
        echo "✅ All automated tests passed successfully!"
    else
        echo "⚠️  Some tests failed or encountered issues"
        echo "   Check the detailed logs for more information"
    fi
fi

echo
echo "📄 Generated Reports:"
echo "-------------------"

# List generated report files
find /tmp -name "*automated_test_report*" -type f 2>/dev/null | while read -r report; do
    echo "📋 $report"
    echo "   Size: $(du -h "$report" | cut -f1)"
    echo "   Modified: $(stat -c %y "$report" 2>/dev/null || stat -f %Sm "$report" 2>/dev/null || echo "Unknown")"
done

if [ -f "/tmp/linkpoint-test-reports/test-execution.log" ]; then
    echo "📜 /tmp/linkpoint-test-reports/test-execution.log"
    echo "   Size: $(du -h /tmp/linkpoint-test-reports/test-execution.log | cut -f1)"
fi

echo
echo "🎯 Quick Results Summary:"
echo "------------------------"

# Extract key metrics from logs
if [ -f "/tmp/linkpoint-test-reports/test-execution.log" ]; then
    echo "📈 Test Metrics:"
    grep -E "(passed|failed|SUCCESS|FAILURE)" /tmp/linkpoint-test-reports/test-execution.log | tail -5
    echo
    
    echo "🔍 Key Findings:"
    grep -E "(✅|❌|⚠️)" /tmp/linkpoint-test-reports/test-execution.log | tail -10
else
    echo "📊 Basic validation completed - check compilation output above"
fi

echo
echo "💡 Next Steps:"
echo "-------------"
echo "1. Review detailed test reports in /tmp/"
echo "2. For AI-powered analysis, set GEMINI_API_KEY and re-run"
echo "3. Address any compatibility issues found"
echo "4. Run individual component tests for focused debugging"
echo "5. Check protocol validation results for C++ compatibility"

echo
echo "🏁 Automated testing suite completed!"
echo "====================================="