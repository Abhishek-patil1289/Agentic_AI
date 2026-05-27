#!/usr/bin/env python3
"""
Extended MCP Server with GitHub Integration
Allows MCP clients to create repos, push code, manage PRs, create issues, etc.
"""

import json
import os
import sys
import asyncio
import base64
from pathlib import Path
from typing import Any, Optional
from datetime import datetime

# ── Load environment variables ─────────────────────────────────────
from dotenv import load_dotenv
load_dotenv()

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")
GITHUB_ORG = os.getenv("GITHUB_ORG", "")
GITHUB_API_BASE = os.getenv("GITHUB_API_BASE", "https://api.github.com")

# ── MCP SDK imports ──────────────────────────────────────────────
try:
    from mcp.server import Server
    from mcp.types import Tool, TextContent
except ImportError:
    print("ERROR: MCP SDK not installed. Install with: pip install mcp")
    sys.exit(1)

try:
    import requests
except ImportError:
    print("ERROR: requests library not installed. Install with: pip install requests")
    sys.exit(1)

# ── Project imports ──────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).parent.resolve()
sys.path.insert(0, str(SCRIPT_DIR))

try:
    from requirement_extractor import run_requirements_pipeline, get_project_catalog
    from test_case_generator import generate_test_cases
    from chatbot_ai import query_chatbot
except ImportError as e:
    print(f"Warning: Could not import all project modules: {e}")

# ── Initialize MCP Server ────────────────────────────────────────
server = Server("project-ai-assistant-github")

# ── GitHub API Helper Class ──────────────────────────────────────

