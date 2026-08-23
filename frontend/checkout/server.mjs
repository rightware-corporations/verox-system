import {createServer} from 'node:http';
import {createReadStream,existsSync,statSync} from 'node:fs';
import {extname,join,normalize} from 'node:path';

const root=join(process.cwd(),'dist');
const port=Number(process.env.PORT||3000);
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.ico':'image/x-icon','.json':'application/json; charset=utf-8'};

function sendFile(file,response){response.statusCode=200;response.setHeader('Content-Type',types[extname(file)]||'application/octet-stream');response.setHeader('X-Content-Type-Options','nosniff');response.setHeader('Referrer-Policy','no-referrer');createReadStream(file).pipe(response)}

createServer((request,response)=>{
  const pathname=decodeURIComponent(new URL(request.url||'/',`http://${request.headers.host||'localhost'}`).pathname);
  const safe=normalize(pathname).replace(/^(\.\.[/\\])+/, '').replace(/^[/\\]+/,'');
  const candidate=join(root,safe);
  if(candidate.startsWith(root)&&existsSync(candidate)&&statSync(candidate).isFile())return sendFile(candidate,response);
  return sendFile(join(root,'index.html'),response);
}).listen(port,'0.0.0.0',()=>console.log(`VEROX Hosted Checkout listening on ${port}`));
