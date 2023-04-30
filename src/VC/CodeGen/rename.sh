#!/bin/bash

shopt -s nullglob

for file in test[0-9]*; do
    if [[ -f "$file" ]]; then
        newname=$file.vc
        mv "$file" "$newname"
        echo "Renamed file $file to $newname"
    fi
done

shopt -s nullglob

for file in result[0-9]*; do
    if [[ -f "$file" ]]; then
        newname=$(echo "$file" | sed 's/^result/test/' | sed 's/$/.sol/')
        mv "$file" "$newname"
        echo "Renamed file $file to $newname"
    fi
done
