<script setup>
import { ref, computed, onMounted } from 'vue'
import { layoutDay } from './schedule.js'
const user=ref(null), page=ref('schedule'), courses=ref([]), users=ref([]), error=ref(''), notice=ref(''), authMode=ref('login'), credentials=ref({username:'',password:''}), week=ref(1), showForm=ref(false), editing=ref(null), query=ref(''), question=ref(''), answer=ref(''), loading=ref(false)
const blank=()=>({name:'',teacher:'',location:'',weekday:1,startPeriod:1,endPeriod:2,startWeek:1,endWeek:16,color:'mint'})
const showSettings=ref(false), settingsDraft=ref({}), aiSettings=ref({url:'',key:'',model:'',systemPrompt:'你是大学课程学习助手，请用中文准确简洁地回答。',temperature:0.7});
const settingsKey=()=>`kexu.ai.settings.${user.value?.id}`;
function loadSettings(){aiSettings.value={url:'',key:'',model:'',systemPrompt:'你是大学课程学习助手，请用中文准确简洁地回答。',temperature:0.7};try{Object.assign(aiSettings.value,JSON.parse(localStorage.getItem(settingsKey())||'{}'))}catch{}}
function openSettings(){settingsDraft.value={...aiSettings.value};showSettings.value=true}
function saveSettings(){try{localStorage.setItem(settingsKey(),JSON.stringify(settingsDraft.value));aiSettings.value={...settingsDraft.value};showSettings.value=false;notice.value='模型设置已保存在当前浏览器';error.value=''}catch{error.value='无法保存设置，请检查浏览器存储权限'}}
function clearSettings(){localStorage.removeItem(settingsKey());loadSettings();notice.value='已清除当前浏览器的模型设置';showSettings.value=false}
const form=ref(blank()); const days=['一','二','三','四','五','六','日']; const today=new Date().getDay()||7
const palette = { mint:'青绿', peach:'暖橙', blue:'浅蓝', lavender:'淡紫', rose:'玫瑰', amber:'琥珀', teal:'青蓝', sky:'天蓝', citron:'浅黄绿', sand:'暖沙' }
const importColor = ref('mint')
const catalog = ref({teacher: [], location: []})
const periods = ref([])
const timeDraft = ref(null)
const timeError = ref('')
const profileKey = () => `kexu.schedule.${user.value.username}`
const emptyPeriods = () => Array.from({length:12}, () => ({start:'', end:''}))
function loadSchedulePreferences() {
  catalog.value = {teacher: [], location: []}
  periods.value = emptyPeriods()
  try {
    const saved = JSON.parse(localStorage.getItem(profileKey()) || '{}')
    if (saved.catalog) catalog.value = saved.catalog
    if (saved.periods?.length === 12) periods.value = saved.periods
  } catch {}
}
function storeSchedulePreferences() {
  localStorage.setItem(profileKey(), JSON.stringify({catalog:catalog.value, periods:periods.value}))
}
function rememberCourses(items) {
  for (const field of ['teacher','location']) {
    catalog.value[field] = [...new Set([
      ...catalog.value[field], ...items.map(c => c[field]?.trim()).filter(Boolean)
    ])].sort((a,b) => a.localeCompare(b,'zh-CN'))
  }
  try { storeSchedulePreferences() } catch {}
}
function openTimes() {
  timeDraft.value = periods.value.map(period => ({...period}))
  timeError.value = ''
}
function saveTimes() {
  for (const [i, period] of timeDraft.value.entries()) {
    if ((period.start || period.end) && (!period.start || !period.end || period.start >= period.end)) {
      timeError.value = `请填写第 ${i+1} 节的上下课时间，且下课时间须晚于上课时间。`
      return
    }
  }
  const previous = periods.value
  periods.value = timeDraft.value
  try {
    storeSchedulePreferences()
    timeDraft.value = null
  } catch {
    periods.value = previous
    timeError.value = '浏览器无法保存节次时间，请检查存储权限。'
  }
}
function courseTime(course) {
  const start = periods.value[course.startPeriod-1]?.start
  const end = periods.value[course.endPeriod-1]?.end
  return start && end ? `${start}–${end}` : ''
}
const visible=computed(()=>courses.value.filter(c=>week.value>=c.startWeek&&week.value<=c.endWeek&&(`${c.name}${c.teacher||''}${c.location||''}`).toLowerCase().includes(query.value.toLowerCase())))
const dayLayouts=computed(()=>days.map((_,i)=>layoutDay(visible.value.filter(c=>c.weekday===i+1))))
const todayCourses=computed(()=>visible.value.filter(c=>c.weekday===today).sort((a,b)=>a.startPeriod-b.startPeriod))
async function api(path, options={}) { const r=await fetch('/api'+path,{headers:{'Content-Type':'application/json'},...options}); if(!r.ok){let d={};try{d=await r.json()}catch{};throw new Error(d.detail||d.message||`请求失败 (${r.status})`)} return r.status===204||r.headers.get('content-length')==='0'?null:r.json().catch(()=>null) }
async function run(fn){error.value='';notice.value='';try{await fn()}catch(e){error.value=e.message}}
async function load(){courses.value=await api('/courses');rememberCourses(courses.value)}
async function authenticate(){await run(async()=>{if(authMode.value==='register'){await api('/auth/register',{method:'POST',body:JSON.stringify(credentials.value)});notice.value='注册成功，请登录';authMode.value='login'}else{user.value=await api('/auth/login',{method:'POST',body:JSON.stringify(credentials.value)});credentials.value.password='';loadSettings();loadSchedulePreferences();await load()}})}
async function logout(){await api('/auth/logout',{method:'POST'});user.value=null;courses.value=[];timeDraft.value=null;importColor.value='mint';aiSettings.value={url:'',key:'',model:'',systemPrompt:'',temperature:0.7}}
function edit(c){editing.value=c?.id||null;form.value=c?{...c}:blank();showForm.value=true}
async function save(){await run(async()=>{await api(editing.value?`/courses/${editing.value}`:'/courses',{method:editing.value?'PUT':'POST',body:JSON.stringify(form.value)});showForm.value=false;await load();notice.value='课程已保存'})}
async function remove(c){if(!confirm(`确定删除“${c.name}”吗？`))return;await run(async()=>{await api(`/courses/${c.id}`,{method:'DELETE'});await load();notice.value='课程已删除'})}
function csvRows(text){let rows=[],row=[],field='',quoted=false;for(let i=0;i<text.length;i++){let ch=text[i];if(ch==='"'){if(quoted&&text[i+1]==='"'){field+='"';i++}else quoted=!quoted}else if(ch===','&&!quoted){row.push(field);field=''}else if((ch==='\n'||ch==='\r')&&!quoted){if(ch==='\r'&&text[i+1]==='\n')i++;row.push(field);if(row.some(x=>x.trim()))rows.push(row);row=[];field=''}else field+=ch}if(quoted)throw Error('CSV 引号未闭合');if(field||row.length){row.push(field);rows.push(row)}return rows}
async function importCsv(event){const file=event.target.files[0];if(!file)return;const defaultColor=importColor.value;await run(async()=>{const rows=csvRows((await file.text()).replace(/^\uFEFF/,''));const expected=['课程名称','教师','地点','星期','开始节','结束节','开始周','结束周'];if(!rows.length||expected.some((x,i)=>rows[0][i]?.trim()!==x))throw Error('CSV 表头不正确，请下载模板');const hasColor=rows[0].length===9&&rows[0][8].trim()==='颜色';if(rows[0].length!==8&&!hasColor)throw Error('可选的第 9 列须为“颜色”');let ok=0;for(const [index,r] of rows.slice(1).entries()){try{if(r.length!==(hasColor ? 9 : 8)||!r[0]?.trim())throw Error('字段数量或课程名称无效');const nums=r.slice(3,8).map(x=>Number(x));if(nums.some(x=>!Number.isInteger(x)))throw Error('星期、节次和周次必须为整数');const specified=r[8]?.trim();const color=specified?(Object.hasOwn(palette,specified)?specified:Object.keys(palette).find(key=>palette[key]===specified)):defaultColor;if(!color)throw Error('颜色不在可选范围内');await api('/courses',{method:'POST',body:JSON.stringify({name:r[0].trim(),teacher:r[1].trim(),location:r[2].trim(),weekday:nums[0],startPeriod:nums[1],endPeriod:nums[2],startWeek:nums[3],endWeek:nums[4],color})});ok++}catch(e){await load();throw Error(`已导入 ${ok} 条；第 ${index+2} 行失败：${e.message}`)}}await load();notice.value=`成功导入 ${ok} 门课程`});event.target.value=''}
function template(){const b=new Blob(['\ufeff课程名称,教师,地点,星期,开始节,结束节,开始周,结束周,颜色\r\n高等数学,张老师,教学楼 A101,1,1,2,1,16,\r\n'],{type:'text/csv;charset=utf-8'});const a=document.createElement('a');a.href=URL.createObjectURL(b);a.download='课序导入模板.csv';a.click();URL.revokeObjectURL(a.href)}
async function ask(){await run(async()=>{loading.value=true;try{const result=await api('/ai/chat',{method:'POST',body:JSON.stringify({message:question.value,url:aiSettings.value.url,key:aiSettings.value.key,model:aiSettings.value.model,systemPrompt:aiSettings.value.systemPrompt,temperature:Number(aiSettings.value.temperature)})});answer.value=result?.choices?.[0]?.message?.content||'模型未返回文本，请检查服务配置。'}finally{loading.value=false}})}
async function openAdmin(){page.value='admin';await run(async()=>{users.value=await api('/admin/users')})}
async function notifications(){if(!('Notification'in window))return error.value='此浏览器不支持通知';const p=await Notification.requestPermission();if(p==='granted')new Notification('课序提醒已开启',{body:'打开课序时，我们会提醒你今天的课程。'});else notice.value='未获得通知权限'}
onMounted(async()=>{try{user.value=await api('/auth/me');loadSettings();loadSchedulePreferences();await load();if('Notification' in window&&Notification.permission==='granted'&&todayCourses.value.length)new Notification('今天的课程',{body:todayCourses.value.map(c=>`${c.name} · 第${c.startPeriod}节`).join('、')})}catch{user.value=null}})
</script>
<template><header><div class="nav"><div class="brand"><img src="/logo-calendar.svg" alt="课序 logo"><div><b>课序</b><small>COURSE ORDER</small></div></div><nav v-if="user"><button :class="{active:page==='schedule'}" @click="page='schedule'">我的课表</button><button :class="{active:page==='stats'}" @click="page='stats'">课程统计</button><button :class="{active:page==='ai'}" @click="page='ai'">课程助手</button><button v-if="user.role==='ADMIN'" @click="openAdmin">管理</button></nav><div v-if="user" class="account">{{user.username}} <button @click="logout">退出</button></div><span v-else class="nav-note">让每一天，都井然有序。</span></div></header>
<main v-if="!user"><section class="hero"><div class="hero-inner"><div><p class="eyebrow">✳ &nbsp; YOUR CAMPUS, IN ORDER</p><h1>把时间留给<br><em>更重要的事。</em></h1><p class="lead">课表、课程与灵感，都在这里。<br>从容安排每一天，发现学习的更多可能。</p><div class="hero-tag">● &nbsp; 专为大学生活设计的课表空间</div></div><div class="login-card"><div class="card-top"><span>WELCOME TO KEXU</span><span>✳</span></div><h2>{{authMode==='login'?'欢迎回来':'创建你的账号'}}</h2><p>让课序陪伴你的每一个学期。</p><form @submit.prevent="authenticate"><label>用户名<input v-model="credentials.username" required autocomplete="username" maxlength="50" :placeholder="authMode==='register'?'1–50 个字符，支持中文':'输入用户名'"></label><label>密码<input v-model="credentials.password" required type="password" :autocomplete="authMode==='login'?'current-password':'new-password'" placeholder="输入密码"></label><p v-if="error" class="error">{{error}}</p><p v-if="notice" class="success">{{notice}}</p><button class="primary full">{{authMode==='login'?'登录课序':'注册账号'}} <span>↗</span></button></form><button class="switch" @click="authMode=authMode==='login'?'register':'login';error='';notice=''">{{authMode==='login'?'还没有账号？立即注册':'已有账号？返回登录'}} →</button></div></div><div class="hero-foot">A LITTLE MORE ORDER <span>DESIGNED FOR YOUR CAMPUS LIFE &nbsp; 01 / 04</span></div></section><section class="intro"><div><p class="section-label">A BETTER WAY TO PLAN</p><h2>给忙碌的日子，<br>一点<span>秩序感。</span></h2></div><p>从第一节课到学期最后一天，课序帮助你整理课程、掌握学习节奏，让每一份安排都清晰可见。</p></section></main>
<main v-else class="workspace"><div class="workspace-head"><div><p class="section-label">YOUR PERSONAL SPACE / 课序</p><h1>{{page==='schedule'?'这一周，从容开始。':page==='stats'?'看见你的学习节奏。':page==='ai'?'和课程助手聊聊。':'用户管理'}}</h1><p>你好，{{user.username}}。{{page==='schedule'?'为每一天留出清晰的空间。':'让每一次学习都有迹可循。'}}</p></div><button v-if="page==='schedule'" class="primary" @click="edit()">＋ 添加课程</button></div><p v-if="error" class="banner error">{{error}}</p><p v-if="notice" class="banner success">{{notice}}</p>
<template v-if="page==='schedule'"><div class="toolbar"><div class="week-select"><button @click="week=Math.max(1,week-1)">←</button><b>第 {{week}} 周</b><button @click="week=Math.min(30,week+1)">→</button></div><input v-model="query" placeholder="搜索课程、教师或地点" aria-label="搜索课程"><div class="toolbar-actions"><button @click="notifications">♧ 开启提醒</button><button @click="template">↓ 下载模板</button><button @click="openTimes">节次时间</button><label class="import-color">导入颜色<select v-model="importColor"><option v-for="(label,color) in palette" :key="color" :value="color">{{label}}</option></select></label><label class="file">↑ 导入 CSV<input type="file" accept=".csv,text/csv" @change="importCsv"></label></div></div><div class="dashboard"><section class="calendar panel"><div class="panel-heading"><div><span>WEEKLY SCHEDULE</span><h2>我的课表 <i>✳</i></h2></div><small>第 {{week}} 周 / 共 {{visible.length}} 门</small></div><div class="table-scroll"><div class="calendar-grid"><div class="grid-head">节次</div><div class="grid-head" v-for="(d,i) in days" :key="d"><small>DAY 0{{i+1}}</small><b :class="{today:i+1===today}">周{{d}}</b></div><div class="period-column"><div v-for="period in 12" :key="period" class="period">{{String(period).padStart(2,'0')}}<template v-if="periods[period-1]?.start"><time>{{periods[period-1].start}}</time><time>{{periods[period-1].end}}</time></template></div></div><div v-for="(items,day) in dayLayouts" :key="day" class="day-column"><button v-for="item in items" :key="item.course.id" class="lesson" :class="item.course.color||'mint'" :style="{top:`calc(${item.course.startPeriod-1} * var(--period-height) + 3px)`,height:`calc(${item.course.endPeriod-item.course.startPeriod+1} * var(--period-height) - 6px)`,left:`calc(${item.lane/item.lanes*100}% + 2px)`,width:`calc(${100/item.lanes}% - 4px)`}" @click="edit(item.course)"><b>{{item.course.name}}</b><small>{{item.course.location || '地点待定'}} · {{item.course.startPeriod}}–{{item.course.endPeriod}}节</small></button></div></div></div><div v-if="!visible.length" class="empty">这一周还没有课程，添加或导入你的第一门课吧。</div></section><aside><section class="panel today-card"><p class="section-label">TODAY'S CLASSES</p><h2>今天的课程 <span>↗</span></h2><p class="muted">星期{{days[today-1]}} · 第 {{week}} 周</p><div v-if="!todayCourses.length" class="today-empty">今天没有安排课程。<br>享受你的自由时间 ✳</div><div v-for="c in todayCourses" :key="c.id" class="today-item"><small>第 {{c.startPeriod}}–{{c.endPeriod}} 节</small><b>{{c.name}}</b><span>{{c.location||'地点待定'}}</span><small v-if="courseTime(c)">{{courseTime(c)}}</small></div></section><section class="tip"><span>✳</span><h3>好计划，从这里开始。</h3><p>下载模板填写课程，或手动添加，让每一天都更有序。</p><button @click="template">获取导入模板 ↗</button></section></aside></div></template>
<section v-else-if="page==='stats'" class="panel content-panel"><p class="section-label">YOUR SEMESTER AT A GLANCE</p><h2>课程统计</h2><div class="stat-grid"><div><b>{{courses.length}}</b><span>课程总数</span></div><div><b>{{visible.length}}</b><span>本周课程</span></div><div><b>{{visible.reduce((n,c)=>n+c.endPeriod-c.startPeriod+1,0)}}</b><span>本周总节次</span></div><div><b>{{new Set(courses.map(c=>c.teacher).filter(Boolean)).size}}</b><span>授课教师</span></div></div><h3>每日课程数量</h3><div v-for="(d,i) in days" :key="d" class="bar-row"><span>周{{d}}</span><div><i :style="{width:Math.min(100,visible.filter(c=>c.weekday===i+1).length*20)+'%'}"></i></div><b>{{visible.filter(c=>c.weekday===i+1).length}}</b></div><p class="muted">统计按当前选择的第 {{week}} 周和搜索条件计算。</p></section>
<section v-else-if="page==='ai'" class="panel content-panel ai"><div class="ai-heading"><div><p class="section-label">LEARNING COMPANION</p><h2>有什么想了解的？</h2></div><button class="settings-button" @click="openSettings">⚙ 模型设置</button></div><p class="muted">输入课程名称或学习问题，课程助手将为你解答。</p><textarea v-model="question" maxlength="4000" placeholder="例如：介绍一下数据结构这门课的主要内容和学习方法。"></textarea><button class="primary" :disabled="loading||!question.trim()" @click="ask">{{loading?'正在思考…':'发送问题'}} ↗</button><div v-if="answer" class="answer">{{answer}}</div></section>
<section v-else class="panel content-panel"><h2>用户列表</h2><p class="muted">仅管理员可见</p><div v-for="u in users" :key="u.id" class="user-row"><span>{{u.username}}</span><b>{{u.role==='ADMIN'?'管理员':'用户'}}</b></div></section></main>
<div v-if="showSettings" class="overlay" @click.self="showSettings=false"><form class="modal" @submit.prevent="saveSettings"><div class="modal-head"><div><p class="section-label">MODEL SETTINGS</p><h2>课程助手设置</h2></div><button type="button" aria-label="关闭" @click="showSettings=false">✕</button></div><p class="muted">填写兼容 Chat Completions 的完整 API 地址。默认支持 OpenAI、DeepSeek、阿里云百炼、Moonshot 和智谱域名；其他域名需由管理员加入允许列表。DeepSeek 可填写 https://api.deepseek.com 或完整接口地址；三项均留空时使用服务器默认配置。</p><label>API 地址<input v-model.trim="settingsDraft.url" type="url" maxlength="2048" placeholder="例如 https://api.deepseek.com/chat/completions"></label><label>模型名称<input v-model.trim="settingsDraft.model" maxlength="100" placeholder="例如 deepseek-flash"></label><label>API 密钥<input v-model="settingsDraft.key" type="password" maxlength="2048" autocomplete="off" placeholder="输入供应商提供的密钥"></label><label>助手角色<textarea v-model="settingsDraft.systemPrompt" maxlength="2000" rows="3" placeholder="描述希望助手如何回答"></textarea></label><label>回答随机性（0–2）<input v-model.number="settingsDraft.temperature" type="number" min="0" max="2" step="0.1" required></label><p class="muted">设置与密钥仅保存在此浏览器当前账号的本地存储中；共享设备请在使用后清除。请求会经由服务器发送至模型供应商。</p><div class="modal-actions"><button type="button" class="delete" @click="clearSettings">清除设置</button><button class="primary">保存设置 ↗</button></div></form></div><div v-if="user && timeDraft" class="overlay" @click.self="timeDraft=null">
  <form class="modal" @submit.prevent="saveTimes">
    <div class="modal-head"><h2>节次时间</h2><button type="button" aria-label="关闭" @click="timeDraft=null">✕</button></div>
    <p class="muted">按学校作息填写上下课时间。留空的节次只显示编号。</p>
    <div v-for="(period,i) in timeDraft" :key="i" class="time-row">
      <b>第 {{i+1}} 节</b>
      <label>上课<input v-model="period.start" type="time"></label>
      <label>下课<input v-model="period.end" type="time"></label>
      <button type="button" class="delete" @click="period.start='';period.end=''">清空</button>
    </div>
    <p v-if="timeError" class="error" role="alert">{{timeError}}</p>
    <div class="modal-actions"><button class="primary">保存时间</button></div>
  </form>
