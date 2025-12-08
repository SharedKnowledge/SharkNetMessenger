#!/bin/bash

################################################################################
# SharkNetMessenger Test Execution Script
# Restructured for basic, complex, and hub encounter tests
################################################################################

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ip="${1:-localhost}"

# overridden by exporting TEST_ENV_NAME in the environment before running
TEST_ENV_NAME="${TEST_ENV_NAME:-macAlone}"

if [[ -x "$SCRIPT_DIR/cleanup_scenarios.sh" ]]; then
  "$SCRIPT_DIR/cleanup_scenarios.sh" > /dev/null 2>&1
fi

export TEST_ROOT="$SCRIPT_DIR"

touch "$TEST_ROOT/eval.txt"
touch "$TEST_ROOT/errorlog.txt"

java -jar "$SCRIPT_DIR/scriptgenerator.jar"

mkdir -p "$TEST_ROOT/testRuns" 2>/dev/null || true

sg_ts=$(date +"%Y-%m-%d_%H-%M-%S%Z")
sg_zip="$TEST_ROOT/testRuns/scriptgenerator_output_${sg_ts}.zip"
(cd "$SCRIPT_DIR" && find . -type f \( -name 'HubHost.txt' -o -name '*_PeerA.txt' -o -name '*_PeerB.txt' \) -print0 | xargs -0 zip -q "$sg_zip") 2>/dev/null || true

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

execute_hub_test() {
  local g="$1"
  local export_name="$(basename "$g")"
  export_name="${export_name%_}"

  echo "Executing hub scenario: $g"
  cp -a "$SCRIPT_DIR/runHubCoreScenario.sh" "$g"
  cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
  chmod +x "$g/runHubCoreScenario.sh" || true
  (cd "$g" && ./runHubCoreScenario.sh "$ip")

  export_hub_test_results "$g" "$export_name"

  rm -rf "$g" 2>/dev/null || true
}

execute_tcp_test() {
  local g="$1"
  local test_type="$2"  # "basic" or "complex"
  local export_name="$(basename "$g")"
  export_name="${export_name%_}"

  echo "Executing $test_type TCP scenario: $g"
  cp -a "$SCRIPT_DIR/runTCPCoreScenario.sh" "$g"
  cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
  chmod +x "$g/runTCPCoreScenario.sh" || true
  (cd "$g" && ./runTCPCoreScenario.sh "$ip")

  # Export results
  export_tcp_test_results "$g" "$export_name"

  # Cleanup scenario directory to free up resources
  rm -rf "$g" 2>/dev/null || true
}

##############################################################################
export_hub_test_results() {
  local g="$1"
  local export_name="$2"
  local export_dir="$TEST_ROOT/testRuns/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
  mkdir -p "$export_dir" 2>/dev/null || true

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

  hub_target="$export_dir/hub/$export_name"
  if [[ -f "$g/hubHostsnmLog.txt" ]]; then
    mkdir -p "$hub_target" 2>/dev/null || true
    cp "$g/hubHostsnmLog.txt" "$hub_target/hubHostsnmLog.txt" 2>/dev/null || true
  fi

  # Process ASAP logs from hub and all peers
  for af in "$g"/asapLogs*.txt "$g"/Peer*/asapLogs*.txt; do
    [[ -f "$af" ]] || continue
    relpath="${af#$g}"
    if [[ "$relpath" == Peer*/* ]]; then
      peer_dir=$(echo "$relpath" | cut -d'/' -f1)
      targetdir="$export_dir/$peer_dir"
    else
      targetdir="$hub_target"
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
    cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
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
        mkdir -p "$hub_target" 2>/dev/null || true
        cp "$el" "$hub_target/$(basename "$el")" 2>/dev/null || true
      fi
    fi
  done

  echo "$g"
}

# Function to export TCP test results
export_tcp_test_results() {
  local g="$1"
  local export_name="$2"
  local export_dir="$TEST_ROOT/testRuns/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
  mkdir -p "$export_dir" 2>/dev/null || true

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
    cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
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
      peer_match=$(echo "$relpath" | grep -oP 'Peer[A-Z]' | head -1)
      if [[ -n "$peer_match" ]]; then
        target="$export_dir/$peer_match/$bname"
      else
        target="$export_dir/$relpath"
      fi

      mkdir -p "$(dirname "$target")" 2>/dev/null || true
      cp -p "$af" "$target" 2>/dev/null || true
    done < <(find "$g" -type f -name 'asapLogs*.txt' -print0 2>/dev/null)
  fi

  if [[ -f "$export_dir/eval_local.txt" ]]; then
    if grep -Fiq "FAIL" "$export_dir/eval_local.txt" 2>/dev/null; then
      dest="$TEST_ROOT/testRunsFailed/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
      mkdir -p "$dest" 2>/dev/null || true
      cp -a "$export_dir/." "$dest/" 2>/dev/null || true
    fi
  fi

  echo "$g"
}

################################################################################
# Helper Functions - Test Type Detection
################################################################################

detect_test_type() {
  local dirpath="$1"
  local export_name="$(basename "$dirpath")"
  export_name="${export_name%_}"

  # Check if this is a hub test
  if [[ -f "$dirpath/HubHost.txt" && $have_hub -eq 1 ]]; then
    echo "hub"
    return 0
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
done < <(find "$SCRIPT_DIR" -mindepth 1 -maxdepth 2 -type d -print0)

# Now process the collected directories
for dirpath in "${scenario_dirs[@]}"; do
  [[ -d "$dirpath" ]] || continue
  dirbase="$(basename "$dirpath")"
  case "$dirbase" in
    testRuns|testRunsFailed|.runs)
      continue
      ;;
  esac

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
      # If there are scenario files in descendant directories, this is just a grouping folder
      if find "$dirpath" -mindepth 1 -maxdepth 2 -type f \( -name 'HubHost.txt' -o -name '*_PeerA.txt' -o -name '*_PeerB.txt' \) -print -quit | grep -q .; then
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
echo "======================================================================="
echo ""

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


