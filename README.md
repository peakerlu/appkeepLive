## 进程保活

源码地址

https://github.com/peakerlu/appkeepLive

当系统内存不足时,系统根据自己的进程回收机制来判断是否kill 掉进程,以腾出内存来供给需要的app, 这套杀进程回收内存的机制就叫 low memory kill

#### 进程的优先级

前台进程--关键优先级

可见进程--高优先级

服务进程--高优先级

后台进程--低优先级

空进程	--低优先级

#### 杀死进程

当手机内存达到一定的范围时,会杀死指定优先级的进程

我们可以使用命令 查看内存情况  和oom_adj 的值 和内存阈值(单位是/4kb)

18432,23040,27648,32256,36864,46080

最小的代表 当前台进程内存小于18432的时候会杀死进程

最大的46080 代表当内存小于46080的时候会杀死空进程

oom_adj 值越小进程的优先级越高

```
E:\360Downloads\JavaDemo>adb -e shell
generic_x86:/ $ su
generic_x86:/ # cat /sys/module/lowmemorykiller/parameters/minfree
18432,23040,27648,32256,36864,46080
1|generic_x86:/ # cat /proc/25613/oom_adj
0
```

#### ActivityManager.java 

| ProcessState级别                       | 取值 | 解释                                                         |
| -------------------------------------- | ---- | ------------------------------------------------------------ |
| PROCESS_STATE_CACHED_EMPTY             | 16   | 进程处于cached状态，且为空进程                               |
| PROCESS_STATE_CACHED_ACTIVITY_CLIENT   | 15   | 进程处于cached状态，且为另一个cached进程(内含Activity)的client进程 |
| PROCESS_STATE_CACHED_ACTIVITY          | 14   | 进程处于cached状态，且内含Activity                           |
| PROCESS_STATE_LAST_ACTIVITY            | 13   | 后台进程，且拥有上一次显示的Activity                         |
| PROCESS_STATE_HOME                     | 12   | 后台进程，且拥有home Activity                                |
| PROCESS_STATE_RECEIVER                 | 11   | 后台进程，且正在运行receiver                                 |
| PROCESS_STATE_SERVICE                  | 10   | 后台进程，且正在运行service                                  |
| PROCESS_STATE_HEAVY_WEIGHT             | 9    | 后台进程，但无法执行restore，因此尽量避免kill该进程          |
| PROCESS_STATE_BACKUP                   | 8    | 后台进程，正在运行backup/restore操作                         |
| PROCESS_STATE_IMPORTANT_BACKGROUND     | 7    | 对用户很重要的进程，用户不可感知其存在                       |
| PROCESS_STATE_IMPORTANT_FOREGROUND     | 6    | 对用户很重要的进程，用户可感知其存在                         |
| PROCESS_STATE_TOP_SLEEPING             | 5    | 与PROCESS_STATE_TOP一样，但此时设备正处于休眠状态            |
| PROCESS_STATE_FOREGROUND_SERVICE       | 4    | 拥有一个前台Service                                          |
| PROCESS_STATE_BOUND_FOREGROUND_SERVICE | 3    | 拥有一个前台Service，且由系统绑定                            |
| PROCESS_STATE_TOP                      | 2    | 拥有当前用户可见的top Activity                               |
| PROCESS_STATE_PERSISTENT_UI            | 1    | persistent系统进程，并正在执行UI操作                         |
| PROCESS_STATE_PERSISTENT               | 0    | persistent系统进程                                           |
| PROCESS_STATE_NONEXISTENT              | -1   | 不存在的进程                                                 |

# ADJ 调度算法的核心方法

- `updateOomAdjLocked`：更新adj，当目标进程为空，或者被杀则返回false；否则返回true;
- `computeOomAdjLocked`：计算adj，返回计算后RawAdj值;
- `applyOomAdjLocked`：使用adj，当需要杀掉目标进程则返回false；否则返回true。



## updateOomAdjLocked

过程比较复杂，主要分为更新adj(满足条件则杀进程)和根据memFactor来调度执行TrimMemory操作； 

第一部分：更新adj(满足条件则杀进程) 

