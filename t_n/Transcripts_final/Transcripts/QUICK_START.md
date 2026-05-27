# GitHub + MCP Quick Start Checklist

## ⚡ Get Running in 30 Minutes

This is a step-by-step checklist to set up GitHub integration with your MCP server.

---

## Phase 1: GitHub Setup (10 minutes)

### Step 1.1: Create GitHub PAT
- [ ] Go to: https://github.com/settings/tokens/new
- [ ] Name: "MCP Server - Project AI Assistant"
- [ ] Expiration: 90 days
- [ ] Select scopes: `repo`, `admin:org`, `workflow`, `user`
- [ ] Click "Generate token"
- [ ] **COPY THE TOKEN** (save it temporarily)

### Step 1.2: Create .env File
- [ ] Create file: `.env` in project root
- [ ] Add these lines:
  ```
  GITHUB_TOKEN=ghp_paste_your_token_here
  GITHUB_ORG=your-organization-name
  GITHUB_API_BASE=https://api.github.com
  DEBUG=false
  ```
- [ ] Replace `your-organization-name` with your actual GitHub organization
- [ ] **Save and close the file**

### Step 1.3: Add to .gitignore
- [ ] Open `.gitignore` in project root
- [ ] Add these lines:
  ```
  .env
  .env.local
  *.pem
  *.key
  ```
- [ ] Save

### Step 1.4: Verify Token Works
- [ ] Open PowerShell in project directory
- [ ] Run:
  ```powershell
  python -c "import os; from dotenv import load_dotenv; load_dotenv(); print('Token:', os.getenv('GITHUB_TOKEN')[:20]); print('Org:', os.getenv('GITHUB_ORG'))"
  ```
- [ ] You should see your token (first 20 chars) and organization name
- [ ] If not, check .env file format

---

## Phase 2: Install Dependencies (5 minutes)

### Step 2.1: Install Python Packages
- [ ] Run in PowerShell:
  ```bash
  pip install mcp requests python-dotenv
  ```
- [ ] Wait for installation to complete

### Step 2.2: Verify Installation
- [ ] Run:
  ```bash
  python -c "import mcp; import requests; print('All packages installed!')"
  ```
- [ ] Should print: `All packages installed!`

---

## Phase 3: Test MCP Server (10 minutes)

### Step 3.1: Start MCP Server
- [ ] Open Terminal 1 in VS Code
- [ ] Navigate to project:
  ```bash
  cd C:\Users\abhishe6\Downloads\t_n\Transcripts_final\Transcripts
  ```
- [ ] Start server:
  ```bash
  python mcp_server_github.py
  ```
- [ ] You should see:
  ```
  🚀 Starting Project AI Assistant MCP Server (with GitHub Integration)...
  🔗 GitHub Organization: your-org-name
  ✅ GitHub authentication: ENABLED
  📋 Available tools: 11
  ✅ Server is running...
  ```

### Step 3.2: Keep Server Running
- [ ] **Leave Terminal 1 open with server running**
- [ ] Server must stay active for MCP to work

### Step 3.3: Test GitHub Connection
- [ ] Open Terminal 2 (new terminal)
- [ ] Run quick test:
  ```bash
  python -c "
  import os
  from dotenv import load_dotenv
  import requests
  load_dotenv()
  
  token = os.getenv('GITHUB_TOKEN')
  headers = {'Authorization': f'Bearer {token}', 'Accept': 'application/vnd.github+json'}
  r = requests.get('https://api.github.com/user', headers=headers)
  
  if r.status_code == 200:
      user = r.json()
      print(f'✅ GitHub Auth Works! Login: {user[\"login\"]}')
  else:
      print(f'❌ GitHub Auth Failed: {r.status_code}')
  "
  ```
- [ ] Should print: `✅ GitHub Auth Works! Login: your-username`

---

## Phase 4: Connect VS Code (5 minutes)

### Step 4.1: Open Copilot Chat
- [ ] Click Copilot Chat icon in VS Code sidebar (or `Ctrl+Shift+I`)

### Step 4.2: Configure MCP Connection
- [ ] Look for MCP server configuration
- [ ] If not auto-connected, add to VS Code settings:
  1. `Ctrl+Shift+P` → "Preferences: Open Settings (JSON)"
  2. Add to settings.json:
     ```json
     {
       "github.copilot.chat.mcpServers": {
         "project-ai": {
           "command": "python",
           "args": ["C:\\Users\\abhishe6\\Downloads\\t_n\\Transcripts_final\\Transcripts\\mcp_server_github.py"]
         }
       }
     }
     ```
  3. Save

### Step 4.3: Test MCP Connection
- [ ] In Copilot Chat, type:
  ```
  What MCP tools are available?
  ```
- [ ] Should see list of 11 tools including GitHub tools
- [ ] If not, restart VS Code and try again