</div>
<div v-if="showForm" class="overlay" @click.self="showForm=false"><form class="modal" @submit.prevent="save"><div class="modal-head"><div><p class="section-label">COURSE DETAILS</p><h2>{{editing?'编辑课程':'添加课程'}}</h2></div><button type="button" @click="showForm=false">✕</button></div><div class="form-grid"><label class="wide">课程名称<input v-model="form.name" maxlength="100" required></label><label>教师<input v-model="form.teacher" maxlength="100" list="known-teachers"><datalist id="known-teachers"><option v-for="name in catalog.teacher" :key="name" :value="name"></option></datalist></label><label>地点<input v-model="form.location" maxlength="100" list="known-rooms"><datalist id="known-rooms"><option v-for="room in catalog.location" :key="room" :value="room"></option></datalist></label><label>星期<select v-model.number="form.weekday"><option v-for="(d,i) in days" :value="i+1">周{{d}}</option></select></label><label>颜色<select v-model="form.color"><option v-for="(label,color) in palette" :key="color" :value="color">{{label}}</option></select></label><label>开始节<input v-model.number="form.startPeriod" type="number" min="1" max="12" required></label><label>结束节<input v-model.number="form.endPeriod" type="number" min="1" max="12" required></label><label>开始周<input v-model.number="form.startWeek" type="number" min="1" max="30" required></label><label>结束周<input v-model.number="form.endWeek" type="number" min="1" max="30" required></label></div><div class="modal-actions"><button v-if="editing" type="button" class="delete" @click="remove(form);showForm=false">删除课程</button><button class="primary">保存课程 ↗</button></div></form></div><footer><span>© 2026 课序 KEXU</span><span>MAKE SPACE FOR WHAT MATTERS.</span></footer></template>