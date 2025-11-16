#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ip="${1:-localhost}"

# overridden by exporting TEST_ENV_NAME in the environment before running
TEST_ENV_NAME="${TEST_ENV_NAME:-macAlone}"

export TEST_ROOT="$SCRIPT_DIR"
> "$TEST_ROOT/eval.txt"

> "$TEST_ROOT/errorlog.txt"
java -jar "$SCRIPT_DIR/scriptgenerator.jar"

mkdir -p "$TEST_ROOT/testRuns" 2>/dev/null || true

sg_ts=$(date +"%Y-%m-%d_%H-%M-%S%Z")
sg_zip="$TEST_ROOT/testRuns/scriptgenerator_output_${sg_ts}.zip"
(cd "$SCRIPT_DIR" && find . -type f \( -name 'HubHost.txt' -o -name '*_PeerA.txt' -o -name '*_PeerB.txt' \) -print0 | xargs -0 zip -q "$sg_zip") 2>/dev/null || true

# read -p "Type IP address: " ip

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

for f in "$SCRIPT_DIR"/*/; do
  [[ -d "$f" ]] || continue
  dirbase="$(basename "$f")"
  case "$dirbase" in
    testRuns|testRunsFailed|.runs)
      continue
      ;;
  esac
  echo "Processing directory: $f"

  dirbase_lc="$(printf '%s' "$dirbase" | tr '[:upper:]' '[:lower:]')"

  if [[ "$dirbase_lc" == "hub" && $have_hub -eq 1 ]]; then
    for g in "$f"*/; do
      [[ -d "$g" ]] || continue
      echo "Processing directory: $g"
      cp -a "$SCRIPT_DIR/runHubCoreScenario.sh" "$g"
      cp -a "$SCRIPT_DIR/SharkNetMessengerCLI.jar" "$g"
      chmod +x "$g/runHubCoreScenario.sh" || true
      (cd "$g" && ./runHubCoreScenario.sh "$ip")

      export_name="$(basename "$g")"
      export_name="${export_name%_}"
      export_dir="$TEST_ROOT/testRuns/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
      mkdir -p "$export_dir" "$export_dir/PeerA" "$export_dir/PeerB" 2>/dev/null || true

      cmd_zip="$export_dir/command_lists.zip"
      files_to_zip=()
      if [[ -f "$g/HubHost.txt" ]]; then
          dirbase="$(basename "$f")"
          dirbase_lc="$(printf '%s' "$dirbase" | tr '[:upper:]' '[:lower:]')"
          case "$dirbase_lc" in
            hub|tcpchain)
              ;;
            *)
              continue
              ;;
          esac
          echo "Processing directory: $f"
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

      for af in "$g"/asapLogs*.txt "$g"/PeerA/*asapLogs*.txt "$g"/PeerB/*asapLogs*.txt; do
        [[ -f "$af" ]] || continue
        relpath="${af#$g}"
        if [[ "$relpath" == PeerA/* || "$relpath" == PeerB/* ]]; then
          targetdir="$export_dir$(dirname "$relpath")"
        else
          targetdir="$hub_target"
        fi
        mkdir -p "$targetdir" 2>/dev/null || true
        cp "$af" "$targetdir/" 2>/dev/null || true
      done

      if [[ -f "$g/PeerA/peerAsnmLog.txt" ]]; then
        cp "$g/PeerA/peerAsnmLog.txt" "$export_dir/PeerA/peerAsnmLog.txt" 2>/dev/null || true
      fi
      if [[ -f "$g/PeerA/errorlogPeerA.txt" ]]; then
        cp "$g/PeerA/errorlogPeerA.txt" "$export_dir/PeerA/errorlogPeerA.txt" 2>/dev/null || true
      fi
      if [[ -f "$g/PeerB/peerBsnmLog.txt" ]]; then
        cp "$g/PeerB/peerBsnmLog.txt" "$export_dir/PeerB/peerBsnmLog.txt" 2>/dev/null || true
      fi
      if [[ -f "$g/PeerB/errorlogPeerB.txt" ]]; then
        cp "$g/PeerB/errorlogPeerB.txt" "$export_dir/PeerB/errorlogPeerB.txt" 2>/dev/null || true
      fi

      if [[ -s "$g/eval_local.txt" ]]; then
        cp "$g/eval_local.txt" "$export_dir/eval_local.txt" 2>/dev/null || true
        cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
        rm -f "$g/eval_local.txt"
      else
        rm -f "$g/eval_local.txt" 2>/dev/null || true
      fi

      for el in "$g"/Peer*/errorlog*.txt "$g"/errorlog*.txt; do
        if [[ -f "$el" && -s "$el" ]]; then
          printf '\n ==== %s: %s ==== %s' "$g" "$(basename "$el")" "\n" >> "$TEST_ROOT/errorlog.txt"
          cat "$el" >> "$TEST_ROOT/errorlog.txt"
          if [[ "$el" == "$g"/Peer*/* ]]; then
            peerdir=$(basename "${el%/*}")
            mkdir -p "$export_dir/$peerdir" 2>/dev/null || true
            cp "$el" "$export_dir/$peerdir/$(basename "$el")" 2>/dev/null || true
          else
            mkdir -p "$hub_target" 2>/dev/null || true
            cp "$el" "$hub_target/$(basename "$el")" 2>/dev/null || true
          fi
        fi
      done

      if command -v find >/dev/null 2>&1; then
        while IFS= read -r -d '' af; do
          [[ -f "$af" ]] || continue
          bname="$(basename "$af")"
          if [[ "$bname" == *PeerA* ]]; then
            target="$export_dir/PeerA/$bname"
          elif [[ "$bname" == *PeerB* ]]; then
            target="$export_dir/PeerB/$bname"
          else
            target="$hub_target/$bname"
          fi
          if [[ ! -d "$(dirname "$target")" ]]; then
          cp -a "$af" "$target" 2>/dev/null || true
          fi
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
  export_name="$(basename "$g")"
  export_name="${export_name%_}"
  export_dir="$TEST_ROOT/testRuns/$export_name/$TEST_ENV_NAME/$date_str/run_$RUN_SEQ"
  mkdir -p "$export_dir" "$export_dir/PeerA" "$export_dir/PeerB"

  cmd_zip="$export_dir/command_lists.zip"
  files_to_zip=()
  if [[ -f "$g/PeerA/${export_name}_PeerA.txt" ]]; then
    files_to_zip+=("$g/PeerA/${export_name}_PeerA.txt")
  fi
  if [[ -f "$g/PeerB/${export_name}_PeerB.txt" ]]; then
    files_to_zip+=("$g/PeerB/${export_name}_PeerB.txt")
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

  if [[ -f "$g/PeerA/${export_name}_PeerA.txt" ]]; then
    mkdir -p "$export_dir/PeerA" 2>/dev/null || true
  fi
  if [[ -f "$g/PeerB/${export_name}_PeerB.txt" ]]; then
    mkdir -p "$export_dir/PeerB" 2>/dev/null || true
  fi

      if [[ -s "$g/eval_local.txt" ]]; then
        cp "$g/eval_local.txt" "$export_dir/eval_local.txt" 2>/dev/null || true
        cat "$g/eval_local.txt" >> "$TEST_ROOT/eval.txt"
        rm -f "$g/eval_local.txt"
      else
        rm -f "$g/eval_local.txt" 2>/dev/null || true
      fi

      for el in "$g"/Peer*/errorlog*.txt "$g"/errorlog*.txt; do
        if [[ -f "$el" && -s "$el" ]]; then
          printf '\n==== %s: %s ====%s' "$g" "$(basename "$el")" "\n" >> "$TEST_ROOT/errorlog.txt"
          cat "$el" >> "$TEST_ROOT/errorlog.txt"
          peerdir=$(basename "${el%/*}")
          mkdir -p "$export_dir/$peerdir" 2>/dev/null || true
          cp "$el" "$export_dir/$peerdir/$(basename "$el")" 2>/dev/null || cp "$el" "$export_dir/$(basename "$el")" 2>/dev/null || true
        fi
      done

      if [[ -f "$g/PeerA/peerAsnmLog.txt" ]]; then
        cp "$g/PeerA/peerAsnmLog.txt" "$export_dir/PeerA/peerAsnmLog.txt" 2>/dev/null || true
      fi
      if [[ -f "$g/PeerA/errorlogPeerA.txt" ]]; then
        cp "$g/PeerA/errorlogPeerA.txt" "$export_dir/PeerA/errorlogPeerA.txt" 2>/dev/null || true
      fi
      if [[ -f "$g/PeerB/peerBsnmLog.txt" ]]; then
        cp "$g/PeerB/peerBsnmLog.txt" "$export_dir/PeerB/peerBsnmLog.txt" 2>/dev/null || true
      fi
      if [[ -f "$g/PeerB/errorlogPeerB.txt" ]]; then
        cp "$g/PeerB/errorlogPeerB.txt" "$export_dir/PeerB/errorlogPeerB.txt" 2>/dev/null || true
      fi


      if command -v find >/dev/null 2>&1; then
        # Route asap logs by filename into PeerA/PeerB if applicable to avoid placing
        # peer logs under the hub subtree.
        while IFS= read -r -d '' af; do
          [[ -f "$af" ]] || continue
          relpath="${af#$g}"
          bname="$(basename "$af")"
          if [[ "$bname" == *PeerA* ]]; then
            target="$export_dir/PeerA/$bname"
          elif [[ "$bname" == *PeerB* ]]; then
            target="$export_dir/PeerB/$bname"
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
    done
  fi
done
wait

ts=$(date +"%Y-%m-%d_%H-%M-%S%Z")
archive="testresults_${ts}.zip"
(
  cd "$TEST_ROOT" || exit 0

  zip -r -q "$archive" testRuns testRunsFailed eval.txt errorlog.txt -x "$archive" "*.sh" "*scriptgenerator.jar" "*SharkNetMessengerCLI.jar" ".runs/*" ".runs" 2>/dev/null || true
)
if [[ -f "$TEST_ROOT/$archive" ]]; then
  echo "Created archive: $TEST_ROOT/$archive"
else
  echo "Failed to create archive $archive" >&2
fi

echo "All scenarios executed. Check eval.txt files for results. Check errorlog.txt for any errors."