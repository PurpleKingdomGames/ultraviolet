#!/usr/bin/env bash

# Run from root.

sbt crossCleanAll crossTestUltravioletOnlyNoClean crossLocalPublishNoClean
