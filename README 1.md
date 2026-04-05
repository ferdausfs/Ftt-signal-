# FTT Signal v6.5 — Android App

WebView-based Android app with native background scanning & notifications.

## 🚀 GitHub এ Setup করার নিয়ম

### Step 1: Repository তৈরি করো
GitHub এ নতুন repository তৈরি করো (যেমন: `FTTSignal`)

### Step 2: সব files push করো
এই ZIP এর সব files GitHub repository তে upload করো।

### Step 3: তোমার HTML add করো ⚠️ IMPORTANT
`app/src/main/assets/app.html` file এ তোমার **original FTT Signal HTML** paste করো।

HTML এর `</head>` ট্যাগের আগে এই script add করো:

```html
<script>
(function(){
  var _t=0;
  function init(){
    if(!window.AndroidBridge){if(_t++<20)setTimeout(init,300);return;}
    window.FTT_NATIVE=true;
    // API base restore
    try{var n=AndroidBridge.getApiBase();if(n&&n.startsWith('http')){if(window.CONFIG){window.CONFIG.API_BASE=n;window.API=n;}localStorage.setItem('ftt_api_base',n);}}catch(e){}
    // Notifications
    window.sendNotif=function(p,d,c){try{AndroidBridge.notify(p,d,parseInt(c)||0);}catch(e){}};
    window.notifPerm=function(){try{return AndroidBridge.notifPermStatus();}catch(e){return'denied';}};
    window.requestNotifPerm=function(){try{AndroidBridge.requestNotifPermission();}catch(e){}};
    // Beep + vibrate
    var _b=window.beep;if(typeof _b==='function'){window.beep=function(t){_b.call(this,t);try{AndroidBridge.vibrate(t==='BUY'||t==='WIN'?200:350);}catch(e){};};}
    // Background scan
    var _tw=window.toggleWLScan;if(typeof _tw==='function'){window.toggleWLScan=function(){_tw.call(this);setTimeout(function(){try{if(window.wlActive)AndroidBridge.startScan(JSON.stringify(window.wlPairs||[]),window.wlInterval||5);else AndroidBridge.stopScan();}catch(e){}},200);};}
    console.log('[FTT] Native bridge active');
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);
  else setTimeout(init,0);
})();
</script>
```

### Step 4: GitHub Actions দিয়ে APK build করো
Repository → Actions → "Build FTT Signal APK" → Run workflow

APK automatically build হবে। Actions tab এ গিয়ে Artifacts থেকে download করো।

---

## 📱 Features

| Feature | Web | Android |
|---------|-----|---------|
| Signal display | ✅ | ✅ |
| Notifications | Browser only | ✅ Native |
| Background scan | ❌ | ✅ Foreground Service |
| Vibration | ❌ | ✅ |
| Offline cache | SessionStorage | SharedPreferences |
| API URL change | localStorage | Native + localStorage |

## 🔔 Background Scanning কীভাবে কাজ করে

1. Watch tab → pairs add করো
2. **▶ Start** button press করো  
3. App background এ গেলেও `ScanService` চলতে থাকে
4. নতুন BUY/SELL signal এলে notification আসে
5. Notification tap করলে app open হয়ে signal দেখায়

## 🔑 Signed APK (optional)

GitHub Secrets এ add করো:
- `KEYSTORE_BASE64` — base64 encoded keystore
- `KEYSTORE_PASS` — keystore password  
- `KEY_ALIAS` — key alias
- `KEY_PASS` — key password

```bash
# Keystore তৈরি করো:
keytool -genkey -v -keystore ftt.jks -alias ftt -keyalg RSA -keysize 2048 -validity 10000

# Base64 করো:
base64 -w 0 ftt.jks
```

## 📂 Project Structure

```
FTTSignal/
├── app/src/main/
│   ├── assets/
│   │   ├── app.html          ← তোমার HTML এখানে
│   │   └── index.html        ← loader (change নাও)
│   ├── java/com/ftt/signal/
│   │   ├── MainActivity.kt   ← WebView + bridge injection
│   │   ├── ScanService.kt    ← Background foreground service
│   │   ├── JsBridge.kt       ← JS ↔ Native interface
│   │   ├── NotificationHelper.kt
│   │   └── FttApplication.kt
│   └── res/...
├── .github/workflows/
│   └── build.yml             ← GitHub Actions
└── README.md
```
