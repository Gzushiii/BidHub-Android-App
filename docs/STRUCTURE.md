# Documentation Structure

This document describes the organization of all project documentation.

## Root Level

- `README.md` - Main project README
- `docs/` - All documentation (this directory)
- `scripts/` - Utility scripts
- `sql/` - Database SQL scripts
- `tests/` - Test files

## Documentation Categories

### `/docs/api/`
API specifications, compatibility reports, and API-related documentation.

### `/docs/development/`
Development guides, implementation notes, bug fixes, performance analysis.
- `/troubleshooting/` - Specific issue resolution guides

### `/docs/database/`
Database schemas, setup guides, migrations, and data management.

### `/docs/testing/`
Test plans, test results, and testing procedures.

### `/docs/deployment/`
Deployment guides, configuration, and production setup.

### `/docs/archive/`
Historical documentation, completed fixes, and deprecated guides.

### `/docs/project-management/`
Project planning, requirements, analysis, and management documents.

### `/docs/payments/`
Payment system documentation (manual topup, etc.).

## Naming Conventions

### Files
- Use UPPERCASE for important reports: `BUG_ANALYSIS_SUMMARY.md`
- Use lowercase with hyphens for guides: `quick-start-guide.md`
- Use descriptive names that indicate content
- Use `.md` extension for markdown files

### Directories
- Use lowercase with hyphens: `project-management/`
- Use descriptive names: `troubleshooting/`, `testing/`

## File Organization Principles

1. **By Category**: Files are grouped by their primary purpose
2. **By Status**: Completed/historical files go to `archive/`
3. **By Audience**: Developer docs in `development/`, deployment in `deployment/`
4. **By Type**: API docs in `api/`, database docs in `database/`

## Quick Reference

| What you're looking for | Where to find it |
|------------------------|------------------|
| API documentation | `docs/api/` |
| Bug fixes | `docs/development/` |
| Troubleshooting | `docs/development/troubleshooting/` |
| Database setup | `docs/database/` |
| Test results | `docs/testing/` |
| Deployment guide | `docs/deployment/` |
| Project planning | `docs/project-management/` |
| Historical docs | `docs/archive/` |
| Scripts | `scripts/` |

