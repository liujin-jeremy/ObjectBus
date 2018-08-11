
## 简介

该库用于
* 执行异步任务
	* 提供回调监听
		* 任务线程回调
		* 主线程回调
* 任意两个类之间通信
* ObjectBus将以上功能串联.减少逻辑的割裂感

> app 有更多完整示例

## 引入

project.gradle

```
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
```

app.gradle

```
dependencies {
	implementation 'com.github.threekilogram:ObjectBus:1.5.7'
}
```

## 异步任务

该类可以在不同的线程上串行执行任务

#### 执行后台任务

```
ObjectBus bus = new ObjectBus();

bus.toUnder(() -> {
    
	// 参数是一个 Runnable(封装需要在后台执行的操作) , 使用Lambda简化
	// do something In threadPool

}).run();
```

#### 执行后台任务,之后切换主线程

```
ObjectBus bus = new ObjectBus();

bus.toUnder(() -> {
   
	// do something In threadPool

}).toMain(() -> {
    
	// 后台任务执行完成之后,回到主线程继续执行后续任务

}).run();
```

#### 线程切换

```
ObjectBus bus = new ObjectBus();

bus.toUnder(() -> {
    
	// ThreadPool

}).toMain(() -> {
    
	// MainThread

})..toUnder(() -> {
    
	// ThreadPool

}).toMain(() -> {
    
	// MainThread

}).run();
```

#### go操作

>go操作用于简化编程,它不具备线程切换能力,它之前的操作位于哪个线程,他就在哪个线程继续执行

```
ObjectBus bus = new ObjectBus();
bus.go(() -> {
   
	// go 没有线程切换能力,在那个线程调用的 run(),就在哪个线程开始执行go里面的操作

}).toUnder(() -> {
   
	// 切换到后台

}).go(() -> {
    
	// 继续在后台执行

}).toMain(() -> {
    
	// 切换回主线程

}).run();
```

#### 线程间传递变量,在传递过程中对变量进行操作

>该操作主要用于向后续操作传递参数

```
ObjectBus bus = new ObjectBus();
bus.go(() -> {
    
	// do something then save result

    bus.take("key", XXX);

}).toUnder(() -> {
    

    XXX result = (XXX) bus.get("key");
   
	// do something with params from last operation , then save it, because after operation need it

    bus.take("key", XXX);

}).go(() -> {
   
    XXX result = (XXX) bus.get("key");
   
	// get Params from last operation , then do something with result, and save
	 
    bus.take("key", XXX);
   
}).toMain(() -> {
   
	XXX result = (XXX) bus.get("key");

	// finaly resume reult
   
}).run();
```

#### 控制一串任务的暂停/恢复

>主要用于在一串任务执行过程中,中间任务需要暂停,等待时机合适再继续执行

```
ObjectBus mBus = new ObjectBus();
```
```
mBus.go(new Runnable() {
    @Override
    public void run() {
        
		// do something, then need wait a signal to continue
		// 执行一些任务,之后的任务需要其他的东西,现在可能没有,需要等待

    }
}).takeRest()			-->  	使用该方法等待
        .go(new Runnable() {
            @Override
            public void run() {

				  // 执行后续的任务,在 mBus.stopRest() 调用之后执行

            }
        }).run();
```
```
mBus.stopRest();		--> 在时机合适时停止等待,继续后面的任务
```

#### 执行任务过程中,向外发送消息

>该方法主要用于监听执行进度; 或者执行完一个任务,触发另一个事件

```
mBus.go(new Runnable() {
   
	// do something
   
}).send(158, mBus, MainManager.getInstance())		--> 	发送一条消息给一个类
        .takeRest()		--> 	等待消息回应之后继续进行
        .go(new Runnable() {
           
			// 执行消息回应之后的操作

        }).run();
```

#### 并发执行一堆任务

```
// 将需要处理的任务添加到列表
List< Runnable > runnableList = new ArrayList<>();

runnableList.add(XXX);
runnableList.add(XXX);
runnableList.add(XXX);

bus.toUnder(runnableList)	--> 后台并发执行
        .go(new Runnable() {
           
			//此处执行执行完任务列表之后的操作

        }).run();

```

#### 更多方法参考示例

* callable 执行,并保存结果
* callable列表并发执行,并保存结果列表
* 监听单个任务执行情况
* ObjectBus执行过程中通信

## 通信

>该类主要用于类在不同线程之间通信

#### 发送消息

>可以发送消息给任何实现了 `OnMessageReceiveListener` 接口的类

```
class MessengerReceiver implements OnMessageReceiveListener{

	@Override
	public void onReceive(int what) {
	   
		//该方法在 Messengers 发送消息的时候回调
		//what 代表是哪条消息

	}

	@Override
	public void onReceive(int what, Object extra) {
		
		//该方法在 Messengers 发送带附件的消息的时候回调
		//what 代表是哪条消息
		//extra 代表附带的附件
	    
	}
}
```
```
mMessengerReceiver = new MessengerReceiver(...);

// 使用 Messengers 可以发送一条消息给接收者

Messengers.send(1, mMessengerReceiver);
Messengers.send(2, mMessengerReceiver);

// 以上 1/2 代表消息标识,数字不同消息不同, 同时还决定了接收者在哪个线程接收到消息,如果是奇数则在主线程接收,如果是偶数则在 Messengers 线程接收
```

#### 发送延迟消息

```
Messengers.send(3, 2000, mMessengerReceiver);	//3 是消息标识,并且表明在主线程接收消息, 2000 是延时时间(毫秒), mMessengerReceiver 是接收者
Messengers.send(4, 2000, mMessengerReceiver);	//4 是消息标识,并且表明在Messengers线程接收消息, 2000 是延时时间(毫秒), mMessengerReceiver 是接收者
```

#### 发送带附件的消息

```
" hello "是消息附带的附件,该附件可以是任何类

Messengers.send(5, " hello ", mMessengerReceiver);
Messengers.send(6, " hello ", mMessengerReceiver);

Messengers.send(7, 2000, " hello ", mMessengerReceiver);
Messengers.send(8, 2000, " hello ", mMessengerReceiver);

```
>如果需要发送多个附件,请封装成一个类发送

#### 取消消息发送

```
//发送一条延时消息给 mMessengerReceiver
Messengers.send(9, 2000, " hello mainManager ", mMessengerReceiver);
```
```
//取消 mMessengerReceiver 的标识 9 的消息的发送 
Messengers.remove(9, mMessengerReceiver);	--> 9 表示取消哪条消息,  mMessengerReceiver表示接收9消息的那个接收者, 
												一条消息需要消息标识和消息接收方才能唯一确定
```