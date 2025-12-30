#!/bin/bash

download() {
	local url="$1"
	local dir1="$2"
	local dir2="$3"
	local file
	file="$(basename "$url")"
	echo "processing: $file"
	mkdir -p "$dir1" "$dir2"
	if [ -f "$file" ]; then
		echo "already downloaded: $file"
	else
		echo "downloading $file ..."
		if command -v aria2c >/dev/null 2>&1; then
			aria2c -x 4 -s 4 -o "$file" "$url"
		elif command -v wget >/dev/null 2>&1; then
			wget -O "$file" "$url"
		else
			echo "ERROR: aria2c & wget not installed"
			exit 1
		fi
	fi
	cp "$file" "$dir1/"
	cp "$file" "$dir2/"
	echo "placed into: $dir1 and $dir2"
	rm -f "$file"
	echo "cleaned up: $file"
	echo
}

download \
	"https://github.com/P3TERX/GeoLite.mmdb/releases/latest/download/GeoLite2-ASN.mmdb" \
	"./scg-webflux-test/src/main/resources/geolite/"

download \
	"https://github.com/P3TERX/GeoLite.mmdb/releases/latest/download/GeoLite2-City.mmdb" \
	"./scg-webflux-test/src/main/resources/geolite/"

download \
	"https://github.com/P3TERX/GeoLite.mmdb/releases/latest/download/GeoLite2-Country.mmdb" \
	"./scg-webflux-test/src/main/resources/geolite/"
