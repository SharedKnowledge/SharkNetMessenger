#!/bin/bash

################################################################################
# SharkNetMessenger Test Execution Script
# Restructured for basic, complex, and hub encounter tests
################################################################################

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ip="${1:-localhost}"

# overridden by exporting TEST_ENV_NAME in the environment before running
TEST_ENV_NAME="${TEST_ENV_NAME:-macAlone}"

if [[ -x "$SCRIPT_DIR/cleanup_scenarios.sh" && "${SKIP_INITIAL_CLEANUP:-0}" != "1" ]]; then
  "$SCRIPT_DIR/cleanup_scenarios.sh" > /dev/null 2>&1
fi

export TEST_ROOT="$SCRIPT_DIR"

: > "$TEST_ROOT/eval.txt"
: > "$TEST_ROOT/errorlog.txt"

java -jar "$SCRIPT_DIR/scriptgenerator.jar"

mkdir -p "$TEST_ROOT/testRuns" 2>/dev/null || true

sg_ts=$(date +"%Y-%m-%d_%H-%M-%S%Z")
sg_zip="$TEST_ROOT/testRuns/scriptgenerator_output_${sg_ts}.zip"
(cd "$SCRIPT_DIR" && find . -type f \( -name '*_PeerA.txt' -o -name '*_PeerB.txt' -o -name '*_PeerC.txt' \) -print0 | xargs -0 zip -q "$sg_zip") 2>/dev/null || true

if [[ ! -f "$SCRIPT_DIR/runTCPCoreScenario.sh" || ! -f "$SCRIPT_DIR/SharkNetMessengerCLI.jar" ]]; then
  echo "Missing required files (runTCPCoreScenario.sh or SharkNetMessengerCLI.jar) in $SCRIPT_DIR"
  exit 1
fi

have_hub=0
if [[ -f "$SCRIPT_DIR/runHubCoreScenario.sh" ]]; then
  have_hub=1
fi

# per-day run enumeration to avoid overwriting multiple runs on the same date
date_str="$(date +"%Y-%m-%d")"
run_base="$TEST_ROOT/.runs/$TEST_ENV_NAME/$date_str"
mkdir -p "$run_base" 2>/dev/null || true

i=1
while [[ -d "$run_base/run_$i" ]]; do
  i=$((i+1))
done
mkdir -p "$run_base/run_$i" 2>/dev/null || true
RUN_SEQ="$i"

# Configuration for automatic killing of stray SharkNet processes before starting a scenario
# Set these environment variables to control behavior:
# AUTO_KILL_SHARK_PROCESSES=1    (default) enable auto kill before each scenario
# KILL_SCOPE=all|dir             (default all) 'all' kills all SharkNet processes, 'dir' kills processes holding files in the scenario dir
# KILL_SHARK_FORCE=0|1           (default 0) if 1, force-kill (SIGKILL)
# KILL_SHARK_TIMEOUT=10          (seconds to wait after SIGTERM before SIGKILL)
# AUTO_KILL_VERBOSE=0|1          (default 0) pass -v to the kill script if set
AUTO_KILL_SHARK_PROCESSES="${AUTO_KILL_SHARK_PROCESSES:-1}"
KILL_SCOPE="${KILL_SCOPE:-all}"
KILL_SHARK_FORCE="${KILL_SHARK_FORCE:-0}"
KILL_SHARK_TIMEOUT="${KILL_SHARK_TIMEOUT:-10}"
AUTO_KILL_VERBOSE="${AUTO_KILL_VERBOSE:-0}"

# Test execution timeout configuration
# TEST_TIMEOUT=60        (default) timeout in seconds for each individual test (60 seconds = 1 minute)
# Set to 0 to disable timeout
TEST_TIMEOUT="${TEST_TIMEOUT:-60}"

# Track timed out scenarios for final summary output
declare -a timed_out_scenarios