class GitHubClient:
    """Wrapper for GitHub API interactions"""
    
    def __init__(self, token: str, org: str, api_base: str = "https://api.github.com"):
        self.token = token
        self.org = org
        self.api_base = api_base
        self.headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28"
        }
    
    def _request(self, method: str, endpoint: str, data: dict = None) -> dict:
        """Make API request to GitHub with detailed logging"""
        url = f"{self.api_base}{endpoint}"
        
        # ✅ Log what we're doing
        print(f"\n{'='*70}")
        print(f"🔗 GitHub API Call")
        print(f"{'='*70}")
        print(f"📍 Method: {method}")
        print(f"📍 Endpoint: {endpoint}")
        print(f"📍 Full URL: {url}")
        if data:
            print(f"📍 Data: {json.dumps(data, indent=2)}")
        
        try:
            if method == "GET":
                response = requests.get(url, headers=self.headers)
                print(f"✅ GET Request sent")
            elif method == "POST":
                response = requests.post(url, headers=self.headers, json=data)
                print(f"✅ POST Request sent")
            elif method == "PUT":
                response = requests.put(url, headers=self.headers, json=data)
                print(f"✅ PUT Request sent")
            elif method == "DELETE":
                response = requests.delete(url, headers=self.headers)
                print(f"✅ DELETE Request sent")
            else:
                error_msg = f"Unknown method: {method}"
                print(f"❌ {error_msg}")
                return {"error": error_msg}
            
            print(f"📊 Status Code: {response.status_code}")
            
            if response.status_code >= 400:
                print(f"❌ Error Response: {response.text}")
            
            response.raise_for_status()
            result = response.json() if response.text else {"success": True}
            print(f"✅ Response: {json.dumps(result, indent=2)[:200]}...")
            print(f"{'='*70}\n")
            return result
            
        except requests.exceptions.HTTPError as e:
            error_msg = str(e)
            print(f"❌ HTTP Error: {error_msg}")
            print(f"Response Body: {e.response.text}")
            print(f"{'='*70}\n")
            return {"error": error_msg, "status": e.response.status_code, "details": e.response.text}
        except Exception as e:
            error_msg = str(e)
            print(f"❌ Error: {error_msg}")
            print(f"{'='*70}\n")
            return {"error": error_msg}
    
    def create_repo(self, name: str, description: str = "", private: bool = False,
                   auto_init: bool = True) -> dict:
        """Create a new GitHub repository for the authenticated user"""
        data = {
            "name": name,
            "description": description,
            "private": private
        }
        # Only add auto_init if True (it defaults to False)
        if auto_init:
            data["auto_init"] = True
        
        print(f"🔧 Creating repository: {name}")
        # Use /user/repos endpoint for personal repos
        return self._request("POST", "/user/repos", data)
    
    def delete_repo(self, repo_name: str) -> dict:
        """Delete a GitHub repository"""
        return self._request("DELETE", f"/repos/{self.org}/{repo_name}")
    
    def create_branch(self, repo_name: str, branch_name: str, from_branch: str = "main") -> dict:
        """Create a new branch in a repository"""
        # Get the SHA of the source branch
        ref_data = self._request("GET", f"/repos/{self.org}/{repo_name}/git/ref/heads/{from_branch}")
        if "error" in ref_data:
            return ref_data
        
        sha = ref_data.get("object", {}).get("sha")
        if not sha:
            return {"error": "Could not get SHA of source branch"}
        
        data = {
            "ref": f"refs/heads/{branch_name}",
            "sha": sha
        }
        return self._request("POST", f"/repos/{self.org}/{repo_name}/git/refs", data)
    
    def push_file(self, repo_name: str, file_path: str, file_content: str,
                 commit_message: str, branch: str = "main") -> dict:
        """Push a file to a repository"""
        # Check if file already exists
        existing = self._request("GET", f"/repos/{self.org}/{repo_name}/contents/{file_path}?ref={branch}")
        
        sha = None
        if "content" in existing:  # File exists
            sha = existing.get("sha")
        
        # Encode content
        encoded_content = base64.b64encode(file_content.encode()).decode()
        
        data = {
            "message": commit_message,
            "content": encoded_content,
            "branch": branch
        }
        
        if sha:
            data["sha"] = sha
        
        return self._request("PUT", f"/repos/{self.org}/{repo_name}/contents/{file_path}", data)
    
    def create_issue(self, repo_name: str, title: str, body: str = "",
                    labels: list = None, assignees: list = None) -> dict:
        """Create a GitHub issue"""
        data = {
            "title": title,
            "body": body
        }
        if labels:
            data["labels"] = labels
        if assignees:
            data["assignees"] = assignees
        
        return self._request("POST", f"/repos/{self.org}/{repo_name}/issues", data)
    
    def create_pull_request(self, repo_name: str, title: str, head_branch: str,
                          base_branch: str = "main", body: str = "",
                          draft: bool = False) -> dict:
        """Create a pull request"""
        data = {
            "title": title,
            "head": head_branch,
            "base": base_branch,
            "body": body,
            "draft": draft
        }
        return self._request("POST", f"/repos/{self.org}/{repo_name}/pulls", data)
    
    def get_repo_info(self, repo_name: str) -> dict:
        """Get repository information"""
        return self._request("GET", f"/repos/{self.org}/{repo_name}")
    
    def list_repos(self) -> dict:
        """List all repositories for the authenticated user"""
        print(f"🔧 Listing repositories...")
        # Use /user/repos endpoint which works for the authenticated user
        # regardless of whether they're in an org or personal account
        return self._request("GET", "/user/repos?per_page=100&sort=updated")

# ── Initialize GitHub Client ────────────────────────────────────

if GITHUB_TOKEN and GITHUB_ORG:
    github_client = GitHubClient(GITHUB_TOKEN, GITHUB_ORG)
else:
    print("⚠️  WARNING: GITHUB_TOKEN or GITHUB_ORG not set. GitHub tools will not work.")
    github_client = None

# ── Extended Tool Definitions (with GitHub tools) ──────────────────

