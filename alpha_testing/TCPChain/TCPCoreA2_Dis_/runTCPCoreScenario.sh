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

# replace FILLER_IP in peer files when IP provided (macOS vs Linux sed)
if [[ $# -eq 1 ]]; then
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' -e "s/FILLER_IP/$ip/g" "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" "$SCRIPT_DIR/PeerB/${folderName}_PeerB.txt" || true
  else
    sed -i -e "s/FILLER_IP/$ip/g" "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" "$SCRIPT_DIR/PeerB/${folderName}_PeerB.txt" || true
  fi
fi


if [[ ! -f "$SCRIPT_DIR/PeerA/${folderName}_PeerA.txt" || ! -f "$SCRIPT_DIR/PeerB/${folderName}_PeerB.txt" ]]; then
  echo "Required files $SCRIPT_DIR/PeerA/${folderName}_PeerA.txt or $SCRIPT_DIR/PeerB/${folderName}_PeerB.txt not found!"
  exit 1
fi

if [[ ! -f "$SCRIPT_DIR/SharkNetMessengerCLI.jar" ]]; then
  echo "Required SharkNetMessengerCLI.jar not found in $SCRIPT_DIR"
  exit 1
fi

(cd "$SCRIPT_DIR/PeerA" && cat "${folderName}_PeerA.txt" | java -jar "$SCRIPT_DIR/SharkNetMessengerCLI.jar" PeerA > peerAsnmLog.txt 2>errorlogPeerA.txt) &
pidA=$!
(cd "$SCRIPT_DIR/PeerB" && cat "${folderName}_PeerB.txt" | java -jar "$SCRIPT_DIR/SharkNetMessengerCLI.jar" PeerB > peerBsnmLog.txt 2>errorlogPeerB.txt) &
pidB=$!

wait "$pidA" "$pidB"

eval_file="$SCRIPT_DIR/eval_local.txt"
{
  echo "$folderName"

  # portable lowercase for older bash (macOS)
  name_lc="$(printf '%s' "$folderName" | tr '[:upper:]' '[:lower:]')"
  countA=0
  countB=0
  if [[ -f "$SCRIPT_DIR/PeerA/peerAsnmLog.txt" ]]; then
    countA=$(grep -F -i -c "$folderName" "$SCRIPT_DIR/PeerA/peerAsnmLog.txt" 2>/dev/null || true)
  fi
  if [[ -f "$SCRIPT_DIR/PeerB/peerBsnmLog.txt" ]]; then
    countB=$(grep -F -i -c "$folderName" "$SCRIPT_DIR/PeerB/peerBsnmLog.txt" 2>/dev/null || true)
  fi
  containsDis="no"
  if [[ "$name_lc" == *dis* ]]; then
    containsDis="yes"
  fi
  echo "DEBUG: folderName=$folderName name_lc=$name_lc containsDis=$containsDis countA=$countA countB=$countB"

  if [[ -f "$SCRIPT_DIR/PeerA/peerAsnmLog.txt" ]]; then
    if (( countA >= 2 )); then
      echo "PeerA: pass: Both messages sent/received successfully."
    else
      if [[ "$containsDis" == "yes" ]]; then
        echo "PeerA: fail: One or both messages failed."
      elif grep -Fiq "$folderName" "$SCRIPT_DIR/PeerA/peerAsnmLog.txt"; then
        echo "PeerA: pass: Message sent/received successfully."
      else
        echo "PeerA: fail"
      fi
    fi
  else
    echo "peerAsnmLog.txt missing"
  fi

  echo "$folderName"
  if [[ -f "$SCRIPT_DIR/PeerB/peerBsnmLog.txt" ]]; then
    if (( countB >= 2 )); then
      echo "PeerB: pass: Both messages sent/received successfully."
    else
      if [[ "$containsDis" == "yes" ]]; then
        echo "PeerB: fail: One or both messages failed."
      elif grep -Fiq "$folderName" "$SCRIPT_DIR/PeerB/peerBsnmLog.txt"; then
        echo "PeerB: pass: Message sent/received successfully."
      else
        echo "PeerB: fail"
      fi
    fi
  else
    echo "peerBsnmLog.txt missing"
  fi
} > "$eval_file"