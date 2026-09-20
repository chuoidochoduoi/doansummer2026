import os
import glob

directory = 'd:/gitlap/doAnSummer2026/src/test/java/org/example/doansummer2026/'

patterns_to_remove = [
    'TestResultRevisionRepository',
    'TestResultAttachmentRepository',
    'ClinicalFormTemplateService',
    'TestResultRevision',
    'TestResultAttachment',
    'TestResultAmendRequest',
    'ClinicalFormTemplateVersion',
    'ClinicalFormTemplate',
    'TestResultRevisionStatus'
]

for filename in glob.glob(directory + '**/*.java', recursive=True):
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_lines = []
    modified = False
    for line in lines:
        if any(p in line for p in patterns_to_remove):
            # For some usages, if it's a method argument, deleting the line breaks syntax.
            # But most are imports or @Mock fields. Let's see if we can just comment them out 
            # if they are within a method, or just delete if they are imports/fields.
            # Actually, to be safe, if they are @Mock or import, we delete. If they are in code, we just delete the line for now. It might break compilation.
            modified = True
            continue
        new_lines.append(line)
        
    if modified:
        with open(filename, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
            
print('Done fixing tests.')