- 遍历mLruProcesses进程 
  - 当进程未分配adj的情况
    - 当进程procState=14或15，则设置`adj=curCachedAdj(初始化=9)`; 
      - 当curCachedAdj != nextCachedAdj，且stepCached大于cachedFactor时 则`curCachedAdj = nextCachedAdj`，（nextCachedAdj加2，nextCachedAdj上限为15）；
    - 否则，则设置`adj=curEmptyAdj(初始化=9)`; 
      - 当curEmptyAdj != nextEmptyAdj，且stepEmpty大于EmptyFactor时 则`curEmptyAdj = nextEmptyAdj`，（nextEmptyAdj加2，nextEmptyAdj上限为15）；
  - 根据当前进程procState状态来决策： 
    - 当curProcState=14或15，且cached进程超过上限(cachedProcessLimit=16)，则杀掉该进程
    - 当curProcState=16的前提下： 
      - 当空进程超过上限(TRIM_EMPTY_APPS=8)，且空闲时间超过30分钟，则杀掉该进程
      - 否则，当空进程超过上限(emptyProcessLimit=16)，则杀掉该进程
  - 没有services运行的孤立进程，则杀掉该进程；

第二部分：根据memFactor来调度执行TrimMemory操作； 

- 根据CachedAndEmpty个数来调整内存因子memFactor(值越大，级别越高)： 
  - 当CachedAndEmpty < 3，则memFactor=3；
  - 当CachedAndEmpty < 5，则memFactor=2；
  - 当CachedAndEmpty >=5，且numCached<=5,numEmpty<=8，则memFactor=1；
  - 当numCached>5 或numEmpty>8，则memFactor=0；
- 当内存因子不是普通0级别的情况下，根据memFactor来调整前台trim级别(fgTrimLevel): 
  - 当memFactor=3，则fgTrimLevel=TRIM_MEMORY_RUNNING_CRITICAL；
  - 当memFactor=2，则fgTrimLevel=TRIM_MEMORY_RUNNING_LOW；
  - 否则(其实就是memFactor=1)，则fgTrimLevel=TRIM_MEMORY_RUNNING_MODERATE 
  - 再遍历mLruProcesses队列进程： 
    - 当curProcState > 12且没有被am杀掉，则执行TrimMemory操作；
    - 否则，当curProcState = 9 且trimMemoryLevel<TRIM_MEMORY_BACKGROUND，则执行TrimMemory操作；
    - 否则，当curProcState > 7， 且pendingUiClean =true时 
      - 当trimMemoryLevel<TRIM_MEMORY_UI_HIDDEN，则执行TrimMemory操作；
      - 当trimMemoryLevel<fgTrimLevel，则执行TrimMemory操作；
- 当内存因子等于0的情况下,遍历mLruProcesses队列进程： 
  - 当curProcState >=7, 且pendingUiClean =true时, 
    - 当trimMemoryLevel< TRIM_MEMORY_UI_HIDDEN，则执行TrimMemory操作；

## computeOomAdjLock()

Service情况

当adj>0 或 schedGroup为后台线程组 或procState>2时： 

- 当service已启动，则procState<=10； 
  - 当service在30分钟内活动过，则adj=5,cached=false;
- 获取service所绑定的connections 
  - 当client与当前app同一个进程，则continue;
  - 当client进程的ProcState >=ActivityManager.PROCESS_STATE_CACHED_ACTIVITY，则设置为空进程
  - 当进程存在显示的ui，则将当前进程的adj和ProcState值赋予给client进程
  - 当不存在显示的ui，且service上次活动时间距离现在超过30分钟，则只将当前进程的adj值赋予给client进程
  - 当前进程adj > client进程adj的情况 
    - 当service进程比较重要时，则设置adj >= -11
    - 当client进程adj<2,且当前进程adj>2时，则设置adj=2;
    - 当client进程adj>1时，则设置adj = clientAdj
    - 否则，设置adj <= 1；
    - 若client进程不是cache进程，则当前进程也设置为非cache进程
  - 当绑定的是前台进程的情况 
    - 当client进程状态为前台时，则设置mayBeTop=true，并设置client进程procState=16
    - 当client进程状态 < 2的前提下：若绑定前台service，则clientProcState=3；否则clientProcState=6
  - 当connections并没有绑定前台service时，则clientProcState >= 7
  - 保证当前进程procState不会比client进程的procState大
