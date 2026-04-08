#!/usr/bin/env bash
# runSingleScenarioTerminals.sh
# Copy this script into any scenario directory and run it there to open each peer (and hub) in its own Terminal window on macOS.
# Usage: ./runSingleScenarioTerminals.sh [IP] [--wait] [--cleanup] [--slow RATE]
#  - IP replaces FILLER_IP tokens (default: localhost)
#  - --wait : block until all scenario processes exit and then produce eval_local.txt (like the original runner)
#  - --cleanup : remove the temporary per-peer launcher scripts when finished
#  - --slow RATE : throttle combined output (chars/sec). Requires `pv` for precise throttling; otherwise falls back to per-line sleeping.

set -euo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
folderName="$(basename "$SCRIPT_DIR")"
# strip single trailing underscore if present
folderName="${folderName%_}"

# defaults
ip="localhost"
WAIT_FOR_FINISH=0
CLEANUP_SCRIPTS=0
SLOW_RATE=0

# parse args (positional IP and flags)
if [[ $# -ge 1 ]]; then
  ip_provided=0
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --wait)
        WAIT_FOR_FINISH=1; shift;;
      --cleanup)
        CLEANUP_SCRIPTS=1; shift;;
      --slow)
        if [[ $# -ge 2 && "$2" != "" && "$2" != --* ]]; then
          SLOW_RATE="$2"; shift 2
        else
          echo "--slow requires a numeric rate argument (chars/sec)" >&2; exit 1
        fi;;
      --slow=*)
        SLOW_RATE="${1#--slow=}"; shift;;
      --*)
        echo "Unknown option: $1" >&2; shift;;
      *)
        if [[ $ip_provided -eq 0 ]]; then ip="$1"; ip_provided=1; fi; shift;;
    esac
  done
fi

# Verbose output if set (export VERBOSE=1 to enable)
VERBOSE="${VERBOSE:-0}"
SCENARIO_WAIT_TIMEOUT="${SCENARIO_WAIT_TIMEOUT:-60}"

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

# Build throttling command if requested
SLOW_CMD="cat"
if [[ -n "${SLOW_RATE:-}" && "$SLOW_RATE" != "0" ]]; then
  if command -v pv >/dev/null 2>&1; then
    SLOW_CMD="pv -qL $SLOW_RATE"
  else
    # Fallback: approximate per-line sleep based on avg chars/line
    avg_chars=80
    # compute sleep in seconds per line (floating), keep 4 decimals
    sleep_per_line=$(awk -v a="$avg_chars" -v r="$SLOW_RATE" 'BEGIN{printf "%.4f", a/r}')
    # use awk to print each line and sleep
    SLOW_CMD="awk '{print; fflush(); system(\"sleep $sleep_per_line\")}'"
  fi
fi

# Only support macOS Terminal windows for interactive viewing (but we still prepare launchers on other OSes)
if [[ "$OSTYPE" != "darwin"* ]]; then
  echo "This script currently supports opening separate Terminal windows only on macOS (darwin)." >&2
  echo "Launcher scripts will be created; open them manually on other OSes." >&2
fi

# Replace FILLER_IP tokens (always run; default is localhost)
ip_escaped=$(printf '%s' "$ip" | sed 's/[\/&]/\\&/g')
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

# Create per-peer filler files
FILLER_SIZE="${FILLER_SIZE:-1024}"
FILLER_NAME="${FILLER_NAME:-${folderName}_filler.txt}"

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

for pd in "${peers_need_filler[@]:-}"; do
  peer_name=$(basename "$pd")
  peer_letter="${peer_name#Peer}"

  if [[ "$FILLER_NAME" == *.* ]]; then
    base="${FILLER_NAME%.*}"
    ext="${FILLER_NAME##*.}"
    per_name="${base}_${peer_letter}.${ext}"
  else
    per_name="${FILLER_NAME}_${peer_letter}"
  fi

  FILLER_PATH="$pd/$per_name"
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
  if [[ "$VERBOSE" == "1" ]]; then
    echo "Created filler: $FILLER_PATH"
  fi
