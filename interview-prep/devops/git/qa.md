# Git & Version Control — Interview Q&A

> 10 questions covering Git commands, branching strategies, workflows  
> Asked in: Machine coding rounds, take-home assignments, DevOps rounds, "how do you work" questions

---

### Q1. Git merge vs rebase — when to use which?

```bash
# MERGE: Creates a merge commit, preserves history
git checkout main
git merge feature-branch
# History: A--B--C--M (merge commit)
#              \   /
#               D-E (feature branch)

# REBASE: Replays commits on top of target, linear history
git checkout feature-branch
git rebase main
# History: A--B--C--D'--E' (linear, clean)

# When to use:
# MERGE: shared branches (main, develop) — preserves who did what
# REBASE: local/feature branches before merge — clean linear history
# ⚠️ NEVER rebase commits that are already pushed to shared branch
```

**Golden rule:** Rebase local, merge shared.

---

### Q2. Git branching strategies.

```
1. Git Flow (traditional)
   main ──────────────────────────────────────→
     ↑                    ↑
   develop ──────────────────────────────────→
     ↑    ↑        ↑
   feature/login  feature/cart  hotfix/bug-123
   
   Branches: main, develop, feature/*, release/*, hotfix/*
   Pro: Clear separation. Con: Complex for small teams.

2. GitHub Flow (simple — recommended for most)
   main ──────────────────────────────────────→
     ↑           ↑             ↑
   feature/A   feature/B    feature/C
   
   Rules: main is always deployable, create PR for every change
   Pro: Simple. Con: No staging/release branch.

3. Trunk-Based Development (Google, Meta style)
   main ────────────────────────────────────→
     ↑  ↑  ↑  ↑  ↑  (short-lived branches, merged daily)
   
   Feature flags control what's visible to users.
   Pro: Fast CI/CD, small PRs. Con: Requires feature flags, strong testing.
```

---

### Q3. Essential Git commands (interview rapid-fire).

```bash
# Basics
git init                          # Initialize repo
git clone <url>                   # Clone remote repo
git status                        # Show working tree status
git add .                         # Stage all changes
git commit -m "message"           # Commit staged changes
git push origin main              # Push to remote

# Branching
git branch feature/login          # Create branch
git checkout -b feature/login     # Create + switch (shortcut)
git switch -c feature/login       # Modern syntax (Git 2.23+)
git branch -d feature/login       # Delete branch (safe)
git branch -D feature/login       # Force delete

# Viewing history
git log --oneline --graph --all   # Visual branch history
git log --author="Karthik"        # Filter by author
git diff main..feature-branch     # Diff between branches
git blame file.java               # Who changed each line

# Undoing changes
git checkout -- file.java         # Discard working dir changes
git restore file.java             # Modern syntax (Git 2.23+)
git reset HEAD file.java          # Unstage
git reset --soft HEAD~1           # Undo last commit, keep changes staged
git reset --hard HEAD~1           # ⚠️ Undo last commit, discard changes
git revert <commit-hash>          # Create new commit that undoes a commit (safe)

# Stashing
git stash                         # Save uncommitted changes
git stash pop                     # Restore stashed changes
git stash list                    # List all stashes
git stash apply stash@{2}        # Apply specific stash

# Advanced
git cherry-pick <commit-hash>     # Apply specific commit to current branch
git bisect start                  # Binary search for bug-introducing commit
git reflog                        # History of HEAD movements (recovery tool)
git clean -fd                     # Remove untracked files and directories
```

---

### Q4. How to resolve merge conflicts?

```bash
# 1. Attempt merge
git merge feature-branch
# CONFLICT (content): Merge conflict in src/OrderService.java

# 2. Open conflicted file
<<<<<<< HEAD
    private int maxRetries = 3;
=======
    private int maxRetries = 5;
>>>>>>> feature-branch

# 3. Choose correct version (or combine), remove markers
    private int maxRetries = 5;  // Use feature branch value

# 4. Stage resolved file and commit
git add src/OrderService.java
git commit  # Git auto-generates merge commit message

# Tips:
# - Use IDE merge tools (IntelliJ, VS Code) — visual 3-way merge
# - Pull main frequently to minimize conflicts
# - Keep PRs small → fewer conflicts
```

---

### Q5. git reset vs git revert vs git checkout.

