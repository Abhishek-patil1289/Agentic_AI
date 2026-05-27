#!/usr/bin/env python3
"""
MCP Server for Project AI Assistant
Exposes project capabilities (code generation, test case generation, requirements extraction, etc.)
"""

import json
import os
import sys
from pathlib import Path
from typing import Any, Optional
import asyncio

# ── MCP SDK imports ──────────────────────────────────────────────
try:
    from mcp.server import Server
    from mcp.types import Tool, TextContent, ToolResult
except ImportError:
    print("ERROR: MCP SDK not installed. Install with: pip install mcp")
    sys.exit(1)

# ── Project imports ──────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).parent.resolve()
sys.path.insert(0, str(SCRIPT_DIR))

try:
    from requirement_extractor import run_requirements_pipeline, get_project_catalog, approve_card
    from test_case_generator import generate_test_cases
    from chatbot_ai import query_chatbot
    from vector_embeddings import initialize_embeddings, search_embeddings
except ImportError as e:
    print(f"Warning: Could not import project modules: {e}")
    print("Some MCP tools may not be available")

# ── Initialize MCP Server ────────────────────────────────────────
server = Server("project-ai-assistant")

# ── Tool Definitions ─────────────────────────────────────────────

TOOLS: list[Tool] = [
    Tool(
        name="extract_requirements",
        description="Extract requirements from documents. Analyzes documents to extract structured requirements, BRD, FDS, TDS.",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project to extract requirements for"
                },
                "document_path": {
                    "type": "string",
                    "description": "Path to the document file to process (optional, if not provided uses default)"
                },
                "include_cards": {
                    "type": "boolean",
                    "description": "Whether to generate requirement cards",
                    "default": True
                }
            },
            "required": ["project_name"]
        }
    ),
    Tool(
        name="generate_test_cases",
        description="Generate test cases from requirements or BRD documents. Creates comprehensive test scenarios.",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project"
                },
                "requirement_type": {
                    "type": "string",
                    "enum": ["brd", "fds", "tds", "card"],
                    "description": "Type of requirement document to generate tests from"
                },
                "scope": {
                    "type": "string",
                    "description": "Specific scope or feature to generate tests for (optional)"
                }
            },
            "required": ["project_name", "requirement_type"]
        }
    ),
    Tool(
        name="query_project_chatbot",
        description="Query the project's RAG-based AI chatbot. Ask questions about requirements, implementation details, etc.",
        inputSchema={
            "type": "object",
            "properties": {
                "question": {
                    "type": "string",
                    "description": "Question to ask the chatbot"
                },
                "project_name": {
                    "type": "string",
                    "description": "Project context for the question (optional)"
                },
                "chat_history": {
                    "type": "array",
                    "description": "Previous chat messages for context",
                    "items": {
                        "type": "object",
                        "properties": {
                            "role": {"type": "string"},
                            "content": {"type": "string"}
                        }
                    }
                }
            },
            "required": ["question"]
        }
    ),
    Tool(
        name="get_project_catalog",
        description="Retrieve catalog of all projects and their artifacts. Shows all available BRDs, FDS, TDS, test cases, and estimations.",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Specific project name (optional, returns all if not specified)"
                }
            }
        }
    ),
    Tool(
        name="search_project_knowledge",
        description="Search through project documentation and embeddings. Find relevant information from BRDs, FDS, TDS, and requirements.",
        inputSchema={
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "Search query"
                },
                "document_type": {
                    "type": "string",
                    "enum": ["brd", "fds", "tds", "all"],
                    "description": "Type of documents to search in",
                    "default": "all"
                },
                "top_k": {
                    "type": "integer",
                    "description": "Number of results to return",
                    "default": 5
                }
            },
            "required": ["query"]
        }
    ),
    Tool(
        name="generate_estimation",
        description="Generate project estimation. Uses FDS/TDS to estimate effort, timeline, and resources.",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project to estimate"
                },
                "scope": {
                    "type": "string",
                    "description": "Scope of work to estimate"
                }
            },
            "required": ["project_name"]
        }
    ),
    Tool(
        name="approve_requirement_card",
        description="Approve a generated requirement card. Marks a card as approved and updates project artifacts.",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project"
                },
                "card_id": {
                    "type": "string",
                    "description": "ID of the card to approve"
                },
                "feedback": {
                    "type": "string",
                    "description": "Optional feedback or comments on the card"
                }
            },
            "required": ["project_name", "card_id"]
        }
    ),
    Tool(
        name="list_project_files",
        description="List all files in a project directory. Shows available artifacts and documents.",
        inputSchema={
            "type": "object",
            "properties": {
                "project_name": {
                    "type": "string",
                    "description": "Name of the project"
                },
                "artifact_type": {
                    "type": "string",
                    "enum": ["brd", "fds", "tds", "cards", "estimation", "tests", "all"],
                    "description": "Type of artifacts to list",
                    "default": "all"
                }
            },
            "required": ["project_name"]
        }
    )
]