- 当进程adj >0，且activity可见 或者resumed 或 正在暂停，则设置adj = 0

## applyOomAdjLocked

- curRawAdj != setRawAdj
- 进程当前OOM的校准 != 进程最后的OOM校准(app.curAdj != app.setAdj)
  - 将adj值 发送给lmkd守护进程
- 最后设置的调度组 != 当前所需的调度组(app.setSchedGroup != app.curSchedGroup)
  - 等待被杀
    - 杀进程，并设置success = false
  - else
    - 设置进程组信息
    - 调整进程的swappiness值
-  最近的前台Activity != 正在运行的前台活动(app.repForegroundActivities != app.foregroundActivities)
- 最近的进程状态 != 当前的进程状态（app.repProcState != app.curProcState）
  - 设置进程状态
- 当setProcState = -1或者curProcState与setProcState值不同时
  - 计算pss下次时间
- else
  - 当前时间超过pss下次时间，则请求统计pss,并计算pss下次时间
- 进程跟踪器最后的状态 != 当前的状态

# 保活的方式

## 	一像素界面保活

主要是通过广播监听手机的息屏和亮屏,来控制这个一像素界面的显示和隐藏

```kotlin
/**
 * 一像素保活主要代码
 */
class OnePixelReceiver : BroadcastReceiver() {
    var mHander: Handler? = null
    init {
         mHander = Handler(Looper.getMainLooper())
    }
    var screenOn = true
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(Intent.ACTION_SCREEN_OFF)) {//屏幕关闭的时候
            screenOn = false
            mHander?.postDelayed({
                if (!screenOn) {//打开界面
                    val intent2 = Intent(context, OnePixelActivity::class.java)
                    intent2.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)//打开app界面 需要时这个
                    val pendingIntent = PendingIntent.getActivity(context, 0, intent2, 0)
                    try {
                        pendingIntent.send()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, 1000)
            //这里可以使用播放无声音乐的方法保活
        } else if (intent?.action.equals(Intent.ACTION_SCREEN_ON)) {//屏幕打开的时候
            screenOn = true
            //关闭一像素界面
          //也可以在OnePixelActivity界面的onResum方法中判断是息屏还是亮屏
            //获取通过其他方式关闭界面
        }
    }
}
```

```xml
  <activity
            android:name=".activity.OnePixelActivity"
            android:excludeFromRecents="true"//是否可以自由出栈
            android:taskAffinity="com.peaker.keep"//定义进程
            android:theme="@style/KeepTheme"></activity>
```

`任务栈TaskAffinity  属性`
`定义activity栈名称`
`TaskAffinity 属性一般跟singleTask模式或者跟allowTaskReparenting属性结合使用`
`allowTaskReparenting 默认是false` 
`它的主要作用是activity的迁移，即从一个task迁移到另一个task，这个迁移跟activity的taskAffinity有关。当allowTaskReparenting的值为“true”时,，则表示Activity能从启动的Task移动到有着affinity的Task（当这个Task进入到前台时），当allowTaskReparenting的值为“false”，表示它必须呆在启动时呆在的那个Task里。如果这个特性没有被设定，元素(当然也可以作用在每次activity元素上)上的allowTaskReparenting属性的值会应用到Activity上。默认值为“false”。`

## 前台进程保活

原理: 启动一个前台服务,从而提高整个应用的优先级

前台进程:当操作所必需的的进程时,如果一个进程满足以下任一条件,即为前台进程

1. 托管用户正在交互的Activity(调用了onResume()方法)

2. 托管某个service ,绑定到用户正在交互的Activity

3. 托管正在前台运行Service(服务已经调用startForeground())

4. 托管正在执行一个生命周期的回调的 Service (onCreate()  onStart()  onDestroy() )

