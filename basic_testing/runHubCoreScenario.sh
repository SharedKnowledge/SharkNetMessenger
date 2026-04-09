#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
folderName="$(basename "$SCRIPT_DIR")"

folderName="${folderName%_}"
ip="${1:-localhost}"

if [[ $# -gt 1 ]]; then
  echo "Too many arguments supplied"
  exit 1
fi

# replace FILLER_IP in ALL peer files using the resolved IP value
# escape characters that sed treats specially in replacement text
ip_escaped=$(printf '%s' "$ip" | sed 's/[\/&]/\\&/g')
for peer_file in "$SCRIPT_DIR"/Peer*/${folderName}_Peer*.txt; do
  if [[ -f "$peer_file" ]]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
      sed -i '' -e "s/FILLER_IP/$ip_escaped/g" "$peer_file" || true
    else
      sed -i -e "s/FILLER_IP/$ip_escaped/g" "$peer_file" || true
    fi
  fi
done

# configurable via environment variables:
# FILLER_SIZE (bytes, default 1024)
# FILLER_NAME (default: <folderName>_filler.txt)
FILLER_SIZE="${FILLER_SIZE:-1024}"
FILLER_NAME="${FILLER_NAME:-${folderName}_filler.txt}"
FILLER_PATH="$SCRIPT_DIR/PeerA/$FILLER_NAME"
mkdir -p "$SCRIPT_DIR/PeerA" 2>/dev/null || true

# Prefer truncate. Fallback to fallocate, then dd from /dev/zero, then head from /dev/zero.
# If multiple peers reference FILLER_FILENAME, create per-peer files below and replace accordingly.
if command -v truncate >/dev/null 2>&1; then
  truncate -s "$FILLER_SIZE" "$FILLER_PATH" || :
elif command -v fallocate >/dev/null 2>&1; then
  fallocate -l "$FILLER_SIZE" "$FILLER_PATH" || :
elif command -v dd >/dev/null 2>&1; then
  dd if=/dev/zero of="$FILLER_PATH" bs=1 count="$FILLER_SIZE" status=none || :
else
  head -c "$FILLER_SIZE" </dev/zero > "$FILLER_PATH" || :
fi

# If other peers reference FILLER_FILENAME, create per-peer variants and replace tokens
shopt -s nullglob
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    peer_letter="${peer_name#Peer}"
    target="$peer_dir/${folderName}_${peer_name}.txt"
    if [[ -f "$target" ]]; then
      if grep -Fq "FILLER_FILENAME" "$target" 2>/dev/null; then
        # build per-peer name
        if [[ "$FILLER_NAME" == *.* ]]; then
          base="${FILLER_NAME%.*}"
          ext="${FILLER_NAME##*.}"
          per_name="${base}_${peer_letter}.${ext}"
        else
          per_name="${FILLER_NAME}_${peer_letter}"
        fi
        per_path="$peer_dir/$per_name"
        if [[ ! -f "$per_path" ]]; then
          if [[ -f "$FILLER_PATH" ]]; then
            cp -p "$FILLER_PATH" "$per_path" 2>/dev/null || true
          else
            mkdir -p "$peer_dir" 2>/dev/null || true
            if command -v truncate >/dev/null 2>&1; then
              truncate -s "$FILLER_SIZE" "$per_path" || :
            elif command -v fallocate >/dev/null 2>&1; then
              fallocate -l "$FILLER_SIZE" "$per_path" || :
            elif command -v dd >/dev/null 2>&1; then
              dd if=/dev/zero of="$per_path" bs=1 count="$FILLER_SIZE" status=none || :
            else
              head -c "$FILLER_SIZE" </dev/zero > "$per_path" || :
            fi
          fi
        fi

        if [[ "$OSTYPE" == "darwin"* ]]; then
          sed -i '' -e "s/FILLER_FILENAME/$per_name/g" "$target" || true
        else
          sed -i -e "s/FILLER_FILENAME/$per_name/g" "$target" || true
        fi
      fi
    fi
  fi
done
shopt -u nullglob

# replace FILLER_FILENAME token in the PeerA command list (macOS vs Linux sed)
# Ensure PeerA replacement happens last so PeerA uses the canonical FILLER_NAME (no letter suffix)
if [[ -f "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" ]]; then
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' -e "s/FILLER_FILENAME/$FILLER_NAME/g" "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" || true
  else
    sed -i -e "s/FILLER_FILENAME/$FILLER_NAME/g" "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" || true
  fi
fi

# Check that at least PeerA and PeerB exist
if [[ ! -f "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" || ! -f "$SCRIPT_DIR/PeerB/${folderName}_PeerB.txt" ]]; then
  echo "Required peer files (PeerA and PeerB) missing in $SCRIPT_DIR"
  exit 1
fi

if [[ ! -f "$SCRIPT_DIR/SharkNetMessengerCLI.jar" ]]; then
  echo "Required SharkNetMessengerCLI.jar not found in $SCRIPT_DIR"
  exit 1
fi

# Execute all peers (PeerA, PeerB, PeerC, PeerD, etc.) in background
pids=()


# Start all peers
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    peer_letter="${peer_name#Peer}"
    (cd "$peer_dir" && cat "${folderName}_${peer_name}.txt" | java -jar "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$peer_name" > "peer${peer_letter}snmLog.txt" 2>"errorlog${peer_letter}.txt") &
    pids+=($!)
  fi
done

# Wait for all processes to complete
wait "${pids[@]}"

# Additional safeguard: wait until no process is using files in the scenario directory
wait_for_scenario_dir_free() {
  local dir="$SCRIPT_DIR"
  local max_wait=60
  local waited=0

  if command -v lsof >/dev/null 2>&1; then
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

# Return two integers: <remote_unique_payload_messages> <local_unique_payload_messages>
count_scenario_payload_sources() {
  local log_file="$1"
  awk '
    BEGIN {
      in_block = 0
      content = ""
      content_lc = ""
      is_payload = 0
      remote = 0
      local = 0
    }
    /^content type:/ {
      in_block = 1
      content = $0
      sub(/^content type:[[:space:]]*/, "", content)
      content_lc = tolower(content)
      is_payload = (index(content_lc, ".txt") > 0)
      next
    }
    in_block && /^sender:/ {
      if (is_payload) {
        line = tolower($0)
        if (line ~ /^sender:[[:space:]]*you([[:space:]]|\||$)/) {
          if (!(content in seen_local)) {
            seen_local[content] = 1
            local++
          }
        } else {
          if (!(content in seen_remote)) {
            seen_remote[content] = 1
            remote++
          }
        }
      }
      in_block = 0
      is_payload = 0
      content = ""
      content_lc = ""
    }
    END {
      printf "%d %d\n", remote, local
    }
  ' "$log_file" 2>/dev/null
}

first_fatal_error_line() {
  local err_log="$1"
  [[ -s "$err_log" ]] || return 1

  # Ignore benign interruption noise caused during coordinated shutdown.
  grep -Eiv "sleep interrupted|InterruptedException" "$err_log" 2>/dev/null \
    | grep -Ei "Connection refused|there is no encounter|NullPointerException|IllegalStateException|RuntimeException|Exception in thread|^Error:" \
    | head -n 1
}

peer_had_encounter() {
  local snm_log="$1"
  grep -Eiq "new encounter to|open encounter|encounter started|encounter to" "$snm_log" 2>/dev/null
}

eval_file="$SCRIPT_DIR/eval_local.txt"
{
  echo "Scenario: $folderName"
  echo "Peer results:"

  # portable lowercase for older bash (macOS)
  name_lc="$(printf '%s' "$folderName" | tr '[:upper:]' '[:lower:]')"

#  containsDis="no"
#  if [[ "$name_lc" == *dis* ]]; then
#   containsDis="yes"
#  fi

  allowEncounter="no"
  if [[ "$name_lc" == *core1* || "$name_lc" == *core2* ]]; then
    allowEncounter="yes"
  fi

  # Check all peers
  all_pass=true
  for peer_dir in "$SCRIPT_DIR"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      peer_letter="${peer_name#Peer}"
      snm_log="$peer_dir/peer${peer_letter}snmLog.txt"
      err_log="$peer_dir/errorlog${peer_letter}.txt"
      cmd_file="$peer_dir/${folderName}_${peer_name}.txt"

      if [[ -f "$snm_log" ]]; then
        read -r remote_count local_count < <(count_scenario_payload_sources "$snm_log" "$folderName")
        remote_count=${remote_count:-0}
        local_count=${local_count:-0}
        is_sender=false
        if [[ -f "$cmd_file" ]] && grep -Fiq "sendMessage" "$cmd_file" 2>/dev/null; then
          is_sender=true
        fi

        fatal_err_line="$(first_fatal_error_line "$err_log")"
        if [[ -n "$fatal_err_line" ]]; then
          echo "  $peer_name | FAIL | Transport/encounter error detected ($fatal_err_line)."
          all_pass=false
        elif [[ "$is_sender" == "true" ]] && (( local_count >= 1 )); then
          echo "  $peer_name | PASS | Sender peer produced local scenario payloads ($local_count unique)."
        elif peer_had_encounter "$snm_log"; then
          if (( remote_count >= 1 )); then
            echo "  $peer_name | PASS | Encounter observed and remote payloads received ($remote_count unique)."
          else
            echo "  $peer_name | FAIL | Encounter observed but no remote payloads were received."
            all_pass=false
          fi
        elif [[ "$allowEncounter" == "yes" ]] && grep -Fiq "open encounter" "$snm_log"; then
          echo "  $peer_name | PASS | Open encounter established."
        else
          if (( local_count > 0 )); then
            echo "  $peer_name | FAIL | Only self-produced scenario payloads observed (no remote delivery evidence)."
          else
            echo "  $peer_name | FAIL | No scenario payloads observed."
          fi
          all_pass=false
        fi
      else
        echo "  $peer_name | FAIL | peer${peer_letter}snmLog.txt missing"
        all_pass=false
      fi
    fi
  done

  # Summary
  echo ""
  echo "Summary:"
  if [[ "$all_pass" == "true" ]]; then
    echo "  PASS | All peers completed successfully"
  else
    echo "  FAIL | One or more peers failed"
  fi
} > "$eval_file"

