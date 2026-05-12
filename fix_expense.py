import os
path = 'app/src/main/java/com/chibychibystore/ui/expense/ExpenseListScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Replace any direct format(startDate) or format(sDate) with safe handling
import re
content = re.sub(r'dateRangeFormatter\.format\((s?tartDate)\)', r'\1?.let { dateRangeFormatter.format(it) } ?: ""', content)
content = re.sub(r'dateRangeFormatter\.format\((e?ndDate)\)', r'\1?.let { dateRangeFormatter.format(it) } ?: ""', content)

with open(path, 'w') as f:
    f.write(content)
