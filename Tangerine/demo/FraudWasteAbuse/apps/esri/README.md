
Notes on Running ESRI components for Tangerine
==============================
```
from ./apps folder:
pip install --target piplib requests
export PYTHONPATH=$PWD/piplib

export http_proxy=http://myproxy:80
export https_proxy=https://myproxy:80

python esri.py --to "500 Sea World Dr, San Diego, CA" --from "2121 San Diego Ave, 92110"
# With --output FILE, output is saved in FILE.
#


```
