#!/usr/bin/env bash
# runSingleScenario.sh
# Copy this script into any scenario directory and run it there to execute only that scenario.
# Usage: ./runSingleScenario.sh [IP]

set -euo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
folderName="$(basename "$SCRIPT_DIR")"
# strip single trailing underscore if present
folderName="${folderName%_}"

# Default IP to localhost if not supplied so replacement happens by default
ip="${1:-localhost}"

# Verbose output if set (export VERBOSE=1 to enable)
VERBOSE="${VERBOSE:-0}"

SCENARIO_WAIT_TIMEOUT="${SCENARIO_WAIT_TIMEOUT:-60}" # seconds to wait for dir to be free after processes exit

# helper: macOS vs linux sed - edit in-place
sed_inplace() {
  local expr="$1"
  local file="$2"
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' -e "$expr" "$file" || true
  else
    sed -i -e "$expr" "$file" || true
  fi
}

# Locate SharkNetMessengerCLI.jar in current directory or up to 3 parent directories
find_cli_jar() {
  local dir="$SCRIPT_DIR"
  for _ in 0 1 2 3; do
    if [[ -f "$dir/SharkNetMessengerCLI.jar" ]]; then
      echo "$dir/SharkNetMessengerCLI.jar"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

CLI_JAR_PATH=""
if cli_path=$(find_cli_jar); then
  CLI_JAR_PATH="$cli_path"
else
  echo "SharkNetMessengerCLI.jar not found in this directory or up to 3 parents. Please copy it here or run from a location with the jar." >&2
  exit 1
fi

# Escape IP for safe sed usage (escape / and &)
ip_escaped=$(printf '%s' "$ip" | sed 's/[\/&]/\\&/g')

# Replace FILLER_IP tokens (always run; default is localhost)
# Use nullglob so the glob expands to nothing when there are no matches
shopt -s nullglob
for peer_file in "$SCRIPT_DIR"/Peer*/${folderName}_Peer*.txt; do
  if [[ -f "$peer_file" ]]; then
    sed_inplace "s/FILLER_IP/$ip_escaped/g" "$peer_file"
    if [[ "$VERBOSE" == "1" ]]; then
      echo "Replaced FILLER_IP in: $peer_file"
    fi
  fi
done
shopt -u nullglob

# Create a filler file and replace FILLER_FILENAME tokens
FILLER_SIZE="${FILLER_SIZE:-1024}"
FILLER_NAME="${FILLER_NAME:-${folderName}_filler.txt}"
FILLER_PATH="$SCRIPT_DIR/PeerA/$FILLER_NAME"
mkdir -p "$SCRIPT_DIR/PeerA" 2>/dev/null || true

if command -v truncate >/dev/null 2>&1; then
  truncate -s "$FILLER_SIZE" "$FILLER_PATH" || :
elif command -v fallocate >/dev/null 2>&1; then
  fallocate -l "$FILLER_SIZE" "$FILLER_PATH" || :
elif command -v dd >/dev/null 2>&1; then
  dd if=/dev/zero of="$FILLER_PATH" bs=1 count="$FILLER_SIZE" status=none || :
else
  head -c "$FILLER_SIZE" </dev/zero > "$FILLER_PATH" || :
fi

if [[ "$VERBOSE" == "1" ]]; then
  echo "Replacing FILLER_FILENAME tokens with $FILLER_NAME..."
fi

shopt -s nullglob
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    target="$peer_dir/${folderName}_${peer_name}.txt"
    if [[ -f "$target" ]]; then
      sed_inplace "s/FILLER_FILENAME/$FILLER_NAME/g" "$target"
      if [[ "$VERBOSE" == "1" ]]; then
        echo "Replaced FILLER_FILENAME in: $target"
      fi
    fi
  fi
done
shopt -u nullglob

# Remove decimal fractions in numeric tokens (e.g., 1100.0 -> 1100), but avoid touching IP dotted-quads
# Uses Perl with lookarounds so it won't alter parts of IP addresses
# This runs on every peer command file
shopt -s nullglob
for peer_file in "$SCRIPT_DIR"/Peer*/${folderName}_Peer*.txt; do
  if [[ -f "$peer_file" ]]; then
    if [[ "$VERBOSE" == "1" ]]; then
      echo "Stripping decimal fractions in: $peer_file"
    fi
    perl -pe 's/(?<!\d\.)\b([0-9]+)\.[0-9]+\b(?!\.\d)/$1/g' "$peer_file" > "$peer_file.tmp" && mv "$peer_file.tmp" "$peer_file"
  fi
done
shopt -u nullglob

# Ensure we have at least one Peer directory
found_any=false
shopt -s nullglob
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    found_any=true
    break
  fi
done
shopt -u nullglob

if [[ "$found_any" == "false" ]]; then
  echo "No Peer* directories found in $SCRIPT_DIR" >&2
  exit 1
fi

# Helper to wait until no processes are using files in dir
wait_for_dir_free() {
  local dir="$SCRIPT_DIR"
  local waited=0
  if command -v lsof >/dev/null 2>&1; then
    while lsof +D "$dir" >/dev/null 2>&1; do
      sleep 1
      waited=$((waited+1))
      if (( waited >= SCENARIO_WAIT_TIMEOUT )); then
        echo "Warning: wait_for_dir_free timed out after ${SCENARIO_WAIT_TIMEOUT}s for $dir" >&2
        break
      fi
    done
  else
    # Fallback: check for java processes that mention the scenario dir
    while ps -eo pid=,command= | grep -F "SharkNetMessengerCLI.jar" | grep -F "$dir" >/dev/null 2>&1; do
      sleep 1
      waited=$((waited+1))
      if (( waited >= SCENARIO_WAIT_TIMEOUT )); then
        echo "Warning: fallback wait_for_dir_free timed out after ${SCENARIO_WAIT_TIMEOUT}s for $dir" >&2
        break
      fi
    done
  fi
}

# Decide hub vs tcp by presence of HubHost.txt
if [[ -f "$SCRIPT_DIR/HubHost.txt" ]]; then
  echo "Detected hub scenario (HubHost.txt present). Starting hub + peers..."
  pids=()
  # start hub
  (cd "$SCRIPT_DIR" && cat HubHost.txt | java -jar "$CLI_JAR_PATH" Hub > hubHostsnmLog.txt 2>&1) &
  pids+=("$!")
  # start peers
  shopt -s nullglob
  for peer_dir in "$SCRIPT_DIR"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      peer_letter="${peer_name#Peer}"
      (cd "$peer_dir" && cat "${folderName}_${peer_name}.txt" | java -jar "$CLI_JAR_PATH" "$peer_name" > "peer${peer_letter}snmLog.txt" 2>"errorlog${peer_letter}.txt") &
      pids+=("$!")
    fi
  done
  shopt -u nullglob
  # wait
  echo "Waiting for hub and peer processes to finish..."
  wait "${pids[@]}" || true
  # additional safeguard
  wait_for_dir_free
else
  echo "Detected TCP-only scenario (no HubHost.txt). Starting peers..."
  pids=()
  shopt -s nullglob
  for peer_dir in "$SCRIPT_DIR"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      peer_letter="${peer_name#Peer}"
      (cd "$peer_dir" && cat "${folderName}_${peer_name}.txt" | java -jar "$CLI_JAR_PATH" "$peer_name" > "peer${peer_letter}snmLog.txt" 2>"errorlog${peer_letter}.txt") &
      pids+=("$!")
    fi
  done
  shopt -u nullglob
  echo "Waiting for peer processes to finish..."
  wait "${pids[@]}" || true
  wait_for_dir_free
fi

# Produce a simple eval summary (same style as other runners)
eval_file="$SCRIPT_DIR/eval_local.txt"
{
  echo "$folderName"
  name_lc="$(printf '%s' "$folderName" | tr '[:upper:]' '[:lower:]')"
  containsDis="no"
  if [[ "$name_lc" == *dis* ]]; then
    containsDis="yes"
  fi

  all_pass=true
  shopt -s nullglob
  for peer_dir in "$SCRIPT_DIR"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      peer_letter="${peer_name#Peer}"
      snm_log="$peer_dir/peer${peer_letter}snmLog.txt"

      if [[ -f "$snm_log" ]]; then
        count=$(grep -F -i -c "$folderName" "$snm_log" 2>/dev/null || true)
        if (( count >= 1 )); then
          echo "$peer_name: pass: Message seen in log."
        else
          if [[ "$containsDis" == "yes" ]]; then
            echo "$peer_name: fail: One or both messages failed."
            all_pass=false
          elif grep -Fiq "$folderName" "$snm_log"; then
            echo "$peer_name: pass: Message sent/received successfully."
          else
            echo "$peer_name: fail"
            all_pass=false
          fi
        fi
      else
        echo "$peer_name: fail (peer${peer_letter}snmLog.txt missing)"
        all_pass=false
      fi
    fi
  done
  shopt -u nullglob

  echo "$folderName"
  if [[ "$all_pass" == "true" ]]; then
    echo "PASS: All peers completed successfully"
  else
    echo "FAIL: One or more peers failed"
  fi
} > "$eval_file"

echo "Scenario complete. eval_local.txt written."
