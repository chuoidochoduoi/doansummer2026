import re
import os

html_path = 'd:/gitlap/doAnSummer2026/build/reports/jacoco/test/html/org.example.doansummer2026.service/PatientJourneyService.java.html'
if not os.path.exists(html_path):
    print('File not found')
    exit(1)

with open(html_path, 'r', encoding='utf-8') as f:
    content = f.read()

matches = re.finditer(r'<span class="([^"]+)" id="L(\d+)"(?: title="([^"]+)")?>', content)
for m in matches:
    cls = m.group(1)
    line = m.group(2)
    title = m.group(3)
    if 'nc' in cls.split():
        print(f'Line {line}: NOT COVERED')
    elif 'pc' in cls.split():
        print(f'Line {line}: PARTIALLY COVERED ({title})')