5. 托管正在执行 onReceive() 方法的 广播 BoadcastReceiver

   `很明显满足第三条的` 内容

   这个方式主要的是通知的适配  主要代码如下:

   ```kotlin
   class ForegroundService : Service() {
       companion object {
           val TAG = "ForegroundService"
           val SERVICE_ID = 11212
           val CHANNEL = "channel"
       }
       @Nullable
       override fun onBind(intent: Intent?): IBinder? {
           return null
       }
       override fun onCreate() {
           super.onCreate()
           if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) { //4.3以下
               //将service设置成前台服务，并且不显示通知栏消息
               startForeground(SERVICE_ID, Notification())
           } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { //Android4.3-->Android7.0
               //将service设置成前台服务
               startForeground(SERVICE_ID, Notification())
               //删除通知栏消息
               startService(Intent(this, InnerService::class.java))
           } else { // 8.0 及以上
               //通知栏消息需要设置channel
               val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
               //NotificationManager.IMPORTANCE_MIN 通知栏消息的重要级别  最低，不让弹出
               //IMPORTANCE_MIN 前台时，在阴影区能看到，后台时 阴影区不消失，增加显示 IMPORTANCE_NONE时 一样的提示
               //IMPORTANCE_NONE app在前台没有通知显示，后台时有
               val channel = NotificationChannel(CHANNEL, "通知", NotificationManager.IMPORTANCE_NONE)
               manager.createNotificationChannel(channel)
               val notification: Notification = Notification.Builder(this, CHANNEL).build()
               //将service设置成前台服务，8.x退到后台会显示通知栏消息，9.0会立刻显示通知栏消息
               startForeground(SERVICE_ID, notification)
           }
       }
       class InnerService : Service() {
           override fun onCreate() {
               super.onCreate()
               Log.e(TAG, "InnerService 服务创建了")
               // 让服务变成前台服务
               startForeground(SERVICE_ID, Notification())
               // 关闭自己
               stopSelf()
           }
           @Nullable
           override fun onBind(intent: Intent): IBinder? {
               return null
           }
       }
   }
   ```

## 进程拉活

   ### 通过广播拉活

   在发生特定系统事件时，系统会发出广播，通过在 AndroidManifest 中静态注册对应的广播监听器，即可在发生响应事件时拉活。但是从android 7.0开始，对广播进行了限制，而且在8.0更加严格

https://developer.android.google.cn/about/versions/oreo/background.html#broadcasts
   可静态注册广播列表：
  	 https://developer.android.google.cn/guide/components/broadcast-exceptions.html

   “全家桶”拉活
	有多个app在用户设备上安装，只要开启其中一个就可以将其他的app也拉活。比如手机里装了手Q、QQ空间、兴趣部落等等，那么打开任意一个app后，其他的app也都会被唤醒。

###    Service系统机制拉活

   START_STICKY：
	“粘性”。如果service进程被kill掉，保留service的状态为开始状态，但不保留递送的intent对象。随后系统会尝试重新创建service，由于服务状态为开始状态，所以创建服务后一定会调用onStartCommand(Intent,int,int)方法。如果在此期间没有任何启动命令被传递到service，那么参数Intent将为null。

START_NOT_STICKY：
	“非粘性的”。使用这个返回值时，如果在执行完onStartCommand后，服务被异常kill掉，系统不会自动重启该服务。

START_REDELIVER_INTENT：
	重传Intent。使用这个返回值时，如果在执行完onStartCommand后，服务被异常kill掉，系统会自动重启该服务，并将Intent的值传入。

START_STICKY_COMPATIBILITY：
	START_STICKY的兼容版本，但不保证服务被kill后一定能重启。

只要 targetSdkVersion 不小于5，就默认是 START_STICKY。
但是某些ROM 系统不会拉活。并且经过测试，Service 第一次被异常杀死后很快被重启，第二次会比第一次慢，第三次又会比前一次慢，一旦在短时间内 Service 被杀死4-5次，则系统不再拉起

###    账户同步拉活

通过在系统上添加同步账号拉活app 但是时间不能把控

主要代码如下:

