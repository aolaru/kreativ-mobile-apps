// Inspect only test-related controls, omitting contacts and unrelated filenames.
import { execFileSync } from 'node:child_process';
import os from 'node:os';import path from 'node:path';
const [serial, action = 'inspect', target] = process.argv.slice(2);
if (!serial || !/^[\w-]+$/.test(serial)) throw new Error('An explicitly authorized serial is required.');
const adbBinary=path.join(process.env.ANDROID_HOME||path.join(os.homedir(),'Library/Android/sdk'),'platform-tools/adb');
const adb=(...args)=>execFileSync(adbBinary,['-s',serial,...args],{encoding:'utf8',timeout:30000});
const temp='/data/local/tmp/kreativ-test-ui.xml';
let xml;
try {
 try { adb('shell','uiautomator','dump',temp); }
 catch (error) { if (!error.stdout?.includes('UI hierchary dumped to:')) throw error; }
 xml=adb('shell','cat',temp);
}
finally { try{adb('shell','rm','-f',temp);}catch{} }
const decode=s=>s.replaceAll('&amp;','&').replaceAll('&quot;','"').replaceAll('&lt;','<').replaceAll('&gt;','>');
const nodes=[...xml.matchAll(/<node\b[^>]*>/g)].map(([tag])=>Object.fromEntries([...tag.matchAll(/([\w-]+)="([^"]*)"/g)].map(m=>[m[1],decode(m[2])])));
const permitted=new Set(['Save','SAVE','Cancel','CANCEL','Downloads','Download','Show roots','Show navigation drawer','Navigate up','Browse','Internal storage','Phone','Done','Allow']);
if(action==='inspect') {
 const controls=nodes.filter(n=>permitted.has(n.text)||permitted.has(n['content-desc'])||n.text?.startsWith('kreativ-')||/content_preview|chooser|resolver|button1|button2|save|filename|title/i.test(n['resource-id']||''));
 console.log(JSON.stringify({packages:[...new Set(nodes.map(n=>n.package))],controls:controls.map(n=>({text:permitted.has(n.text)||n.text?.startsWith('kreativ-')?n.text:undefined,description:permitted.has(n['content-desc'])?n['content-desc']:undefined,id:n['resource-id'],bounds:n.bounds,enabled:n.enabled}))},null,2));
} else if(action==='tap') {
 if(!permitted.has(target))throw new Error('Only an allowlisted test control may be selected.');
 const node=nodes.find(n=>(n.text===target||n['content-desc']===target)&&n.enabled==='true');
 if(!node)throw new Error(`Test control not found: ${target}`);
 const match=node.bounds.match(/\[(\d+),(\d+)\]\[(\d+),(\d+)\]/);
 if(!match)throw new Error('No bounds');
 adb('shell','input','tap',String(Math.round((+match[1]+ +match[3])/2)),String(Math.round((+match[2]+ +match[4])/2)));
 console.log(`Selected ${target}.`);
} else throw new Error('Unknown action');