---

## Phase 5: Test First GitHub Command (5 minutes)

### Step 5.1: List Existing Repos
- [ ] In Copilot Chat, type:
  ```
  List all GitHub repositories in my organization
  ```
- [ ] Click "Use tool" if prompted
- [ ] Should see your organization's repositories listed

### Step 5.2: Create Test Repo
- [ ] In Copilot Chat, type:
  ```
  Create a GitHub repository called "mcp-test" 
  with description "Test repository for MCP"
  ```
- [ ] Should see:
  ```
  ✅ Repository Created!
  Name: mcp-test
  URL: https://github.com/your-org/mcp-test
  ```
- [ ] Go to https://github.com/your-org/mcp-test to verify

### Step 5.3: Push Test Code
- [ ] In Copilot Chat, type:
  ```
  Push a test file to the mcp-test repository.
  File: test.txt
  Content: Hello from MCP!
  Message: Initial test commit
  ```
- [ ] Should see:
  ```
  ✅ Code Pushed Successfully!
  ```
- [ ] Check on GitHub to verify file was created

### Step 5.4: Clean Up Test Repo
- [ ] In Copilot Chat, type:
  ```
  Delete the mcp-test repository
  ```
- [ ] Confirm when prompted

---

## Phase 6: Real Workflow Test (5 minutes)

### Step 6.1: Extract Requirements
- [ ] In Copilot Chat:
  ```
  Extract requirements from the "new" project
  ```
- [ ] Should see BRD information displayed

### Step 6.2: Create Real Repo
- [ ] In Copilot Chat:
  ```
  Create a GitHub repository called "notification-service"
  ```
- [ ] Verify on GitHub

### Step 6.3: Generate and Push Code
- [ ] In Copilot Chat:
  ```
  Generate test cases from "new" project requirements
  and push them to the notification-service repository
  ```
- [ ] Verify file was created on GitHub

---

## ✅ Verification Checklist

### Before Declaring Success:
- [ ] `.env` file created with GITHUB_TOKEN and GITHUB_ORG
- [ ] `.env` added to `.gitignore`
- [ ] All packages installed (mcp, requests, python-dotenv)
- [ ] MCP server starts without errors
- [ ] GitHub authentication test passes
- [ ] VS Code Copilot Chat connects to MCP server
- [ ] Can list GitHub repositories via MCP
- [ ] Can create test repository via MCP
- [ ] Can push files to GitHub via MCP
- [ ] Can delete repository via MCP
- [ ] Can extract requirements via MCP
- [ ] Can generate test cases via MCP

---

## 🚀 You're Ready When:

All checkmarks above are completed! You can now:
- ✅ Extract requirements from projects
- ✅ Generate code automatically
- ✅ Create GitHub repositories
- ✅ Push code to GitHub
- ✅ Create branches and PRs
- ✅ Create issues
- ✅ All with AI assistance in VS Code

---

## 📞 Troubleshooting Quick Links

| Problem | Solution |
|---------|----------|
| "GITHUB_TOKEN not found" | Check .env file format, ensure no extra spaces |
| "GitHub API rate limit" | Your token is working but used too many requests |
| "Repository not found" | Check organization name matches GitHub org |
| "Cannot create branch" | Ensure repo exists first and is initialized |
| "MCP server not starting" | Check Python installed, run `python --version` |
| "Cannot push code" | Verify GitHub_TOKEN has `repo` scope |

---

## 📚 Full Documentation

After quick start, read these for detailed info:
1. [GITHUB_SETUP.md](GITHUB_SETUP.md) - Detailed GitHub PAT setup
2. [GITHUB_WORKFLOW.md](GITHUB_WORKFLOW.md) - Complete end-to-end workflow
3. [MCP_SCENARIOS.md](MCP_SCENARIOS.md) - Real-life use case examples
4. [MCP_EXPLANATION.md](MCP_EXPLANATION.md) - How MCP works

---

## 🎓 Next Steps After Success

1. **Train team**: Share this checklist with team members
2. **Create SOP**: Document team's standard workflow
3. **Add more tools**: Create custom tools for your use cases
4. **Monitor usage**: Track MCP tool usage and gather feedback
5. **Scale**: Deploy to shared server if needed

---

## ✨ Success Indicators

When it's working properly, you'll see:

```
Copilot Chat:
"Generate code for user authentication"

Your workflow (auto-completed):
✅ Extracted requirements
✅ Generated test cases
✅ Generated implementation
✅ Created feature branch
✅ Pushed code to GitHub
✅ Created pull request
✅ Ready for team review!

Time taken: ~2 minutes
Manual time saved: ~2-3 hours
```

---

**Ready? Start with Step 1.1 above!**

Let me know when you complete each phase and I can help with any issues.
