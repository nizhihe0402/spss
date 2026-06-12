# GitHub 使用指南

本文档汇总本项目接入 GitHub、GitHub CLI、`.gitignore` 清理、推送失败处理，以及日常常用 Git/GitHub 命令。

## 1. 本项目当前背景

本地项目目录：

```powershell
D:\workspace\spss-rule-engine-codex
```

远端仓库：

```text
https://github.com/nizhihe0402/spss.git
```

当前遇到过的问题：

- 首次提交把 `.claude/`、`.idea/`、`output/`、`target/`、`pom.zip` 推到了 GitHub。
- 后续已经新增 `.gitignore`，并使用 `git rm --cached` 把这些目录/文件从 Git 索引移除，但保留本地文件。
- `git push` 出现过 `403`：

```text
remote: Permission to nizhihe0402/spss.git denied to nizhihe0402.
fatal: unable to access 'https://github.com/nizhihe0402/spss.git/': The requested URL returned error: 403
```

根因通常是 GitHub token 只有只读权限，或者 Git 没有使用 GitHub CLI 的登录凭据。

## 2. Git 和 GitHub CLI 的区别

Git 是版本控制工具，负责本地提交、分支、推送、拉取：

```powershell
git status
git add .
git commit -m "message"
git push
git pull
```

GitHub CLI 是 GitHub 官方命令行工具，命令是 `gh`，负责登录 GitHub、查看 PR、Actions、创建 PR 等：

```powershell
gh auth login
gh auth status
gh pr list
gh run list
```

Git for Windows 安装后通常会出现：

```text
Open Git GUI here
Open Git Bash here
```

这说明 Git 已安装，但不代表 `gh` 已安装。

## 3. 安装和验证 GitHub CLI

安装：

```powershell
winget install GitHub.cli
```

安装后如果当前终端找不到 `gh`，关闭终端重新打开，再验证：

```powershell
gh --version
```

如果仍然找不到，可以用完整路径验证：

```powershell
& "C:\Program Files\GitHub CLI\gh.exe" --version
```

## 4. 登录 GitHub CLI

推荐使用浏览器登录：

```powershell
gh auth login
```

建议选择：

```text
GitHub.com
HTTPS
Authenticate Git with your GitHub credentials: Yes
Login with a web browser
```

登录后检查：

```powershell
gh auth status
```

让 Git 使用 GitHub CLI 凭据：

```powershell
gh auth setup-git
```

## 5. Personal Access Token 权限

如果 `gh auth login` 要求粘贴 token，不要把 token 发到聊天或提交到仓库。

### Fine-grained token 推荐配置

Repository access 不要选：

```text
Public repositories
```

这个选项对公开仓库通常只有只读权限，不能 push。

推荐选择：

```text
Only select repositories -> nizhihe0402/spss
```

或：

```text
All repositories
```

Repository permissions 至少配置：

```text
Contents: Read and write
Metadata: Read-only
```

如果需要操作 GitHub Actions，再加：

```text
Actions: Read and write
Workflows: Read and write
```

Account permissions 一般不需要添加。

### Classic token 推荐配置

如果使用 classic token，至少勾选：

```text
repo
```

如果要操作 Actions/workflow，再勾选：

```text
workflow
```

## 6. 处理 git push 403

先确认当前登录账号：

```powershell
gh auth status
```

确认 Git 使用 GitHub CLI 凭据：

```powershell
gh auth setup-git
```

再推送：

```powershell
git push
```

如果仍然 403，检查当前账号对仓库的权限：

```powershell
gh repo view nizhihe0402/spss --json viewerPermission,nameWithOwner
```

`viewerPermission` 至少需要是：

```text
WRITE
MAINTAIN
ADMIN
```

如果不是，不能直接 push，需要仓库管理员授权，或者推送到自己的 fork 后开 PR。

如果 Windows 缓存了旧凭据，可以在：

```text
控制面板 -> 凭据管理器 -> Windows 凭据
```

删除 GitHub 相关项，例如：

```text
git:https://github.com
github.com
```

然后重新登录：

```powershell
gh auth logout
gh auth login
gh auth setup-git
git push
```

## 7. .gitignore 和已跟踪文件清理

`.gitignore` 只会阻止未跟踪文件被加入 Git。已经提交过的文件，即使后来写进 `.gitignore`，仍然会继续被 Git 跟踪。

本项目建议忽略：

```gitignore
/target/
/output/
/.idea/
/.claude/settings.local.json
/pom.zip
*.log
*.tmp
*.temp
.DS_Store
Thumbs.db
```

把已经被 Git 跟踪的忽略项从索引移除，但保留本地文件：

```powershell
git rm --cached -r target output .idea .claude/settings.local.json pom.zip
git add .gitignore
git commit -m "Add gitignore and stop tracking generated files"
git push
```

注意：`git rm --cached` 只是不再让 Git 跟踪这些文件，不会删除本地磁盘文件。

如果 GitHub 页面上仍然看到这些目录，说明清理提交还没有 push 成功。

## 8. 常用 Git 命令

查看状态：

```powershell
git status
git status --short
git status --short --branch
```

查看远端：

```powershell
git remote -v
```

添加文件：

```powershell
git add .
git add .gitignore
git add src/main/java
```

提交：

```powershell
git commit -m "Describe the change"
```

推送：

```powershell
git push
git push -u origin main
```

拉取：

```powershell
git pull
git pull --rebase
```

查看提交历史：

```powershell
git log --oneline
git log --oneline --graph --decorate --all
```

查看差异：

```powershell
git diff
git diff --cached
git diff --stat
```

查看已暂存变更：

```powershell
git diff --cached --name-status
```

撤销未暂存的某个文件改动：

```powershell
git restore path/to/file
```

取消暂存某个文件：

```powershell
git restore --staged path/to/file
```

创建分支：

```powershell
git switch -c feature/my-change
```

切换分支：

```powershell
git switch main
```

查看分支：

```powershell
git branch
git branch -a
```

## 9. 常用 GitHub CLI 命令

查看登录：

```powershell
gh auth status
```

登录：

```powershell
gh auth login
```

让 Git 使用 `gh` 凭据：

```powershell
gh auth setup-git
```

查看仓库信息：

```powershell
gh repo view
gh repo view nizhihe0402/spss
gh repo view nizhihe0402/spss --json nameWithOwner,viewerPermission,url
```

查看 PR：

```powershell
gh pr list
gh pr list --state open
gh pr view 1
gh pr checks 1
```

创建 PR：

```powershell
gh pr create --title "Title" --body "Description"
```

查看 Actions：

```powershell
gh run list
gh run view
gh run view --log
```

搜索与自己相关的 PR：

```powershell
gh search prs --review-requested=@me --state=open
gh search prs --involves=@me --state=open
gh search prs --author=@me --state=open
```

## 10. 推荐提交顺序

日常开发建议：

```powershell
git status
git add .
git diff --cached --stat
git commit -m "Meaningful commit message"
git pull --rebase
git push
```

推送前确认没有误提交构建产物、本地配置或敏感信息：

```powershell
git status --short
git diff --cached --name-status
```

敏感信息包括：

- GitHub token
- 数据库密码
- 私钥
- 本地配置
- 个人 IDE 配置
