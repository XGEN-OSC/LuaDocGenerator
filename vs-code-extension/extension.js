const vscode = require('vscode');
const path = require('path');
const { spawn } = require('child_process');
const fs = require('fs');
const { config } = require('process');

async function runLuaDoc(context) {
    const jarPath = path.join(context.extensionPath, 'LuaDocGenerator.jar');
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
        vscode.window.showInformationMessage("No active editor");
        return;
    }

    const filePath = editor.document.uri.fsPath;
    const process = spawn('java', ['-jar', jarPath, '--project', filePath, 'output.json'], {
        cwd: vscode.workspace.rootPath
    });

    process.stdout.on('data', (data) => {
        console.log(`stdout: ${data}`);
    });

    process.stderr.on('data', (data) => {
        console.error(`stderr: ${data}`);
    });



    process.on('close', async (code) => {
        if (code !== 0) {
            vscode.window.showErrorMessage(`LuaDocGenerator failed with code ${code}`);
            return;
        }

        const generatedFile = path.join(vscode.workspace.rootPath, "output.json");

        // If output.json exists, update docviewconfig.js to point at it and open the viewer
        if (fs.existsSync(generatedFile)) {
            try {
                const configPath = path.join(context.extensionPath, 'docviewconfig.js');

                const content = `const JSON_CONTENT = ${fs.readFileSync(generatedFile, 'utf8')};`;
                fs.writeFileSync(configPath, content, { encoding: 'utf8' });

                // Open the viewer HTML in the default browser
                const indexPath = path.join(context.extensionPath, 'index.html');
                await vscode.env.openExternal(vscode.Uri.file(indexPath));
            } catch (err) {
                vscode.window.showInformationMessage(
                    `Error updating docviewconfig.js: ${err.message}`
                );
                const doc = await vscode.workspace.openTextDocument(generatedFile);
                vscode.window.showTextDocument(doc);
            }
        }
    });
}

function activate(context) {
    // Command for manual triggering (also used by the button)
    let disposable = vscode.commands.registerCommand('extension.generateLuaDocs', () => {
        runLuaDoc(context);
    });
    context.subscriptions.push(disposable);

    // Add editor title button for project.json files
    vscode.window.registerUriHandler({
        handleUri(uri) {
            // Not used here, placeholder if needed for custom links
        }
    });

    // No extra code needed; button is configured via package.json
}

function deactivate() {}

module.exports = {
    activate,
    deactivate
};