################################################################################
# Helper function to run commands with timeout
################################################################################
kill_scenario_processes() {
  local scenario_dir="$1"
  [[ -n "$scenario_dir" ]] || return 0

  # Prefer the dedicated helper when available.
  if [[ -x "$SCRIPT_DIR/kill_shark_processes.sh" ]]; then
    "$SCRIPT_DIR/kill_shark_processes.sh" -d "$scenario_dir" -f >/dev/null 2>&1 || true
  fi

  # Fallback: kill any remaining java SharkNet processes that still reference the scenario dir.
  if command -v pgrep >/dev/null 2>&1; then
    while IFS= read -r pid; do
      [[ -n "$pid" ]] || continue
      if ps -p "$pid" -o command= | grep -F "SharkNetMessengerCLI.jar" | grep -F "$scenario_dir" >/dev/null 2>&1; then
        kill -9 "$pid" 2>/dev/null || true
      fi
    done < <(pgrep -f "SharkNetMessengerCLI.jar" || true)
  fi
}

run_with_timeout() {
  local timeout_sec="$1"
  local scenario_dir="$2"
  shift
  shift
  local cmd=("$@")

  if [[ "$timeout_sec" -le 0 ]]; then
    # No timeout, run the command directly
    "${cmd[@]}"
    return $?
  fi

  # Run with timeout using sleep + background process + wait with timeout
  "${cmd[@]}" &
  local pid=$!
  local count=0

  while kill -0 "$pid" 2>/dev/null; do
    if [[ $count -ge $timeout_sec ]]; then
      echo "⚠ WARNING: Test exceeded timeout of ${timeout_sec} seconds, force-killing..." >&2
      # Kill the parent wrapper and then aggressively clean up scenario java processes.
      kill -9 "$pid" 2>/dev/null || true
      kill_scenario_processes "$scenario_dir"
      sleep 1
      return 124  # Timeout exit code
    fi
    sleep 1
    count=$((count + 1))
  done

  wait "$pid"
  return $?
}

execute_hub_test() {
  local g="$1"
  local export_name="$(basename "$g")"
  export_name="${export_name%_}"

  # Optionally ensure no stray SharkNet processes are running before starting this scenario
  if [[ "$AUTO_KILL_SHARK_PROCESSES" == "1" ]]; then
    if [[ -x "$SCRIPT_DIR/kill_shark_processes.sh" ]]; then
      echo "Ensuring no stray SharkNet processes are running before executing $g..."
      kill_args=()
      # choose scope
      if [[ "$KILL_SCOPE" == "dir" ]]; then
        kill_args+=( -d "$g" )
      else
        kill_args+=( -a )
      fi
      # force or timeout
      if [[ "$KILL_SHARK_FORCE" == "1" ]]; then
        kill_args+=( -f )
      else
        kill_args+=( -t "$KILL_SHARK_TIMEOUT" )
      fi
      if [[ "$AUTO_KILL_VERBOSE" == "1" ]]; then
        kill_args+=( -v )
      fi
      # run the kill helper but do not fail the test runner if it fails
      "$SCRIPT_DIR/kill_shark_processes.sh" "${kill_args[@]}" || true
    else
      echo "Warning: kill_shark_processes.sh not found or not executable; skipping auto-kill." >&2
    fi
  fi

  echo "Executing hub scenario: $g (timeout: ${TEST_TIMEOUT}s)"
  cp -a "$SCRIPT_DIR/runHubCoreScenario.sh" "$g"
  cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
  chmod +x "$g/runHubCoreScenario.sh" || true

  run_with_timeout "$TEST_TIMEOUT" "$g" bash -c "cd '$g' && ./runHubCoreScenario.sh '$ip'"
  local test_exit_code=$?
  if [[ $test_exit_code -eq 124 ]]; then
    timed_out_scenarios+=("$export_name (hub)")
  fi

  export_hub_test_results "$g" "$export_name" "$test_exit_code"

  rm -rf "$g" 2>/dev/null || true
}

