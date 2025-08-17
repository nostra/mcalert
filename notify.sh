#!/usr/bin/env bash

TYPE=$1
GROUP=$2
ALERT="${@:3}"
echo "`date`: Activated with line: $*" >> /tmp/notify.log

if [[ "$TYPE" == "status" ]]; then
  echo "Status change to $GROUP" >> /tmp/notify.log
fi

if [[ "$TYPE" == "alert" ]]; then
  #echo "Alert with $@" >> /tmp/notify.log
  trimmed_ALERT="${ALERT%%[[:space:]]*}"
  if [[ "$GROUP" != "$trimmed_ALERT" ]]; then
      echo "Sending: \"$GROUP\" \"$ALERT\"" >> /tmp/notify.log
      which notify-send >/dev/null 2>&1 && notify-send "$GROUP" "$ALERT" >> /tmp/notify.log 2>&1
      which osascript >/dev/null 2>&1 && osascript -e  "display notification \"$ALERT\" with title \"$GROUP\"" >> /tmp/notify.log 2>&1
      which dunstify >/dev/null 2>&1 && dunstify -u critical "$GROUP" "$ALERT" >> /tmp/notify.log 2>&1

  fi
fi
