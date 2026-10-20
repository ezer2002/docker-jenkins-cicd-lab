import json
from http.server import BaseHTTPRequestHandler, HTTPServer

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        size = int(self.headers.get("Content-Length", 0))
        try:
            data = json.loads(self.rfile.read(size))
        except ValueError:
            data = {}
        for a in data.get("alerts", []):
            print("[%s] %s (severity=%s) - %s" % (
                a.get("status"), a["labels"].get("alertname"),
                a["labels"].get("severity"), a["annotations"].get("summary")), flush=True)
        self.send_response(200)
        self.end_headers()

    def log_message(self, *args):
        pass

HTTPServer(("127.0.0.1", 5001), Handler).serve_forever()