done
shopt -u nullglob

# Replace FILLER_FILENAME tokens per peer
shopt -s nullglob
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    peer_letter="${peer_name#Peer}"
    target="$peer_dir/${folderName}_${peer_name}.txt"
    if [[ -f "$target" ]]; then
      if [[ "$FILLER_NAME" == *.* ]]; then
        base="${FILLER_NAME%.*}"
        ext="${FILLER_NAME##*.}"
        per_name="${base}_${peer_letter}.${ext}"
      else
        per_name="${FILLER_NAME}_${peer_letter}"
      fi
      sed_inplace "s/FILLER_FILENAME/$per_name/g" "$target"
      if [[ "$VERBOSE" == "1" ]]; then
        echo "Replaced FILLER_FILENAME in: $target"
      fi
    fi
  fi
done
shopt -u nullglob

# Remove decimal fractions like 1100.0 -> 1100
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

# Helper to open a new Terminal window and run a script (macOS)
# Optionally accepts four numeric arguments for bounds: x y x2 y2
open_terminal_with_script() {
  local script_path="$1"; shift
  local b1="$1"; local b2="$2"; local b3="$3"; local b4="$4"
  if [[ "$OSTYPE" == "darwin"* ]]; then
    esc_script_path=$(printf '%s' "$script_path" | sed "s/'/'\\''/g")
    if [[ -n "$b1" && -n "$b2" && -n "$b3" && -n "$b4" ]]; then
      # Create new window running the script, then set its bounds
      osascript -e "tell application \"Terminal\" to do script \"bash -lc '$esc_script_path'\"" \
                -e "delay 0.1" \
                -e "tell application \"Terminal\" to set bounds of front window to {${b1}, ${b2}, ${b3}, ${b4}}" >/dev/null 2>&1 || true
    else
      osascript -e "tell application \"Terminal\" to do script \"bash -lc '$esc_script_path'\"" >/dev/null 2>&1 || true
    fi
  else
    echo "Terminal launching only supported on macOS. Script path: $script_path"
  fi
}

# Create small launcher scripts for hub and each peer so Terminal windows run them cleanly
launcher_paths=()

