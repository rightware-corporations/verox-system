import {createServer} from 'node:http';
import {request as httpsRequest} from 'node:https';
import {createReadStream,existsSync,statSync} from 'node:fs';
import {extname,join,normalize} from 'node:path';

const root=join(process.cwd(),'dist');
const port=Number(process.env.PORT||3000);
const backendOrigin=(process.env.VEROX_BACKEND_BASE_URL||process.env.VITE_VEROX_BACKEND_BASE_URL||'https://verox-backend-production.up.railway.app').replace(/\/$/,'');
const backendUrl=new URL(backendOrigin);
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.ico':'image/x-icon','.json':'application/json; charset=utf-8'};
const hopByHopHeaders=new Set(['connection','keep-alive','proxy-authenticate','proxy-authorization','te','trailer','transfer-encoding','upgrade','host','content-length']);

function sendFile(file,response){response.statusCode=200;response.setHeader('Content-Type',types[extname(file)]||'application/octet-stream');response.setHeader('X-Content-Type-Options','nosniff');response.setHeader('Referrer-Policy','strict-origin-when-cross-origin');createReadStream(file).pipe(response)}

function rewriteCookie(cookie){
  if(!/^VEROX_(SESSION|CSRF)=/i.test(cookie))return cookie;
  return cookie
    .replace(/;\s*Domain=[^;]+/ig,'')
    .replace(/;\s*Path=\/platform\/v1(?=;|$)/i,'; Path=/');
}

function proxyPlatform(request,response){
  const incomingUrl=new URL(request.url||'/',`http://${request.headers.host||'localhost'}`);
  const headers={};
  for(const [name,value] of Object.entries(request.headers)){
    if(value==null||hopByHopHeaders.has(name.toLowerCase()))continue;
    headers[name]=value;
  }
  headers.host=backendUrl.host;
  headers['x-forwarded-host']=request.headers.host||'';
  headers['x-forwarded-proto']='https';

  const upstream=httpsRequest({
    protocol:backendUrl.protocol,
    hostname:backendUrl.hostname,
    port:backendUrl.port||443,
    method:request.method||'GET',
    path:`${incomingUrl.pathname}${incomingUrl.search}`,
    headers,
  },upstreamResponse=>{
    response.statusCode=upstreamResponse.statusCode||502;
    for(const [name,value] of Object.entries(upstreamResponse.headers)){
      if(value==null||hopByHopHeaders.has(name.toLowerCase())||name.toLowerCase()==='set-cookie')continue;
      response.setHeader(name,value);
    }
    const setCookies=upstreamResponse.headers['set-cookie'];
    if(setCookies?.length)response.setHeader('Set-Cookie',setCookies.map(rewriteCookie));
    upstreamResponse.pipe(response);
  });

  upstream.setTimeout(15000,()=>upstream.destroy(new Error('upstream timeout')));
  upstream.on('error',error=>{
    console.error('VEROX Merchant Platform proxy error',error);
    if(response.headersSent){response.destroy();return;}
    response.statusCode=502;
    response.setHeader('Content-Type','application/json; charset=utf-8');
    response.setHeader('Cache-Control','no-store');
    response.end(JSON.stringify({error:{code:'PLATFORM_PROXY_UNAVAILABLE',message:'Não foi possível contactar o VEROX Server.'}}));
  });
  request.pipe(upstream);
}

createServer((request,response)=>{
  const pathname=decodeURIComponent(new URL(request.url||'/',`http://${request.headers.host||'localhost'}`).pathname);
  if(pathname==='/platform/v1'||pathname.startsWith('/platform/v1/'))return proxyPlatform(request,response);
  const safe=normalize(pathname).replace(/^(\.\.[/\\])+/, '').replace(/^[/\\]+/,'');
  const candidate=join(root,safe);
  if(candidate.startsWith(root)&&existsSync(candidate)&&statSync(candidate).isFile())return sendFile(candidate,response);
  return sendFile(join(root,'index.html'),response);
}).listen(port,'0.0.0.0',()=>console.log(`VEROX Merchant Platform listening on ${port} with same-origin Platform API proxy`));
