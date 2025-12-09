#!/bin/bash

################################################################################
# Cleanup Script for SharkNetMessenger Test Scenarios
# Removes test scenario directories after tests complete
################################################################################

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Cleaning up test scenario directories..."

if [[ -d "$SCRIPT_DIR/basic" ]]; then
  echo "Removing basic test scenarios..."
  find "$SCRIPT_DIR/basic" -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} + 2>/dev/null || true
fi

if [[ -d "$SCRIPT_DIR/complex" ]]; then
  echo "Removing complex test scenarios..."
  find "$SCRIPT_DIR/complex" -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} + 2>/dev/null || true
fi

if [[ -d "$SCRIPT_DIR/hub" ]]; then
  echo "Removing hub test scenarios..."
  find "$SCRIPT_DIR/hub" -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} + 2>/dev/null || true
fi

if [[ -d "$SCRIPT_DIR/TCPChain" ]]; then
  echo "Removing TCPChain directories..."
  rm -rf "$SCRIPT_DIR/TCPChain" 2>/dev/null || true
fi

echo "Cleanup complete - test scenario directories removed"

