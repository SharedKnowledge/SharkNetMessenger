#!/usr/bin/env bash
# kill_shark_processes.sh
# Find and kill SharkNet Messenger processes (java processes running SharkNetMessengerCLI.jar).
# Supports targeting by scenario directory (processes that hold files under a directory) or all SharkNet processes.

set -euo pipefail
IFS=$'\n\t'

PROG_NAME=$(basename "$0")

usage() {
  cat <<EOF
Usage: $PROG_NAME [options]

Options:
  -d DIR    Target only processes using files under DIR (uses lsof). Can be specified multiple times.
  -a        Target all SharkNet processes (default if -d not provided).
  -n        Dry-run: show matching processes but do not kill.
  -t SEC    Timeout in seconds to wait after SIGTERM before sending SIGKILL (default 10).
  -f        Force (send SIGKILL immediately instead of SIGTERM). Implies non-interactive.
  -v        Verbose output.
  -h        Show this help.

Examples:
  # Dry-run: show all SharkNet processes
  $PROG_NAME -n

  # Kill processes that hold files under the current scenario dir, wait 20s for graceful exit
  $PROG_NAME -d . -t 20

  # Kill all SharkNet processes immediately (force)
  $PROG_NAME -a -f

EOF
}

# Defaults
declare -a DIRS=()
DRY_RUN=0
TARGET_ALL=0
TIMEOUT=${TIMEOUT:-10}
FORCE=0
VERBOSE=0

# Parse opts
while getopts ":d:ant:vfsh" opt; do
  case "$opt" in
    d) DIRS+=("${OPTARG}") ;;
    a) TARGET_ALL=1 ;;
    n) DRY_RUN=1 ;;
    t) TIMEOUT="${OPTARG}" ;;
    f) FORCE=1 ;;
    v) VERBOSE=1 ;;
    h) usage; exit 0 ;;
    :) echo "Missing arg for -$OPTARG" >&2; usage; exit 2 ;;
    \?) echo "Unknown option: -$OPTARG" >&2; usage; exit 2 ;;
  esac
done
shift $((OPTIND-1))

# Helper: get command line for PID
cmd_for_pid() {
  local pid=$1
  if ps -p "$pid" -o command= >/dev/null 2>&1; then
    ps -p "$pid" -o command= | sed -e 's/^\s*//'
  else
    echo "(cmd unavailable)"
  fi
}

# Collect candidate PIDs
# NOTE: macOS ships with bash 3 which doesn't support associative arrays (declare -A).
# Use a portable array and helper to keep a unique list of PIDs.
declare -a pid_list=()
add_pid() {
  local p="$1"
  # skip empty
  [[ -z "$p" ]] && return 1
  # ensure numeric
  [[ ! "$p" =~ ^[0-9]+$ ]] && return 1
  for _existing in "${pid_list[@]:-}"; do
    [[ "$_existing" == "$p" ]] && return 0
  done
  pid_list+=("$p")
}

# If DIRs provided, try lsof to find processes with open files under DIR
if (( ${#DIRS[@]} > 0 )); then
  for d in "${DIRS[@]}"; do
    if [[ ! -d "$d" && ! -f "$d" ]]; then
      echo "Warning: target path does not exist: $d" >&2
      continue
    fi
    if command -v lsof >/dev/null 2>&1; then
      # lsof +D can be slow for big trees; use it but handle failures gracefully
      if lsof +D "$d" >/dev/null 2>&1; then
        while read -r pid _; do
          # Only include java processes that run SharkNetMessengerCLI.jar
          if [[ -n "$pid" ]]; then
            if ps -p "$pid" -o command= | grep -F "SharkNetMessengerCLI.jar" >/dev/null 2>&1; then
              add_pid "$pid"
            fi
          fi
        done < <(lsof +D "$d" 2>/dev/null | awk '{print $2, $1}')
      else
        # lsof succeeded but returned nothing or couldn't access; fallback to pgrep inspection
        if command -v pgrep >/dev/null 2>&1; then
          while read -r pid; do
            if ps -p "$pid" -o command= | grep -F "$d" >/dev/null 2>&1; then
              add_pid "$pid"
            fi
          done < <(pgrep -f "SharkNetMessengerCLI.jar" || true)
        fi
      fi
    else
      # lsof not available; use pgrep + inspect cmdline
      if command -v pgrep >/dev/null 2>&1; then
        while read -r pid; do
          if ps -p "$pid" -o command= | grep -F "$d" >/dev/null 2>&1; then
            add_pid "$pid"
          fi
        done < <(pgrep -f "SharkNetMessengerCLI.jar" || true)
      fi
    fi
  done
fi

# If targeting all or nothing collected yet, add all SharkNet processes
if [[ $TARGET_ALL -eq 1 || ${#pid_list[@]} -eq 0 ]]; then
  if command -v pgrep >/dev/null 2>&1; then
    while read -r pid; do
      add_pid "$pid"
    done < <(pgrep -f "SharkNetMessengerCLI.jar" || true)
  else
    # fallback: parse ps output
    while read -r pid cmd; do
      if [[ "$cmd" == *SharkNetMessengerCLI.jar* ]]; then
        add_pid "$pid"
      fi
    done < <(ps -eo pid=,command=)
  fi
fi

# Build PID list (filter numeric and existing)
pids=()
for pid in "${pid_list[@]:-}"; do
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" >/dev/null 2>&1; then
    pids+=("$pid")
  fi
done

if (( ${#pids[@]} == 0 )); then
  echo "No SharkNetMessengerCLI.jar processes found (no matching PIDs)." >&2
  exit 0
fi

# Print matching processes
echo "Found ${#pids[@]} matching process(es):"
for pid in "${pids[@]}"; do
  printf "  PID %s: %s\n" "$pid" "$(cmd_for_pid "$pid")"
done

if [[ $DRY_RUN -eq 1 ]]; then
  echo "Dry-run: not killing anything. Use without -n to kill." >&2
  exit 0
fi

# Kill logic
if [[ $FORCE -eq 1 ]]; then
  echo "Force: sending SIGKILL to all matched processes..."
  kill -9 "${pids[@]}" 2>/dev/null || true
  echo "SIGKILL sent."
  exit 0
fi

# Send SIGTERM then wait up to TIMEOUT seconds
echo "Sending SIGTERM to matched processes..."
kill -TERM "${pids[@]}" 2>/dev/null || true

# Wait for them to exit
end=$((SECONDS + TIMEOUT))
while (( SECONDS < end )); do
  still=()
  for pid in "${pids[@]}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      still+=("$pid")
    fi
  done
  if (( ${#still[@]} == 0 )); then
    echo "All processes exited cleanly after SIGTERM."
    exit 0
  fi
  sleep 1
done

# After timeout, prompt and optionally SIGKILL
echo "Timeout reached. The following PIDs did not exit:" >&2
for pid in "${pids[@]}"; do
  if kill -0 "$pid" >/dev/null 2>&1; then
    printf "  PID %s: %s\n" "$pid" "$(cmd_for_pid "$pid")" >&2
  fi
done

# If script is non-interactive or user requested, send SIGKILL
if [[ -t 0 ]]; then
  read -r -p "Send SIGKILL to these processes? [y/N]: " reply || true
  reply=${reply:-N}
  if [[ "$reply" =~ ^[Yy]$ ]]; then
    kill -9 "${pids[@]}" 2>/dev/null || true
    echo "SIGKILL sent." >&2
  else
    echo "Leaving processes running." >&2
  fi
else
  echo "Non-interactive shell; not sending SIGKILL. Rerun with -f to force kill." >&2
fi

exit 0
