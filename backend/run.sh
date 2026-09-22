#!/usr/bin/env bash
python3 -m pip install -r requirements.txt
python3 -m uvicorn server:APP --host 0.0.0.0 --port 5000
