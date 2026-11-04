const https = require("https");
const http = require("http");
const url = require("url");

module.exports = async (req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Admin-Token, X-Requested-With, X-Visitor-Public-Ip, X-Visitor-City, X-Visitor-Region, X-Visitor-Country, X-Browser-Hash, X-Browser-Signal, Origin, Referer");

  if (req.method === "OPTIONS") {
    res.status(204).end();
    return;
  }

  const backendBase = (process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL || "https://raviranjan-os-backend.onrender.com").replace(/\/$/, "");
  const targetUrl = url.parse(backendBase + req.url);
  const isHttps = targetUrl.protocol === "https:";
  const transport = isHttps ? https : http;

  const fwdHeaders = { ...req.headers };
  delete fwdHeaders["host"];
  fwdHeaders["host"] = targetUrl.host;

  const clientReq = transport.request({
    hostname: targetUrl.hostname,
    port: targetUrl.port || (isHttps ? 443 : 80),
    path: targetUrl.path,
    method: req.method,
    headers: fwdHeaders
  }, (clientRes) => {
    res.writeHead(clientRes.statusCode, clientRes.headers);
    clientRes.pipe(res);
  });

  clientReq.on("error", (err) => {
    if (!res.headersSent) {
      res.status(502).json({
        success: false,
        message: "Render backend is starting up or temporarily unreachable.",
        error: err.message
      });
    }
  });

  req.pipe(clientReq);
};
