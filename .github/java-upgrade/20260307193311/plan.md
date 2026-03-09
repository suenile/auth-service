# Upgrade Plan: auth-service (20260307193311)

- **Generated**: 2026-03-07 19:33:11
- **HEAD Branch**: main
- **HEAD Commit ID**: 874a77a6ce1fbcbef9b16ad8f49cc6d57d64b1f5

## Available Tools

**JDKs**
- JDK 21: **<TO_BE_INSTALLED>** (current project JDK, required for Step 2 baseline and all subsequent steps)

**Build Tools**
- Maven: **<TO_BE_INSTALLED>** (no wrapper present, no system installation found)

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: appmod/java-upgrade-20260307193311
- Run tests before and after the upgrade: true

## Upgrade Goals

- Upgrade Spring Boot from 3.2.3 to 3.4.3

### Technology Stack

| Technology/Dependency | Current | Min Compatible | Why Incompatible |
| --------------------- | ------- | -------------- | ---------------- |
| Java                  | 21      | 21             | -                |
| Spring Boot           | 3.2.3   | 3.4.3          | User requested upgrade to latest stable version |
| Spring Framework      | 6.1.4   | 6.2.x          | Spring Boot 3.4 requires Spring Framework 6.2+ |
| Spring Security       | 6.2.2   | 6.4.x          | Spring Boot 3.4 requires Spring Security 6.4+ |
| Spring Security OAuth2 Authorization Server | 1.2.2 | 1.4.x | Spring Boot 3.4 requires version 1.4+ |
| Hibernate (JPA)       | 6.4.1   | 6.6.x          | Spring Boot 3.4 requires Hibernate 6.6+ |
| PostgreSQL Driver     | 42.7.2  | 42.7.2         | -                |
| JJWT                  | 0.12.5  | 0.12.5         | -                |
| Bucket4j              | 8.7.0   | 8.7.0          | -                |

### Derived Upgrades

The following upgrades will happen automatically through Spring Boot's dependency management (BOM) when upgrading to Spring Boot 3.4.3:

- **Spring Framework**: 6.1.4 → 6.2.x (Spring Boot 3.4 requires Spring Framework 6.2+)
- **Spring Security**: 6.2.2 → 6.4.x (Spring Boot 3.4 requires Spring Security 6.4+)
- **Spring Security OAuth2 Authorization Server**: 1.2.2 → 1.4.x (Spring Boot 3.4 requires version 1.4+)
- **Hibernate**: 6.4.1 → 6.6.x (Spring Boot 3.4 requires Hibernate 6.6+)
- **PostgreSQL Driver**: May receive minor version update through BOM
- **Other Spring Boot managed dependencies**: Will be upgraded to versions compatible with Spring Boot 3.4.3

## Upgrade Steps

- **Step 1: Setup Environment**
  - **Rationale**: Install required JDK 21 and Maven build tool that are missing from the system.
  - **Changes to Make**:
    - [ ] Install JDK 21 (current project JDK, required for baseline and all subsequent steps)
    - [ ] Install Maven (no wrapper present, no system installation found)
  - **Verification**:
    - Command: `#list_jdks` and `#list_mavens` to confirm installations
    - Expected: JDK 21 and Maven available at specified paths

---

- **Step 2: Setup Baseline**
  - **Rationale**: Establish pre-upgrade compile and test results with current Spring Boot 3.2.3 to measure upgrade success against.
  - **Changes to Make**:
    - [ ] Run baseline compilation with JDK 21
    - [ ] Run baseline tests with JDK 21
    - [ ] Document test pass rate and any existing failures
  - **Verification**:
    - Command: `mvn clean compile test-compile && mvn clean test`
    - JDK: JDK 21 (from Step 1)
    - Expected: Document SUCCESS/FAILURE, test pass rate (forms acceptance criteria for Step 4)

---

- **Step 3: Upgrade Spring Boot to 3.4.3**
  - **Rationale**: Core framework upgrade from 3.2.3 to 3.4.3. Spring Framework, Spring Security, Hibernate, and other managed dependencies will auto-upgrade via Spring Boot BOM.
  - **Changes to Make**:
    - [ ] Update spring-boot-starter-parent version from 3.2.3 to 3.4.3 in pom.xml
    - [ ] Review release notes for Spring Boot 3.3 and 3.4 for any breaking changes
    - [ ] Fix any compilation errors resulting from API changes
    - [ ] Verify project compiles successfully
  - **Verification**:
    - Command: `mvn clean test-compile`
    - JDK: JDK 21 (from Step 1)
    - Expected: Compilation SUCCESS (tests may fail - will be fixed in Step 4)

---

- **Step 4: Final Validation**
  - **Rationale**: Verify upgrade goal met (Spring Boot 3.4.3), project compiles successfully, and all tests pass.
  - **Changes to Make**:
    - [ ] Verify spring-boot-starter-parent is 3.4.3 in pom.xml
    - [ ] Verify derived upgrades (Spring Framework 6.2.x, Spring Security 6.4.x, Hibernate 6.6.x) via dependency tree
    - [ ] Clean rebuild with JDK 21
    - [ ] Run full test suite and fix ALL test failures (iterative fix loop until 100% pass)
    - [ ] Resolve any deprecation warnings or migration notices
  - **Verification**:
    - Command: `mvn clean test`
    - JDK: JDK 21 (from Step 1)
    - Expected: Compilation SUCCESS + 100% tests pass (matching or exceeding baseline from Step 2)

## Key Challenges

- **Spring Security 6.2 to 6.4 API Changes**
  - **Challenge**: Spring Security 6.4 (bundled with Spring Boot 3.4) may introduce deprecations or minor API changes in security configuration, authorization, or authentication flows.
  - **Strategy**: Review Spring Security 6.3 and 6.4 release notes during Step 3. Test authentication and authorization flows thoroughly in Step 4. Update any deprecated SecurityFilterChain configurations if needed.

- **Hibernate 6.4 to 6.6 Behavior Changes**
  - **Challenge**: Hibernate 6.6 may have query syntax improvements, schema validation changes, or entity mapping updates that could affect JPA repositories.
  - **Strategy**: Review Hibernate 6.5 and 6.6 migration guides during Step 3. Run full test suite in Step 4 to catch any JPA query or entity behavior changes. Monitor for schema validation warnings.

- **Minimal Risk Upgrade**
  - **Note**: This is a minor version upgrade within the same Spring Boot 3.x major version (3.2 → 3.4). No major breaking changes expected. Project is already on Java 21 and Jakarta EE 9+, so no namespace migration or JDK upgrade required. The upgrade should be straightforward with minimal code changes.

## Plan Review

**Status**: Complete and ready for user confirmation

**Verification Results**:
- ✓ All placeholders replaced (TO_BE_INSTALLED markers are intentional)
- ✓ All required sections populated
- ✓ Step format compliance verified
- ✓ Mandatory step sequence verified (Setup Environment → Setup Baseline → Upgrade → Final Validation)
- ✓ Technology Stack table complete with all columns
- ✓ Derived Upgrades documented
- ✓ Key Challenges identified
- ✓ All HTML comments removed

**Issues Found**: None

The upgrade plan is comprehensive and follows all required guidelines. The plan outlines a straightforward minor version upgrade from Spring Boot 3.2.3 to 3.4.3 with proper environment setup, baseline establishment, controlled upgrade execution, and final validation steps.
