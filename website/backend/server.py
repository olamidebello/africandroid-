#!/usr/bin/env python3
"""Read-only release API; published catalog remains the trusted release source."""
import json, os, re, sqlite3
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

DATA = Path(os.environ.get("AFRICANDROID_DATA", "/var/lib/africandroid-public"))
DB = os.environ.get("AFRICANDROID_DB", "/var/lib/africandroid-api/releases.sqlite3")

def releases():
    data = json.loads((DATA / "catalog.json").read_text())
    rows = data["releases"]
    if not isinstance(rows, list):
        raise ValueError("Invalid release catalog")
    for row in rows:
        digest = row["sha256"]
        if not re.fullmatch(r"[a-f0-9]{64}", digest) or row["url"] != "/downloads/" + digest + ".apk":
            raise ValueError("Invalid download path")
        if row["channel"] not in ("stable", "development") or not isinstance(row["abis"], list):
            raise ValueError("Invalid release metadata")
        if not row["source_url"].startswith("https://github.com/"):
            raise ValueError("Invalid source URL")
        if not (DATA / "downloads" / (digest + ".apk")).is_file():
            raise ValueError("Published APK is missing")
    with sqlite3.connect(DB, timeout=10) as db:
        db.execute("CREATE TABLE IF NOT EXISTS releases (id INTEGER PRIMARY KEY, sha256 TEXT NOT NULL, name TEXT NOT NULL, package TEXT NOT NULL, channel TEXT NOT NULL, metadata TEXT NOT NULL)")
        db.execute("DELETE FROM releases")
        db.executemany("INSERT INTO releases(sha256,name,package,channel,metadata) VALUES(?,?,?,?,?)",
                       [(r["sha256"],r["name"],r["package"],r["channel"],json.dumps(r)) for r in rows])
        return [json.loads(r[0]) for r in db.execute("SELECT metadata FROM releases ORDER BY id")]

class Handler(BaseHTTPRequestHandler):
    def reply(self, status, data):
        body = json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        url = urlparse(self.path)
        if url.path not in ("/api/health", "/api/releases") and not re.fullmatch(r"/api/download/[a-f0-9]{64}", url.path):
            return self.reply(404, {"error":"Not found"})
        try:
            rows = releases()
        except (OSError, ValueError, KeyError, TypeError, sqlite3.Error):
            return self.reply(503, {"error":"Release catalog temporarily unavailable"})
        if url.path == "/api/health":
            return self.reply(200, {"status":"ok", "releases":len(rows), "schema_version":1})
        if url.path == "/api/releases":
            query = parse_qs(url.query)
            term = query.get("q", [""])[0].lower()[:200]
            channel = query.get("channel", ["all"])[0]
            if channel not in ("all", "stable", "development"):
                return self.reply(400, {"error":"Invalid channel"})
            rows = [r for r in rows if (channel == "all" or r["channel"] == channel) and term in (r["name"]+" "+r["package"]).lower()]
            return self.reply(200, {"releases":rows})
        digest = url.path.rsplit("/", 1)[1]
        if not any(r["sha256"] == digest for r in rows):
            return self.reply(404, {"error":"Download not published"})
        self.send_response(302)
        self.send_header("Location", "/downloads/"+digest+".apk")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", "0")
        self.end_headers()

if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