execute_tcp_test() {
  local g="$1"
  local test_type="$2"  # "basic" or "complex"
  local export_name="$(basename "$g")"
  export_name="${export_name%_}"

  # Optionally ensure no stray SharkNet processes are running before starting this scenario
  if [[ "$AUTO_KILL_SHARK_PROCESSES" == "1" ]]; then
    if [[ -x "$SCRIPT_DIR/kill_shark_processes.sh" ]]; then
      echo "Ensuring no stray SharkNet processes are running before executing $g..."
      kill_args=()
      # choose scope
      if [[ "$KILL_SCOPE" == "dir" ]]; then
        kill_args+=( -d "$g" )
      else
        kill_args+=( -a )
      fi
      # force or timeout
      if [[ "$KILL_SHARK_FORCE" == "1" ]]; then
        kill_args+=( -f )
      else
        kill_args+=( -t "$KILL_SHARK_TIMEOUT" )
      fi
      if [[ "$AUTO_KILL_VERBOSE" == "1" ]]; then
        kill_args+=( -v )
      fi
      # run the kill helper but do not fail the test runner if it fails
      "$SCRIPT_DIR/kill_shark_processes.sh" "${kill_args[@]}" || true
    else
      echo "Warning: kill_shark_processes.sh not found or not executable; skipping auto-kill." >&2
    fi
  fi

  echo "Executing $test_type TCP scenario: $g (timeout: ${TEST_TIMEOUT}s)"
  cp -a "$SCRIPT_DIR/runTCPCoreScenario.sh" "$g"
  cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
  chmod +x "$g/runTCPCoreScenario.sh" || true

  run_with_timeout "$TEST_TIMEOUT" "$g" bash -c "cd '$g' && ./runTCPCoreScenario.sh '$ip'"
  local test_exit_code=$?
  if [[ $test_exit_code -eq 124 ]]; then
    timed_out_scenarios+=("$export_name ($test_type)")
  fi

  # Export results
  export_tcp_test_results "$g" "$export_name" "$test_exit_code"

  # Cleanup scenario directory to free up resources
  rm -rf "$g" 2>/dev/null || true
}

