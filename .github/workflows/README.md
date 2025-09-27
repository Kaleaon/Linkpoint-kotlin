# GitHub Actions Workflows

This directory contains automated workflows for the Linkpoint-kotlin batch processing system.

## Workflows

### 1. Batch Processing System (`batch-processing.yml`)

**Purpose**: Automated execution of the complete batch download and conversion system.

**Triggers**:
- **Manual**: Can be triggered manually via GitHub Actions UI with customizable options
- **Scheduled**: Runs weekly on Sundays at 2 AM UTC for regular updates
- **Push**: Triggers on pushes to main branch that affect batch processing files

**Features**:
- Downloads all target repositories (SecondLife, Firestorm, Libremetaverse, RLV)
- Converts C++/C# code to Kotlin with LLSD standards
- Generates processing reports and metrics
- Creates GitHub issues with results
- Uploads artifacts for review
- Handles failures with automatic issue creation

**Runtime**: Up to 6 hours (for large repository processing)

**Outputs**:
- Converted Kotlin components with LLSD labeling
- Processing summary reports
- GitHub issue with results
- Downloadable artifacts

### 2. Quick Batch Processing (`batch-processing-quick.yml`)

**Purpose**: Lightweight testing and development workflow.

**Triggers**:
- **Manual**: With options for demo mode or single repository testing  
- **Pull Request**: On changes to batch processing system

**Features**:
- Demo mode for system validation
- Single repository testing
- Component validation checks
- Quick feedback for development

**Runtime**: 30 minutes maximum

**Outputs**:
- Test results and validation status
- System component verification

## Usage

### Running Full Batch Processing

1. **Via GitHub Actions UI**:
   - Go to Actions → Batch Processing System
   - Click "Run workflow"
   - Choose options:
     - `repositories`: Comma-separated list or "all" (default)
     - `skip_confirmation`: Skip interactive prompts (default: true)

2. **Automatic Scheduled Runs**:
   - Runs every Sunday at 2 AM UTC
   - Processes all repositories
   - Creates issue with results

### Running Quick Tests

1. **Via GitHub Actions UI**:
   - Go to Actions → Quick Batch Processing  
   - Click "Run workflow"
   - Choose options:
     - `demo_mode`: Run demonstration only (default: true)
     - `single_repo`: Test specific repository

2. **Automatic PR Runs**:
   - Runs on pull requests affecting batch processing
   - Validates system components

## Workflow Configuration

### Environment Variables

The workflows support these environment variables:

- `JAVA_OPTS`: JVM options (default: "-Xmx4g -XX:+UseG1GC")
- `REPOSITORIES`: Repositories to process (default: "all")
- `SKIP_CONFIRMATION`: Skip interactive prompts (default: true in automation)
- `AUTOMATED_RUN`: Flag for automated execution mode

### Secrets Required

- `GITHUB_TOKEN`: Automatically provided by GitHub Actions
- No additional secrets required for basic functionality

### Permissions

The workflows require these permissions:
- `contents: write` - For repository access and artifact uploads
- `issues: write` - For creating result/failure issues
- `pull-requests: write` - For PR comments and status

## Artifacts

### Full Batch Processing Artifacts

1. **batch-processing-results-{run_id}**:
   - Converted Kotlin components
   - Processing reports and metrics
   - Summary documentation
   - Retention: 30 days

2. **downloaded-repositories-{run_id}**:
   - Source repositories (if successful)
   - Retention: 7 days (due to size)

### Quick Test Artifacts

1. **quick-batch-test-results**:
   - Test validation results
   - Demo outputs
   - Retention: 3 days

## Monitoring and Notifications

### Success Notifications

- **GitHub Issue**: Created with processing summary and results
- **Labels**: `batch-processing`, `automated`, `conversion-results`
- **Content**: Statistics, artifact links, next steps

### Failure Notifications  

- **GitHub Issue**: Created with failure details and investigation steps
- **Labels**: `batch-processing`, `automated`, `bug`, `high-priority`
- **Assignment**: Mentions @copilot for investigation
- **Content**: Error details, troubleshooting steps, resource links

## Troubleshooting

### Common Issues

1. **Timeout Errors**:
   - Large repositories may exceed time limits
   - Solution: Increase `timeout-minutes` or process repositories individually

2. **Memory Errors**:
   - Java heap space issues during processing
   - Solution: Adjust `JAVA_OPTS` memory settings

3. **Network Errors**:
   - Repository download failures
   - Solution: Check repository URLs and network connectivity

4. **Build Failures**:
   - Kotlin compilation issues
   - Solution: Review dependencies in `batch-processor/build.gradle.kts`

### Debugging Steps

1. **Check Workflow Logs**:
   - Go to Actions → Failed workflow
   - Review step-by-step execution logs
   - Look for specific error messages

2. **Review Artifacts**:
   - Download available artifacts
   - Check processing reports for details
   - Verify converted components

3. **Manual Testing**:
   - Run `./batch-download-convert.sh` locally
   - Test individual repositories
   - Validate system components

### Performance Optimization

1. **Caching**:
   - Gradle dependencies cached between runs
   - Downloaded repositories cached when possible
   - Reduces execution time for repeated runs

2. **Parallel Processing**:
   - Multiple repository downloads can run in parallel
   - Conversion tasks distributed across available resources

3. **Resource Management**:
   - Memory limits configured for optimal performance
   - Temporary files cleaned up automatically
   - Large artifacts archived appropriately

## Maintenance

### Regular Tasks

1. **Update Dependencies**:
   - Review and update action versions
   - Update Kotlin and Java versions
   - Verify compatibility with latest changes

2. **Monitor Performance**:
   - Check execution times and resource usage
   - Optimize workflows based on actual usage patterns
   - Adjust timeouts and resource limits as needed

3. **Review Results**:
   - Regularly check generated issues
   - Validate conversion quality
   - Update processing logic based on findings

### Configuration Updates

When updating workflow configuration:

1. Test changes with Quick Batch Processing first
2. Use manual triggers for validation
3. Monitor initial runs for issues
4. Update documentation as needed

## Integration

### With Existing Systems

The workflows integrate with:
- **Linkpoint-kotlin build system**: Uses existing Gradle configuration
- **Repository structure**: Follows established module organization  
- **Documentation system**: Updates and references existing docs
- **Issue tracking**: Creates and links relevant issues

### With External Tools

The workflows can be extended to integrate with:
- **Code quality tools**: SonarQube, CodeClimate integration
- **Notification systems**: Slack, email notifications
- **Project management**: Jira, Linear task creation
- **Monitoring systems**: Custom metric collection

## Best Practices

### Workflow Design

1. **Idempotent Operations**: Workflows can be re-run safely
2. **Graceful Failures**: Clear error messages and recovery steps
3. **Resource Efficiency**: Appropriate timeouts and resource limits
4. **Clear Outputs**: Comprehensive artifacts and summaries

### Security

1. **Minimal Permissions**: Only required permissions granted
2. **Secret Management**: No additional secrets required
3. **Safe Defaults**: Conservative settings for automated runs
4. **Input Validation**: Proper handling of user inputs

### Maintainability

1. **Clear Documentation**: Comprehensive inline comments
2. **Modular Design**: Reusable components and patterns
3. **Version Control**: Proper tracking of workflow changes
4. **Testing Strategy**: Quick validation before full processing

This workflow system provides a robust, automated solution for continuous batch processing of virtual world viewer codebases, ensuring the Linkpoint-kotlin project stays current with upstream changes while maintaining high quality standards.