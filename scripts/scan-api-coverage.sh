#!/usr/bin/env bash
# V3.5.17+ API 接口覆盖率扫描
green()  { echo -e "\033[32m$*\033[0m"; }
red()    { echo -e "\033[31m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }
echo "═══════════════════════════════════════════════════════════"
echo "  V3.5.17+ API 接口覆盖率扫描"
echo "═══════════════════════════════════════════════════════════"

# 1. 前端 endpoint
FE_ENDPOINTS=$(grep -rhoE "http\.(get|post|put|delete|patch)\(\s*['\"\`]([^'\"\`]+)['\"\`]" frontend/src/api/ 2>/dev/null | \
    sed -E "s/.*['\"\`]([^'\"\`]+)['\"\`].*/\1/" | sort -u | grep "^/")
FE_COUNT=$(echo "$FE_ENDPOINTS" | wc -l)
echo "  前端 endpoint: $FE_COUNT 个"

# 2. 后端 endpoint (class+method, {id} → PLACEHOLDER)
python3 > /tmp/be_norm.txt << 'PYEOF'
from pathlib import Path
import re
endpoints = set()
for f in Path('backend').rglob('*Controller.java'):
    try: c = f.read_text()
    except: continue
    for cm in re.finditer(r'public\s+(?:class|interface)\s+\w+', c):
        cls_start = cm.start()
        before = c[:cls_start]
        last_rm = None
        for m in re.finditer(r'@RequestMapping\s*(?:\(\s*["\']([^"\']+)["\']\s*\))?', before):
            last_rm = m
        if not (last_rm and last_rm.group(1)): continue
        cp = last_rm.group(1)
        next_cls = re.search(r'public\s+(?:class|interface)\s+\w+', c[cls_start+1:])
        end = cls_start + 1 + next_cls.start() if next_cls else len(c)
        body = c[cls_start:end]
        for m in re.finditer(r'@(?:Post|Get|Put|Delete|Patch)Mapping[^\n]*', body):
            line = m.group(0)
            pm = re.search(r'value\s*=\s*["\']([^"]*)["\']', line)
            if not pm:
                pm = re.search(r'\(\s*["\']([^"\']*)["\']', line)
            if pm:
                mp = pm.group(1)
                full = (cp.rstrip('/') + '/' + mp.lstrip('/')) if mp else cp
                full = re.sub(r'/\{[^}]+\}', '/PLACEHOLDER', full)
                endpoints.add(full)
for e in sorted(endpoints): print(e)
PYEOF
BE_COUNT=$(wc -l < /tmp/be_norm.txt)
echo "  后端 endpoint: $BE_COUNT 个"
echo ""

# 3. 匹配
MATCHED=0; MISSING=0
for ep in $FE_ENDPOINTS; do
    norm=$(echo "$ep" | sed -E 's|/\$\{[^}]+\}|/PLACEHOLDER|g; s|\?.*||')
    if grep -qF "$norm" /tmp/be_norm.txt 2>/dev/null; then
        MATCHED=$((MATCHED+1))
    else
        MISSING=$((MISSING+1))
        [[ $MISSING -le 20 ]] && yellow "  未匹配: $ep → $norm"
    fi
done
TOTAL=$((MATCHED + MISSING))
COVERAGE=$((MATCHED * 100 / TOTAL))
echo "  匹配:   $MATCHED / $TOTAL"
echo "  覆盖率: $COVERAGE%"

# 4. 后端孤岛
ORPHAN=0
while IFS= read -r ep; do
    if ! echo "$FE_ENDPOINTS" | grep -qF "$ep" 2>/dev/null; then
        ORPHAN=$((ORPHAN+1))
    fi
done < /tmp/be_norm.txt
echo "  后端孤岛: $ORPHAN / $BE_COUNT"
echo "═══════════════════════════════════════════════════════════"