TOOLS: list[Tool] = [
    # ── Original Tools ──────────────────────────────────────
    Tool(
        name="extract_requirements",
        description="Extract requirements from documents and generate BRD/FDS/TDS",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project"
                },
                "document_path": {
                    "type": "string",
                    "description": "Path to document to analyze (optional)"
                }
            },
            "required": ["project_name"]
        }
    ),
    Tool(
        name="generate_test_cases",
        description="Generate test cases from requirements",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project"
                },
                "requirement_type": {
                    "type": "string",
                    "enum": ["brd", "fds", "tds"],
                    "description": "Type of requirement document"
                }
            },
            "required": ["project_name", "requirement_type"]
        }
    ),
    Tool(
        name="search_project_knowledge",
        description="Search project documentation and embeddings",
        inputSchema={
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "Search query"
                }
            },
            "required": ["query"]
        }
    ),
    
    # ── NEW: GitHub Tools ──────────────────────────────────
    Tool(
        name="create_github_repo",
        description="Create a new GitHub repository in the organization",
        inputSchema={
            "type": "object",
            "properties": {
                "repo_name": {
                    "type": "string",
                    "description": "Name of the repository to create (lowercase, hyphens allowed)"
                },
                "description": {
                    "type": "string",
                    "description": "Repository description"
                },
                "private": {
                    "type": "boolean",
                    "description": "Whether repo should be private",
                    "default": False
                }
            },
            "required": ["repo_name"]
        }
    ),
    Tool(
        name="push_code_to_github",
        description="Push generated code files to a GitHub repository",
        inputSchema={
            "type": "object",
            "properties": {
                "repo_name": {
                    "type": "string",
                    "description": "Name of the GitHub repository"
                },
                "file_path": {
                    "type": "string",
                    "description": "Path in repo where file should be saved (e.g., src/main.py)"
                },
                "file_content": {
                    "type": "string",
                    "description": "The code/content to push"
                },
                "commit_message": {
                    "type": "string",
                    "description": "Git commit message"
                },
                "branch": {
                    "type": "string",
                    "description": "Branch to push to (default: main)",
                    "default": "main"
                }
            },
            "required": ["repo_name", "file_path", "file_content", "commit_message"]
        }
    ),
    Tool(
        name="create_github_branch",
        description="Create a new branch in a GitHub repository",
        inputSchema={
            "type": "object",
            "properties": {
                "repo_name": {
                    "type": "string",
                    "description": "Name of the repository"
                },
                "branch_name": {
                    "type": "string",
                    "description": "Name of new branch"
                },
                "from_branch": {
                    "type": "string",
                    "description": "Source branch (default: main)",
                    "default": "main"
                }
            },
            "required": ["repo_name", "branch_name"]
        }
    ),
    Tool(
        name="create_github_issue",
        description="Create a GitHub issue in a repository",
        inputSchema={
            "type": "object",
            "properties": {
                "repo_name": {
                    "type": "string",
                    "description": "Name of the repository"
                },
                "title": {
                    "type": "string",
                    "description": "Issue title"
                },
                "body": {
                    "type": "string",
                    "description": "Issue description/body"
                },
                "labels": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "Labels to add (e.g., ['bug', 'enhancement'])"
                }
            },
            "required": ["repo_name", "title"]
        }
    ),
    Tool(
        name="create_pull_request",
        description="Create a pull request on GitHub",
        inputSchema={
            "type": "object",
            "properties": {
                "repo_name": {
                    "type": "string",
                    "description": "Name of the repository"
                },
                "title": {
                    "type": "string",
                    "description": "PR title"
                },
                "head_branch": {
                    "type": "string",
                    "description": "Source branch with changes"
                },
                "base_branch": {
                    "type": "string",
                    "description": "Target branch (default: main)",
                    "default": "main"
                },
                "body": {
                    "type": "string",
                    "description": "PR description"
                },
                "draft": {
                    "type": "boolean",
                    "description": "Whether to create as draft PR",
                    "default": False
                }
            },
            "required": ["repo_name", "title", "head_branch"]
        }
    ),
    Tool(
        name="delete_github_repo",
        description="Delete a GitHub repository",
        inputSchema={
            "type": "object",
            "properties": {
                "repo_name": {
                    "type": "string",
                    "description": "Name of repository to delete"
                },
                "confirm": {
                    "type": "boolean",
                    "description": "Confirmation flag",
                    "default": False
                }
            },
            "required": ["repo_name", "confirm"]
        }
    ),
    Tool(
        name="list_github_repos",
        description="List all repositories in the organization",
        inputSchema={"type": "object", "properties": {}}
    ),
    Tool(
        name="generate_and_push_code",
        description="Complete workflow: Generate code from requirements and push to GitHub",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Project to extract requirements from"
                },
                "repo_name": {
                    "type": "string",
                    "description": "GitHub repo to push code to"
                },
                "code_type": {
                    "type": "string",
                    "enum": ["test_cases", "documentation", "implementation"],
                    "description": "Type of code to generate"
                },
                "create_pr": {
                    "type": "boolean",
                    "description": "Create PR instead of pushing to main",
                    "default": True
                }
            },
            "required": ["project_name", "repo_name", "code_type"]
        }
    )
]

