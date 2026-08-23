export type AccountResponse = {merchantId:string;name:string;environment:string};

export async function getAccount(signal?:AbortSignal):Promise<AccountResponse>{
  const response=await fetch('/v1/account',{credentials:'include',signal});
  if(!response.ok) throw new Error(`Account API returned ${response.status}`);
  return response.json() as Promise<AccountResponse>;
}

export async function configureWebhook(url:string){
  const response=await fetch('/v1/webhook-endpoint',{
    method:'PUT',
    headers:{'Content-Type':'application/json'},
    credentials:'include',
    body:JSON.stringify({url})
  });
  if(!response.ok) throw new Error(`Webhook endpoint returned ${response.status}`);
  return response.json() as Promise<unknown>;
}