```
                     Working Dir    Staging Area    Commit History
git checkout -- f    Restores       No change       No change
git restore f        Restores       No change       No change
git reset HEAD f     No change      Unstages        No change
git reset --soft     No change      No change       Moves HEAD back
git reset --mixed    No change      Unstages        Moves HEAD back (default)
git reset --hard     Restores       Clears          Moves HEAD back ⚠️
git revert <hash>    No change      No change       NEW commit that undoes

Key difference:
- reset: rewrites history (dangerous for shared branches)
- revert: adds new commit (safe for shared branches)
```

---

### Q6. Git hooks — automate quality checks.

```bash
# pre-commit: runs before commit (lint, format)
#!/bin/sh
./gradlew spotlessCheck || exit 1  # Fail commit if formatting wrong

# pre-push: runs before push (tests)
#!/bin/sh
./gradlew test || exit 1  # Fail push if tests fail

# commit-msg: validate commit message format
#!/bin/sh
if ! grep -qE "^(feat|fix|docs|refactor|test|chore)\(.+\): .+" "$1"; then
    echo "Commit message must follow: type(scope): description"
    exit 1
fi

# Common tools:
# Husky (JS): manages git hooks in package.json
# pre-commit (Python): framework for managing multi-language hooks
```

---

### Q7. Conventional Commits — why and how?

```
Format: <type>(<scope>): <description>

Types:
  feat:     New feature
  fix:      Bug fix
  docs:     Documentation only
  refactor: Code change (no feature/fix)
  test:     Adding tests
  chore:    Build process, dependencies
  perf:     Performance improvement
  ci:       CI/CD changes

Examples:
  feat(auth): add JWT token refresh endpoint
  fix(payment): handle null amount in UPI flow
  refactor(tracking): extract event processing to separate service
  test(order): add integration tests for checkout saga

Benefits:
- Auto-generate changelogs
- Semantic versioning automation
- Clear commit history
- Easier code review
```

---

### Q8. Pull Request best practices (code review culture).

```
PR checklist:
□ Title follows conventional format
□ Description explains WHAT and WHY (not HOW — code shows how)
□ Linked to ticket/issue
□ Self-reviewed (read your own diff before requesting review)
□ Tests added/updated
□ No console.log / System.out.println left behind
□ No hardcoded credentials or secrets
□ Small scope (< 400 lines ideally)

Good PR description template:
## What
Added JWT refresh token endpoint

## Why
Tokens were expiring mid-session, causing user logouts

## How
- Added /auth/refresh endpoint
- Stores refresh tokens in Redis (7-day TTL)
- Rotates refresh token on each use

## Testing
- Unit tests for token generation/validation
- Integration test for full refresh flow
- Manual test: verified token rotation in Postman
```

---

### Q9. .gitignore — what to exclude?

```gitignore
# IDE files
.idea/
.vscode/
*.iml

# Build outputs
target/
build/
dist/
node_modules/

# Environment files
.env
.env.local
application-local.yml

# OS files
.DS_Store
Thumbs.db

# Secrets (NEVER commit)
*.pem
*.key
credentials.json

# Logs
*.log
logs/
```

**If you accidentally committed a secret:**
```bash
# Remove from history (nuclear option)
git filter-branch --force --index-filter \
  'git rm --cached --ignore-unmatch path/to/secret.key' HEAD
# Or use BFG Repo-Cleaner (faster)
bfg --delete-files secret.key
# Then force-push and rotate the secret immediately
```

---

### Q10. Git internals — how does Git store data? (Senior-level question)

```
Git is a content-addressable filesystem. Everything is stored as objects:

Objects (stored in .git/objects/):
  blob   — file content (no filename!)
  tree   — directory listing (filename → blob SHA)
  commit — snapshot: tree + parent + author + message
  tag    — named reference to a commit

References (stored in .git/refs/):
  heads/main    — points to latest commit on main
  heads/feature — points to latest commit on feature branch
  remotes/origin/main — remote tracking branch
  tags/v1.0     — tag reference

How it works:
  1. git add → creates blob objects (content SHA-1 hash)
  2. git commit → creates tree (directory snapshot) + commit object
  3. Branches are just pointers (40-byte file) to commit SHA
  4. HEAD is a pointer to current branch ref

  commit3 ← main (HEAD)
    ↓
  commit2
    ↓
  commit1

  Moving a branch = updating a 40-byte file. That's why branching is instant.
```