# ── Tool Handlers ────────────────────────────────────────────────

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> Any:
    """Handle tool calls from MCP clients"""
    
    try:
        if name == "create_github_repo":
            return await _handle_create_repo(arguments)
        elif name == "push_code_to_github":
            return await _handle_push_code(arguments)
        elif name == "create_github_branch":
            return await _handle_create_branch(arguments)
        elif name == "create_github_issue":
            return await _handle_create_issue(arguments)
        elif name == "create_pull_request":
            return await _handle_create_pr(arguments)
        elif name == "delete_github_repo":
            return await _handle_delete_repo(arguments)
        elif name == "list_github_repos":
            return await _handle_list_repos(arguments)
        elif name == "generate_and_push_code":
            return await _handle_generate_and_push(arguments)
        # Original tools
        elif name == "extract_requirements":
            return await _handle_extract_requirements(arguments)
        elif name == "generate_test_cases":
            return await _handle_generate_test_cases(arguments)
        elif name == "search_project_knowledge":
            return await _handle_search_knowledge(arguments)
        else:
            return TextContent(type="text", text=f"Unknown tool: {name}")
    except Exception as e:
        return TextContent(type="text", text=f"Error executing {name}: {str(e)}")

# ── GitHub Tool Handlers ────────────────────────────────────────

async def _handle_create_repo(args: dict) -> TextContent:
    """Create a GitHub repository"""
    print(f"\n{'*'*70}")
    print(f"🎯 TOOL CALLED: create_github_repo")
    print(f"📋 Arguments: {json.dumps(args, indent=2)}")
    print(f"{'*'*70}")
    
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    repo_name = args.get("repo_name")
    description = args.get("description", "")
    private = args.get("private", False)
    
    if not repo_name:
        error = "❌ repo_name is required"
        print(error)
        return TextContent(type="text", text=error)
    
    print(f"📍 Creating repo: {repo_name} (private={private})")
    result = github_client.create_repo(repo_name, description, private)
    
    if "error" in result:
        error_text = f"❌ Error creating repo: {result['error']}"
        print(f"{error_text}")
        return TextContent(type="text", text=error_text)
    
    output = f"""✅ Repository Created Successfully!
    
📍 Name: {result.get('name')}
🔗 URL: {result.get('html_url')}
📝 Description: {result.get('description', 'N/A')}
🔒 Private: {result.get('private')}
💾 Clone: {result.get('clone_url')}
"""
    print(output)
    print(f"{'*'*70}\n")
    
    return TextContent(type="text", text=output)

async def _handle_push_code(args: dict) -> TextContent:
    """Push code to GitHub"""
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    repo_name = args.get("repo_name")
    file_path = args.get("file_path")
    file_content = args.get("file_content")
    commit_message = args.get("commit_message", f"Add {file_path}")
    branch = args.get("branch", "main")
    
    result = github_client.push_file(repo_name, file_path, file_content, commit_message, branch)
    
    if "error" in result:
        return TextContent(type="text", text=f"❌ Error pushing file: {result['error']}")
    
    return TextContent(
        type="text",
        text=f"""✅ Code Pushed Successfully!

Repository: {repo_name}
File: {file_path}
Branch: {branch}
Message: {commit_message}
Commit URL: {result.get('commit', {}).get('html_url', 'N/A')}
"""
    )

