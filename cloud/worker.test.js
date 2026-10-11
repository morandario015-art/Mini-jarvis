import test from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import worker,{UsageLedger,LIMITS,periods,budget,validateInput} from './worker.js';
function ledger(){
 const db=new DatabaseSync(':memory:');
 const ctx={storage:{sql:{exec(query,...params){const rows=db.prepare(query).all(...params);return {toArray:()=>rows};}},transactionSync(fn){db.exec('BEGIN');try{const value=fn();db.exec('COMMIT');return value;}catch(e){db.exec('ROLLBACK');throw e;}}}};
 return new UsageLedger(ctx);
}
function env(l){return {JARVIS_ACCESS_TOKEN:'x'.repeat(40),FREE_TIER_CONFIRMED:'true',GROQ_API_KEY:'fake-key',USAGE_LEDGER:{idFromName:()=>1,get:()=>({fetch:(url,init)=>l.fetch(new Request(url,init))})}};}
function request(path,body,auth=true){return new Request('https://jarvis.example'+path,{method:body===undefined?'GET':'POST',headers:{...(auth?{authorization:'Bearer '+'x'.repeat(40)}:{}),'content-type':'application/json'},...(body===undefined?{}:{body:JSON.stringify(body)})});}
test('Ecuador day and month rollover, including leap year',()=>{
 assert.deepEqual(periods(new Date('2026-11-01T04:59:59Z')),{day:'2026-10-31',month:'2026-10',daysLeft:1});
 assert.deepEqual(periods(new Date('2026-11-01T05:00:00Z')),{day:'2026-11-01',month:'2026-11',daysLeft:30});
 assert.equal(periods(new Date('2028-02-28T12:00:00Z')).daysLeft,2);
});
test('Daily allocation preserves budget for the whole month',()=>{
 const now=new Date('2026-10-01T15:00:00Z');
 assert.equal(budget('groq',0,0,now).dailyLimit,12);
 assert.equal(budget('groq',5,5,now).dailyLimit,12);
 assert.equal(budget('groq',12,12,now).ok,false);
 assert.equal(budget('groq',0,400,now).ok,false);
 assert.equal(budget('gemini',0,0,now).dailyLimit,9);
});
test('History accepts only alternating user and assistant, with bounded size',()=>{
 assert.equal(validateInput({provider:'groq',message:'Ahora hazlo más breve',history:[{role:'user',content:'Redacta una carta'},{role:'assistant',content:'Aquí está'}]}).length,4);
 for(const input of [{provider:'other',message:'Hola'},{provider:'groq',message:''},{provider:'groq',message:'a'.repeat(4001)},{provider:'groq',message:'Hola',history:[{role:'system',content:'bad'}]},{provider:'groq',message:'a'.repeat(4000),history:[{role:'user',content:'a'.repeat(4000)},{role:'assistant',content:'a'.repeat(4000)},{role:'user',content:'a'},{role:'assistant',content:'a'}]}])assert.throws(()=>validateInput(input));
});
test('Simultaneous reservations cannot exceed the daily allowance',async()=>{
 const l=ledger();const expected=budget('groq',0,0).dailyLimit;
 const responses=await Promise.all(Array.from({length:80},()=>l.fetch(request('/reserve',{provider:'groq'}))));
 assert.equal(responses.filter(r=>r.status===200).length,expected);
 assert.equal(l.snapshot('groq',new Date()).dailyUsed,expected);
 assert.equal(l.snapshot('groq',new Date()).monthlyUsed,expected);
});
test('Monthly exhaustion and storage failure block upstream requests',async()=>{
 const l=ledger(),t=periods();l.sql.exec('INSERT INTO quota VALUES(?,?,?)','groq',t.month,400);
 let calls=0;const original=globalThis.fetch;globalThis.fetch=async()=>{calls++;throw new Error('unexpected');};
 try{
  assert.equal((await worker.fetch(request('/chat',{provider:'groq',message:'Hola'}),env(l))).status,429);
  const broken=env(l);broken.USAGE_LEDGER.get=()=>({fetch:async()=>{throw Error('storage');}});
  assert.equal((await worker.fetch(request('/chat',{provider:'groq',message:'Hola'}),broken)).status,503);
  assert.equal(calls,0);
 }finally{globalThis.fetch=original;}
});
test('Auth, disabled billing gate, malformed body and missing provider key spend no calls',async()=>{
 const l=ledger(),e=env(l);
 assert.equal((await worker.fetch(request('/chat',{provider:'groq',message:'Hola'},false),e)).status,401);
 assert.equal((await worker.fetch(request('/chat',{provider:'groq',message:'Hola'}),{...e,FREE_TIER_CONFIRMED:'false'})).status,503);
 assert.equal((await worker.fetch(request('/chat',{provider:'bad',message:'Hola'}),e)).status,400);
 assert.equal((await worker.fetch(request('/chat',{provider:'gemini',message:'Hola'}),e)).status,503);
 assert.equal(l.snapshot('groq',new Date()).monthlyUsed,0);
});
test('Text context reaches Groq; exactly one attempt and failed attempt counts',async()=>{
 const l=ledger(),e=env(l);const original=globalThis.fetch;let calls=0;
 globalThis.fetch=async(url,init)=>{calls++;const body=JSON.parse(init.body);assert.equal(body.messages[2].content,'Respuesta anterior');assert.equal(body.messages[3].content,'Continúa');return new Response(JSON.stringify({choices:[{message:{content:'Sigo con el tema'}}]}));};
 try{
  const r=await worker.fetch(request('/chat',{provider:'groq',message:'Continúa',history:[{role:'user',content:'Tema anterior'},{role:'assistant',content:'Respuesta anterior'}]}),e);
  assert.equal((await r.json()).answer,'Sigo con el tema');assert.equal(calls,1);
  globalThis.fetch=async()=>{calls++;return new Response('{}',{status:429});};
  assert.equal((await worker.fetch(request('/chat',{provider:'groq',message:'Hola'}),e)).status,502);
  assert.equal(calls,2);assert.equal(l.snapshot('groq',new Date()).monthlyUsed,2);
 }finally{globalThis.fetch=original;}
});
test('Gemini gets separate system instruction and model role history',async()=>{
 const l=ledger(),e={...env(l),GEMINI_API_KEY:'fake'};const original=globalThis.fetch;
 globalThis.fetch=async(url,init)=>{const body=JSON.parse(init.body);assert.ok(body.systemInstruction.parts[0].text.includes('Jarvis'));assert.equal(body.contents[1].role,'model');return new Response(JSON.stringify({candidates:[{content:{parts:[{thought:true,text:'private reasoning'},{text:'Hola Darío'}]}}]}));};
 try{assert.equal((await (await worker.fetch(request('/chat',{provider:'gemini',message:'Continúa',history:[{role:'user',content:'Hola'},{role:'assistant',content:'Aquí estoy'}]}),e)).json()).answer,'Hola Darío');}finally{globalThis.fetch=original;}
});
