#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ip="${1:-localhost}"

export TEST_ROOT="$SCRIPT_DIR"
> "$TEST_ROOT/eval.txt"
# initialize central error log
> "$TEST_ROOT/errorlog.txt"
java -jar "$SCRIPT_DIR/scriptgenerator.jar"

# read -p "Type IP address: " ip

if [[ ! -f "$SCRIPT_DIR/runTCPCoreScenario.sh" || ! -f "$SCRIPT_DIR/SharkNetMessengerCLI.jar" ]]; then
  echo "Missing required files (runTCPCoreScenario.sh or SharkNetMessengerCLI.jar) in $SCRIPT_DIR"
  exit 1
fi

have_hub=0
if [[ -f "$SCRIPT_DIR/runHubCoreScenario.sh" ]]; then
  have_hub=1
fi

for f in "$SCRIPT_DIR"/*/; do
  [[ -d "$f" ]] || continue
  echo "Processing directory: $f"
  dirbase="$(basename "$f")"
  # lowercase in a portable way (works on macOS / older bash)
  dirbase_lc="$(printf '%s' "$dirbase" | tr '[:upper:]' '[:lower:]')"

  if [[ "$dirbase_lc" == "hub" && $have_hub -eq 1 ]]; then
    for g in "$f"*/; do
      [[ -d "$g" ]] || continue
      echo "Processing directory: $g"
      cp -a "$SCRIPT_DIR/runHubCoreScenario.sh" "$g"
      cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
      chmod +x "$g/runHubCoreScenario.sh" || true
      (cd "$g" && ./runHubCoreScenario.sh "$ip")
      if [[ -s "$g/eval_local.txt" ]]; then
        cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
        rm -f "$g/eval_local.txt"
      else
        rm -f "$g/eval_local.txt" 2>/dev/null || true
      fi

      for el in "$g"/Peer*/errorlog*.txt "$g"/errorlog*.txt; do
        if [[ -f "$el" && -s "$el" ]]; then
          printf '\n==== %s: %s ====%s' "$g" "$(basename "$el")" "\n" >> "$TEST_ROOT/errorlog.txt"
          cat "$el" >> "$TEST_ROOT/errorlog.txt"
        fi
      done
      echo "$g"
    done
  fi

  if [[ "$dirbase_lc" == "tcpchain" ]]; then
    for g in "$f"*/; do
      echo "Processing directory: $g"
      [[ -d "$g" ]] || continue
      cp -a "$SCRIPT_DIR/runTCPCoreScenario.sh" "$g"
      cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
      chmod +x "$g/runTCPCoreScenario.sh" || true
      (cd "$g" && ./runTCPCoreScenario.sh "$ip")
      if [[ -s "$g/eval_local.txt" ]]; then
        cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
        rm -f "$g/eval_local.txt"
      else
        rm -f "$g/eval_local.txt" 2>/dev/null || true
      fi

      for el in "$g"/Peer*/errorlog*.txt "$g"/errorlog*.txt; do
        if [[ -f "$el" && -s "$el" ]]; then
          printf '\n==== %s: %s ====%s' "$g" "$(basename "$el")" "\n" >> "$TEST_ROOT/errorlog.txt"
          cat "$el" >> "$TEST_ROOT/errorlog.txt"
        fi
      done
      echo "$g"
    done
  fi
done

wait

envfile="$TEST_ROOT/test_environment.txt"
{
  echo "Timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "OS: $(uname -s 2>/dev/null || true)"
  echo "Kernel: $(uname -r 2>/dev/null || true)"
  echo "Arch: $(uname -m 2>/dev/null || true)"

  # Distribution / release detection
  if [[ "$(uname -s)" == "Darwin" ]]; then
    echo "Distribution: macOS"
    if command -v sw_vers >/dev/null 2>&1; then
      sw_vers 2>/dev/null | sed 's/^/  /'
    fi
  elif [[ -f /etc/os-release ]]; then
    . /etc/os-release
    echo "Distribution: ${PRETTY_NAME:-$NAME $VERSION}"
  else
    if command -v lsb_release >/dev/null 2>&1; then
      lsb_release -d 2>/dev/null | sed 's/^Description:\s*/Distribution: /'
    fi
    if [[ -f /etc/lsb-release ]]; then
      . /etc/lsb-release
      [[ -n "$DISTRIB_DESCRIPTION" ]] && echo "Distribution: $DISTRIB_DESCRIPTION"
    fi
    if [[ -f /etc/debian_version ]]; then
      echo "Distribution: Debian $(cat /etc/debian_version)"
    fi
    if [[ -f /etc/redhat-release ]]; then
      echo "Distribution: $(cat /etc/redhat-release)"
    fi
  fi
} > "$envfile"

ts=$(date +"%Y-%m-%d_%H-%M-%S%Z")
archive="testresults_${ts}.zip"
(
  cd "$TEST_ROOT" || exit 0
  
  zip -r -q "$archive" . -x "$archive"
)
if [[ -f "$TEST_ROOT/$archive" ]]; then
  echo "Created archive: $TEST_ROOT/$archive"
else
  echo "Failed to create archive $archive" >&2
fi

echo "All scenarios executed. Check eval.txt files for results. Check errorlog.txt for any errors."