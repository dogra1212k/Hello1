#!/usr/bin/env python3
"""Check the exact built-in files are reachable complete films, without downloading them.

This runs separately from unit tests: external media availability can change.
The report records what was verified from the CI runner, not on an Android device.
"""
import concurrent.futures
import json
from pathlib import Path
import struct
import sys
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
LIMIT = 2 * 1024 * 1024


def read_range(url, byte_range):
    request = urllib.request.Request(url, headers={"Range": byte_range, "User-Agent": "StreamBox-source-check/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read(LIMIT), dict(response.headers), response.status


def duration(data):
    index = data.find(b"mvhd")
    if index < 0:
        return None
    version = data[index + 4]
    if version == 0:
        scale, ticks = struct.unpack_from(">II", data, index + 16)
    elif version == 1:
        scale, ticks = struct.unpack_from(">IQ", data, index + 24)
    else:
        raise ValueError("Unsupported movie header")
    return ticks / scale if scale else None


def check(movie):
    result = {"title": movie["title"], "url": movie["videoUrl"]}
    try:
        data, headers, status = read_range(movie["videoUrl"], f"bytes=0-{LIMIT - 1}")
        seconds = duration(data)
        if seconds is None:
            tail, _, _ = read_range(movie["videoUrl"], f"bytes=-{LIMIT}")
            seconds = duration(tail)
        if seconds is None or seconds < movie["minimumDurationSeconds"]:
            raise ValueError(f"Expected complete film; observed duration {seconds}")
        poster, _, poster_status = read_range(movie["posterUrl"], "bytes=0-1023")
        if not poster.startswith((b"\xff\xd8\xff", b"\x89PNG")):
            raise ValueError("Poster did not return an image")
        result.update(status="passed", http_status=status, duration_seconds=round(seconds, 2), poster_status=poster_status)
    except Exception as exc:
        result.update(status="failed", error=str(exc))
    return result


def main():
    movies = json.loads((ROOT / "app/src/main/assets/open_movies.json").read_text())
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        results = list(executor.map(check, movies))
    report = ROOT / "app/build/reports/media-sources.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(results, indent=2) + "\n")
    for item in results:
        print(json.dumps(item))
    return 0 if all(item["status"] == "passed" for item in results) else 1


if __name__ == "__main__":
    sys.exit(main())
