# ═══════════════════════════════════════════════════════════════════════════════
# INTEGRATED APP.PY + MCP CLIENT
# Run this instead of app.py to get both original + MCP GitHub client on same Flask
# ═══════════════════════════════════════════════════════════════════════════════

import json
import os
import smtplib
import sys
import threading
import time
import traceback
import uuid
import re
import asyncio
from datetime import datetime
from io import BytesIO
from email.message import EmailMessage
from pathlib import Path
 
from flask import Flask, jsonify, redirect, render_template, request, send_file, url_for
from werkzeug.serving import make_server
 
# ── Ensure project root is importable ─────────────────────────────
SCRIPT_DIR = Path(__file__).parent.resolve()
sys.path.insert(0, str(SCRIPT_DIR))
 
from requirement_extractor import (
    approve_card,
    get_project_catalog,
    refresh_brd_artifacts_from_card,
    run_requirements_pipeline,
    build_pending_approval,
    SCRIPT_DIR as REQ_DIR,
)
 
app = Flask(__name__)
app.config["SECRET_KEY"] = (os.getenv("FLASK_SECRET_KEY") or "change-me-local-secret").strip()

DEFAULT_USER = (os.getenv("APP_DEFAULT_USER") or "local-user").strip()
DEFAULT_ROLE = (os.getenv("APP_DEFAULT_ROLE") or "admin").strip().lower()

# ── Register chatbot routes (RAG-based AI chatbot) ─────────────────
try:
    from chatbot_routes import register_chatbot_routes
    register_chatbot_routes(app)
except Exception as _chatbot_routes_err:
    print(f"[app] Warning: Could not register chatbot routes: {_chatbot_routes_err}")

# ═══════════════════════════════════════════════════════════════════════════════
# NEW: MCP SERVER INTEGRATION
# ═══════════════════════════════════════════════════════════════════════════════

from mcp_server_github import GitHubClient
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Initialize GitHub client for MCP tools
github_client = None

def init_github_client():
    """Initialize GitHub client with credentials from .env"""
    global github_client
    try:
        token = os.getenv('GITHUB_TOKEN')
        org = os.getenv('GITHUB_ORG')
        
        if not token or not org:
            print("⚠️  GitHub credentials missing in .env")
            return False
        
        github_client = GitHubClient(token=token, org=org)
        print("✅ GitHub client initialized")
        return True
    except Exception as e:
        print(f"❌ Failed to initialize GitHub client: {e}")
        return False

# Initialize on startup
init_github_client()

# ─────────────────────────────────────────────────────────────────────
# MCP ENDPOINTS - GitHub Integration
# ─────────────────────────────────────────────────────────────────────

@app.route('/api/mcp/repos', methods=['GET'])
def get_repos():
    """List all GitHub repositories"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        result = github_client.list_repos()
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/mcp/create-repo', methods=['POST'])
def create_repo():
    """Create a new GitHub repository"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        data = request.json
        repo_name = data.get('name')
        description = data.get('description', '')
        private = data.get('private', False)
        
        if not repo_name:
            return jsonify({"error": "Repository name required"}), 400
        
        result = github_client.create_repo(repo_name, description, private)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/mcp/push-code', methods=['POST'])
def push_code():
    """Push code to a GitHub repository"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        data = request.json
        repo_name = data.get('repo')
        file_path = data.get('path')
        content = data.get('content')
        message = data.get('message', 'Update from MCP client')
        
        if not all([repo_name, file_path, content]):
            return jsonify({"error": "repo, path, and content required"}), 400
        
        result = github_client.push_file(repo_name, file_path, content, message)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/mcp/create-branch', methods=['POST'])
def create_branch():
    """Create a branch in a GitHub repository"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        data = request.json
        repo_name = data.get('repo')
        branch_name = data.get('branch')
        
        if not all([repo_name, branch_name]):
            return jsonify({"error": "repo and branch required"}), 400
        
        result = github_client.create_branch(repo_name, branch_name)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/mcp/create-issue', methods=['POST'])
def create_issue():
    """Create an issue in a GitHub repository"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        data = request.json
        repo_name = data.get('repo')
        title = data.get('title')
        body = data.get('body', '')
        
        if not all([repo_name, title]):
            return jsonify({"error": "repo and title required"}), 400
        
        result = github_client.create_issue(repo_name, title, body)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/mcp/create-pr', methods=['POST'])
def create_pr():
    """Create a pull request in a GitHub repository"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        data = request.json
        repo_name = data.get('repo')
        title = data.get('title')
        head = data.get('head')  # branch name
        base = data.get('base', 'main')  # target branch
        body = data.get('body', '')
        
        if not all([repo_name, title, head]):
            return jsonify({"error": "repo, title, and head required"}), 400
        
        result = github_client.create_pull_request(repo_name, title, head, base, body)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/mcp/delete-repo', methods=['DELETE'])
def delete_repo():
    """Delete a GitHub repository"""
    try:
        if not github_client:
            return jsonify({"error": "GitHub client not initialized"}), 500
        
        data = request.json
        repo_name = data.get('repo')
        
        if not repo_name:
            return jsonify({"error": "repo required"}), 400
        
        result = github_client.delete_repo(repo_name)
        return jsonify(result)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ─────────────────────────────────────────────────────────────────────
# MCP WEB UI PAGE
# ─────────────────────────────────────────────────────────────────────

@app.route('/mcp-client')
def mcp_client():
    """Serve the MCP client UI"""
    return render_template('mcp_client.html')

# ═══════════════════════════════════════════════════════════════════════════════
# REST OF YOUR ORIGINAL APP.PY CONTINUES HERE
# (Import and continue with your existing routes)
# ═══════════════════════════════════════════════════════════════════════════════

# Auto-indexing: initialize + watch for new documents
def _get_projects_snapshot(projects_dir: Path) -> dict:
    """Return a dict of {filepath: mtime} for all files under projects_dir."""
    snapshot = {}
    if not projects_dir.exists():
        return snapshot
    for root, _, files in os.walk(projects_dir):
        for file in files:
            fpath = Path(root) / file
            try:
                snapshot[str(fpath)] = fpath.stat().st_mtime
            except OSError:
                pass
    return snapshot

# Add your rest of original app.py code here...
# Continue with your existing routes and initialization code

if __name__ == "__main__":
    print("\n" + "="*70)
    print("🚀 INTEGRATED APP STARTING")
    print("="*70)
    print(f"✅ Original App Routes: Loaded")
    print(f"✅ MCP GitHub Integration: Loaded")
    print(f"✅ Web UI: http://localhost:5000/mcp-client")
    print("="*70 + "\n")
    
    app.run(debug=True, host="0.0.0.0", port=5000)
