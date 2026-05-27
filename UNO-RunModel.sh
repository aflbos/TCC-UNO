#!/bin/bash
cd "$(dirname "$0")"
./UNO-RunModel --algo maskableppo --num-envs 1 --host localhost --start-port 5000 --device cpu --max-steps 0 --model-path "modelo.zip"