##############################################################################
export_hub_test_results() {
  local g="$1"
  local export_name="$2"
  local test_exit_code="${3:-0}"
  local export_dir="$TEST_ROOT/testRuns/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
  mkdir -p "$export_dir" 2>/dev/null || true

  # Handle timeout case
  if [[ $test_exit_code -eq 124 ]]; then
    echo "⚠ TIMEOUT: Test exceeded ${TEST_TIMEOUT}s timeout limit" >> "$TEST_ROOT/errorlog.txt"
    echo "TIMEOUT: Test exceeded ${TEST_TIMEOUT}s timeout limit" > "$export_dir/TIMEOUT.txt"
  fi

  # collect command lists from ALL peers (not just A and B)
  cmd_zip="$export_dir/command_lists.zip"
  files_to_zip=()

  # Find all peer directories and their command files
  if [[ -d "$g" ]]; then
    for peer_dir in "$g"/Peer*; do
      if [[ -d "$peer_dir" ]]; then
        peer_name=$(basename "$peer_dir")
        cmd_file="$peer_dir/${export_name}_${peer_name}.txt"
        if [[ -f "$cmd_file" ]]; then
          files_to_zip+=("$cmd_file")
        fi
      fi
    done
  fi

  if [[ ${#files_to_zip[@]} -gt 0 ]]; then
    (cd "$SCRIPT_DIR" || exit 0
      for p in "${files_to_zip[@]}"; do
        rel="${p#$SCRIPT_DIR/}"
        if [[ -f "$SCRIPT_DIR/$rel" ]]; then
          zip -q "$cmd_zip" "$rel" 2>/dev/null || true
        fi
      done
    ) 2>/dev/null || true
  fi

  # Process ASAP logs from hub and all peers
  for af in "$g"/asapLogs*.txt "$g"/Peer*/asapLogs*.txt; do
    [[ -f "$af" ]] || continue
    relpath="${af#$g}"
    if [[ "$relpath" == Peer*/* ]]; then
      peer_dir=$(echo "$relpath" | cut -d'/' -f1)
      targetdir="$export_dir/$peer_dir"
    else
      targetdir="$export_dir"
    fi
    mkdir -p "$targetdir" 2>/dev/null || true
    cp "$af" "$targetdir/" 2>/dev/null || true
  done

  # Process SNM logs and error logs from ALL peers
  for peer_dir in "$g"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      export_peer_dir="$export_dir/$peer_name"
      mkdir -p "$export_peer_dir" 2>/dev/null || true

      # Copy SNM logs
      snm_log="$peer_dir/peer${peer_name#Peer}snmLog.txt"
      if [[ -f "$snm_log" ]]; then
        cp "$snm_log" "$export_peer_dir/peer${peer_name#Peer}snmLog.txt" 2>/dev/null || true
      fi

      # Copy error logs
      for err_log in "$peer_dir"/errorlog*.txt; do
        if [[ -f "$err_log" ]]; then
          cp "$err_log" "$export_peer_dir/" 2>/dev/null || true
        fi
      done
    fi
  done

  if [[ -s "$g/eval_local.txt" ]]; then
    cp "$g/eval_local.txt" "$export_dir/eval_local.txt" 2>/dev/null || true
    printf '\n===== %s =====\n' "$export_name" >> "$TEST_ROOT/eval.txt"
    cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
    printf '\n' >> "$TEST_ROOT/eval.txt"
    rm -f "$g/eval_local.txt"
  else
    rm -f "$g/eval_local.txt" 2>/dev/null || true
  fi

  # gather error logs from hub root and all peers
  for el in "$g"/Peer*/errorlog*.txt "$g"/errorlog*.txt; do
    if [[ -f "$el" && -s "$el" ]]; then
      printf '\n ==== %s: %s ==== %s' "$g" "$(basename "$el")" "\n" >> "$TEST_ROOT/errorlog.txt"
      cat "$el" >> "$TEST_ROOT/errorlog.txt"
      if [[ "$el" == "$g"/Peer*/* ]]; then
        peer_dir=$(basename "${el%/*}")
        mkdir -p "$export_dir/$peer_dir" 2>/dev/null || true
        cp "$el" "$export_dir/$peer_dir/$(basename "$el")" 2>/dev/null || true
      else
          cp "$el" "$export_dir/$(basename "$el")" 2>/dev/null || true
      fi
    fi
  done

  # Mirror failed hub runs into testRunsFailed (timeout and/or FAIL in eval output)
  mark_failed=0
  if [[ $test_exit_code -eq 124 ]]; then
    mark_failed=1
  fi
  if [[ -f "$export_dir/eval_local.txt" ]] && grep -Fiq "FAIL" "$export_dir/eval_local.txt" 2>/dev/null; then
    mark_failed=1
  fi
  if [[ "$mark_failed" -eq 1 ]]; then
    dest="$TEST_ROOT/testRunsFailed/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
    mkdir -p "$dest" 2>/dev/null || true
    cp -a "$export_dir/." "$dest/" 2>/dev/null || true
  fi

}

# Function to export TCP test results
export_tcp_test_results() {
  local g="$1"
  local export_name="$2"
  local test_exit_code="${3:-0}"
  local export_dir="$TEST_ROOT/testRuns/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
  mkdir -p "$export_dir" 2>/dev/null || true

  # Handle timeout case
  if [[ $test_exit_code -eq 124 ]]; then
    echo "⚠ TIMEOUT: Test exceeded ${TEST_TIMEOUT}s timeout limit" >> "$TEST_ROOT/errorlog.txt"
    echo "TIMEOUT: Test exceeded ${TEST_TIMEOUT}s timeout limit" > "$export_dir/TIMEOUT.txt"
  fi

  # Collect command lists from ALL peers
  cmd_zip="$export_dir/command_lists.zip"
  files_to_zip=()

  # Find all peer directories and their command files
  if [[ -d "$g" ]]; then
    for peer_dir in "$g"/Peer*; do
      if [[ -d "$peer_dir" ]]; then
        peer_name=$(basename "$peer_dir")
        cmd_file="$peer_dir/${export_name}_${peer_name}.txt"
        if [[ -f "$cmd_file" ]]; then
          files_to_zip+=("$cmd_file")
        fi
      fi
    done
  fi

  if [[ ${#files_to_zip[@]} -gt 0 ]]; then
    (cd "$SCRIPT_DIR" || exit 0
      for p in "${files_to_zip[@]}"; do
        rel="${p#$SCRIPT_DIR/}"
        if [[ -f "$SCRIPT_DIR/$rel" ]]; then
          zip -q "$cmd_zip" "$rel" 2>/dev/null || true
        fi
      done
    ) 2>/dev/null || true
  fi

  # Process SNM logs and error logs from ALL peers
  for peer_dir in "$g"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      export_peer_dir="$export_dir/$peer_name"
      mkdir -p "$export_peer_dir" 2>/dev/null || true

      # Copy SNM logs (e.g., peerAsnmLog.txt, peerBsnmLog.txt, etc.)
      snm_log="$peer_dir/peer${peer_name#Peer}snmLog.txt"
      if [[ -f "$snm_log" ]]; then
        cp "$snm_log" "$export_peer_dir/peer${peer_name#Peer}snmLog.txt" 2>/dev/null || true
      fi

      # Copy error logs
      for err_log in "$peer_dir"/errorlog*.txt; do
        if [[ -f "$err_log" ]]; then
          cp "$err_log" "$export_peer_dir/" 2>/dev/null || true
        fi
      done
    fi
  done

  if [[ -s "$g/eval_local.txt" ]]; then
    cp "$g/eval_local.txt" "$export_dir/eval_local.txt" 2>/dev/null || true
    printf '\n===== %s =====\n' "$export_name" >> "$TEST_ROOT/eval.txt"
    cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
    printf '\n' >> "$TEST_ROOT/eval.txt"
    rm -f "$g/eval_local.txt"
  else
    rm -f "$g/eval_local.txt" 2>/dev/null || true
  fi

  # Gather error logs from all peers
  for el in "$g"/Peer*/errorlog*.txt "$g"/errorlog*.txt; do
    if [[ -f "$el" && -s "$el" ]]; then
      printf '\n==== %s: %s ====%s' "$g" "$(basename "$el")" "\n" >> "$TEST_ROOT/errorlog.txt"
      cat "$el" >> "$TEST_ROOT/errorlog.txt"
      peer_dir=$(basename "${el%/*}")
      mkdir -p "$export_dir/$peer_dir" 2>/dev/null || true
      cp "$el" "$export_dir/$peer_dir/$(basename "$el")" 2>/dev/null || cp "$el" "$export_dir/$(basename "$el")" 2>/dev/null || true
    fi
  done

  # Process ASAP logs from all peers
  if command -v find >/dev/null 2>&1; then
    while IFS= read -r -d '' af; do
      [[ -f "$af" ]] || continue
      relpath="${af#$g}"
      bname="$(basename "$af")"

      # Determine target directory based on which peer the file belongs to
      # Extract first occurrence of Peer<Letter> (e.g., PeerA) using bash regex (portable)
      peer_match=""
      if [[ "$relpath" =~ (Peer[A-Z]) ]]; then
        peer_match="${BASH_REMATCH[1]}"
      fi
      if [[ -n "$peer_match" ]]; then
        target="$export_dir/$peer_match/$bname"
      else
        target="$export_dir/$relpath"
      fi

      mkdir -p "$(dirname "$target")" 2>/dev/null || true
      cp -p "$af" "$target" 2>/dev/null || true
    done < <(find "$g" -type f -name 'asapLogs*.txt' -print0 2>/dev/null)
  fi

  # Mirror failed TCP runs into testRunsFailed (timeout and/or FAIL in eval output)
  mark_failed=0
  if [[ $test_exit_code -eq 124 ]]; then
    mark_failed=1
  fi
  if [[ -f "$export_dir/eval_local.txt" ]] && grep -Fiq "FAIL" "$export_dir/eval_local.txt" 2>/dev/null; then
    mark_failed=1
  fi
  if [[ "$mark_failed" -eq 1 ]]; then
    dest="$TEST_ROOT/testRunsFailed/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
    mkdir -p "$dest" 2>/dev/null || true
    cp -a "$export_dir/." "$dest/" 2>/dev/null || true
  fi

}

################################################################################
# Helper Functions - Test Type Detection
################################################################################

detect_test_type() {
  local dirpath="$1"
  local export_name="$(basename "$dirpath")"
  export_name="${export_name%_}"

  # Check if this is a hub test
  if [[ $have_hub -eq 1 ]]; then

    dirbase="$(basename "$dirpath")"
    dirbase_lc="$(printf '%s' "$dirbase" | tr '[:upper:]' '[:lower:]')"
    export_name_lc="$(printf '%s' "$export_name" | tr '[:upper:]' '[:lower:]')"
    if [[ "$dirpath" == */hub/* || "$dirbase_lc" == *hub* || "$export_name_lc" == *hub* ]]; then
      echo "hub"
      return 0
    fi
  fi

  # Check if this is a TCP test by looking for any peer directory with command file
  # This handles PeerA, PeerB, PeerC, PeerD, etc.
  for peer_dir in "$dirpath"/Peer*; do
    if [[ -d "$peer_dir" ]]; then
      peer_name=$(basename "$peer_dir")
      cmd_file="$peer_dir/${export_name}_${peer_name}.txt"
      if [[ -f "$cmd_file" ]]; then
        echo "tcp"
        return 0
      fi
    fi
  done

  echo "unknown"
  return 1
}

determine_test_category() {
  local dirpath="$1"

  if [[ "$dirpath" == */basic/* ]]; then
    echo "basic"
  else
    echo "complex"
  fi
}

################################################################################
# Main Test Processing Loop
################################################################################

# Initialize test counters
basic_test_count=0
complex_test_count=0
hub_test_count=0

echo "======================================================================="
echo "Starting Test Discovery and Execution"
echo "======================================================================="
echo ""

# Collect all directories FIRST, before processing
# This avoids race conditions when directories are created/deleted during execution
declare -a scenario_dirs
while IFS= read -r -d '' dirpath; do
  [[ -d "$dirpath" ]] && scenario_dirs+=("$dirpath")
done < <(find "$SCRIPT_DIR" -mindepth 1 -maxdepth 3 -type d -print0)

# Keep track of directories we've processed to avoid duplicates (hub root + child dirs)
declare -a processed_dirs

# Now process the collected directories
for dirpath in "${scenario_dirs[@]}"; do
  [[ -d "$dirpath" ]] || continue
  # Skip artifact/result directories anywhere in the tree
  case "$dirpath" in
    */testRuns/*|*/testRunsFailed/*|*/.runs/*|*/testRuns|*/testRunsFailed|*/.runs)
      continue
      ;;
  esac

  # If we've already processed this directory (e.g. as a child of a hub grouping), skip it
  if printf '%s\n' "${processed_dirs[@]}" | grep -Fxq "$dirpath"; then
    continue
  fi

  dirbase="$(basename "$dirpath")"
  dirbase_lc="$(printf '%s' "$dirbase" | tr '[:upper:]' '[:lower:]')"

  # Special-case: a top-level `hub` folder is a grouping folder; iterate its subfolders
  if [[ "$dirbase_lc" == "hub" ]]; then
    if [[ $have_hub -eq 1 ]]; then
      for g in "$dirpath"/*; do
        [[ -d "$g" ]] || continue
        # mark child as processed to avoid duplicate work later
        processed_dirs+=("$g")
        echo "Processing hub scenario directory: $g"
        execute_hub_test "$g"
        ((hub_test_count++))
        echo ""
      done
    fi
    # mark the hub grouping dir processed and skip further handling
    processed_dirs+=("$dirpath")
    continue
  fi

  # Mark current dir as processed (prevents duplicates)
  processed_dirs+=("$dirpath")

  echo "Processing directory: $dirpath"

  # Detect test type using helper function
  test_type=$(detect_test_type "$dirpath")

  case "$test_type" in
    hub)
      echo "  -> Detected: Hub Encounter Test"
      execute_hub_test "$dirpath"
      ((hub_test_count++))
      echo ""
      ;;
    tcp)
      # Determine if this is a basic or complex test
      test_category=$(determine_test_category "$dirpath")
      echo "  -> Detected: $test_category TCP Encounter Test"
      execute_tcp_test "$dirpath" "$test_category"
      if [[ "$test_category" == "basic" ]]; then
        ((basic_test_count++))
      else
        ((complex_test_count++))
      fi
      echo ""
      ;;
    unknown)
      # If there are scenario files in descendant directories, or descendant dirs with 'hub' in their name,
      # this is just a grouping folder
      if find "$dirpath" -mindepth 1 -maxdepth 2 \( -type f \( -name '*_PeerA.txt' -o -name '*_PeerB.txt' \) -o -type d -iname '*hub*' \) -print -quit | grep -q .; then
        echo "  -> Skipping: Parent/grouping directory (contains scenario subdirs)"
      else
        echo "  -> Skipping: No recognizable scenario files"
      fi
      ;;
  esac
done

# Clean up test scenario directories after all tests complete
# This is critical because peers maintain state in these directories
# and old state must be removed before the next test run
echo ""
echo "Cleaning up test scenario directories..."
if [[ -x "$SCRIPT_DIR/cleanup_scenarios.sh" ]]; then
  "$SCRIPT_DIR/cleanup_scenarios.sh"
else
  # Fallback if cleanup script is not available
  rm -rf "$SCRIPT_DIR/basic"/* 2>/dev/null || true
  rm -rf "$SCRIPT_DIR/complex"/* 2>/dev/null || true
  rm -rf "$SCRIPT_DIR/hub"/* 2>/dev/null || true
  rm -rf "$SCRIPT_DIR/TCPChain" 2>/dev/null || true
fi

# Ensure top-level hub and TCPChain directories are removed entirely to avoid reuse of old scenarios
# This mirrors the user's request to delete these folders containing test scenarios
rm -rf "$SCRIPT_DIR/hub" 2>/dev/null || true
rm -rf "$SCRIPT_DIR/TCPChain" 2>/dev/null || true

################################################################################
# Generate Final Summary and Archive Results
################################################################################

echo ""
echo "======================================================================="
echo "Test Execution Summary"
echo "======================================================================="
echo "  Basic Encounter Tests:   $basic_test_count"
echo "  Complex Encounter Tests: $complex_test_count"
echo "  Hub Encounter Tests:     $hub_test_count"
echo "  Total Tests:             $((basic_test_count + complex_test_count + hub_test_count))"
echo "  Timed Out Tests:         ${#timed_out_scenarios[@]}"
echo "======================================================================="
echo ""

if (( ${#timed_out_scenarios[@]} > 0 )); then
  echo "Timed Out Scenario List:"
  for timeout_entry in "${timed_out_scenarios[@]}"; do
    echo "  - $timeout_entry"
  done
  echo ""
fi

ts=$(date +"%Y-%m-%d_%H-%M-%S%Z")
archive="testresults_${ts}.zip"
echo "Creating results archive: $archive"
(
  cd "$TEST_ROOT" || exit 0
  zip -r -q "$archive" testRuns testRunsFailed eval.txt errorlog.txt -x "$archive" "*.sh" "*scriptgenerator.jar" "*SharkNetMessengerCLI.jar" ".runs/*" ".runs" 2>/dev/null || true
)

if [[ -f "$TEST_ROOT/$archive" ]]; then
  echo "✓ Archive created successfully: $TEST_ROOT/$archive"
else
  echo "✗ Failed to create archive $archive" >&2
fi

echo ""
echo "======================================================================="
echo "Test Execution Complete"
echo "======================================================================="
echo "Results Location:"
echo "  Passed:  testRuns/"
echo "  Failed:  testRunsFailed/"
echo "  Summary: eval.txt"
echo "  Errors:  errorlog.txt"
echo "======================================================================="
