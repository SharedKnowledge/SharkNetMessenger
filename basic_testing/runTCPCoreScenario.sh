#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
folderName="$(basename "$SCRIPT_DIR")"
# strip single trailing underscore
folderName="${folderName%_}"
ip="${1:-localhost}"

if [[ $# -gt 1 ]]; then
  echo "Too many arguments supplied"
  exit 1
fi

# replace FILLER_IP in ALL peer files when IP provided (macOS vs Linux sed)
if [[ $# -eq 1 ]]; then
  # Find all peer command files and replace FILLER_IP in each one
  for peer_file in "$SCRIPT_DIR"/Peer*/${folderName}_Peer*.txt; do
    if [[ -f "$peer_file" ]]; then
      if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' -e "s/FILLER_IP/$ip/g" "$peer_file" || true
      else
        sed -i -e "s/FILLER_IP/$ip/g" "$peer_file" || true
      fi
    fi
  done
fi

# create filler files only for peers whose command lists actually reference FILLER_FILENAME
# configurable via environment variables:
# FILLER_SIZE (bytes, default 1024)
# FILLER_NAME (default: <folderName>_filler.txt)
FILLER_SIZE="${FILLER_SIZE:-1024}"
FILLER_NAME="${FILLER_NAME:-${folderName}_filler.txt}"
# Verbose for filler creation
FILLER_VERBOSE="${FILLER_VERBOSE:-0}"

# Determine which peer directories need a filler file (if their command file contains FILLER_FILENAME)
shopt -s nullglob
peers_need_filler=()
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    target="$peer_dir/${folderName}_${peer_name}.txt"
    if [[ -f "$target" ]]; then
      if grep -Fq "FILLER_FILENAME" "$target" 2>/dev/null || grep -Fq "sendMessage" "$target" 2>/dev/null; then
        peers_need_filler+=("$peer_dir")
      fi
    fi
  fi
done

# Create filler files where needed
for pd in "${peers_need_filler[@]:-}"; do
  FILLER_PATH="$pd/$FILLER_NAME"
  mkdir -p "$pd" 2>/dev/null || true
  if command -v truncate >/dev/null 2>&1; then
    truncate -s "$FILLER_SIZE" "$FILLER_PATH" || :
  elif command -v fallocate >/dev/null 2>&1; then
    fallocate -l "$FILLER_SIZE" "$FILLER_PATH" || :
  elif command -v dd >/dev/null 2>&1; then
    dd if=/dev/zero of="$FILLER_PATH" bs=1 count="$FILLER_SIZE" status=none || :
  else
    head -c "$FILLER_SIZE" </dev/zero > "$FILLER_PATH" || :
  fi
  if [[ "$FILLER_VERBOSE" == "1" ]]; then
    echo "Created filler: $FILLER_PATH"
  fi
done
shopt -u nullglob

# replace FILLER_FILENAME token in each Peer command list (macOS vs Linux sed)
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    target="$peer_dir/${folderName}_${peer_name}.txt"
    if [[ -f "$target" ]]; then
      if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' -e "s/FILLER_FILENAME/$FILLER_NAME/g" "$target" || true
      else
        sed -i -e "s/FILLER_FILENAME/$FILLER_NAME/g" "$target" || true
      fi
    fi
  fi
done


found_any=false
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    found_any=true
    shopt -s nullglob
    matches=("$peer_dir"/"${folderName}_Peer"*.txt)
    shopt -u nullglob
    if (( ${#matches[@]} == 0 )); then
      echo "Required file ${folderName}_Peer*.txt not found in $peer_dir"
      exit 1
    fi
  fi
done

if [[ "$found_any" == "false" ]]; then
  echo "No peer directories found in $SCRIPT_DIR"
  exit 1
fi

if [[ ! -f "$SCRIPT_DIR/SharkNetMessengerCLI.jar" ]]; then
  echo "Required SharkNetMessengerCLI.jar not found in $SCRIPT_DIR"
  exit 1
fi

pids=()
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    peer_letter="${peer_name#Peer}"
    (cd "$peer_dir" && cat "${folderName}_${peer_name}.txt" | java -jar "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$peer_name" > "peer${peer_letter}snmLog.txt" 2>"errorlog${peer_letter}.txt") &
    pids+=($!)
  fi
done

# Wait for all peer processes to complete
wait "${pids[@]}"

# Additional safeguard: wait until no process is using files in the scenario directory
wait_for_scenario_dir_free() {
  local dir="$SCRIPT_DIR"
  local max_wait=60
  local waited=0

  if command -v lsof >/dev/null 2>&1; then
    # lsof +D can be slow; poll with timeout
    while lsof +D "$dir" >/dev/null 2>&1; do
      sleep 1
      waited=$((waited+1))
      if (( waited >= max_wait )); then
        echo "Warning: wait_for_scenario_dir_free timeout for $dir" >&2
        break
      fi
    done
  else
    # Fallback: look for java processes referencing the scenario directory on their command line
    while ps -eo pid=,command= | grep -F "SharkNetMessengerCLI.jar" | grep -F "$dir" >/dev/null 2>&1; do
      sleep 1
      waited=$((waited+1))
      if (( waited >= max_wait )); then
        echo "Warning: fallback wait_for_scenario_dir_free timeout for $dir" >&2
        break
      fi
    done
  fi
}

wait_for_scenario_dir_free

eval_file="$SCRIPT_DIR/eval_local.txt"
{
  echo "$folderName"

  # portable lowercase for older bash (macOS)
  name_lc="$(printf '%s' "$folderName" | tr '[:upper:]' '[:lower:]')"

  containsDis="no"
  if [[ "$name_lc" == *dis* ]]; then
    containsDis="yes"
  fi

  # Check all peers
  all_pass=true
  for peer_dir in "$SCRIPT_DIR"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      peer_letter="${peer_name#Peer}"
      snm_log="$peer_dir/peer${peer_letter}snmLog.txt"

      if [[ -f "$snm_log" ]]; then
        count=$(grep -F -i -c "$folderName" "$snm_log" 2>/dev/null || true)
        if (( count >= 2 )); then
          echo "$peer_name: pass: Both messages sent/received successfully."
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

  # Summary
  echo "$folderName"
  if [[ "$all_pass" == "true" ]]; then
    echo "PASS: All peers completed successfully"
  else
    echo "FAIL: One or more peers failed"
  fi
} > "$eval_file"