async def _handle_create_branch(args: dict) -> TextContent:
    """Create a branch"""
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    repo_name = args.get("repo_name")
    branch_name = args.get("branch_name")
    from_branch = args.get("from_branch", "main")
    
    result = github_client.create_branch(repo_name, branch_name, from_branch)
    
    if "error" in result:
        return TextContent(type="text", text=f"❌ Error creating branch: {result['error']}")
    
    return TextContent(
        type="text",
        text=f"""✅ Branch Created!

Repository: {repo_name}
New Branch: {branch_name}
Source: {from_branch}
"""
    )

async def _handle_create_issue(args: dict) -> TextContent:
    """Create a GitHub issue"""
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    repo_name = args.get("repo_name")
    title = args.get("title")
    body = args.get("body", "")
    labels = args.get("labels", [])
    
    result = github_client.create_issue(repo_name, title, body, labels)
    
    if "error" in result:
        return TextContent(type="text", text=f"❌ Error creating issue: {result['error']}")
    
    return TextContent(
        type="text",
        text=f"""✅ Issue Created!

Repository: {repo_name}
Title: {result.get('title')}
Issue URL: {result.get('html_url')}
Number: #{result.get('number')}
"""
    )

async def _handle_create_pr(args: dict) -> TextContent:
    """Create a pull request"""
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    repo_name = args.get("repo_name")
    title = args.get("title")
    head_branch = args.get("head_branch")
    base_branch = args.get("base_branch", "main")
    body = args.get("body", "")
    draft = args.get("draft", False)
    
    result = github_client.create_pull_request(repo_name, title, head_branch, base_branch, body, draft)
    
    if "error" in result:
        return TextContent(type="text", text=f"❌ Error creating PR: {result['error']}")
    
    return TextContent(
        type="text",
        text=f"""✅ Pull Request Created!

Repository: {repo_name}
Title: {result.get('title')}
PR URL: {result.get('html_url')}
Number: #{result.get('number')}
Status: {'Draft' if draft else 'Ready for Review'}
"""
    )

async def _handle_delete_repo(args: dict) -> TextContent:
    """Delete a repository"""
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    repo_name = args.get("repo_name")
    confirm = args.get("confirm", False)
    
    if not confirm:
        return TextContent(type="text", text=f"⚠️ Confirm deletion of '{repo_name}' by setting confirm=true")
    
    result = github_client.delete_repo(repo_name)
    
    if "error" in result:
        return TextContent(type="text", text=f"❌ Error deleting repo: {result['error']}")
    
    return TextContent(type="text", text=f"✅ Repository '{repo_name}' has been deleted.")

async def _handle_list_repos(args: dict) -> TextContent:
    """List all organization repositories"""
    print(f"\n{'*'*70}")
    print(f"🎯 TOOL CALLED: list_github_repos")
    print(f"{'*'*70}")
    
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    result = github_client.list_repos()
    
    if "error" in result:
        error_text = f"❌ Error listing repos: {result['error']}"
        print(f"{error_text}")
        return TextContent(type="text", text=error_text)
    
    repos = result if isinstance(result, list) else []
    
    repo_list = "\n".join([
        f"  • {r['name']}: {r.get('description', 'No description')}"
        for r in repos
    ])
    
    output = f"""📦 Your GitHub Repositories:

{repo_list if repo_list else "No repositories found"}

Total: {len(repos)} repositories
"""
    print(output)
    print(f"{'*'*70}\n")
    
    return TextContent(type="text", text=output)