```kotlin
object AccountHelper {
    private val TAG = "AccountHelper"
    private val ACCOUNT_TYPE = "com.peakerkeepliveapp.account"
    /**
     * 添加账号
     */
    fun addAccount(context: Context) {
        var accountManager = context.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager
        //获取此类型的账户
        //需要增加权限 GET_ACCOUNTS
        var accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
        if (accounts.isNotEmpty()) {
            //账户已存在
            Log.d(TAG, "addAccount: 账户已存在")
            return
        }
        val account = Account("keeplive", ACCOUNT_TYPE)
        // 给这个账户类型添加一个账户
        // 需要增加权限  AUTHENTICATE_ACCOUNTS
        accountManager.addAccountExplicitly(account, "password", Bundle())
    }
    /**
     * 设置账户自动同步
     */
    fun autoSync() {
        val account = Account("keeplive", ACCOUNT_TYPE)
        // 下面三个都需要同一个权限  WRITE_SYNC_SETTINGS
        // 设置同步
        ContentResolver.setIsSyncable(account, "com.peakerkeepliveapp.provider", 1)
        // 自动同步
        ContentResolver.setSyncAutomatically(account, "com.peakerkeepliveapp.provider", true)
        // 设置同步周期
        ContentResolver.addPeriodicSync(account, "com.peakerkeepliveapp.provider", Bundle(), 1)
    }
}
```

   ```xml
 <!--   让系统能够找到这个账户服务-->
        <service android:name=".account.AuthenticationService">
            <intent-filter>
                <action android:name="android.accounts.AccountAuthenticator" />
            </intent-filter>
            <meta-data
                android:name="android.accounts.AccountAuthenticator"
                android:resource="@xml/account_authenticator" />
        </service>
        <provider
            android:name=".account.SyncProvider"
            android:authorities="com.peakerkeepliveapp.provider" />

   ```

account_authenticator.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<account-authenticator xmlns:android="http://schemas.android.com/apk/res/android"
    android:accountType="com.peakerkeepliveapp.account"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name" />
    <!--android:label="Daemon"  不能这么写-->
```

sync_adapter.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<sync-adapter xmlns:android="http://schemas.android.com/apk/res/android"
    android:accountType="com.peakerkeepliveapp.account"
    android:allowParallelSyncs="false"
    android:contentAuthority="com.peakerkeepliveapp.provider"
    android:isAlwaysSyncable="true"
    android:userVisible="true" />
    <!-- allowParallelSyncs 是否支持多账号同时同步-->
    <!--contentAuthority 指定要同步的 ContentProvider-->
    <!--android:userVisible 显示开关按钮 给用户控制-->
```

AuthenticationService

```kotlin
/**
 * 创建 可添加的用户
 */
class AuthenticationService : Service() {
    lateinit var authenticator: Authenticator
    override fun onCreate() {
        super.onCreate()
        authenticator = Authenticator(this)
    }
    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
    class Authenticator(context: Context) : AbstractAccountAuthenticator(context) {
        override fun getAuthTokenLabel(authTokenType: String?): String? = null
        override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle? {
            return null
        }
        override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle? {
            return null
        }
        override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle? {
            return null
        }
        override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle? {
            return null
        }
        override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle? {
            return null
        }
        override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle? {
            return null
        }
    }
```

SyncProvider

```kotlin
class SyncProvider : ContentProvider() {
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

    override fun onCreate(): Boolean = false

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String? = null
}
```

SyncService

```kotlin
class SyncService : Service() {
    lateinit var syncAdapter:SyncAdapter
    companion object{
        private  val TAG = "SyncService"
    }
    override fun onCreate() {
        super.onCreate()
        syncAdapter= SyncAdapter(applicationContext,true)
    }
    override fun onBind(intent: Intent?): IBinder? {
       return syncAdapter.syncAdapterBinder
    }
    class SyncAdapter(context: Context?, autoInitialize: Boolean) : AbstractThreadedSyncAdapter(context, autoInitialize) {
        override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
            Log.e(TAG, "同步账户")
            //与互联网 或者 本地数库同据步账户
        }
    }
}
```



`注意  涉及到`

 `android:accountType="com.peakerkeepliveapp.account"`
    `android:contentAuthority="com.peakerkeepliveapp.provider"`

`所有的地方必须保持一致`

### JobScheduler  拉活

JobScheduler允许在特定状态与特定时间间隔周期执行任务。可以利用它的这个特点完成保活的功能,效果即开启一个定时器，与普通定时器不同的是其调度由系统完成。
	同样在某些ROM可能并不能达到需要的效果

调用 startJob方法

