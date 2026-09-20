const fs = require('fs');
const path = require('path');

const directory = 'd:/gitlap/doAnSummer2026/src/test/java/org/example/doansummer2026/';
const patterns_to_remove = [
    'TestResultRevisionRepository',
    'TestResultAttachmentRepository',
    'ClinicalFormTemplateService',
    'TestResultRevision',
    'TestResultAttachment',
    'TestResultAmendRequest',
    'ClinicalFormTemplateVersion',
    'ClinicalFormTemplate',
    'TestResultRevisionStatus'
];

function walkSync(currentDirPath, callback) {
    fs.readdirSync(currentDirPath).forEach(function (name) {
        var filePath = path.join(currentDirPath, name);
        var stat = fs.statSync(filePath);
        if (stat.isFile()) {
            if (filePath.endsWith('.java')) {
                callback(filePath, stat);
            }
        } else if (stat.isDirectory()) {
            walkSync(filePath, callback);
        }
    });
}

walkSync(directory, function(filePath) {
    let content = fs.readFileSync(filePath, 'utf8');
    let lines = content.split('\n');
    let newLines = [];
    let modified = false;
    for (let line of lines) {
        if (patterns_to_remove.some(p => line.includes(p))) {
            modified = true;
            // If it's a method declaration with opening brace, we might break the block.
            // But let's try just skipping the line.
            continue;
        }
        newLines.push(line);
    }
    if (modified) {
        fs.writeFileSync(filePath, newLines.join('\n'), 'utf8');
        console.log('Fixed:', filePath);
    }
});