# ── Tool Handlers ────────────────────────────────────────────────

@server.call_tool()
async def call_tool(name: str, arguments: dict) -> Any:
    """Handle tool calls from MCP clients"""
    
    try:
        if name == "extract_requirements":
            return await _handle_extract_requirements(arguments)
        elif name == "generate_test_cases":
            return await _handle_generate_test_cases(arguments)
        elif name == "query_project_chatbot":
            return await _handle_query_chatbot(arguments)
        elif name == "get_project_catalog":
            return await _handle_get_catalog(arguments)
        elif name == "search_project_knowledge":
            return await _handle_search_knowledge(arguments)
        elif name == "generate_estimation":
            return await _handle_generate_estimation(arguments)
        elif name == "approve_requirement_card":
            return await _handle_approve_card(arguments)
        elif name == "list_project_files":
            return await _handle_list_files(arguments)
        else:
            return TextContent(type="text", text=f"Unknown tool: {name}")
    except Exception as e:
        return TextContent(type="text", text=f"Error executing tool {name}: {str(e)}")

async def _handle_extract_requirements(args: dict) -> TextContent:
    """Extract requirements from documents"""
    project_name = args.get("project_name")
    document_path = args.get("document_path")
    include_cards = args.get("include_cards", True)
    
    try:
        # Call the project's requirement extraction pipeline
        result = await asyncio.to_thread(
            run_requirements_pipeline,
            project_name,
            document_path
        )
        return TextContent(
            type="text",
            text=f"Requirements extracted successfully for {project_name}. Result:\n{json.dumps(result, indent=2)}"
        )
    except Exception as e:
        return TextContent(type="text", text=f"Error extracting requirements: {str(e)}")

async def _handle_generate_test_cases(args: dict) -> TextContent:
    """Generate test cases"""
    project_name = args.get("project_name")
    requirement_type = args.get("requirement_type")
    scope = args.get("scope")
    
    try:
        result = await asyncio.to_thread(
            generate_test_cases,
            project_name,
            requirement_type,
            scope
        )
        return TextContent(
            type="text",
            text=f"Test cases generated for {project_name}. Result:\n{json.dumps(result, indent=2)}"
        )
    except Exception as e:
        return TextContent(type="text", text=f"Error generating test cases: {str(e)}")

async def _handle_query_chatbot(args: dict) -> TextContent:
    """Query the project chatbot"""
    question = args.get("question")
    project_name = args.get("project_name")
    chat_history = args.get("chat_history", [])
    
    try:
        result = await asyncio.to_thread(
            query_chatbot,
            question,
            project_name,
            chat_history
        )
        return TextContent(type="text", text=f"Chatbot response: {result}")
    except Exception as e:
        return TextContent(type="text", text=f"Error querying chatbot: {str(e)}")

async def _handle_get_catalog(args: dict) -> TextContent:
    """Get project catalog"""
    project_name = args.get("project_name")
    
    try:
        catalog = await asyncio.to_thread(get_project_catalog, project_name)
        return TextContent(
            type="text",
            text=f"Project catalog:\n{json.dumps(catalog, indent=2)}"
        )
    except Exception as e:
        return TextContent(type="text", text=f"Error getting catalog: {str(e)}")