# Hub (if present)
dirbase="$(basename "$SCRIPT_DIR")"
dirbase_lc="$(printf '%s' "$dirbase" | tr '[:upper:]' '[:lower:]')"
if [[ "$SCRIPT_DIR" == */hub/* || "$dirbase_lc" == *hub* ]]; then
  is_hub=1
else
  is_hub=0
fi

# Hub (if present)
if [[ $is_hub -eq 1 ]]; then
  # create hub launcher only if HubHost.txt exists
  if [[ -f "$SCRIPT_DIR/HubHost.txt" ]]; then
     hub_launcher="$SCRIPT_DIR/.run_hub.sh"
     cat > "$hub_launcher" <<EOF
#!/usr/bin/env bash
cd "$SCRIPT_DIR"
# set terminal title to help identify the window
printf '\\033]0;HubHost\\007'
# start hub and show output live while saving logs
# merged stdout+stderr -> throttled -> tee into both hubHostsnmLog.txt and hubHost_errorlog.txt
cat HubHost.txt | java -jar "$CLI_JAR_PATH" Hub 2>&1 | $SLOW_CMD | tee "hubHostsnmLog.txt" | tee "hubHost_errorlog.txt"
# keep the terminal open when finished
printf "\nHub finished. Press ENTER to close...\n"
read -r
EOF
     chmod +x "$hub_launcher"
     launcher_paths+=("$hub_launcher")
  else
    echo "Notice: Hub scenario detected by name/placement but HubHost.txt is missing in $SCRIPT_DIR; peers only." >&2
  fi
fi

# Create peer launchers
shopt -s nullglob
for peer_dir in "$SCRIPT_DIR"/Peer*; do
  if [[ -d "$peer_dir" ]]; then
    peer_name=$(basename "$peer_dir")
    peer_letter="${peer_name#Peer}"
    target_file="$peer_dir/${folderName}_${peer_name}.txt"
    if [[ -f "$target_file" ]]; then
      peer_launcher="$peer_dir/.run_peer_${peer_letter}.sh"
      cat > "$peer_launcher" <<EOF
#!/usr/bin/env bash
cd "$peer_dir"
# set terminal title to identify the peer window
printf '\\033]0;${peer_name}\\007'
# run the peer, merge stdout+stderr, throttle if requested, and save logs (both files receive same combined output)
cat "${folderName}_${peer_name}.txt" | java -jar "$CLI_JAR_PATH" "$peer_name" 2>&1 | $SLOW_CMD | tee "peer${peer_letter}snmLog.txt" | tee "errorlog${peer_letter}.txt"
printf "\nPeer ${peer_name} finished. Press ENTER to close...\n"
read -r
EOF
      chmod +x "$peer_launcher"
      launcher_paths+=("$peer_launcher")
    fi
  fi
done
shopt -u nullglob

# Position windows in a simple grid to avoid covering each other
n=${#launcher_paths[@]}
if (( n == 0 )); then
  echo "No hub or peer launchers created; nothing to open."
  exit 1
fi
# choose number of columns for grid (tweakable)
columns=3
# compute rows (not used directly but kept for future use)
# rows=$(( (n + columns - 1) / columns ))
# window geometry (pixels)
WIN_W=800
WIN_H=600
MARGIN=20

# Launch all created scripts in separate Terminal windows with positioning
i=0
for lp in "${launcher_paths[@]:-}"; do
  col=$(( i % columns ))
  row=$(( i / columns ))
  x=$(( MARGIN + col * (WIN_W + MARGIN) ))
  y=$(( MARGIN + row * (WIN_H + MARGIN) ))
  x2=$(( x + WIN_W ))
  y2=$(( y + WIN_H ))
  if [[ "$VERBOSE" == "1" ]]; then
    echo "Opening Terminal for: $lp at bounds {$x,$y,$x2,$y2}"
  fi
  open_terminal_with_script "$lp" "$x" "$y" "$x2" "$y2"
  sleep 0.2
  i=$((i+1))
done

if (( WAIT_FOR_FINISH == 1 )); then
  # Wait until no java processes reference this scenario directory (same approach as other runners)
  if command -v lsof >/dev/null 2>&1; then
    waited=0
    while lsof +D "$SCRIPT_DIR" >/dev/null 2>&1; do
      sleep 1
      waited=$((waited+1))
      if (( waited >= SCENARIO_WAIT_TIMEOUT )); then
        echo "Warning: wait timed out after ${SCENARIO_WAIT_TIMEOUT}s for $SCRIPT_DIR" >&2
        break
      fi
    done
  else
    waited=0
    while ps -eo pid=,command= | grep -F "SharkNetMessengerCLI.jar" | grep -F "$SCRIPT_DIR" >/dev/null 2>&1; do
      sleep 1
      waited=$((waited+1))
      if (( waited >= SCENARIO_WAIT_TIMEOUT )); then
        echo "Warning: fallback wait timed out after ${SCENARIO_WAIT_TIMEOUT}s for $SCRIPT_DIR" >&2
        break
      fi
    done
  fi

  # Produce eval identical to runSingleScenario.sh
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

  echo "Eval written to $eval_file"
fi

# Optionally cleanup the temporary launcher scripts
if (( CLEANUP_SCRIPTS == 1 )); then
  for lp in "${launcher_paths[@]:-}"; do
    rm -f "$lp" || true
  done
  if [[ "$VERBOSE" == "1" ]]; then
    echo "Removed temporary launcher scripts"
  fi
fi

echo "Launched ${#launcher_paths[@]} terminal window(s)."
