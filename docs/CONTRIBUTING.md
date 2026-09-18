# Contributing Guide

Thank you for your interest in contributing to the Enterprise RAG Platform.  
This document covers the full workflow: fork → develop → test → commit → push → PR.

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [Development Workflow](#2-development-workflow)
3. [Code Conventions](#3-code-conventions)
4. [Commit Message Format](#4-commit-message-format)
5. [Running Tests Before Pushing](#5-running-tests-before-pushing)
6. [Opening a Pull Request](#6-opening-a-pull-request)
7. [Publishing the Repo (First Time)](#7-publishing-the-repo-first-time)

---

## 1. Getting Started

### Fork and clone

```bash
# Fork via GitHub CLI
gh repo fork skpradhan-repo/enterprise-rag --clone --remote

cd enterprise-rag

# Verify remotes
git remote -v
# origin    https://github.com/<YOUR-USERNAME>/enterprise-rag.git (fetch)
# upstream  https://github.com/skpradhan-repo/enterprise-rag.git (fetch)
```

### Install prerequisites

See [RUNBOOK.md — Prerequisites](../RUNBOOK.md#2-prerequisites) for the full list.  
Short version: Java 17+, Maven 3.9+, Docker or Podman, Node 20+.

### Initial setup

```bash
cp .env.example .env
mvn clean package -DskipTests
podman compose up -d        # or: docker compose up -d
```

---

## 2. Development Workflow

### Create a feature branch

Always branch off `master`:

```bash
git checkout master
git pull upstream master          # sync with upstream first
git checkout -b feat/my-feature   # or fix/my-fix, docs/my-docs
```

### Branch naming

| Prefix | Use for |
|--------|---------|
| `feat/` | New features |
| `fix/` | Bug fixes |
| `docs/` | Documentation only |
| `test/` | Test additions or fixes |
| `refactor/` | Code changes with no behaviour change |
| `chore/` | Build, deps, CI changes |

### Making backend changes

```bash
# Edit Java source
# ...

# Rebuild and restart only the app container
mvn clean package -DskipTests
podman compose up -d --build app

# Watch logs
podman logs rag-app -f --tail 50
```

### Making frontend changes

```bash
# Option A — hot reload dev server (fastest for UI work)
cd frontend
npm run dev
# Open http://localhost:5173 — changes reflect instantly

# Option B — rebuild the container (tests the full Docker build)
podman compose up -d --build frontend
```

### Keep your branch up to date

```bash
git fetch upstream
git rebase upstream/master
```

---

## 3. Code Conventions

### Java (Spring Boot)

- Follow existing package structure: `api`, `rag`, `document`, `conversation`, `security`, `audit`, `configuration`
- Use constructor injection (not field injection)
- Annotate controllers with `@RestController` + `@RequestMapping`
- All new endpoints require `@PreAuthorize` with the appropriate role
- Use `@Slf4j` (Lombok) for logging — no `System.out.println`
- Do not log PII, tokens, or passwords — see [SECURITY.md](SECURITY.md)

### TypeScript / React

- Functional components only — no class components
- Co-locate API types in [`src/types/api.ts`](../frontend/src/types/api.ts)
- API calls via the axios client in [`src/api/`](../frontend/src/api/) — not raw `fetch`
- Use `@tanstack/react-query` for data fetching on pages
- Inline styles for now (matching existing pattern) — no new CSS frameworks

### Line endings

A [`.gitattributes`](../.gitattributes) file normalises all text files to LF in the repository.  
You do not need to change your editor settings — Git handles conversion automatically.

---

## 4. Commit Message Format

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

**Types:** `feat` · `fix` · `docs` · `test` · `refactor` · `chore`  
**Scopes (optional):** `backend` · `frontend` · `infra` · `rag` · `security` · `docs`

**Examples:**

```
feat(rag): add re-ranking step to retrieval pipeline

fix(frontend): clear error state when user retypes in chat input

docs: add TESTING.md with Playwright setup guide

test(frontend): add e2e tests for DocumentsPage delete flow

chore: upgrade Spring Boot to 3.4.6
```

**Rules:**
- Summary line: 72 characters max, imperative mood, no period at end
- Body: explain *why*, not *what* (the diff shows what)
- Reference issues in the footer: `Closes #12`

---

## 5. Running Tests Before Pushing

Run these checks before opening a PR:

```bash
# 1. Backend — compile and unit tests
mvn clean package -DskipTests      # fast compile check
mvn test                           # full test suite (needs Docker for Testcontainers)

# 2. Frontend — e2e tests (no running stack needed)
cd frontend
npm run test:e2e

# 3. Frontend — type check
cd frontend
npx tsc --noEmit
```

All three must pass with zero failures before pushing.

---

## 6. Opening a Pull Request

```bash
# Push your branch
git push origin feat/my-feature

# Open a PR via GitHub CLI
gh pr create \
  --title "feat(rag): add re-ranking step to retrieval pipeline" \
  --body "## What this does
Adds a cross-encoder re-ranking step after the initial vector search.

## How to test
1. Start the stack: \`podman compose up -d\`
2. Upload any PDF document
3. Ask a question — top results should be more relevant

Closes #42" \
  --base master
```

Or open it in the browser:

```bash
gh pr create --web
```

### PR checklist

- [ ] Branch is up to date with `upstream/master`
- [ ] `mvn test` passes
- [ ] `npm run test:e2e` passes (all 46 tests)
- [ ] `npx tsc --noEmit` passes with no errors
- [ ] New features have corresponding tests
- [ ] `RUNBOOK.md` updated if a new known issue or setup step was discovered
- [ ] No secrets, tokens, or real PII committed

---

## 7. Publishing the Repo (First Time)

This section documents exactly how this repository was first published to GitHub.  
Useful if you fork and want to publish under your own account.

### Prerequisites

- `git` installed
- `gh` (GitHub CLI) installed — `winget install GitHub.cli` on Windows
- A GitHub account

### Step-by-step

```bash
# 1. Authenticate GitHub CLI (opens browser for PKCE flow)
gh auth login --hostname github.com --web
# Follow prompts: select HTTPS, authenticate Git with credentials
# Copy the one-time code shown, paste at https://github.com/login/device
# Expected: "✓ Logged in as <your-username>"

# 2. Verify authentication
gh auth status
# Expected: ✓ Logged in to github.com account <username>
# Token scopes should include: repo, workflow, gist, read:org

# 3. Initialise git in the project root (if not already a repo)
git init
git config user.name "Your Name"
git config user.email "your-username@users.noreply.github.com"

# 4. Stage all files
git add .
# Check what will be committed (confirm .env is NOT listed)
git status --short | grep -v "^??"

# 5. Make the initial commit
git commit -m "feat: initial commit — Enterprise RAG Platform"

# 6. Create the remote repo and push in one command
gh repo create <your-username>/enterprise-rag \
  --public \
  --description "Production-grade offline Enterprise RAG: Spring AI + pgvector + Ollama + Keycloak + React." \
  --source=. \
  --remote=origin \
  --push

# 7. Verify
gh repo view <your-username>/enterprise-rag --json name,url,visibility
```

### What NOT to commit

The [`.gitignore`](../.gitignore) already excludes:

| Path | Why excluded |
|------|-------------|
| `.env` | Contains passwords — **never commit** |
| `target/` | Maven build output — regenerated by `mvn package` |
| `frontend/node_modules/` | npm dependencies — regenerated by `npm install` |
| `frontend/dist/` | Vite build output — built inside the Docker container |
| `uploads/` | User-uploaded documents — runtime data, not source |
| `frontend/playwright-report/` | Test run artefacts |

### Pushing subsequent changes

```bash
git add .
git commit -m "docs: update RUNBOOK with Podman DNS fix"
git push origin master             # or: git push (branch already tracked)
```

### After pushing — verify on GitHub

```bash
# View repo summary
gh repo view

# Open in browser
gh repo view --web

# Check the latest commit reached GitHub
gh api repos/<username>/enterprise-rag/commits/master --jq '.commit.message'
```