async def _handle_search_knowledge(args: dict) -> TextContent:
    """Search project knowledge base"""
    query = args.get("query")
    doc_type = args.get("document_type", "all")
    top_k = args.get("top_k", 5)
    
    try:
        results = await asyncio.to_thread(
            search_embeddings,
            query,
            top_k,
            doc_type
        )
        return TextContent(
            type="text",
            text=f"Search results for '{query}':\n{json.dumps(results, indent=2)}"
        )
    except Exception as e:
        return TextContent(type="text", text=f"Error searching knowledge: {str(e)}")

async def _handle_generate_estimation(args: dict) -> TextContent:
    """Generate project estimation"""
    project_name = args.get("project_name")
    scope = args.get("scope")
    
    return TextContent(
        type="text",
        text=f"Estimation feature for {project_name} coming soon..."
    )

async def _handle_approve_card(args: dict) -> TextContent:
    """Approve a requirement card"""
    project_name = args.get("project_name")
    card_id = args.get("card_id")
    feedback = args.get("feedback", "")
    
    try:
        result = await asyncio.to_thread(
            approve_card,
            project_name,
            card_id,
            feedback
        )
        return TextContent(type="text", text=f"Card approved: {result}")
    except Exception as e:
        return TextContent(type="text", text=f"Error approving card: {str(e)}")

async def _handle_list_files(args: dict) -> TextContent:
    """List project files"""
    project_name = args.get("project_name")
    artifact_type = args.get("artifact_type", "all")
    
    try:
        projects_dir = SCRIPT_DIR / "projects" / project_name
        if not projects_dir.exists():
            return TextContent(type="text", text=f"Project directory not found: {project_name}")
        
        files = []
        if artifact_type in ["brd", "all"]:
            brd_dir = projects_dir / "brd"
            if brd_dir.exists():
                files.extend([f"brd/{f.name}" for f in brd_dir.glob("*")])
        
        if artifact_type in ["fds", "all"]:
            fds_dir = projects_dir / "fds"
            if fds_dir.exists():
                files.extend([f"fds/{f.name}" for f in fds_dir.glob("*")])
        
        if artifact_type in ["tds", "all"]:
            tds_dir = projects_dir / "tds"
            if tds_dir.exists():
                files.extend([f"tds/{f.name}" for f in tds_dir.glob("*")])
        
        if artifact_type in ["cards", "all"]:
            cards_dir = projects_dir / "cards"
            if cards_dir.exists():
                files.extend([f"cards/{f.name}" for f in cards_dir.glob("*")])
        
        if artifact_type in ["estimation", "all"]:
            est_dir = projects_dir / "estimation"
            if est_dir.exists():
                files.extend([f"estimation/{f.name}" for f in est_dir.glob("*")])
        
        if artifact_type in ["tests", "all"]:
            test_dir = projects_dir / "tests"
            if test_dir.exists():
                files.extend([f"tests/{f.name}" for f in test_dir.glob("*")])
        
        return TextContent(
            type="text",
            text=f"Files in {project_name}:\n" + "\n".join(files) if files else "No files found"
        )
    except Exception as e:
        return TextContent(type="text", text=f"Error listing files: {str(e)}")

# ── Resource definitions (optional) ──────────────────────────────

@server.list_tools()
async def list_tools() -> list[Tool]:
    """List available MCP tools"""
    return TOOLS

# ── Main entry point ────────────────────────────────────────────

async def main():
    """Start the MCP server"""
    print("🚀 Starting Project AI Assistant MCP Server...")
    print(f"📁 Project directory: {SCRIPT_DIR}")
    print(f"📋 Available tools: {len(TOOLS)}")
    for tool in TOOLS:
        print(f"   - {tool.name}: {tool.description}")
    print("\n✅ Server is running and ready for MCP clients to connect")
    
    async with server:
        await server.wait_for_shutdown()

if __name__ == "__main__":
    asyncio.run(main())