async def _handle_generate_and_push(args: dict) -> TextContent:
    """Complete workflow: Generate code and push to GitHub"""
    if not github_client:
        return TextContent(type="text", text="Error: GitHub not configured")
    
    project_name = args.get("project_name")
    repo_name = args.get("repo_name")
    code_type = args.get("code_type", "test_cases")
    create_pr = args.get("create_pr", True)
    
    try:
        # Step 1: Extract requirements
        requirements = await asyncio.to_thread(
            run_requirements_pipeline,
            project_name
        )
        
        # Step 2: Generate code based on type
        if code_type == "test_cases":
            generated_code = await asyncio.to_thread(
                generate_test_cases,
                project_name,
                "brd"
            )
            filename = f"test_cases_{datetime.now().strftime('%Y%m%d_%H%M%S')}.py"
        elif code_type == "implementation":
            generated_code = str(requirements).replace("\\n", "\n")
            filename = f"implementation_{datetime.now().strftime('%Y%m%d_%H%M%S')}.py"
        else:
            generated_code = json.dumps(requirements, indent=2)
            filename = f"README.md"
        
        # Step 3: Create branch if using PR
        branch_name = f"feature/{code_type}-{datetime.now().strftime('%Y%m%d')}"
        if create_pr:
            await asyncio.to_thread(github_client.create_branch, repo_name, branch_name)
        
        # Step 4: Push code
        push_result = await asyncio.to_thread(
            github_client.push_file,
            repo_name,
            filename,
            str(generated_code),
            f"Add auto-generated {code_type}",
            branch_name if create_pr else "main"
        )
        
        # Step 5: Create PR if requested
        if create_pr:
            pr_result = await asyncio.to_thread(
                github_client.create_pull_request,
                repo_name,
                f"Auto-generated {code_type}",
                branch_name,
                "main",
                f"Automatically generated {code_type} from project '{project_name}'"
            )
            
            return TextContent(
                type="text",
                text=f"""✅ Complete Workflow Finished!

Step 1: ✓ Extracted requirements from '{project_name}'
Step 2: ✓ Generated {code_type}
Step 3: ✓ Created branch: {branch_name}
Step 4: ✓ Pushed code: {filename}
Step 5: ✓ Created PR: #{pr_result.get('number')}

PR URL: {pr_result.get('html_url')}
Repository: {repo_name}

Next: Review and merge the PR!
"""
            )
        else:
            return TextContent(
                type="text",
                text=f"""✅ Code Generated and Pushed!

Generated: {filename}
Repository: {repo_name}
Branch: main

Code is ready for review!
"""
            )
    
    except Exception as e:
        return TextContent(type="text", text=f"❌ Error in workflow: {str(e)}")

# ── Original Tool Handlers (from previous mcp_server.py) ────────

async def _handle_extract_requirements(args: dict) -> TextContent:
    """Extract requirements"""
    project_name = args.get("project_name")
    try:
        result = await asyncio.to_thread(run_requirements_pipeline, project_name)
        return TextContent(type="text", text=f"Requirements extracted:\n{json.dumps(result, indent=2)}")
    except Exception as e:
        return TextContent(type="text", text=f"Error: {str(e)}")

async def _handle_generate_test_cases(args: dict) -> TextContent:
    """Generate test cases"""
    project_name = args.get("project_name")
    requirement_type = args.get("requirement_type")
    try:
        result = await asyncio.to_thread(generate_test_cases, project_name, requirement_type)
        return TextContent(type="text", text=f"Test cases generated:\n{json.dumps(result, indent=2)}")
    except Exception as e:
        return TextContent(type="text", text=f"Error: {str(e)}")

async def _handle_search_knowledge(args: dict) -> TextContent:
    """Search project knowledge"""
    query = args.get("query")
    return TextContent(type="text", text=f"Search results for '{query}': Coming soon")

# ── List Tools ──────────────────────────────────────────────────

@server.list_tools()
async def list_tools() -> list[Tool]:
    """List available MCP tools"""
    return TOOLS

# ── Main ────────────────────────────────────────────────────────

async def main():
    """Start the MCP server"""
    print("🚀 Starting Project AI Assistant MCP Server (with GitHub Integration)...")
    print(f"📁 Project directory: {SCRIPT_DIR}")
    
    if github_client:
        print(f"🔗 GitHub Organization: {GITHUB_ORG}")
        print(f"✅ GitHub authentication: ENABLED")
    else:
        print("⚠️  GitHub authentication: DISABLED (check .env)")
    
    print(f"📋 Available tools: {len(TOOLS)}")
    for tool in TOOLS:
        print(f"   - {tool.name}")
    
    print("\n✅ Server is running and ready for MCP clients to connect")
    
    # Keep the server running indefinitely
    try:
        while True:
            await asyncio.sleep(1)
    except KeyboardInterrupt:
        print("\n🛑 Server shutting down...")

if __name__ == "__main__":
    asyncio.run(main())