```kotlin
@SuppressLint("NewApi")
class KeepJobService : JobService() {

    private val TAG = "KeepJobService"
    override fun onStopJob(params: JobParameters?): Boolean = false

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.e(TAG, "onStartJob")
        // 如果7.0以上 轮询
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startJob(this)
        }
        return false
    }
    companion object {
        fun startJob(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            //setPersisted 在设备重启依然执行
            // 需要增加权限 RECEIVE_BOOT_COMPLETED
            val builder = JobInfo.Builder(8, ComponentName(context.packageName,
                    KeepJobService::class.java.name)).setPersisted(true)

            // 小于7.0
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // 每隔 1s 执行一次 job
                // 版本23 开始 进行了改进，最小周期为 5s
                builder.setPeriodic(1000)
            } else {
                // 延迟执行任务
                builder.setMinimumLatency(1000)
            }
            jobScheduler.schedule(builder.build())
        }
    }
}
```

```xml
<service
            android:name=".service.KeepJobService"
            android:permission="android.permission.BIND_JOB_SERVICE" />
```

### 双进程服务保活

就是写两个不在一个进程的服务相互调用

```kotlin
class RemoteService : Service() {
    companion object {
        private var SERVICE_ID = 112
    }

    lateinit var mBinder: MyBinder
    lateinit var serviceConnection: MServiceConnection

    class MyBinder : IMyAidlInterface.Stub() {
        override fun basicTypes(anInt: Int, aLong: Long, aBoolean: Boolean, aFloat: Float, aDouble: Double, aString: String?) {

        }

    }

    override fun onBind(intent: Intent?): IBinder? = mBinder

    override fun onCreate() {
        super.onCreate()
        mBinder = MyBinder()
        serviceConnection = MServiceConnection(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) { //4.3以下
            //将service设置成前台服务，并且不显示通知栏消息
            startForeground(SERVICE_ID, Notification())
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { //Android4.3-->Android7.0
            //将service设置成前台服务
            startForeground(SERVICE_ID, Notification())
            //删除通知栏消息
            startService(Intent(this, InnerService::class.java))
        } else { // 8.0 及以上
            //通知栏消息需要设置channel
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //NotificationManager.IMPORTANCE_MIN 通知栏消息的重要级别  最低，不让弹出
            //IMPORTANCE_MIN 前台时，在阴影区能看到，后台时 阴影区不消失，增加显示 IMPORTANCE_NONE时 一样的提示
            //IMPORTANCE_NONE app在前台没有通知显示，后台时有
            val channel = NotificationChannel("channel", "xx", NotificationManager.IMPORTANCE_NONE)
            manager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(this, "channel").build()
            //将service设置成前台服务，8.x退到后台会显示通知栏消息，9.0会立刻显示通知栏消息
            startForeground(SERVICE_ID, notification)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        bindService(Intent(this, OneLocalService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        return super.onStartCommand(intent, flags, startId)
    }
    class MServiceConnection(val c: Context) : ServiceConnection {
        //服务连接断开
        override fun onServiceDisconnected(name: ComponentName?) {

            c.startService(Intent(c, OneLocalService::class.java))
            c.bindService(Intent(c, OneLocalService::class.java),
                    this, Context.BIND_AUTO_CREATE)
        }
        //服务连接连接
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        }
    }

    /**
     * 8.0之情可以取消通知
     */
    class InnerService : Service() {
        override fun onCreate() {
            super.onCreate()
            // 让服务变成前台服务
            startForeground(SERVICE_ID, Notification())
            // 关闭自己
            stopSelf()
        }
        @Nullable
        override fun onBind(intent: Intent): IBinder? {
            return null
        }
    }
}
```

`补充`

上面用到的两个方法

```kotlin
object Utils {

    /**
     * 判断服务是否运行
     */
    fun isRunningService(context: Context, name: String?): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = am.getRunningServices(100)
        for (info in runningServices) {
            if (TextUtils.equals(info.service.className, name)) {
                return true
            }
        }
        return false
    }
    /**
     * 判断进程是否存活
     */
    fun isRunningProcess(manager: ActivityManager, packageName: String): Boolean {
        val runnings: List<ActivityManager.RunningAppProcessInfo> = manager.runningAppProcesses

        for (info in runnings) {
            if (TextUtils.equals(info.processName, packageName)) {
                return true
            }
        }
        return false

    }
}
```


