import {createServer} from 'node:http';
import {createReadStream,existsSync,statSync} from 'node:fs';
import {extname,join,normalize} from 'node:path';

const root=join(process.cwd(),'dist');
const port=Number(process.env.PORT||3000);
const backendOrigin=(process.env.VEROX_BACKEND_BASE_URL||process.env.VITE_VEROX_BACKEND_BASE_URL||'https://verox-backend-production.up.railway.app').replace(/\/$/,'');
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.ico':'image/x-icon','.json':'application/json; charset=utf-8'};
const hopByHopHeaders=new Set(['connection','keep-alive','proxy-authenticate','proxy-authorization','te','trailer','transfer-encoding','upgrade','host','content-length']);

function sendFile(file,response){response.statusCode=200;response.setHeader('Content-Type',types[extname(file)]||'application/octet-stream');response.setHeader('X-Content-Type-Options','nosniff');response.setHeader('Referrer-Policy','strict-origin-when-cross-origin');createReadStream(file).pipe(response)}

function readBody(request){return new Promise((resolve,reject)=>{const chunks=[];let size=0;request.on('data',chunk=>{size+=chunk.length;if(size>1024*1024){reject(new Error('request body too large'));request.destroy();return}chunks.push(chunk)});request.on('end',()=>resolve(chunks.length?Buffer.concat(chunks):undefined));request.on('error',reject)})}

function rewriteCookie(cookie){
  if(!cookie.startsWith('VEROX_CSRF='))return cookie;
  return cookie.replace(/;\s*Path=\/platform\/v1(?=;|$)/i,'; Path=/');
}

async function proxyPlatform(request,response){
  try{
    const incomingUrl=new URL(request.url||'/',`http://${request.headers.host||'localhost'}`);
    const target=new URL(`${incomingUrl.pathname}${incomingUrl.search}`,backendOrigin);
    const headers=new Headers();
    for(const [name,value] of Object.entries(request.headers)){
      if(value==null||hopByHopHeaders.has(name.toLowerCase()))continue;
      if(Array.isArray(value))for(const item of value)headers.append(name,item);else headers.set(name,value);
    }
    headers.set('X-Forwarded-Host',request.headers.host||'');
    headers.set('X-Forwarded-Proto','https');
    const method=request.method||'GET';
    const body=(method==='GET'||method==='HEAD')?undefined:await readBody(request);
    const upstream=await fetch(target,{method,headers,body,redirect:'manual'});
    response.statusCode=upstream.status;
    for(const [name,value] of upstream.headers){
      const lower=name.toLowerCase();
      if(lower==='set-cookie'||hopByHopHeaders.has(lower))continue;
      response.setHeader(name,value);
    }
    const setCookies=typeof upstream.headers.getSetCookie==='function'?upstream.headers.getSetCookie():[];
    if(setCookies.length)response.setHeader('Set-Cookie',setCookies.map(rewriteCookie));
    const payload=Buffer.from(await upstream.arrayBuffer());
    if(!response.hasHeader('Content-Length'))response.setHeader('Content-Length',String(payload.length));
    response.end(payload);
  }catch(error){
    console.error('VEROX Merchant Platform proxy error',error);
    response.statusCode=502;
    response.setHeader('Content-Type','application/json; charset=utf-8');
    response.setHeader('Cache-Control','no-store');
    response.end(JSON.stringify({error:{code:'PLATFORM_PROXY_UNAVAILABLE',message:'Não foi possível contactar o VEROX Server.'}}));
  }
}

createServer((request,response)=>{
  const pathname=decodeURIComponent(new URL(request.url||'/',`http://${request.headers.host||'localhost'}`).pathname);
  if(pathname==='/platform/v1'||pathname.startsWith('/platform/v1/'))return void proxyPlatform(request,response);
  const safe=normalize(pathname).replace(/^(\.\.[/\\])+/, '').replace(/^[/\\]+/,'');
  const candidate=join(root,safe);
  if(candidate.startsWith(root)&&existsSync(candidate)&&statSync(candidate).isFile())return sendFile(candidate,response);
  return sendFile(join(root,'index.html'),response);
}).listen(port,'0.0.0.0',()=>console.log(`VEROX Merchant Platform listening on ${port} with same-origin Platform API proxy`));
