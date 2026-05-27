# How to Add MCP Server Config to VS Code Settings

## 3 Easy Steps

---

## Step 1: Open Settings JSON

In VS Code, press: `Ctrl+Shift+P`

You'll see a search box at the top:
```
>
```

Type: `settings.json`

Click on: **"Preferences: Open Settings (JSON)"**

---

## Step 2: Find Where to Add the Config

Your settings.json will open. It might look like:

```json
{
  "[python]": {
    "editor.defaultFormatter": "ms-python.python",
    "editor.formatOnSave": true
  },
  "python.analysis.typeCheckingMode": "basic"
}
```

**You need to add the MCP config inside the `{ }` braces.**

If the file ends with `}`, add a comma before it and add the new config.

---

## Step 3: Add This Config

**Find the last setting** (before the closing `}`) and add a comma, then paste this:

```json
  "github.copilot.chat.mcpServers": {
    "project-ai": {
      "command": "python",
      "args": [
        "C:\\Users\\abhishe6\\Downloads\\t_n\\Transcripts_final\\Transcripts\\mcp_server_github.py"
      ]
    }
  }
```

---

## Example: Complete settings.json

If your file was:
```json
{
  "[python]": {
    "editor.defaultFormatter": "ms-python.python"
  }
}
```

After adding MCP config, it should look like:
```json
{
  "[python]": {
    "editor.defaultFormatter": "ms-python.python"
  },
  "github.copilot.chat.mcpServers": {
    "project-ai": {
      "command": "python",
      "args": [
        "C:\\Users\\abhishe6\\Downloads\\t_n\\Transcripts_final\\Transcripts\\mcp_server_github.py"
      ]
    }
  }
}
```

---

## ⚠️ Important: Commas and Braces

- Each setting needs a comma after it (except the LAST one before `}`)
- Make sure all `{` have matching `}`
- Make sure all `[` have matching `]`

If you see a red squiggly line, there's a syntax error. Check your commas and braces!

---

## Step 4: Save

Press: `Ctrl+S`

You should see the file is saved (no dot on the tab).

---

## Step 5: Restart VS Code

Close VS Code completely:
1. Click the X button
2. Wait 2 seconds
3. Reopen VS Code

---

## Step 6: Verify It Worked

1. Make sure MCP server is still running in PowerShell (you should see the terminal with the server output)
2. Open Copilot Chat: `Ctrl+Shift+I`
3. Test: Type `List my GitHub repositories`

**If it works**, you'll see:
- Your repos list OR
- "No repositories found" (both are success!)

**If it doesn't work**, you'll see something like:
- "I don't have a tool to..." (means MCP not connected)

---

## 🆘 Troubleshooting

### Error: "Invalid JSON"
- Check your commas
- Make sure you didn't accidentally edit the syntax
- Copy the exact config from above (check spacing)

### MCP Server Still Not Connecting
1. Is the PowerShell terminal with `mcp_server_github.py` still running?
2. Is the Python path correct? (should be: `C:\Users\abhishe6\Downloads\t_n\Transcripts_final\Transcripts\mcp_server_github.py`)
3. Did you restart VS Code after saving settings.json?

### Python Path is Wrong
If you're not sure of your exact path:
1. Open PowerShell
2. Run: `pwd` (shows your current directory)
3. It should be: `C:\Users\abhishe6\Downloads\t_n\Transcripts_final\Transcripts`
4. Verify this matches the path in settings.json

---

## ✅ Success Indicators

When configured correctly:
- ✅ MCP server running in terminal
- ✅ VS Code restarted
- ✅ Copilot Chat responds to "List my GitHub repositories"
- ✅ You see actual results instead of "I don't have a tool..."

---

## Video Steps (If Stuck)

1. `Ctrl+Shift+P`
2. Type `settings`
3. Click `Preferences: Open Settings (JSON)`
4. Scroll to the end (before the last `}`)
5. Add comma after last setting
6. Paste the MCP config
7. `Ctrl+S` to save
8. Close and reopen VS Code
9. Test in Copilot Chat

---

**Done! Now test it out!** 🚀
