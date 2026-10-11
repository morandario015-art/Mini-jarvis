/**
 * JARVIS cloud gateway — Cloudflare Worker.
 * Secrets: GROQ_API_KEY, GEMINI_API_KEY, JARVIS_ACCESS_TOKEN.
 * KV binding: USAGE. No paid fallback and no API keys in the Android APK.
 * One personal user; do not expose as a multi-tenant service without authentication redesign.
 */
const LIMITS={groq:{day:20,month:400},gemini:{day:15,month:300}};
const HEADERS={"content-type":"application/json;charset=utf-8","cache-control":"no-store"};
const json=(obj,status=200)=>new Response(JSON.stringify(obj),{status,headers:HEADERS});
const utc=()=>new Date().toISOString();
const day=()=>utc().slice(0,10);
const month=()=>utc().slice(0,7);
const key=(provider,period)=>"quota:"+provider+":"+period;
async function count(env,provider,period){return Number(await env.USAGE.get(key(provider,period))||0);}
async function allowed(env,provider){
 const l=LIMITS[provider],d=await count(env,provider,day()),m=await count(env,provider,month());
 return {ok:d<l.day&&m<l.month,dailyUsed:d,monthlyUsed:m,dailyLimit:l.day,monthlyLimit:l.month};
}
async function increment(env,provider){
 const d=key(provider,day()),m=key(provider,month());
 await env.USAGE.put(d,String(await count(env,provider,day())+1),{expirationTtl:86400*3});
 await env.USAGE.put(m,String(await count(env,provider,month())+1),{expirationTtl:86400*40});
}
function plan(data){
 const daysLeft=Math.max(1,new Date(Date.UTC(+month().slice(0,4),+month().slice(5,7),0)).getUTCDate()-new Date().getUTCDate()+1);
 return Object.fromEntries(Object.entries(data).map(([name,q])=>[name,{...q,recommendedDailyRemaining:Math.max(0,Math.floor((q.monthlyLimit-q.monthlyUsed)/daysLeft)),warning:q.monthlyUsed>=q.monthlyLimit*.8||q.dailyUsed>=q.dailyLimit*.8}]));
}
async function groq(env,messages){
 const r=await fetch("https://api.groq.com/openai/v1/chat/completions",{method:"POST",headers:{"authorization":"Bearer "+env.GROQ_API_KEY,"content-type":"application/json"},body:JSON.stringify({model:"openai/gpt-oss-20b",messages,temperature:0.5,max_completion_tokens:750})});
 if(!r.ok)throw new Error("Groq HTTP "+r.status);
 const v=await r.json();return v.choices?.[0]?.message?.content||"No recibí una respuesta.";
}
async function gemini(env,messages){
 const prompt=messages.map(m=>(m.role==="user"?"Usuario: ":"Asistente: ")+m.content).join("\n");
 const r=await fetch("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",{method:"POST",headers:{"x-goog-api-key":env.GEMINI_API_KEY,"content-type":"application/json"},body:JSON.stringify({contents:[{role:"user",parts:[{text:prompt}]}],generationConfig:{maxOutputTokens:750}})});
 if(!r.ok)throw new Error("Gemini HTTP "+r.status);
 const v=await r.json();return v.candidates?.[0]?.content?.parts?.map(x=>x.text||"").join("")||"No recibí una respuesta.";
}
export default {async fetch(request,env){
 if(!env.USAGE||!env.JARVIS_ACCESS_TOKEN)return json({error:"Configuración incompleta"},503);
 const auth=request.headers.get("authorization")||"";
 if(auth!=="Bearer "+env.JARVIS_ACCESS_TOKEN)return json({error:"No autorizado"},401);
 const url=new URL(request.url);
 if(request.method==="GET"&&url.pathname==="/budget"){
  const data={};for(const p of Object.keys(LIMITS))data[p]=await allowed(env,p);
  return json({providers:plan(data),recommendation:"Prioriza órdenes locales; reserva la nube para investigación y redacción."});
 }
 if(request.method!=="POST"||url.pathname!=="/chat")return json({error:"Ruta desconocida"},404);
 let input;try{input=await request.json();}catch{return json({error:"JSON inválido"},400);}
 const provider=input.provider==="gemini"?"gemini":"groq";
 const prompt=String(input.message||"").trim().slice(0,4000);
 if(!prompt)return json({error:"Falta el mensaje"},400);
 const quota=await allowed(env,provider);
 if(!quota.ok)return json({error:"Cuota interna agotada. JARVIS continúa en modo local.",quota},429);
 if(!(provider==="groq"?env.GROQ_API_KEY:env.GEMINI_API_KEY))return json({error:"Proveedor no configurado"},503);
 // Reserve before request; conservative accounting avoids accidental retries consuming free-tier allowance.
 await increment(env,provider);
 try{
  const system="Eres JARVIS, asistente Android en español. Sé útil y claro. No afirmes que abriste apps, investigaste en web o guardaste archivos si no hay herramienta ejecutada. No inventes fuentes. Para tareas escolares, pide datos y explica los pasos.";
  const messages=[{role:"system",content:system},{role:"user",content:prompt}];
  const answer=provider==="groq"?await groq(env,messages):await gemini(env,messages);
  return json({answer,provider,quota:await allowed(env,provider)});
 }catch(e){return json({error:"Proveedor temporalmente no disponible. No se efectuó ningún cargo de pago desde JARVIS.",detail:String(e.message).slice(0,100)},502);}
 }};
